package com.hp.javabase.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 对话请求参数，定义用户消息和可选模型名的基础协议校验约束。
 */
@Getter
@Setter
public class ChatRequest {

    @Schema(description = "用户输入消息", example = "帮我总结今天的任务")
    @NotBlank(message = "message must not be blank")
    @Size(max = 4000, message = "message length must be less than or equal to 4000")
    private String message;

    @Schema(description = "可选模型名，不传则使用服务端默认模型", example = "gpt-5.4-mini")
    @Size(max = 100, message = "model length must be less than or equal to 100")
    private String model;
}
