package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;

/**
 * @description 师资管理
 * @author JIU-W
 * @date 2024-11-15
 * @version 1.0
 */
public interface CourseTeacherService {

    /**
     * 根据课程id查询课程教师信息
     * @param courseId
     * @return
     */
    CourseTeacher selectCourseTeacherByCourseId(Long courseId);

    /**
     * 新增课程教师信息
     * @param addCourseTeacherDto
     * @return
     */
    CourseTeacher addCourseTeacher(Long companyId, AddCourseTeacherDto addCourseTeacherDto);

    /**
     * 修改课程教师信息
     * @param courseTeacher
     * @return
     */
    CourseTeacher updateCourseTeacher(CourseTeacher courseTeacher);

    /**
     * 删除课程教师信息
     * @param companyId
     * @param courseId
     * @param id
     */
    void deleteCourseTeacherByCourseIdAndId(Long companyId, Long courseId, Long id);

}
