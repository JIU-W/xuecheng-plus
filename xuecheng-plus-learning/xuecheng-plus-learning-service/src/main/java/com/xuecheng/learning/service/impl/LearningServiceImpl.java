package com.xuecheng.learning.service.impl;


import com.alibaba.fastjson.JSON;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xuecheng.learning.service.LearningService;

@Slf4j
@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Autowired
    private MyCourseTablesService myCourseTablesService;


    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {

        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            RestResponse.validfail("课程不存在");
        }
        //todo:1.判断是否可以试学
        String teachplan = coursepublish.getTeachplan();
        TeachplanDto teachplanDto = JSON.parseObject(teachplan, TeachplanDto.class);
        if(teachplanDto.getIsPreview() == "1"){
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }


        //2.判断用户是否登录
        if (StringUtils.isNotEmpty(userId)) {//用户已登录
            //获取学习资格(判断是否选课)
            XcCourseTablesDto learningStatus = myCourseTablesService.getLearningStatus(userId, courseId);
            //学习资格，[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},
            //{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = learningStatus.getLearnStatus();
            if (learnStatus.equals("702001")) {
                //远程调用媒资管理服务，获取视频播放地址
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            } else if (learnStatus.equals("702002")) {
                return RestResponse.validfail("无法观看，由于没有选课或选课后没有支付");
            } else if (learnStatus.equals("702003")) {
                return RestResponse.validfail("您的选课已过期需要申请续期或重新支付");
            }
        }

        //未登录判断是否收费
        String charge = coursepublish.getCharge();
        if (charge.equals("201000")) {
            //免费可以正常学习
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }

        return RestResponse.validfail("请购买课程后继续学习");
    }


}
