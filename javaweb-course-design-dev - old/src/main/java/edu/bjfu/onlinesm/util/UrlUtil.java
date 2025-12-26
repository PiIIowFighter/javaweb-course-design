package edu.bjfu.onlinesm.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 生成系统内可在邮件中点击的绝对 URL。
 */
public class UrlUtil {

    public static String baseUrl(HttpServletRequest req) {
        if (req == null) return null;
        String scheme = req.getScheme();
        String server = req.getServerName();
        int port = req.getServerPort();
        String context = req.getContextPath();

        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(server);
        if (!defaultPort) {
            sb.append(":").append(port);
        }
        if (context != null) {
            sb.append(context);
        }
        return sb.toString();
    }

    public static String abs(HttpServletRequest req, String pathAndQuery) {
        String base = baseUrl(req);
        if (base == null) return pathAndQuery;
        if (pathAndQuery == null) return base;
        if (pathAndQuery.startsWith("http://") || pathAndQuery.startsWith("https://")) {
            return pathAndQuery;
        }
        if (!pathAndQuery.startsWith("/")) {
            return base + "/" + pathAndQuery;
        }
        return base + pathAndQuery;
    }
}
