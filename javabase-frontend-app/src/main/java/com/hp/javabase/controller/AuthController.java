package com.hp.javabase.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.utils.ClientIpUtils;
import com.hp.javabase.common.utils.resonse.ResponseUtils;
import com.hp.javabase.model.dto.ChangePasswordDTO;
import com.hp.javabase.model.dto.ForgotPasswordResetDTO;
import com.hp.javabase.model.dto.LogoutAllDTO;
import com.hp.javabase.model.dto.PasswordLoginDTO;
import com.hp.javabase.model.dto.PhoneCodeLoginDTO;
import com.hp.javabase.model.dto.PhoneRegisterDTO;
import com.hp.javabase.model.dto.SendEmailCodeDTO;
import com.hp.javabase.model.dto.SendPhoneCodeDTO;
import com.hp.javabase.model.dto.SessionKickoutDTO;
import com.hp.javabase.model.dto.SetPasswordDTO;
import com.hp.javabase.model.vo.CurrentUserVO;
import com.hp.javabase.model.vo.LoginUserVO;
import com.hp.javabase.model.vo.UserSessionDeviceVO;
import com.hp.javabase.model.vo.VerificationCodeSendVO;
import com.hp.javabase.service.login.AuthService;
import com.hp.javabase.service.login.VerificationCodeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台认证控制器，暴露手机号注册登录、密码管理、验证码发送和设备会话管理接口。
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Frontend Auth", description = "前台手机号注册登录、密码管理与设备会话接口。")
public class AuthController {

    private final AuthService authService;
    private final VerificationCodeUtils verificationCodeUtils;

    public AuthController(AuthService authService, VerificationCodeUtils verificationCodeUtils) {
        this.authService = authService;
        this.verificationCodeUtils = verificationCodeUtils;
    }

    @PostMapping("/code/phone/send")
    @Operation(summary = "发送手机验证码", description = "按手机号与业务场景发送前台验证码。")
    public BaseResponse<VerificationCodeSendVO> sendPhoneCode(
            @Valid @RequestBody SendPhoneCodeDTO request,
            HttpServletRequest httpServletRequest) {
        return ResponseUtils.success(verificationCodeUtils.sendPhoneCode(
                request.getPhone(),
                request.getScene(),
                ClientIpUtils.resolveClientIp(httpServletRequest)));
    }

    @PostMapping("/code/email/send")
    @Operation(summary = "发送邮箱验证码", description = "按邮箱与业务场景发送前台验证码。")
    public BaseResponse<VerificationCodeSendVO> sendEmailCode(
            @Valid @RequestBody SendEmailCodeDTO request,
            HttpServletRequest httpServletRequest) {
        return ResponseUtils.success(verificationCodeUtils.sendEmailCode(
                request.getEmail(),
                request.getScene(),
                ClientIpUtils.resolveClientIp(httpServletRequest)));
    }

    @PostMapping("/register/phone")
    @Operation(summary = "手机号注册", description = "手机号完成验证码校验后创建账号并直接登录。")
    public BaseResponse<LoginUserVO> registerByPhone(
            @Valid @RequestBody PhoneRegisterDTO request,
            HttpServletRequest httpServletRequest) {
        return ResponseUtils.success(authService.registerByPhone(
                request,
                ClientIpUtils.resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/login/password")
    @Operation(summary = "手机号密码登录", description = "按手机号与密码完成前台登录。")
    public BaseResponse<LoginUserVO> loginByPassword(
            @Valid @RequestBody PasswordLoginDTO request,
            HttpServletRequest httpServletRequest) {
        return ResponseUtils.success(authService.loginByPassword(
                request,
                ClientIpUtils.resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/login/phone-code")
    @Operation(summary = "手机号验证码登录", description = "按手机号与短信验证码完成前台登录。")
    public BaseResponse<LoginUserVO> loginByPhoneCode(
            @Valid @RequestBody PhoneCodeLoginDTO request,
            HttpServletRequest httpServletRequest) {
        return ResponseUtils.success(authService.loginByPhoneCode(
                request,
                ClientIpUtils.resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/password/set")
    @Operation(summary = "设置密码", description = "首次设密或待补全账号补设密码。")
    public BaseResponse<Void> setPassword(@Valid @RequestBody SetPasswordDTO request) {
        authService.setPassword(request);
        return ResponseUtils.success(null);
    }

    @PostMapping("/password/forgot/reset")
    @Operation(summary = "忘记密码重置", description = "通过短信验证码或邮箱验证码重置密码。")
    public BaseResponse<Void> forgotResetPassword(@Valid @RequestBody ForgotPasswordResetDTO request) {
        authService.forgotResetPassword(request);
        return ResponseUtils.success(null);
    }

    @SaCheckLogin
    @PostMapping("/password/change")
    @Operation(
            summary = "修改密码",
            description = "已登录状态下校验旧密码后修改密码，并强制全端重新登录。",
            security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO request) {
        authService.changePassword(request);
        return ResponseUtils.success(null);
    }

    @SaCheckLogin
    @PostMapping("/logout")
    @Operation(summary = "退出当前设备", description = "退出当前前台登录设备。", security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<Void> logout() {
        authService.logout();
        return ResponseUtils.success(null);
    }

    @SaCheckLogin
    @PostMapping("/logout-all")
    @Operation(summary = "退出全部设备", description = "退出当前账号的全部设备，可保留当前设备。", security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<Void> logoutAll(@RequestBody(required = false) LogoutAllDTO request) {
        authService.logoutAll(request == null || request.getKeepCurrent() == null || request.getKeepCurrent());
        return ResponseUtils.success(null);
    }

    @SaCheckLogin
    @GetMapping("/session/list")
    @Operation(summary = "查询当前账号在线设备", description = "查询当前账号在线设备列表。", security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<List<UserSessionDeviceVO>> listSessions() {
        return ResponseUtils.success(authService.listMySessions());
    }

    @SaCheckLogin
    @PostMapping("/session/kickout")
    @Operation(summary = "踢指定设备下线", description = "用户侧按设备会话记录踢指定设备下线，不允许踢当前设备。", security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<Void> kickoutSession(@Valid @RequestBody SessionKickoutDTO request) {
        authService.kickoutSession(request.getSessionId());
        return ResponseUtils.success(null);
    }

    @SaCheckLogin
    @GetMapping("/me")
    @Operation(summary = "查询当前登录用户", description = "根据当前 Sa-Token 查询安全过滤后的当前用户信息。", security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<CurrentUserVO> me() {
        return ResponseUtils.success(authService.getCurrentUserProfile());
    }

    @SaCheckLogin
    @GetMapping("/token-info")
    @Operation(summary = "查询当前 token 信息", description = "输出当前 Sa-Token 元信息，便于本地调试。", security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<Object> tokenInfo() {
        return ResponseUtils.success(StpUtil.getTokenInfo());
    }
}
