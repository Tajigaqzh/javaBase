package com.hp.javabase.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.response.ResponseCodeEnum;
import com.hp.javabase.common.utils.ResponseUtils;
import com.hp.javabase.model.dto.LoginRequest;
import com.hp.javabase.model.entity.User;
import com.hp.javabase.model.vo.LoginUserVO;
import com.hp.javabase.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，提供登录态建立、退出和当前登录信息读取能力。
 *
 * <p>当前控制器依赖 {@link AuthService} 处理认证流程，并复用统一响应模型输出登录相关结果。
 * 主要承担基础参数校验、协议转换和受保护接口的 HTTP 暴露职责。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 按用户名执行登录并返回登录用户信息与 token。
     *
     * <p>该方法先做最小化参数校验，再委托 {@link AuthService} 完成用户查询与登录态建立。
     * 当前实现只演示用户名登录，后续可扩展为密码或多因子认证。
     *
     * @param request 登录请求体，至少需要提供非空用户名
     * @return 统一响应包装的登录结果
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, "username must not be blank");
        }
        return ResponseUtils.success(authService.login(request.getUsername()));
    }

    /**
     * 退出当前登录态。
     *
     * <p>该接口依赖 Sa-Token 的登录态校验，只有已登录用户才能触发退出流程。
     *
     * @return 统一响应包装的空结果
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public BaseResponse<Void> logout() {
        authService.logout();
        return ResponseUtils.success(null);
    }

    @SaCheckLogin
    @GetMapping("/me")
    public BaseResponse<User> me() {
        return ResponseUtils.success(authService.findCurrentUser());
    }

    /**
     * 查询当前 token 的 Sa-Token 信息，便于调试登录态与权限上下文。
     *
     * @return 当前 token 的元信息
     */
    @SaCheckLogin
    @GetMapping("/token-info")
    public BaseResponse<Object> tokenInfo() {
        return ResponseUtils.success(StpUtil.getTokenInfo());
    }
}
