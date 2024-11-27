package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author JIU-W
 * @version 1.0
 * @description TODO
 * @date 2022/10/15 11:58
 */
@Slf4j
@Component
public class VideoTask {

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MediaFileProcessService mediaFileProcessService;

    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {

        //分片参数:
        //分片序号(也就是执行器的序号，从0开始)
        int shardIndex = XxlJobHelper.getShardIndex();
        //分片总数(也就是执行器总数)(也就是集群中执行器数量)
        int shardTotal = XxlJobHelper.getShardTotal();

        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        try {
            //取出电脑CPU核心数作为一次处理数据的条数
            int processors = Runtime.getRuntime().availableProcessors();
            //======查询待处理视频任务======
            //一次处理视频数量不要超过cpu核心数
            mediaProcessList = mediaFileProcessService
                    .getMediaProcessList(shardIndex, shardTotal, processors);
            size = mediaProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (size <= 0) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //启动size个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //线程池计数器：
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池()
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                try {
                    //任务id
                    Long taskId = mediaProcess.getId();
                    //======开启任务======
                    //抢占任务
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        return;
                    }
                    log.debug("开始执行任务:{}", mediaProcess);
                    //下边是处理逻辑
                    //桶
                    String bucket = mediaProcess.getBucket();
                    //存储路径
                    String filePath = mediaProcess.getFilePath();
                    //原始视频的md5值
                    String fileId = mediaProcess.getFileId();
                    //原始文件名称
                    String filename = mediaProcess.getFilename();
                    //将要处理的文件下载到本地(项目进行部署了的话就相当于下载到服务器上)
                    File originalFile = mediaFileService.downloadFileFromMinIO(mediaProcess.getBucket(),
                                                                mediaProcess.getFilePath());
                    if (originalFile == null) {
                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3",
                                fileId, null, "下载待处理文件失败");
                        return;
                    }

                    //处理下载的视频文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建mp4临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3",
                                fileId, null, "创建mp4临时文件失败");
                        return;
                    }

                    //======处理视频(执行视频转码)(avi ---> mp4)======
                    //视频处理结果
                    String result = "";

                    //源avi视频的路径
                    String video_path = originalFile.getAbsolutePath();
                    //转换后mp4文件的名称
                    String mp4name = mp4File.getName();
                    //转换后mp4文件的路径
                    String mp4_path = mp4File.getAbsolutePath();
                    try {
                        //开始处理视频   (avi ---> mp4)
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4name, mp4_path);
                        //开始视频转换，成功将返回success
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    }
                    if (!result.equals("success")) {
                        //记录错误信息
                        log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3",
                                fileId, null, result);
                        return;
                    }

                    //======将mp4上传至minio======
                    //mp4在minio的存储路径
                    String objectName = getFilePath(fileId, ".mp4");
                    //访问url
                    String url = "/" + bucket + "/" + objectName;
                    try {
                        //将mp4上传至minio
                        mediaFileService.addMediaFilesToMinIO(mp4_path, "video/mp4",
                                                                        bucket, objectName);

                        //将url存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
                        //======保存任务处理结果======
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2",
                                fileId, url, null);
                    } catch (Exception e) {
                        log.error("上传视频失败或入库失败,视频地址:{},错误信息:{}", bucket + objectName,
                                e.getMessage());
                        //最终还是失败了
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3",
                                fileId, null, "处理后视频上传或入库失败");
                    }
                } finally {
                    //只要开始执行了上面try{。。。}里面的代码，不管是整个执行成功了还是return走了，都会来这里减一
                    //线程结束，计数器减一
                    countDownLatch.countDown();
                }
            });
        });

        //阻塞(等待)(执行完任务的线程在这里阻塞，等待未执行完的线程，直到所有线程都完成了任务)
        //正常情况是countDownLatch减到0就解除阻塞。
        //但是还要做一个保底策略:(给一个充裕的超时时间，防止无限等待，到达超时时间还没有处理完成则结束任务、解除阻塞)
        //只有解除阻塞了线程才会释放，才会有后面的调度接着来调度任务。
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    //获取文件路径
    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

}
