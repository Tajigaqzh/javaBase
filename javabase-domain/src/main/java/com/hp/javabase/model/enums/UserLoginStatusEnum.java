package com.hp.javabase.model.enums;

/**
 * 登录日志状态枚举。
 */
public enum UserLoginStatusEnum {

    SUCCESS(1),
    FAIL(0);

    private final int code;

    UserLoginStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
