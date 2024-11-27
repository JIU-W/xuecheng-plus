package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.annotations.Delete;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author JIU-W
 * @version 1.0
 * @description 课程计划编辑接口
 * @date 2024-11-14
 */
@Api(value = "课程计划编辑接口", tags = "课程计划编辑接口")
@RestController
public class TeachplanController {

    @Resource
    private TeachplanService teachplanService;

    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId", name = "课程Id", required = true, dataType = "Long", paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId) {
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        return teachplanTree;
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto saveTeachplanDto) {
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    @ApiOperation("课程计划删除")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable Long id) {
        teachplanService.deleteTeachplan(id);
    }

    @ApiOperation("课程计划排序")
    @PostMapping("/teachplan/{moveType}/{id}")
    public void orderby(@PathVariable String moveType, @PathVariable Long id) {
        teachplanService.orderby(moveType, id);
    }

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto) {
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    @ApiOperation(value = "课程计划和媒资信息解除绑定")
    @DeleteMapping("/teachplan/association/media/{teachPlanId}/{mediaId}")
    public void deleteAssociationMedia(@PathVariable Long teachPlanId, @PathVariable String mediaId) {
        System.out.println("删除媒资信息");
        teachplanService.deleteTeachplanMedia(teachPlanId, mediaId);
    }


}
