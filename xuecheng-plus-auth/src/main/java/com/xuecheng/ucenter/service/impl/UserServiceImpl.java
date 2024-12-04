package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author Mr.M
 * @version 1.0
 * @description 自定义UserDetailsService
 * 实现 UserDetailsService 接口查询数据库得到用户信息返回 UserDetails 类型的用户信息即可,
 * 框架调用 loadUserByUsername()方法拿到用户信息
 * @date 2022/9/28 18:09
 */
@Service
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper xcUserMapper;

    /**
     * @param s 账号
     * @return org.springframework.security.core.userdetails.UserDetails
     * @description 根据账号查询用户信息
     * @author JIU-W
     * @date 2022/9/28 18:30
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        //账号
        String username = s;
        //根据username账号查询数据库
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getUsername, username);
        XcUser user = xcUserMapper.selectOne(queryWrapper);

        //查询到用户不存在，返回null即可，spring security框架抛出异常用户不存在。
        if (user == null) {
            return null;
        }
        //取出数据库存储的正确密码
        String password = user.getPassword();

        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        String[] authorities = {"test"};

        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);

        //创建UserDetails对象,权限信息待实现授权功能时再向UserDetail中加入
        UserDetails userDetails = User.withUsername(userString).password(password)
                .authorities(authorities).build();
        return userDetails;
    }


}
