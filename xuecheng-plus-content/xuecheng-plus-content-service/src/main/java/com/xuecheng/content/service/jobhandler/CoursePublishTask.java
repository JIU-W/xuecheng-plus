package com.xuecheng.content.service.jobhandler;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author JIU-W
 * @version 1.0
 * @description
 * @date 2024-11-28
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    private SearchServiceClient searchServiceClient;

    @Autowired
    private RedisTemplate redisTemplate;


    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        //分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex=" + shardIndex + ",shardTotal=" + shardTotal);

        //取出电脑CPU核心数作为一次处理数据的条数
        int processors = Runtime.getRuntime().availableProcessors();
        log.debug("一次处理视频数量不要超过cpu核心数:{}", processors);

        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex, shardTotal, "course_publish", processors, 60);
                                                                        //1
    }


    //课程发布任务处理
    //执行课程发布任务的逻辑，如果此方法抛出异常说明任务执行失败
    public boolean execute(MqMessage mqMessage) {
        //获取消息相关的业务信息
        //即课程id
        String businessKey1 = mqMessage.getBusinessKey1();
        long courseId = Integer.parseInt(businessKey1);
        //课程静态化
        generateCourseHtml(mqMessage, courseId);
        //课程索引
        saveCourseIndex(mqMessage, courseId);
        //课程缓存
        saveCourseCache(mqMessage, courseId);
        return true;
    }


    //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        log.debug("开始进行课程静态化,课程id:{}", courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne == 1) {
            log.debug("课程静态化已处理直接返回，课程id:{}", courseId);
            return;
        }
        //生成静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);

        //上传静态化页面
        if (file != null) {
            coursePublishService.uploadCourseHtml(courseId, file);
        }
        //保存第一阶段状态
        mqMessageService.completedStageOne(id);
    }

    //将课程信息缓存至Redis
    //课程发布信息的特点的是查询较多，修改很少，这里考虑将课程发布信息进行缓存。
    public void saveCourseCache(MqMessage mqMessage, long courseId) {
        log.debug("将课程信息缓存至redis,课程id:{}", courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageTwo = mqMessageService.getStageTwo(id);
        if(stageTwo == 1){
            log.debug("课程缓存已处理直接返回，课程id:{}", courseId);
            return;
        }
        //先查询缓存看是否有数据(大概率是没有数据的，但是为了保证程序的健壮性还是先查再存)
        Object jsonObj = redisTemplate.opsForValue().get("course:" + courseId);
        if(jsonObj == null){
            //从数据库查询
            CoursePublish coursePublish = coursePublishService.getCoursePublish(courseId);
            if (coursePublish != null) {
                redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish));
            }
        }
        //保存第二阶段状态
        mqMessageService.completedStageTwo(id);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.debug("保存课程索引信息,课程id:{}", courseId);
        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageThree = mqMessageService.getStageThree(id);
        if (stageThree == 1) {
            log.debug("课程索引信息已写入，直接返回，课程id:{}", courseId);
            return;
        }
        //取出课程发布信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);

        //拷贝至课程索引对象
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);

        //远程调用搜索服务api添加课程信息到索引
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            throw new XueChengPlusException("添加索引失败");
        }

        //保存第二阶段状态
        mqMessageService.completedStageThree(id);
    }

}
