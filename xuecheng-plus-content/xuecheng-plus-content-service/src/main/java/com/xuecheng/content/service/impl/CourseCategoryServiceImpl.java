package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {


    @Autowired
    private CourseCategoryMapper courseCategoryMapper;


    /**
     * 课程分类树形结构查询
     * @param id
     * @return
     */
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {//id一定是1

        //只是查到整张表的数据，但是其数据的结构 不符合 返回给前端数据的结构。
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        //将list数据塞到map里,以备 后面查询 使用,排除根节点
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId()))   //排除根节点
                .collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));

        //最终返回的list
        List<CourseCategoryTreeDto> categoryTreeDtos = new ArrayList<>();

        //依次遍历每个元素,排除根节点
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId()))//因为根节点只是根节点，没有数据，所以不加入到结果list中
                .forEach(item->{
            if(item.getParentid().equals(id)){
                categoryTreeDtos.add(item);
            }
            //找到当前节点的父节点
            CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
            //如果父节点为空，说明是根节点(因为之前list转map的时候排除了根节点)，不用做任何处理。
            //如果父节点不为空，说明有父节点。把当前元素加到父节点的childrenTreeNodes中。
            if(courseCategoryTreeDto != null){
                //如果父节点的childrenTreeNodes为空，创建集合，否则直接使用(第一次给父节点的childrenTreeNodes塞数据时要创建)
                if(courseCategoryTreeDto.getChildrenTreeNodes() == null){
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //下边开始往ChildrenTreeNodes属性中放子节点
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        });

        return categoryTreeDtos;
    }



}
