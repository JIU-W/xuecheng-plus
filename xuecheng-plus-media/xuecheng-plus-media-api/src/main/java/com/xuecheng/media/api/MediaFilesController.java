package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author JIU-W
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2024-11-19
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);
    }

    @ApiOperation("上传文件")
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile fileData) throws IOException {
        //fileData是一个临时文件，需要转存到指定位置(目标位置是minio文件系统)，否则本次请求完成后临时文件会删除。
        //因为获取不到fileData的路径，所以先把他手动转存到另一个临时文件tempFile中，而此时tempFile的路径
        //就是参数本地文件路径(absolutePath)从而传到service层用以将文件上传到minio
        Long companyId = 1232141425L;
        //准备上传文件的信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        //原始文件名称
        uploadFileParamsDto.setFilename(fileData.getOriginalFilename());
        //文件类型为图片
        uploadFileParamsDto.setFileType("001001");
        //文件大小
        long fileSize = fileData.getSize();
        uploadFileParamsDto.setFileSize(fileSize);

        //创建临时文件(在本地创建临时文件)(项目上线后就是在服务器上创建临时文件)
        File tempFile = File.createTempFile("minio", "temp");
        //上传的文件拷贝到临时文件
        fileData.transferTo(tempFile);
        //文件路径
        String absolutePath = tempFile.getAbsolutePath();

        //上传文件
        UploadFileResultDto uploadFileResultDto = mediaFileService.uploadFile(companyId,
                uploadFileParamsDto, absolutePath);

        return uploadFileResultDto;
    }


}
