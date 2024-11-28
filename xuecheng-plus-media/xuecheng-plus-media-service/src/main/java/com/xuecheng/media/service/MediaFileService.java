package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author JIU-W
 * @date 2024-11-23
 * @version 1.0
 */
public interface MediaFileService {

    /**
     * 媒资文件查询方法
     *
     * @param companyId
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return
     */
     PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     * @param companyId
     * @param uploadFileParamsDto
     * @param localFilePath
     * @return
     */
    UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);

    /**
     * @description 将文件信息添加到数据库文件表
     * @param companyId  机构id
     * @param fileMd5  文件md5值
     * @param uploadFileParamsDto  上传文件的信息
     * @param bucket  桶
     * @param objectName 对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @date 2022/10/12 21:22
     */
    MediaFiles addMediaFilesToDb(Long companyId, String fileMd5,
                                 UploadFileParamsDto uploadFileParamsDto,
                                 String bucket, String objectName);

    /**
     * @description 检查文件是否存在
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @author JIU-W
     * @date 2022/9/13 15:38
     */
    RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * @description 检查分块是否存在
     * @param fileMd5  文件的md5
     * @param chunkIndex  分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @author JIU-W
     * @date 2022/9/13 15:39
     */
    RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    /**
     * @description 上传分块
     * @param fileMd5  文件md5
     * @param chunk  分块序号
     * @param localChunkFilePath  分块文件本地路径
     * @return com.xuecheng.base.model.RestResponse
     * @author JIU-W
     * @date 2022/9/13 15:50
     */
    RestResponse uploadChunk(String fileMd5,int chunk,String localChunkFilePath);


    /**
     * @description 合并分块
     * @param companyId  机构id
     * @param fileMd5  文件md5
     * @param chunkTotal 分块总和
     * @param uploadFileParamsDto 文件信息
     * @return com.xuecheng.base.model.RestResponse
     * @author JIU-W
     * @date 2022/9/13 15:56
     */
    RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal,
                             UploadFileParamsDto uploadFileParamsDto);

    /**
     * 从minio下载文件
     * @param bucket
     * @param objectName
     * @return
     */
    File downloadFileFromMinIO(String bucket, String objectName);

    /**
     * 上传文件到minio
     * @param localFilePath
     * @param mimeType
     * @param bucket
     * @param objectName
     * @return
     */
    boolean addMediaFilesToMinIO(String localFilePath, String mimeType,
                                        String bucket, String objectName);

    /**
     * 根据媒体id查询文件信息
     * @param mediaId
     * @return
     */
    MediaFiles getFileById(String mediaId);

}
