package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程预览、发布接口
 * @date 2024-11-27
 */
public interface CoursePublishService {


    /**
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto.CoursePreviewDto
     * @description 获取课程预览信息
     */
    CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * @description 提交审核
     * @param courseId  课程id
     * @return void
     */
    void commitAudit(Long companyId,Long courseId);

    /**
     * @description 课程发布接口
     * @param companyId 机构id
     * @param courseId 课程id
     * @return void
     */
    void publish(Long companyId,Long courseId);

}
