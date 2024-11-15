package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Slf4j
@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Autowired
    private CourseBaseMapper courseBaseMapper;


    public CourseTeacher selectCourseTeacherByCourseId(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        if (courseTeachers.size() == 0) {
            throw new XueChengPlusException("此课程还未添加对应老师");
        }
        return courseTeachers.get(0);
    }

    @Transactional
    public CourseTeacher addCourseTeacher(Long companyId, AddCourseTeacherDto addCourseTeacherDto) {

        CourseBase courseBase = courseBaseMapper.selectById(addCourseTeacherDto.getCourseId());
        //校验本机构只能添加本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("本机构只能添加本机构的课程");
        }

        //因为course_id和teacher_name两个字段的组合是表course_teacher的唯一键，所以不能添加重复数据
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, addCourseTeacherDto.getCourseId());
        queryWrapper.eq(CourseTeacher::getTeacherName, addCourseTeacherDto.getTeacherName());
        CourseTeacher courseTeacher1 = courseTeacherMapper.selectOne(queryWrapper);
        if (courseTeacher1 != null) {
            throw new XueChengPlusException("此课程已添加过此老师");
        }
        //符合条件，可以新增
        CourseTeacher courseTeacher = new CourseTeacher();
        BeanUtils.copyProperties(addCourseTeacherDto, courseTeacher);
        int insert = courseTeacherMapper.insert(courseTeacher);
        if (insert <= 0) {
            throw new XueChengPlusException("添加课程教师信息失败");
        }
        return courseTeacher;
    }

    @Transactional
    public CourseTeacher updateCourseTeacher(CourseTeacher courseTeacher) {
        CourseTeacher courseTeacherNew = new CourseTeacher();
        BeanUtils.copyProperties(courseTeacher, courseTeacherNew);
        int i = courseTeacherMapper.updateById(courseTeacherNew);
        if (i <= 0){
            throw new XueChengPlusException("修改课程教师信息失败");
        }
        return courseTeacherNew;
    }

    @Transactional
    public void deleteCourseTeacherByCourseIdAndId(Long companyId, Long courseId, Long id) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //校验本机构只能添加本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("本机构只能添加本机构的课程");
        }

        CourseTeacher courseTeacher = courseTeacherMapper.selectById(id);
        if(courseTeacher == null){
            throw new XueChengPlusException("此课程没有此老师");
        }

        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getId, id);
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        int delete = courseTeacherMapper.delete(queryWrapper);
        if(delete <= 0){
            throw new XueChengPlusException("删除课程教师信息失败");
        }
    }


}
