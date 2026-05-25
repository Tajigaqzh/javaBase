package com.hp.javabase.service;

import cn.dev33.satoken.stp.StpUtil;
import com.hp.javabase.mapper.UserMapper;
import com.hp.javabase.model.entity.User;
import com.hp.javabase.model.vo.LoginUserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务，负责登录态建立、退出和按登录信息读取当前用户。
 *
 * <p>当前服务依赖 {@link UserMapper} 读取用户信息，并通过 Sa-Token 的 {@link StpUtil}
 * 建立与销毁登录态，是登录链路的核心业务入口。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按用户名完成登录流程并返回登录用户视图。
     *
     * <p>执行步骤包括：
     * 1. 校验用户名非空；
     * 2. 查询用户是否存在；
     * 3. 调用 Sa-Token 建立登录态；
     * 4. 组装返回 token 与基础用户信息。
     *
     * @param username 登录用户名
     * @return 登录用户视图对象
     */
    public LoginUserVO login(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username must not be blank");
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("user does not exist");
        }
        StpUtil.login(user.getId());
        return new LoginUserVO(user.getId(), user.getUsername(), StpUtil.getTokenValue());
    }

    public void logout() {
        StpUtil.logout();
    }

    public User findCurrentUser() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return findByLoginId(loginId);
    }

    /**
     * 根据登录态中的 loginId 查询当前用户。
     *
     * <p>该方法承担登录态标识到用户实体的转换职责，依赖 {@link UserMapper} 按主键查询用户。
     *
     * @param loginId Sa-Token 保存的登录标识
     * @return 查到的用户；若 loginId 为空则返回 null
     */
    public User findByLoginId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        Long userId = Long.valueOf(String.valueOf(loginId));
        return userMapper.selectById(userId);
    }
}
