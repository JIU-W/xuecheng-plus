package com.xuecheng.orders.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * @author JIU-W
 * @version 1.0
 * @description 创建商品订单  前端传过来的数据封装
 * @date 2024-12-07
 */

@Data
@ToString
public class AddOrderDto {

    /**
     * 总价
     */
    private Float totalPrice;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 订单名称
     */
    private String orderName;
    /**
     * 订单描述
     */
    private String orderDescrip;

    /**
     * 订单明细json，不可为空
     * [{"goodsId":"","goodsType":"","goodsName":"","goodsPrice":"","goodsDetail":""},{...}]
     */
    private String orderDetail;

    /**
     * 外部系统业务id，这里是指选课记录id
     */
    private String outBusinessId;

}
