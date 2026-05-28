package com.hp.javabase.common.exception;

/**
 * 业务异常，统一表达认证、会话和账号规则校验失败等可预期业务错误。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
