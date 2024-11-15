package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 课程-教师关系表 前端控制器
 * </p>
 *
 * @author itcast
 */
@Slf4j
@RestController
@RequestMapping("courseTeacher")
public class CourseTeacherController {

    @Autowired
    private CourseTeacherService courseTeacherService;


    /**
     * 根据课程id查询课程教师信息
     * @param courseId
     * @return
     */
    @ApiOperation("根据课程id查询课程教师信息")
    @GetMapping("/list/{courseId}")
    public CourseTeacher selectCourseTeacherById(@PathVariable Long courseId){
        return courseTeacherService.selectCourseTeacherByCourseId(courseId);
    }

    /**
     * 新增课程教师信息
     * @param addCourseTeacherDto
     * @return
     */
    @ApiOperation("新增课程教师信息")
    @PostMapping()
    public CourseTeacher addCourseTeacher(@RequestBody AddCourseTeacherDto addCourseTeacherDto){
        Long companyId = 1232141425L;
        CourseTeacher courseTeacher = courseTeacherService.addCourseTeacher(companyId, addCourseTeacherDto);
        return courseTeacher;
    }

    /**
     * 修改课程教师信息
     * @param courseTeacher
     * @return
     */
    @ApiOperation("修改课程教师信息")
    @PutMapping()
    public CourseTeacher updateCourseTeacher(@RequestBody CourseTeacher courseTeacher){
        //Long companyId = 1232141425L;
        CourseTeacher courseTeacherNew = courseTeacherService.updateCourseTeacher(courseTeacher);
        return courseTeacherNew;
    }

    /**
     * 根据课程id和id删除课程教师信息
     * @param courseId
     * @param id
     */
    @ApiOperation("根据课程id和id删除课程教师信息")
    @DeleteMapping("/course/{courseId}/{id}")
    public void deleteCourseTeacherByCourseIdAndId(@PathVariable Long courseId, @PathVariable Long id){
        Long companyId = 1232141425L;
        courseTeacherService.deleteCourseTeacherByCourseIdAndId(companyId, courseId, id);
    }

}
