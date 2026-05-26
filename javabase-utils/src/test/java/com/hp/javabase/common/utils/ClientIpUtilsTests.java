package com.hp.javabase.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link ClientIpUtils} 测试，覆盖常见代理头和 remoteAddr 兜底逻辑。
 */
class ClientIpUtilsTests {

    @Test
    void shouldUseFirstIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

        String clientIp = ClientIpUtils.resolveClientIp(request);

        assertEquals("203.0.113.10", clientIp);
    }

    @Test
    void shouldFallbackToXRealIpWhenForwardedForInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "198.51.100.8");

        String clientIp = ClientIpUtils.resolveClientIp(request);

        assertEquals("198.51.100.8", clientIp);
    }

    @Test
    void shouldFallbackToRemoteAddrWhenHeadersMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.20");

        String clientIp = ClientIpUtils.resolveClientIp(request);

        assertEquals("192.168.1.20", clientIp);
    }

    @Test
    void shouldNormalizeIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");

        String clientIp = ClientIpUtils.resolveClientIp(request);

        assertEquals("127.0.0.1", clientIp);
    }
}
