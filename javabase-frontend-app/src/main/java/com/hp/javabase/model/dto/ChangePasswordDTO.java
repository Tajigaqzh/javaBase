package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 已登录用户修改密码请求，必须校验旧密码。
 */
@Getter
@Setter
public class ChangePasswordDTO {

    @Schema(description = "旧密码", example = "Abcdef!234")
    @NotBlank(message = "oldPassword must not be blank")
    @Size(min = 10, max = 64, message = "oldPassword length must be between 10 and 64")
    private String oldPassword;

    @Schema(description = "新密码", example = "Abcdef!567")
    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 10, max = 64, message = "newPassword length must be between 10 and 64")
    private String newPassword;

    @AssertTrue(message = "newPassword must be different from oldPassword")
    private boolean isNewPasswordDifferentFromOldPassword() {
        if (oldPassword == null || newPassword == null) {
            return true;
        }
        return !oldPassword.equals(newPassword);
    }
}
