package com.hp.javabase.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 客户端 IP 工具，统一处理代理场景下的来源 IP 提取、内网判断和 IP 过滤匹配。
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

    public static final String REGX_0_255 = "(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)";

    public static final String REGX_IP = "((" + REGX_0_255 + "\\.){3}" + REGX_0_255 + ")";

    public static final String REGX_IP_WILDCARD = "(((\\*\\.){3}\\*)|("
            + REGX_0_255 + "(\\.\\*){3})|("
            + REGX_0_255 + "\\." + REGX_0_255 + ")(\\.\\*){2}"
            + "|((" + REGX_0_255 + "\\.){3}\\*))";

    public static final String REGX_IP_SEG = "(" + REGX_IP + "\\-" + REGX_IP + ")";

    private static final List<String> IP_HEADER_CANDIDATES = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_REAL_IP",
            "x-forwarded-for");

    private static final Pattern IP_PATTERN = Pattern.compile(REGX_IP);

    private static final Pattern IP_WILDCARD_PATTERN = Pattern.compile(REGX_IP_WILDCARD);

    private static final Pattern IP_SEG_PATTERN = Pattern.compile(REGX_IP_SEG);

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

    /**
     * 判断指定 IP 是否属于内网地址。
     *
     * @param ip IP 地址
     * @return 是否为内网地址
     */
    public static boolean internalIp(String ip) {
        byte[] address = textToNumericFormatV4(ip);
        return internalIp(address) || "127.0.0.1".equals(ip);
    }

    /**
     * 获取当前主机 IP。
     *
     * @return 当前主机 IP，解析失败时返回 127.0.0.1
     */
    public static String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException exception) {
            return "127.0.0.1";
        }
    }

    /**
     * 获取当前主机名。
     *
     * @return 当前主机名，解析失败时返回 未知
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "未知";
        }
    }

    /**
     * 从多级反向代理头中获取第一个非 unknown 的 IP。
     *
     * @param ip 原始 IP 字符串
     * @return 截断后的首个有效 IP
     */
    public static String getMultistageReverseProxyIp(String ip) {
        if (ip != null && ip.contains(",")) {
            String[] ipArray = ip.trim().split(",");
            for (String subIp : ipArray) {
                if (!isUnknown(subIp)) {
                    ip = subIp.trim();
                    break;
                }
            }
        }
        return substring(ip, 0, 255);
    }

    /**
     * 判断字符串是否为空或 unknown。
     *
     * @param value 待判断字符串
     * @return 是否未知
     */
    public static boolean isUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value);
    }

    /**
     * 判断是否为标准 IPv4。
     *
     * @param ip 待判断 IP
     * @return 是否为标准 IPv4
     */
    public static boolean isIP(String ip) {
        return ip != null && !ip.isBlank() && IP_PATTERN.matcher(ip).matches();
    }

    /**
     * 判断是否为带 * 通配符的 IP 规则。
     *
     * @param ip 待判断规则
     * @return 是否为 IP 通配符规则
     */
    public static boolean isIpWildCard(String ip) {
        return ip != null && !ip.isBlank() && IP_WILDCARD_PATTERN.matcher(ip).matches();
    }

    /**
     * 判断 IP 是否命中通配符规则。
     *
     * @param ipWildCard 通配符规则
     * @param ip 待判断 IP
     * @return 是否命中
     */
    public static boolean ipIsInWildCardNoCheck(String ipWildCard, String ip) {
        String[] wildcardParts = ipWildCard.split("\\.");
        String[] ipParts = ip.split("\\.");
        boolean matched = true;
        for (int index = 0; index < wildcardParts.length && !"*".equals(wildcardParts[index]); index++) {
            if (!wildcardParts[index].equals(ipParts[index])) {
                matched = false;
                break;
            }
        }
        return matched;
    }

    /**
     * 判断是否为形如 10.10.10.1-10.10.10.99 的 IP 网段规则。
     *
     * @param ipSeg 网段规则
     * @return 是否为有效网段规则
     */
    public static boolean isIPSegment(String ipSeg) {
        return ipSeg != null && !ipSeg.isBlank() && IP_SEG_PATTERN.matcher(ipSeg).matches();
    }

    /**
     * 判断 IP 是否位于指定网段内。
     *
     * @param ipArea 网段规则
     * @param ip 待判断 IP
     * @return 是否位于网段内
     */
    public static boolean ipIsInNetNoCheck(String ipArea, String ip) {
        int splitIndex = ipArea.indexOf('-');
        String[] startIp = ipArea.substring(0, splitIndex).split("\\.");
        String[] endIp = ipArea.substring(splitIndex + 1).split("\\.");
        String[] targetIp = ip.split("\\.");
        long start = 0L;
        long end = 0L;
        long target = 0L;
        for (int index = 0; index < 4; index++) {
            start = start << 8 | Integer.parseInt(startIp[index]);
            end = end << 8 | Integer.parseInt(endIp[index]);
            target = target << 8 | Integer.parseInt(targetIp[index]);
        }
        if (start > end) {
            long temp = start;
            start = end;
            end = temp;
        }
        return start <= target && target <= end;
    }

    /**
     * 判断 IP 是否命中过滤规则，支持精确 IP、* 通配符和网段。
     *
     * @param filter 过滤规则，多个规则使用 ; 分隔
     * @param ip 待判断 IP
     * @return 是否命中
     */
    public static boolean isMatchedIp(String filter, String ip) {
        if (filter == null || filter.isEmpty() || ip == null || ip.isEmpty()) {
            return false;
        }
        String[] ruleArray = filter.split(";");
        for (String rule : ruleArray) {
            if (isIP(rule) && rule.equals(ip)) {
                return true;
            }
            if (isIpWildCard(rule) && ipIsInWildCardNoCheck(rule, ip)) {
                return true;
            }
            if (isIPSegment(rule) && ipIsInNetNoCheck(rule, ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将 IPv4 文本转换为字节数组。
     *
     * @param text IPv4 文本
     * @return 字节数组；格式非法时返回 null
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try {
            long value;
            int index;
            switch (elements.length) {
                case 1:
                    value = Long.parseLong(elements[0]);
                    if (value < 0L || value > 4294967295L) {
                        return null;
                    }
                    bytes[0] = (byte) (value >> 24 & 0xFF);
                    bytes[1] = (byte) ((value & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) ((value & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (value & 0xFF);
                    break;
                case 2:
                    value = Integer.parseInt(elements[0]);
                    if (value < 0L || value > 255L) {
                        return null;
                    }
                    bytes[0] = (byte) (value & 0xFF);
                    value = Integer.parseInt(elements[1]);
                    if (value < 0L || value > 16777215L) {
                        return null;
                    }
                    bytes[1] = (byte) (value >> 16 & 0xFF);
                    bytes[2] = (byte) ((value & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (value & 0xFF);
                    break;
                case 3:
                    for (index = 0; index < 2; index++) {
                        value = Integer.parseInt(elements[index]);
                        if (value < 0L || value > 255L) {
                            return null;
                        }
                        bytes[index] = (byte) (value & 0xFF);
                    }
                    value = Integer.parseInt(elements[2]);
                    if (value < 0L || value > 65535L) {
                        return null;
                    }
                    bytes[2] = (byte) (value >> 8 & 0xFF);
                    bytes[3] = (byte) (value & 0xFF);
                    break;
                case 4:
                    for (index = 0; index < 4; index++) {
                        value = Integer.parseInt(elements[index]);
                        if (value < 0L || value > 255L) {
                            return null;
                        }
                        bytes[index] = (byte) (value & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        return bytes;
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

    private static boolean internalIp(byte[] address) {
        if (address == null || address.length < 2) {
            return true;
        }
        final byte first = address[0];
        final byte second = address[1];
        final byte section1 = 0x0A;
        final byte section2 = (byte) 0xAC;
        final byte section3 = (byte) 0x10;
        final byte section4 = (byte) 0x1F;
        final byte section5 = (byte) 0xC0;
        final byte section6 = (byte) 0xA8;
        switch (first) {
            case section1:
                return true;
            case section2:
                if (second >= section3 && second <= section4) {
                    return true;
                }
            case section5:
                return second == section6;
            default:
                return false;
        }
    }

    private static String substring(String value, int start, int end) {
        if (value == null) {
            return null;
        }
        int actualStart = Math.max(start, 0);
        int actualEnd = Math.min(end, value.length());
        if (actualStart >= actualEnd) {
            return "";
        }
        return value.substring(actualStart, actualEnd);
    }
}
