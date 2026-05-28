package com.hp.javabase.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码发送返回对象，用于告知前端剩余冷却时间和开发环境调试码。
 */
@Getter
@AllArgsConstructor
public class VerificationCodeSendVO {

    @Schema(description = "验证码有效秒数", example = "300")
    private long expireSeconds;

    @Schema(description = "再次发送剩余冷却秒数", example = "60")
    private long cooldownSeconds;

    @Schema(description = "仅开发环境返回的调试验证码", example = "123456", nullable = true)
    private String debugCode;
}
