package com.hp.javabase.model.dto;

import com.hp.javabase.model.enums.VerificationSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 手机验证码发送请求。
 */
@Getter
@Setter
public class SendPhoneCodeDTO {

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "phone must not be blank")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Schema(description = "验证码业务场景编码：1-注册 2-登录 3-忘记密码 4-设置密码", example = "1")
    @NotNull(message = "scene must not be null")
    private VerificationSceneEnum scene;
}
