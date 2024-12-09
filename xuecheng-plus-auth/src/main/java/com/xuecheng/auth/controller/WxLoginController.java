package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.WxAuthService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * @author JIU-W
 * @version 1.0
 * @description
 * @date 2024-12-05
 */
@Slf4j
@Controller
public class WxLoginController {

    @Autowired
    private WxAuthService wxAuthService;

    //1.定义这个接口接收微信下发的授权码。
    //前端用户扫了登录的微信二维码并且同意后，微信后台会向这个接口发送授权码(因为前端的wxsign.html里写了重定向到这个接口)，
    //然后拿到用户信息，最后跳转到本项目的登录页面。
    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException {

        log.debug("微信扫码回调,code:{},state:{}", code, state);

        //远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        XcUser xcUser = wxAuthService.wxAuth(code);
        if (xcUser == null) {
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();
        //5.重定向到浏览器自动登录
        //进入我们的统一入口：自定义的UserServiceImpl实现类的loadUserByUsername方法 从而进入execute方法
        return "redirect:http://www.51xuecheng.cn/sign.html?username=" + username + "&authType=wx";

    }
}
