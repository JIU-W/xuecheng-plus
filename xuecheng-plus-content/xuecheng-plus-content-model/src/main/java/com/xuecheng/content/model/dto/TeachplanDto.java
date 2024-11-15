package com.xuecheng.content.model.dto;


import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程计划树型结构dto
 * @date 2024-11-14
 */
@Data
@ToString
public class TeachplanDto extends Teachplan {

    //课程计划关联的媒资信息
    private TeachplanMedia teachplanMedia;

    //子结点
    private List<TeachplanDto> teachPlanTreeNodes;

}
