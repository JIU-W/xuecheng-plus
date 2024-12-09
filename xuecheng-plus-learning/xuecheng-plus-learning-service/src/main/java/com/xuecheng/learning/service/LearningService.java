package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;

/**
 * @author Mr.M
 * @version 1.0
 * @description 学习过程管理service接口
 * @date 2022/10/2 16:07
 */
public interface LearningService {

    /**
     * @param courseId    课程id
     * @param teachplanId 课程计划id
     * @param mediaId     视频文件id
     * @return com.xuecheng.base.model.RestResponse<java.lang.String>
     * @description 获取教学视频
     */
    RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);

}
