package com.hp.javabase.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 设置密码请求，适用于首次设密或待补全账号补设密码。
 */
@Getter
@Setter
public class SetPasswordDTO {

    @Schema(description = "手机号，手机号设密时传", example = "13800138000")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @Schema(description = "邮箱，邮箱设密时传", example = "demo@example.com")
    @Email(message = "email format is invalid")
    @Size(max = 255, message = "email length must be less than or equal to 255")
    private String email;

    @Schema(description = "验证码", example = "123456")
    @NotBlank(message = "verificationCode must not be blank")
    @Pattern(regexp = "^\\d{6}$", message = "verificationCode must be 6 digits")
    private String verificationCode;

    @Schema(description = "新密码", example = "Abcdef!234")
    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 10, max = 64, message = "newPassword length must be between 10 and 64")
    private String newPassword;

    @AssertTrue(message = "exactly one of phone or email must be provided")
    private boolean isExactlyOneSetPasswordTargetProvided() {
        boolean hasPhone = StringUtils.hasText(phone);
        boolean hasEmail = StringUtils.hasText(email);
        return hasPhone ^ hasEmail;
    }
}
