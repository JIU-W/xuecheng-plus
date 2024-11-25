package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件处理业务方法
 * @author JIU-W
 * @date 2024-11-25
 * @version 1.0
 */
public interface MediaFileProcessService {

    /**
     * @param shardIndex 分片序号
     * @param shardTotal 分片总数
     * @param count      获取记录数
     * @return java.util.List<com.xuecheng.media.model.po.MediaProcess>
     * @description 获取待处理任务
     * @author JIU-W
     */
    List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);


}
