package com.hp.javabase.common.aspect;

import com.hp.javabase.common.annotation.RateLimit;
import com.hp.javabase.common.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String limiterKey = buildLimiterKey(joinPoint, method, rateLimit);

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(limiterKey);
        rateLimiter.trySetRate(rateLimit.rateType(), rateLimit.rate(), rateLimit.rateInterval(), rateLimit.rateIntervalUnit());

        if (!rateLimiter.tryAcquire(1)) {
            throw new RateLimitException(rateLimit.message());
        }
        return joinPoint.proceed();
    }

    private String buildLimiterKey(ProceedingJoinPoint joinPoint, Method method, RateLimit rateLimit) {
        String businessKey = resolveBusinessKey(joinPoint, method, rateLimit);
        if (!rateLimit.includeIp()) {
            return RATE_LIMIT_PREFIX + businessKey;
        }
        return RATE_LIMIT_PREFIX + businessKey + ":" + getClientIp();
    }

    private String resolveBusinessKey(ProceedingJoinPoint joinPoint, Method method, RateLimit rateLimit) {
        if (rateLimit.key() == null || rateLimit.key().isBlank()) {
            return method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        }
        EvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        return expressionParser.parseExpression(rateLimit.key()).getValue(context, String.class);
    }

    private String getClientIp() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return "unknown";
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
