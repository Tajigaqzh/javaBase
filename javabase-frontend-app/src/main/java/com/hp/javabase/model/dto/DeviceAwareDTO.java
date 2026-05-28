package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备感知请求基类，统一承载登录和会话相关接口需要的终端、设备实例和指纹信息。
 */
@Getter
@Setter
public abstract class DeviceAwareDTO {

    @Schema(description = "终端类型，支持 WEB、ANDROID、IOS", example = "WEB")
    @NotBlank(message = "terminalType must not be blank")
    @Pattern(regexp = "WEB|ANDROID|IOS", message = "terminalType must be one of WEB, ANDROID, IOS")
    private String terminalType;

    @Schema(description = "客户端生成并持久化的设备实例 ID", example = "web-mac-001")
    @NotBlank(message = "deviceId must not be blank")
    @Size(max = 128, message = "deviceId length must be less than or equal to 128")
    private String deviceId;

    @Schema(description = "浏览器指纹或设备指纹摘要", example = "fingerprint-8f1b0d")
    @Size(max = 255, message = "fingerprint length must be less than or equal to 255")
    private String fingerprint;

    @Schema(description = "设备展示名称", example = "Mac Chrome")
    @Size(max = 128, message = "deviceName length must be less than or equal to 128")
    private String deviceName;
}
