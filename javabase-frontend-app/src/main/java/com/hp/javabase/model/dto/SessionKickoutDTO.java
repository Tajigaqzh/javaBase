package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户侧踢设备下线请求，按业务侧设备会话主键精确定位目标设备。
 */
@Getter
@Setter
public class SessionKickoutDTO {

    @Schema(description = "设备会话记录 ID", example = "1")
    @NotNull(message = "sessionId must not be null")
    @Positive(message = "sessionId must be greater than 0")
    private Long sessionId;
}
