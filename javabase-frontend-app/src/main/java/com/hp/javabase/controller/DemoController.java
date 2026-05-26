package com.hp.javabase.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.utils.ResponseUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sa-Token 示例控制器，用于演示公开接口、登录态校验和权限校验的最小用法。
 *
 * <p>该控制器不依赖业务 service，主要承担权限注解示例和统一响应样例输出职责。
 */
@RestController
@Tag(name = "Frontend Demo", description = "前台示例接口，用于演示公开、登录态和权限校验。")
public class DemoController {

    @GetMapping("/demo")
    @Operation(summary = "公开示例接口", description = "无需登录即可访问的示例接口。")
    public BaseResponse<String> getUserName() {
        return ResponseUtils.success("user");
    }

    @SaCheckLogin
    @GetMapping("/demo/login")
    @Operation(
            summary = "登录态示例接口",
            description = "要求已登录的示例接口。",
            security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<String> loginProtected() {
        return ResponseUtils.success("login success");
    }

    @SaCheckPermission("user:write")
    @GetMapping("/demo/permission")
    @Operation(
            summary = "权限示例接口",
            description = "要求具备 user:write 权限的示例接口。",
            security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<String> permissionProtected() {
        return ResponseUtils.success("permission success");
    }
}
