package com.xuecheng.search.controller;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.search.po.CourseIndex;
import com.xuecheng.search.service.IndexService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程索引接口
 * @date 2024-12-01
 */
@Api(value = "课程信息索引接口", tags = "课程信息索引接口")
@RestController
@RequestMapping("/index")
public class CourseIndexController {

    @Value("${elasticsearch.course.index}")
    private String courseIndexStore;

    @Autowired
    private IndexService indexService;


    /**
     * 给course_public索引库(课程发布索引库)添加一条文档数据(添加索引)
     *      这里的“添加索引”功能的索引不是指索引库，
     *      而是泛指那些文档数据经过分词(有些数据不用分词也会建立索引)后建立起的索引，这些索引用于加快查询速度。
     *      总结就是：添加文档就是创建倒排索引的过程
     * @param courseIndex
     * @return
     */
    @ApiOperation("添加课程索引")
    @PostMapping("course")
    public Boolean add(@RequestBody CourseIndex courseIndex) {

        Long id = courseIndex.getId();
        if (id == null) {
            XueChengPlusException.cast("课程id为空");
        }
        Boolean result = indexService.addCourseIndex(courseIndexStore, String.valueOf(id), courseIndex);
        if (!result) {
            XueChengPlusException.cast("添加课程索引失败");
        }
        return result;

    }
}
