package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程计划service接口实现类
 * @date 2022/9/9 11:14
 */
@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Resource
    private TeachplanMapper teachplanMapper;

    /**
     * 查询课程计划(树形结构)
     * @param courseId
     * @return
     */
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    /**
     * 新增或修改课程计划
     * @param teachplanDto
     */
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        Long id = teachplanDto.getId();
        if(id == null){
            //新增
            //取出同父同级别的课程计划数量
            Teachplan teachplan = new Teachplan();
            //设置排序号
            //这里根据数量来确定排位不太合理，如果删除后再添加则orderBy会重复
            //int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            //应该根据 存在的数据的最大排序号 来确定排序号
            int maxCount = getOrderByMax(teachplanDto.getCourseId(), teachplanDto.getParentid());
            teachplan.setOrderby(maxCount + 1);
            BeanUtils.copyProperties(teachplanDto, teachplan);
            teachplanMapper.insert(teachplan);
        }else{
            //修改
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    /**
     * 删除课程计划
     * @param id
     */
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        if(teachplan == null){
            throw new XueChengPlusException("课程计划不存在");
        }
        Long parentid = teachplan.getParentid();
        if(parentid == 0){
            LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Teachplan::getParentid,id);
            List<Teachplan> tps = teachplanMapper.selectList(queryWrapper);
            if(tps.size() == 0){
                //删除大章节
                int i = teachplanMapper.deleteById(id);
                if(i <= 0){
                    throw new XueChengPlusException("删除大章节失败");
                }
            }else {
                throw new XueChengPlusException("本大章节下有小章节，不允许删除");
            }
        }else{
            //删除小节
            int ii = teachplanMapper.deleteById(id);
            if(ii <= 0){
                throw new XueChengPlusException("删除小章节失败");
            }
            //同时删除小节 关联的媒资文件
            teachplanMapper.delectTeachplanMedia(id);
        }
    }

    /**
     * 获取课程计划最大排序号
     * @param courseId
     * @param parentid
     * @return
     */
    private int getOrderByMax(Long courseId, Long parentid) {
        Integer maxCount = teachplanMapper.selectMaxOrderBy(courseId, parentid);
        if(maxCount == null){
            return 0;
        }
        return maxCount;
    }

    /**
     * 获取课程计划数量
     * @param courseId
     * @param parentid
     * @return
     */
    private int getTeachplanCount(Long courseId, Long parentid) {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId);
        queryWrapper.eq(Teachplan::getParentid, parentid);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }

}
