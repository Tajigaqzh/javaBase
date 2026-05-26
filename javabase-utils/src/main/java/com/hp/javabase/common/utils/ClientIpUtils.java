package com.hp.javabase.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 客户端 IP 解析工具，统一处理代理场景下的来源 IP 提取。
 *
 * <p>当前解析顺序优先考虑常见反向代理头，例如：
 * 1. X-Forwarded-For
 * 2. X-Real-IP
 * 3. Proxy-Client-IP
 * 4. WL-Proxy-Client-IP
 *
 * <p>注意：任何请求头都可能被伪造，因此该工具只能提升“识别准确率”，
 * 不能单独作为安全信任边界。生产环境仍应要求网关或反向代理统一清洗并写入可信头。
 */
public final class ClientIpUtils {

    private static final List<String> IP_HEADER_CANDIDATES = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_REAL_IP");

    private ClientIpUtils() {
    }

    /**
     * 从请求中解析客户端 IP。
     *
     * <p>解析规则：
     * 1. 依次尝试常见代理头；
     * 2. 对 X-Forwarded-For 这类多值头，取第一个非空且非 unknown 的地址；
     * 3. 兜底使用 {@link HttpServletRequest#getRemoteAddr()}；
     * 4. 将 IPv6 本地回环地址归一化为 127.0.0.1。
     *
     * @param request HTTP 请求对象
     * @return 解析后的客户端 IP；若无法解析，则返回 unknown
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        for (String headerName : IP_HEADER_CANDIDATES) {
            String headerValue = request.getHeader(headerName);
            String candidateIp = extractFirstValidIp(headerValue);
            if (candidateIp != null) {
                return normalizeLoopbackIp(candidateIp);
            }
        }
        return normalizeLoopbackIp(request.getRemoteAddr());
    }

    private static String extractFirstValidIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] ipList = headerValue.split(",");
        for (String ip : ipList) {
            String candidate = ip.trim();
            if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalizeLoopbackIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
