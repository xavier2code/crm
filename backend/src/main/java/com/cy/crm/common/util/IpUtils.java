package com.cy.crm.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址工具类
 * 统一处理反向代理场景下的客户端 IP 获取
 */
public final class IpUtils {

    private IpUtils() {
        // 工具类禁止实例化
    }

    /**
     * 获取客户端真实 IP 地址
     *
     * <p>优先从常见代理头获取，处理 X-Forwarded-For 多 IP 情况（取第一个）。
     * 如果所有代理头都未提供或值为 unknown，则回退到 request.getRemoteAddr()。
     *
     * @param request HTTP 请求
     * @return 客户端 IP，如果 request 为 null 则返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isEmptyIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmptyIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmptyIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    private static boolean isEmptyIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
