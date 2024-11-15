package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "AddCourseTeacherDto", description = "添加课程教师信息")
public class AddCourseTeacherDto implements Serializable {

    /**
     * 课程标识
     */
    private Long courseId;

    /**
     * 教师标识
     */
    @NotEmpty(message = "教师名称不能为空")
    private String teacherName;

    /**
     * 教师职位
     */
    @NotEmpty(message = "教师职位不能为空")
    private String position;

    /**
     * 教师简介
     */
    @NotEmpty(message = "教师简介不能为空")
    private String introduction;

}
