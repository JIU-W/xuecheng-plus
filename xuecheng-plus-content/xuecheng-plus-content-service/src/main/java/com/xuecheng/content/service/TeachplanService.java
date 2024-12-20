package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.*;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.TeachplanMedia;

import java.util.List;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程基本信息管理业务接口
 * @date 2024-11-14
 */
public interface TeachplanService {

    /**
     * 查询课程计划树型结构
     *
     * @param courseId
     * @return
     */
    List<TeachplanDto> findTeachplanTree(long courseId);


    /**
     * 保存课程计划
     * @param teachplanDto
     */
    void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * 删除课程计划
     * @param id
     */
    void deleteTeachplan(Long id);

    /**
     * 课程计划排序
     * @param moveType
     * @param id
     */
    void orderby(String moveType, Long id);

    /**
     * @description 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return com.xuecheng.content.model.po.TeachplanMedia
     */
    TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);

    /**
     * 解除教学计划与媒资的绑定
     * @param teachPlanId
     * @param mediaId
     */
    void deleteTeachplanMedia(Long teachPlanId, String mediaId);

}
