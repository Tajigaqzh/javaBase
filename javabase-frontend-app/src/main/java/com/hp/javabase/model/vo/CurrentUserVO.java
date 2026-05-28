package com.hp.javabase.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 当前登录用户视图，屏蔽密码等敏感字段，仅暴露前台当前会话所需信息。
 */
@Getter
@Builder
@AllArgsConstructor
public class CurrentUserVO {

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "用户编号", example = "U202605260001")
    private String userNo;

    @Schema(description = "昵称", example = "测试用户")
    private String nickname;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "demo@example.com")
    private String email;

    @Schema(description = "账号状态码", example = "1")
    private Integer status;
}
