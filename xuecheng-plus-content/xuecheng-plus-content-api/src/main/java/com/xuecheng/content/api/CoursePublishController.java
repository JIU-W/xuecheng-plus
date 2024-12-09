package com.xuecheng.content.api;

import com.alibaba.fastjson.JSON;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;


/**
 * @author JIU-W
 * @version 1.0
 * @description 课程预览，提交课程审核，课程发布
 * @date 2024-11-27
 */
@Controller
public class CoursePublishController {

    @Autowired
    private CoursePublishService coursePublishService;

    @ApiOperation("课程预览")
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId) {

        //获取课程预览信息
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model", coursePreviewInfo);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }


    @ApiOperation("提交课程审核")
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.commitAudit(companyId,courseId);
    }

    @ApiOperation("课程发布")
    @ResponseBody
    @PostMapping ("/coursepublish/{courseId}")
    public void coursePublish(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.publish(companyId,courseId);
    }

    @ApiOperation("查询课程发布信息")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId) {
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        return coursePublish;
    }


    /**
     * 查询课程预览信息
     * @param courseId
     * @return
     */
    @ApiOperation("获取课程发布信息")
    @ResponseBody
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getCoursePublish(@PathVariable("courseId") Long courseId) {
        //查询课程发布信息
        CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
        if (coursePublish == null) {
            return new CoursePreviewDto();
        }

        //课程基本信息
        CourseBaseInfoDto courseBase = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish, courseBase);
        //课程计划
        List<TeachplanDto> teachplans = JSON.parseArray(coursePublish.getTeachplan(), TeachplanDto.class);
        CoursePreviewDto coursePreviewInfo = new CoursePreviewDto();
        coursePreviewInfo.setCourseBase(courseBase);
        coursePreviewInfo.setTeachplans(teachplans);
        return coursePreviewInfo;
    }



}
