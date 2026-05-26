package com.hp.javabase.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVO {

    @Schema(description = "登录用户 ID", example = "1")
    private Long userId;

    @Schema(description = "登录用户名", example = "tester")
    private String username;

    @Schema(description = "Sa-Token 登录凭证", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String token;
}
