package com.hp.javabase.model.enums;

/**
 * 用户状态枚举，约束账号是否允许登录和是否需要进入补全流程。
 */
public enum UserStatusEnum {

    NORMAL(1),
    PENDING_COMPLETE(2),
    DISABLED(3),
    FROZEN(4),
    CANCELLED(5);

    private final int code;

    UserStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
