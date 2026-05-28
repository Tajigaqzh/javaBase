package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 手机号注册请求，要求短信验证码校验通过后创建账号并建立前台登录态。
 */
@Getter
@Setter
public class PhoneRegisterDTO extends DeviceAwareDTO {

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "phone must not be blank")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Schema(description = "短信验证码", example = "123456")
    @NotBlank(message = "smsCode must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "smsCode must be 6 digits")
    private String smsCode;

    @Schema(description = "登录密码", example = "Abcdef!234")
    @NotBlank(message = "password must not be blank")
    @Size(min = 10, max = 64, message = "password length must be between 10 and 64")
    private String password;

    @Schema(description = "昵称，可选；未传则默认使用手机号后四位生成", example = "测试用户")
    @Size(max = 64, message = "nickname length must be less than or equal to 64")
    private String nickname;
}
