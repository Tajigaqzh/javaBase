package com.hp.javabase.common.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * {@link ClientIpUtils} 单元测试，覆盖代理头解析、内网判断和 IP 过滤匹配能力。
 */
class ClientIpUtilsTests {

    @Test
    void resolveClientIpShouldUseFirstValidForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown, 10.0.0.1, 8.8.8.8");
        request.setRemoteAddr("127.0.0.2");

        String clientIp = ClientIpUtils.resolveClientIp(request);

        assertEquals("10.0.0.1", clientIp);
    }

    @Test
    void resolveClientIpShouldNormalizeIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String clientIp = ClientIpUtils.resolveClientIp(request);

        assertEquals("127.0.0.1", clientIp);
    }

    @Test
    void internalIpShouldRecognizePrivateAndPublicAddresses() {
        assertTrue(ClientIpUtils.internalIp("10.1.2.3"));
        assertTrue(ClientIpUtils.internalIp("172.16.0.1"));
        assertTrue(ClientIpUtils.internalIp("192.168.1.10"));
        assertTrue(ClientIpUtils.internalIp("127.0.0.1"));
        assertFalse(ClientIpUtils.internalIp("8.8.8.8"));
    }

    @Test
    void textToNumericFormatV4ShouldConvertIpv4Text() {
        byte[] address = ClientIpUtils.textToNumericFormatV4("192.168.0.1");

        assertArrayEquals(new byte[] {(byte) 192, (byte) 168, 0, 1}, address);
    }

    @Test
    void getMultistageReverseProxyIpShouldReturnFirstKnownIp() {
        assertEquals("192.168.0.1",
                ClientIpUtils.getMultistageReverseProxyIp("unknown, 192.168.0.1, 10.0.0.1"));
    }

    @Test
    void isMatchedIpShouldSupportExactWildcardAndSegment() {
        assertTrue(ClientIpUtils.isMatchedIp("192.168.1.10", "192.168.1.10"));
        assertTrue(ClientIpUtils.isMatchedIp("10.20.*.*", "10.20.3.9"));
        assertTrue(ClientIpUtils.isMatchedIp("172.16.1.10-172.16.1.20", "172.16.1.15"));
        assertFalse(ClientIpUtils.isMatchedIp("172.16.1.10-172.16.1.20", "172.16.1.21"));
    }

    @Test
    void hostInfoShouldReturnFallbackOrResolvedValue() {
        assertNotNull(ClientIpUtils.getHostIp());
        assertFalse(ClientIpUtils.getHostIp().isBlank());
        assertNotNull(ClientIpUtils.getHostName());
        assertFalse(ClientIpUtils.getHostName().isBlank());
    }
}
