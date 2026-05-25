package com.hp.javabase.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.utils.ResponseUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sa-Token 示例控制器，用于演示公开接口、登录态校验和权限校验的最小用法。
 *
 * <p>该控制器不依赖业务 service，主要承担权限注解示例和统一响应样例输出职责。
 */
@RestController
public class DemoController {

    @GetMapping("/demo")
    public BaseResponse<String> getUserName() {
        return ResponseUtils.success("user");
    }

    @SaCheckLogin
    @GetMapping("/demo/login")
    public BaseResponse<String> loginProtected() {
        return ResponseUtils.success("login success");
    }

    @SaCheckPermission("user:write")
    @GetMapping("/demo/permission")
    public BaseResponse<String> permissionProtected() {
        return ResponseUtils.success("permission success");
    }
}
