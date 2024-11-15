package com.xuecheng.content.service;


import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * @description 课程基本信息管理业务接口
 * @author JIU-W
 * @date 2024-11-11
 * @version 1.0
 */
public interface CourseBaseInfoService {

    /**
     * 课程查询接口(分页)
     * @param pageParams
     * @param queryCourseParamsDto
     * @return
     */
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    /**
     * 添加课程基本信息和营销信息
     * @param companyId 教学机构id
     * @param addCourseDto 课程基本信息
     * @return
     */
    CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);


    /**
     * 根据id查询课程基本信息
     * @param courseId
     * @return
     */
    CourseBaseInfoDto getCourseBaseInfo(long courseId);

    /**
     * 修改课程信息
     * @param companyId 机构id
     * @param dto 课程信息
     * @return
     */
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto);

}
