package com.hp.javabase.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 登录成功返回对象，承载当前登录用户、会话 token 和设备端基础信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVO {

    @Schema(description = "登录用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录手机号", example = "13800138000")
    private String phone;

    @Schema(description = "Sa-Token 登录凭证", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String token;

    @Schema(description = "终端类型", example = "WEB")
    private String terminalType;

    @Schema(description = "设备实例 ID", example = "web-mac-001")
    private String deviceId;

    @Schema(description = "账号状态码", example = "1")
    private Integer status;
}
