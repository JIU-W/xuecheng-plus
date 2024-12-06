package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

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
     * @param courseId 课程id
     * @return void
     * @description 提交审核
     */
    void commitAudit(Long companyId, Long courseId);

    /**
     * @param companyId 机构id
     * @param courseId  课程id
     * @return void
     * @description 课程发布接口
     */
    void publish(Long companyId, Long courseId);

    /**
     * @param courseId 课程id
     * @return File 静态化文件
     * @description 课程静态化
     */
    File generateCourseHtml(Long courseId);

    /**
     * @param file 静态化文件
     * @return void
     * @description 上传课程静态化页面
     */
    void uploadCourseHtml(Long courseId, File file);

    /**
     * @param courseId
     * @return
     * @description 根据课程id查询课程发布信息
     */
    CoursePublish getCoursePublish(Long courseId);

}
