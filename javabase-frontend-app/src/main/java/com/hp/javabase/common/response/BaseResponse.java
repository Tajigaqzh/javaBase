package com.hp.javabase.common.response;

import lombok.Getter;
import lombok.Setter;

@Setter
public class BaseResponse<T> {

    private ResponseCodeEnum code;

    @Getter
    private String message;

    @Getter
    private T data;

    public BaseResponse() {
    }

    public BaseResponse(ResponseCodeEnum code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code == null ? null : code.getCode();
    }

}
