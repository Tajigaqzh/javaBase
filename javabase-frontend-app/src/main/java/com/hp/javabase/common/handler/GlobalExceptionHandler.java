package com.hp.javabase.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.hp.javabase.common.exception.RateLimitException;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.response.ResponseCodeEnum;
import com.hp.javabase.common.utils.ResponseUtils;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一收敛限流、鉴权、参数校验和业务参数异常的接口输出格式。
 */
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, extractBindingMessage(exception));
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<Void> handleBindException(BindException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, extractBindingMessage(exception));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ResponseCodeEnum.BAD_REQUEST.getMessage());
        return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, message);
    }

    private String extractBindingMessage(BindException exception) {
        if (exception.getBindingResult().getFieldError() != null) {
            return exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseCodeEnum.BAD_REQUEST.getMessage();
    }
}
