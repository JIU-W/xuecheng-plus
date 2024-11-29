package com.xuecheng.content.feignclient;

import com.xuecheng.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资管理服务远程接口
 * @date 2022/9/20 20:29
 */
@FeignClient(value = "media-api", configuration = MultipartSupportConfig.class)
public interface MediaServiceClient {

    @RequestMapping(value = "/media/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadFile(@RequestPart("filedata") MultipartFile upload,
                      @RequestParam(value = "objectName", required = false) String objectName);

}
