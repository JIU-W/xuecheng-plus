package com.xuecheng.learning;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

/**
 * @author JIU-W
 * @version 1.0
 * @description Feign接口测试类
 * @date 2023/2/22 20:14
 */
@SpringBootTest
public class FeignClientTest {

    @Autowired
    ContentServiceClient contentServiceClient;


    @Test
    public void testContentServiceClient() {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(18L);
        Assertions.assertNotNull(coursepublish);
    }

}
