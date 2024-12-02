package com.xuecheng.search.controller;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.search.dto.SearchCourseParamDto;
import com.xuecheng.search.dto.SearchPageResultDto;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.CourseSearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程搜索接口
 * @date 2024-12-01
 */
@Api(value = "课程搜索接口", tags = "课程搜索接口")
@RestController
@RequestMapping("/course")
public class CourseSearchController {

    @Autowired
    CourseSearchService courseSearchService;

    //复合
    @ApiOperation("课程搜索列表")
    @GetMapping("/list")
    public SearchPageResultDto<CourseIndex> list(PageParams pageParams,
                                                 SearchCourseParamDto searchCourseParamDto) {

        return courseSearchService.queryCoursePubIndex(pageParams, searchCourseParamDto);

    }
}
