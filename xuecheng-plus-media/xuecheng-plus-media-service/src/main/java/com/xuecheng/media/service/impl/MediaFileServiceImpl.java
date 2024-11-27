package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author JIU-W
 * @version 1.0
 * @description 媒资文件管理业务实现类
 * @date 2024-11-19
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MinioClient minioClient;

    //普通文件桶
    @Value("${minio.bucket.files}")
    private String bucket_Files;

    //存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_videoFiles;

    @Autowired
    MediaFileService currentProxy;

    @Autowired
    private MediaProcessMapper mediaProcessMapper;


    /**
     * 根据条件查询文件
     *
     * @param companyId
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //根据 文件扩展名 获取 mimeType
    private String getMimeType(String extension) {
        if (extension == null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * 将上传文件到minio
     *
     * @param localFilePath
     * @param mimeType
     * @param bucket
     * @param objectName
     * @return
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(testbucket);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XueChengPlusException.cast("上传文件到文件系统失败");
        }
        return false;
    }

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket              桶
     * @param objectName          对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @description 将文件信息添加到文件表
     * @author JIU-W
     * @date 2022/10/12 21:22
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto,
                                        String bucket, String objectName) {
        //从数据库查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //拷贝基本信息
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setStatus("1");
            mediaFiles.setAuditStatus("002003");
            //保存文件信息到文件表
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
            }

            //这个方法加入了事务控制
            //"记录待处理任务"，因为这个也要操作数据库表并且要保证一定是“上传视频文件成功后”才“记录待处理任务”，
            //所以要加入事务控制
            addWaitingTask(mediaFiles);

            log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
        }
        return mediaFiles;
    }

    /**
     * 添加待处理任务(添加到待处理任务表)
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles) {
        //文件名称
        String filename = mediaFiles.getFilename();
        //文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //获取文件mimeType
        String mimeType = getMimeType(extension);
        //如果是avi视频添加到视频待处理表
        if (mimeType.equals("video/x-msvideo")) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            //状态
            mediaProcess.setStatus("1");//未处理
            //创建时间(有自动填充策略)
            //mediaProcess.setCreateDate(LocalDateTime.now());
            //失败次数
            mediaProcess.setFailCount(0);//失败次数默认为0
            mediaProcess.setUrl(null);
            mediaProcessMapper.insert(mediaProcess);
        }
        //这里只考虑了avi类型的，如果还有其它非MP4类型的视频，那就把所有的这些类型
        //写成一个集合或者枚举或者直接配置化，然后判断 mimeType 属不属于这个集合，然后统一处理即可。
        //统一处理的逻辑就是如上逻辑。

        //但如果上传的是MP4文件，则不需要加入待处理任务表。
    }


    /**
     * 上传普通文件
     *
     * @param companyId
     * @param uploadFileParamsDto
     * @param localFilePath
     * @return
     */
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto,
                                          String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengPlusException.cast("文件不存在");
        }
        //文件名称
        String filename = uploadFileParamsDto.getFilename();
        //文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));

        //获取文件mimeType
        String mimeType = getMimeType(extension);

        //获取文件的md5值
        String fileMd5 = getFileMd5(file);

        //文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();

        //存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + fileMd5 + extension;

        //将文件上传到minio
        boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_Files, objectName);
        if (!result) {
            XueChengPlusException.cast("上传文件到文件系统失败");
        }
        //将文件信息保存到数据库
        //文件大小
        uploadFileParamsDto.setFileSize(file.length());
        //将文件信息存储到数据库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5,
                uploadFileParamsDto, bucket_Files, objectName);
        if (mediaFiles == null) {
            XueChengPlusException.cast("文件上传后保存文件信息失败");
        }
        //准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }


    /**
     * 检查文件是否存在(文件存在的情况是所有的分块都上传上来了)
     *
     * @param fileMd5 文件的md5
     * @return
     */
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //查询文件信息
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //如果文件信息为空，表示数据库没有此记录，则表示数据库不存在此分块文件，则说明要文件不存在。
        //如果文件信息不为空，说明数据库存在此分块文件，则继续判断minio中分块文件是否存在。
        if (mediaFiles != null) {
            //桶
            String bucket = mediaFiles.getBucket();
            //存储目录
            String filePath = mediaFiles.getFilePath();
            //文件流
            InputStream stream = null;
            try {
                stream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(filePath)
                        .build());
                if (stream != null) {
                    //文件已存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                //报异常就说明文件不存在，不用抛出直接捕获即可。
                e.printStackTrace();
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }


    /**
     * 检查分块是否存在
     *
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return
     */
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        //根据md5得到分块文件目录
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunkIndex;

        //文件流
        InputStream fileInputStream = null;
        try {
            fileInputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket_videoFiles)
                            .object(chunkFilePath)
                            .build());

            if (fileInputStream != null) {
                //分块已存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //分块未存在
        return RestResponse.success(false);
    }

    //得到分块文件的目录
    //分块存储的路径是：md5前两位为两个目录，chunk存储分块文件
    //1/2/fileMd5/chunk/
    //这样设置“存储目录规则”主要是尽量让目录呈树型状，可以加快对文件的查找。
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }


    /**
     * 上传分块(将本地的分块上传到minio)
     *
     * @param fileMd5            文件md5
     * @param chunk              分块序号
     * @param localChunkFilePath 分块文件本地路径
     * @return
     */
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {

        //得到分块文件的目录路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunk;
        //获取mimeType
        String mimeType = getMimeType(null);

        //将文件存储至minIO
        boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_videoFiles, chunkFilePath);
        if (!b) {
            log.debug("上传分块文件失败:{}", chunkFilePath);
            return RestResponse.validfail(false, "上传分块失败");
        }
        log.debug("上传分块文件成功:{}", chunkFilePath);
        return RestResponse.success(true);
    }

    /**
     * 合并分块
     *
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息
     * @return
     */
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal,
                                    UploadFileParamsDto uploadFileParamsDto) {

        //=====获取分块文件路径=====
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //组成将分块文件路径组成 List<ComposeSource>
        List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_videoFiles)
                        .object(chunkFileFolderPath.concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());


        //=====在minio合并分块文件=====
        //文件名称
        String fileName = uploadFileParamsDto.getFilename();
        //文件扩展名
        String extName = fileName.substring(fileName.lastIndexOf("."));
        //合并文件路径
        String mergeFilePath = getFilePathByMd5(fileMd5, extName);
        try {
            //合并文件
            ObjectWriteResponse response = minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket_videoFiles)
                            .object(mergeFilePath)
                            .sources(sourceObjectList)
                            .build());
            log.debug("合并文件成功:{}", mergeFilePath);
        } catch (Exception e) {
            log.debug("合并文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            return RestResponse.validfail(false, "合并文件失败。");
        }

        //====验证md5====
        //从minio下载合并后的文件和之前上传上去的原始文件进行md5校验
        //下载合并后的文件
        File minioFile = downloadFileFromMinIO(bucket_videoFiles, mergeFilePath);
        if (minioFile == null) {
            log.debug("下载合并后文件失败,mergeFilePath:{}", mergeFilePath);
            return RestResponse.validfail(false, "下载合并后文件失败。");
        }

        try (InputStream newFileInputStream = new FileInputStream(minioFile)) {
            //minio上文件的md5值
            String md5Hex = DigestUtils.md5Hex(newFileInputStream);
            //比较md5值，不一致则说明文件不完整
            if (!fileMd5.equals(md5Hex)) {
                return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
            }
            //设置文件大小
            uploadFileParamsDto.setFileSize(minioFile.length());
        } catch (Exception e) {
            log.debug("校验文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage(), e);
            return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
        } finally {
            if (minioFile != null) {
                minioFile.delete();
            }
        }

        //=====文件入库=====
        //使用代理对象执行方法，使得事务生效。
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5,
                uploadFileParamsDto, bucket_videoFiles, mergeFilePath);
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "向数据库保存文件信息失败。");
        }

        //=====清除分块文件=====
        clearChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true);
    }

    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 得到合并后的文件的地址
     *
     * @param fileMd5 文件id即md5值
     * @param fileExt 文件扩展名
     * @return
     */
    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 清除分块文件
     *
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                    .bucket("video")
                    .objects(deleteObjects)
                    .build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            //要想真正删除，遍历删除结果
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清除分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清除分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }


}
