package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author JIU-W
 * @version 1.0
 * @description 微信扫码认证
 * @date 2024-12-05
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Value("${weixin.appid}")
    String appid;

    @Value("${weixin.secret}")
    String secret;

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    WxAuthServiceImpl currentProxy;


    //远程调用微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
    public XcUser wxAuth(String code) {

        //收到code调用微信接口申请access_token
        Map<String, String> access_token_map = getAccess_token(code);

        if (access_token_map == null) {
            return null;
        }
        System.out.println(access_token_map);

        String openid = access_token_map.get("openid");
        String access_token = access_token_map.get("access_token");

        //拿access_token查询用户信息
        Map<String, String> userinfo = getUserinfo(access_token, openid);
        if (userinfo == null) {
            return null;
        }
        //将用户信息保存到数据库
        XcUser xcUser = currentProxy.addWxUser(userinfo);
        return xcUser;
    }

    /**
     * 申请访问令牌,响应示例
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    //2.收到授权码调用微信接口申请令牌access_token
    private Map<String, String> getAccess_token(String code) {

        String wxUrl_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, appid, secret, code);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);
        //远程调用此URL
        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);
        //获取响应的结果
        String result = exchange.getBody();
        log.info("调用微信接口申请access_token: 返回值:{}", result);
        //转换为map
        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;
    }

    /**
     * 获取用户信息，示例如下：
     * {
     * "openid":"OPENID",
     * "nickname":"NICKNAME",
     * "sex":1,
     * "province":"PROVINCE",
     * "city":"CITY",
     * "country":"COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege":[
     * "PRIVILEGE1",
     * "PRIVILEGE2"
     * ],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    //3.申请到令牌调用微信获取用户信息
    private Map<String, String> getUserinfo(String access_token, String openid) {
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, access_token, openid);
        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.POST, null, String.class);

        //防止乱码进行转码
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

        log.info("调用微信接口申请access_token: 返回值:{}", result);

        Map<String, String> resultMap = JSON.parseObject(result, Map.class);
        return resultMap;
    }

    //4.获取用户信息成功将其写入本项目用户中心数据库
    @Transactional
    public XcUser addWxUser(Map userInfo_map) {
        String unionid = userInfo_map.get("unionid").toString();
        //根据unionid查询数据库  unionid(微信unionid)
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (xcUser != null) {
            return xcUser;
        }
        //添加用户记录
        xcUser = new XcUser();
        String userId = UUID.randomUUID().toString();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        //添加用户角色关系表记录
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }


    //访问链接：http://www.51xuecheng.cn/sign.html?username=t1&authType=wx
    //会进入这个接口
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //得到账号
        String username = authParamsDto.getUsername();
        //根据账号查询数据库
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }


}
