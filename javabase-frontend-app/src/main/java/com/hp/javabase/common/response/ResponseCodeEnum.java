package com.hp.javabase.common.response;

import lombok.Getter;

@Getter
public enum ResponseCodeEnum {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    BUSINESS_ERROR(422, "business error"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    SYSTEM_ERROR(500, "system error");

    private final int code;
    private final String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
