package edu.bjfu.journal.util;

import javax.servlet.http.HttpServletRequest;

public final class IpUtil {
    private IpUtil() {}
    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && xff.length() > 0 && !"unknown".equalsIgnoreCase(xff)) {
            int idx = xff.indexOf(',');
            return idx > 0 ? xff.substring(0, idx).trim() : xff.trim();
        }
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) return ip.trim();
        return request.getRemoteAddr();
    }
}
