package com.hp.javabase.model.dto;

import com.hp.javabase.model.enums.VerificationSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 邮箱验证码发送请求。
 */
@Getter
@Setter
public class SendEmailCodeDTO {

    @Schema(description = "邮箱", example = "demo@example.com")
    @NotBlank(message = "email must not be blank")
    @Email(message = "email format is invalid")
    @Size(max = 255, message = "email length must be less than or equal to 255")
    private String email;

    @Schema(description = "验证码业务场景编码：1-注册 2-登录 3-忘记密码 4-设置密码", example = "3")
    @NotNull(message = "scene must not be null")
    private VerificationSceneEnum scene;
}
