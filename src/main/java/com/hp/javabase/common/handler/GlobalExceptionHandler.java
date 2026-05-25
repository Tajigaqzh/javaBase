package com.hp.javabase.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.hp.javabase.common.exception.RateLimitException;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.response.ResponseCodeEnum;
import com.hp.javabase.common.utils.ResponseUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public BaseResponse<Void> handleRateLimitException(RateLimitException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.TOO_MANY_REQUESTS, exception.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<Void> handleNotLoginException(NotLoginException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.FORBIDDEN, "not login");
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<Void> handleNotPermissionException(NotPermissionException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.FORBIDDEN, "no permission");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, exception.getMessage());
    }
}
