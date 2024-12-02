package com.xuecheng.search.dto;

import lombok.Data;
import lombok.ToString;

/**
 * @author JIU-W
 * @version 1.0
 * @description 搜索课程参数dto(前端传过来的)
 * @date 2024-12-01
 */
@Data
@ToString
public class SearchCourseParamDto {

    //关键字
    private String keywords;

    //大分类
    private String mt;

    //小分类
    private String st;

    //难度等级
    private String grade;


}
