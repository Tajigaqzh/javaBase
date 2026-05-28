package com.hp.javabase.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.common.exception.RateLimitException;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.response.ResponseCodeEnum;
import com.hp.javabase.common.utils.resonse.ResponseUtils;
import com.hp.javabase.service.FeishuBotService;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，统一收敛限流、鉴权、参数校验和业务参数异常的接口输出格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final FeishuBotService feishuBotService;

    public GlobalExceptionHandler(FeishuBotService feishuBotService) {
        this.feishuBotService = feishuBotService;
    }

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

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException exception) {
        return ResponseUtils.fail(ResponseCodeEnum.BUSINESS_ERROR, exception.getMessage());
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException illegalArgumentException) {
            return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, illegalArgumentException.getMessage());
        }
        return ResponseUtils.fail(ResponseCodeEnum.BAD_REQUEST, ResponseCodeEnum.BAD_REQUEST.getMessage());
    }

    private String extractBindingMessage(BindException exception) {
        if (exception.getBindingResult().getFieldError() != null) {
            return exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseCodeEnum.BAD_REQUEST.getMessage();
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception exception, HttpServletRequest request) {
        notifySystemException(exception, request);
        return ResponseUtils.fail(ResponseCodeEnum.SYSTEM_ERROR, exception.getMessage());
    }

    /**
     * 将未分类系统异常发送到飞书机器人，补充请求路径与异常摘要，且通知失败不影响原始接口返回。
     *
     * @param exception 原始系统异常
     * @param request 当前请求
     */
    private void notifySystemException(Exception exception, HttpServletRequest request) {
        String requestUri = request == null ? "unknown" : request.getRequestURI();
        String requestMethod = request == null ? "unknown" : request.getMethod();
        String message = exception.getMessage() == null ? "no message" : exception.getMessage();
        String text = """
                [JavaBase] System exception
                method: %s
                uri: %s
                exception: %s
                message: %s
                """.formatted(requestMethod, requestUri, exception.getClass().getName(), message);
        try {
            feishuBotService.sendTextMessage(text);
        } catch (Exception notifyException) {
            log.error("failed to notify feishu for system exception, uri={}", requestUri, notifyException);
        }
    }
}
