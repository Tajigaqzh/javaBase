package com.hp.javabase.common.utils;

import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.response.ResponseCodeEnum;

public final class ResponseUtils {

    private ResponseUtils() {
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ResponseCodeEnum.SUCCESS, ResponseCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(ResponseCodeEnum.SUCCESS, message, data);
    }

    public static <T> BaseResponse<T> fail(ResponseCodeEnum codeEnum) {
        return new BaseResponse<>(codeEnum, codeEnum.getMessage(), null);
    }

    public static <T> BaseResponse<T> fail(ResponseCodeEnum codeEnum, String message) {
        return new BaseResponse<>(codeEnum, message, null);
    }

    public static <T> BaseResponse<T> fail(String message) {
        return new BaseResponse<>(ResponseCodeEnum.SYSTEM_ERROR, message, null);
    }
}
