package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 手机号密码登录请求。
 */
@Getter
@Setter
public class PasswordLoginDTO extends DeviceAwareDTO {

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "phone must not be blank")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Schema(description = "登录密码", example = "Abcdef!234")
    @NotBlank(message = "password must not be blank")
    @Size(min = 10, max = 64, message = "password length must be between 10 and 64")
    private String password;
}
