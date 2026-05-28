package com.hp.javabase.model.enums;

import lombok.Getter;

/**
 * 验证码目标类型枚举，统一约束验证码面向手机号或邮箱的目标语义。
 */
@Getter
public enum VerificationTargetTypeEnum {

    PHONE("phone"),
    EMAIL("email");

    private final String blankMessagePrefix;

    VerificationTargetTypeEnum(String blankMessagePrefix) {
        this.blankMessagePrefix = blankMessagePrefix;
    }

}
