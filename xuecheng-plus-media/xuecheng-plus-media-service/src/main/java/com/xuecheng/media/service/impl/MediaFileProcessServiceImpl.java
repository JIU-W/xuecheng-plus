package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/14 14:41
 */
@Slf4j
@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;


    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses =
                mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result <= 0 ? false : true;
    }

    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status,
                                        String fileId, String url, String errorMsg) {
        //查出任务，如果不存在则直接返回
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(mediaProcess == null){
            return ;
        }

        //处理失败，更新任务处理结果
        LambdaQueryWrapper<MediaProcess> queryWrapperById = new LambdaQueryWrapper<>();
        queryWrapperById.eq(MediaProcess::getId, taskId);

        //LambdaQueryWrapper<MediaProcess> queryWrapperById1 = new LambdaQueryWrapper<MediaProcess>()
        //        .eq(MediaProcess::getId, taskId);

        //处理失败
        if(status.equals("3")){
            MediaProcess mediaProcess_u = new MediaProcess();
            mediaProcess_u.setStatus("3");
            mediaProcess_u.setErrormsg(errorMsg);
            mediaProcess_u.setFailCount(mediaProcess.getFailCount() + 1);
            mediaProcessMapper.update(mediaProcess_u, queryWrapperById);
            log.debug("更新任务处理状态为失败，任务信息:{}",mediaProcess_u);
            return ;
        }

        //任务处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if(mediaFiles != null){
            //更新媒资文件中的访问url
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }
        //处理成功，更新url和状态
        mediaProcess.setUrl(url);
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcess);

        //添加到历史记录
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        //删除mediaProcess
        mediaProcessMapper.deleteById(mediaProcess.getId());

    }

}
