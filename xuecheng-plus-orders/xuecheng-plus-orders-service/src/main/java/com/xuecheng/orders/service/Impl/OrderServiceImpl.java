package com.xuecheng.orders.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;

    @Autowired
    XcOrdersMapper ordersMapper;

    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;

    @Autowired
    XcPayRecordMapper payRecordMapper;

    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        //创建商品订单
        XcOrders orders = saveXcOrders(userId, addOrderDto);
        if (orders == null) {
            XueChengPlusException.cast("订单创建失败");
        }
        //生成支付记录
        XcPayRecord payRecord = createPayRecord(orders);

        //生成二维码
        String qrCode = null;
        try {
            //payNo为支付交易流水号，要唯一。后面要传给支付系统支付宝。
            Long payNo = payRecord.getPayNo();
            //url要可以被模拟器访问到，url为下单接口路径(稍后定义)
            //url格式如下：http://192.168.101.1:63030/orders/requestpay?payNo=%s
            String url = String.format(qrcodeurl, payNo);
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            throw new XueChengPlusException("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        //设置二维码图片:为base64编码格式
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    //创建商品订单: 1.插入订单表 2.插入订单明细表
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //幂等性处理
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (order != null) {
            return order;
        }
        order = new XcOrders();
        //使用雪花算法生成订单号
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001");//未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        //设置选课记录id
        order.setOutBusinessId(addOrderDto.getOutBusinessId());
        //插入订单表
        ordersMapper.insert(order);
        //插入订单明细表
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoodsList.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods, xcOrdersGoods);
            //设置订单号，也就是订单表的id
            xcOrdersGoods.setOrderId(orderId);
            ordersGoodsMapper.insert(xcOrdersGoods);//这里循环调数据库，性能不高，后续优化
        });
        return order;
    }

    //根据外部业务id(从学习服务传过来的选课记录id)查询订单
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>()
                .eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    //创建支付记录
    public XcPayRecord createPayRecord(XcOrders orders) {
        //如果此订单不存在不能添加支付记录
        if (orders == null) {
            throw new XueChengPlusException("订单不存在");
        }
        //订单状态
        String status = orders.getStatus();
        //如果此订单支付结果为成功，不能添加支付记录，避免重复支付
        if ("600002".equals(status)) {
            throw new XueChengPlusException("订单已支付");
        }
        XcPayRecord payRecord = new XcPayRecord();
        //生成支付交易流水号(后面要传给支付宝)
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        //设置商品订单号，也就是订单表的id
        payRecord.setOrderId(orders.getId());
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        payRecordMapper.insert(payRecord);
        return payRecord;
    }


    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>()
                .eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }


}
