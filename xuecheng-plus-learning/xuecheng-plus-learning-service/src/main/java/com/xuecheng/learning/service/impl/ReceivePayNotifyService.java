package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @description 接收支付结果
 * @author JIU-W
 * @date 2024-12-09
 * @version 1.0
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MyCourseTablesService myCourseTablesService;


    //监听消息队列接收支付结果通知
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message, Channel channel) {
        //让接受方法休眠5秒，因为配置了消费者确认机制，添加选课失败，抛出异常时消息会重回队列，
        //并重试消费(重新投递到消费者)，为了让重试不那么迅速，所以就休眠5秒。
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //获取消息并转成对象
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);

        //消息类型
        String messageType = mqMessage.getMessageType();
        //订单类型,60201表示购买课程
        String orderType = mqMessage.getBusinessKey2();
        //这里只处理支付结果通知
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(orderType)) {
            //选课记录id
            String choosecourseId = mqMessage.getBusinessKey1();
            //更新选课记录表，向我的课程表插入记录
            boolean b = myCourseTablesService.saveChooseCourseSuccess(choosecourseId);
            if (!b) {
                //添加选课失败，抛出异常，消息重回队列，并重试消费(重新投递到消费者)。
                XueChengPlusException.cast("收到支付结果，添加选课失败");
            }
        }


    }


}
