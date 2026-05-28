package com.hp.javabase.model.enums;

/**
 * 设备会话状态枚举。
 */
public enum UserSessionStatusEnum {

    ONLINE(1),
    LOGOUT(2),
    KICKOUT(3),
    EXPIRED(4);

    private final int code;

    UserSessionStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
