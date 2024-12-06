package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;

/**
 * @author JIU-W
 * @version 1.0
 * @description 我的课程表service接口
 * @date 2024-12-06
 */
public interface MyCourseTablesService {

    /**
     * @param userId   用户id
     * @param courseId 课程id
     * @return com.xuecheng.learning.model.dto.XcChooseCourseDto
     * @description 添加选课
     * @author Mr.M
     * @date 2022/10/24 17:33
     */
    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

}
