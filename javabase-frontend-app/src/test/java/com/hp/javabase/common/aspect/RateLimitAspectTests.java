package com.hp.javabase.common.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hp.javabase.common.annotation.RateLimit;
import com.hp.javabase.common.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link RateLimitAspect} 单元测试，覆盖按客户端 IP 拼装限流 key 的行为。
 */
class RateLimitAspectTests {

    @Test
    void doRateLimitShouldUseForwardedHeaderIpInLimiterKey() throws Throwable {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RRateLimiter rateLimiter = Mockito.mock(RRateLimiter.class);
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
        RateLimitAspect aspect = new RateLimitAspect(redissonClient);
        Method method = RateLimitTarget.class.getDeclaredMethod("limitWithIp", String.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown, 10.0.0.8");
        request.setMethod("POST");
        request.setRequestURI("/test/rate-limit");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"13800138000"});
        when(redissonClient.getRateLimiter("rate_limit:phone-code:10.0.0.8")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(1)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            Object result = aspect.doRateLimit(joinPoint, method.getAnnotation(RateLimit.class));

            assertEquals("ok", result);
            verify(redissonClient).getRateLimiter("rate_limit:phone-code:10.0.0.8");
            verify(rateLimiter).trySetRate(eq(RateType.OVERALL), eq(2L), eq(60L), eq(RateIntervalUnit.SECONDS));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void doRateLimitShouldThrowWhenLimiterRejectsAcquire() throws Throwable {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RRateLimiter rateLimiter = Mockito.mock(RRateLimiter.class);
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        MethodSignature methodSignature = Mockito.mock(MethodSignature.class);
        RateLimitAspect aspect = new RateLimitAspect(redissonClient);
        Method method = RateLimitTarget.class.getDeclaredMethod("limitWithIp", String.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"13800138000"});
        when(redissonClient.getRateLimiter("rate_limit:phone-code:127.0.0.1")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(1)).thenReturn(false);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            RateLimitException exception = assertThrows(
                    RateLimitException.class,
                    () -> aspect.doRateLimit(joinPoint, method.getAnnotation(RateLimit.class)));

            assertEquals("too many requests", exception.getMessage());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    static class RateLimitTarget {

        @RateLimit(key = "'phone-code'", rate = 2, rateInterval = 60, rateIntervalUnit = RateIntervalUnit.SECONDS, includeIp = true)
        public void limitWithIp(String phone) {
        }
    }
}
