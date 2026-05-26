package com.hp.javabase.model.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 前台登录请求参数，负责承载认证接口的基础入参并声明协议级校验规则。
 */
@Getter
@Setter
public class LoginRequest {

    @Schema(description = "登录用户名", example = "tester")
    @NotBlank(message = "username must not be blank")
    private String username;
}
