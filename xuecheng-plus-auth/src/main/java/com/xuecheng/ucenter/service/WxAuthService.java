package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author Mr.M
 * @version 1.0
 * @description 微信认证接口
 * @date 2023/2/21 22:15
 */
public interface WxAuthService {

    /**
     * 远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
     * @param code
     * @return
     */
    XcUser wxAuth(String code);

}
