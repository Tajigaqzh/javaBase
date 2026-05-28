package com.hp.javabase.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 用户设备会话视图，供前台安全中心展示在线设备和当前设备状态。
 */
@Getter
@Builder
@AllArgsConstructor
public class UserSessionDeviceVO {

    @Schema(description = "设备会话记录 ID", example = "1")
    private Long sessionId;

    @Schema(description = "终端类型", example = "WEB")
    private String terminalType;

    @Schema(description = "设备实例 ID", example = "web-mac-001")
    private String deviceId;

    @Schema(description = "设备名称", example = "Mac Chrome")
    private String deviceName;

    @Schema(description = "设备指纹摘要", example = "fingerprint-8f1b0d")
    private String fingerprint;

    @Schema(description = "登录 IP", example = "127.0.0.1")
    private String ip;

    @Schema(description = "用户代理", example = "Mozilla/5.0")
    private String userAgent;

    @Schema(description = "会话状态码", example = "1")
    private Integer sessionStatus;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "最近活跃时间")
    private LocalDateTime lastActiveTime;

    @Schema(description = "是否当前设备", example = "true")
    private Boolean currentDevice;
}
