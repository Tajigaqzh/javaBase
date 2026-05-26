package com.hp.javabase.common.response;

public class BaseResponse<T> {

    private ResponseCodeEnum code;
    private String message;
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

    public void setCode(ResponseCodeEnum code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
