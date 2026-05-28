package com.hp.javabase.model.enums;

/**
 * 用户注册来源枚举，统一约束账号创建来源的稳定编码。
 */
public enum RegisterSourceEnum {

    PHONE("phone"),
    EMAIL("email"),
    QQ("qq"),
    ADMIN_CREATE("admin_create");

    private final String code;

    RegisterSourceEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
