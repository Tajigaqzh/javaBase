package com.hp.javabase.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 验证码业务场景枚举，统一约束验证码的发送与校验用途。
 */
public enum VerificationSceneEnum {

    REGISTER(1),
    LOGIN(2),
    FORGOT_PASSWORD(3),
    SET_PASSWORD(4);

    private final int code;

    VerificationSceneEnum(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static VerificationSceneEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (VerificationSceneEnum sceneEnum : values()) {
            if (sceneEnum.code == code) {
                return sceneEnum;
            }
        }
        throw new IllegalArgumentException("scene must be one of 1, 2, 3, 4");
    }
}
