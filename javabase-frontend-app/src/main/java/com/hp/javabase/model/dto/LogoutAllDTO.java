package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 全设备退出请求，支持保留当前设备。
 */
@Getter
@Setter
public class LogoutAllDTO {

    @Schema(description = "是否保留当前设备", example = "true")
    private Boolean keepCurrent = Boolean.TRUE;
}
