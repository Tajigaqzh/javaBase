package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 手机验证码登录请求。
 */
@Getter
@Setter
public class PhoneCodeLoginDTO extends DeviceAwareDTO {

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "phone must not be blank")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Schema(description = "短信验证码", example = "123456")
    @NotBlank(message = "smsCode must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "smsCode must be 6 digits")
    private String smsCode;
}
