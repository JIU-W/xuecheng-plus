package com.xuecheng.ucenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author JIU-W
 * @version 1.0
 * @description 验证码服务远程接口
 * @date 2024-12-05
 */
@FeignClient(value = "checkcode", fallbackFactory = CheckCodeClientFactory.class)
@RequestMapping("/checkcode")
public interface CheckCodeClient {

    @PostMapping(value = "/verify")
    Boolean verify(@RequestParam("key") String key, @RequestParam("code") String code);

}
