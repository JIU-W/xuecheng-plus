package com.xuecheng.content;

import com.xuecheng.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
 * @author JIU-W
 * @version 1.0
 * @description 测试使用feign远程上传文件
 * @date 2022/9/20 20:36
 */
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    private MediaServiceClient mediaServiceClient;

    //远程调用，上传文件
    @Test
    public void test() {

        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(
                new File("D:\\音频视频\\test.html"));

        mediaServiceClient.uploadFile(multipartFile,"course/test.html");
    }

}
