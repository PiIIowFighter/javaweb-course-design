package edu.bjfu.onlinesm.util;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.*;

/**
 * 操作日志“兜底”过滤器：
 *  - 解决“发生操作但未写入 dbo.OperationLogs，导致管理员日志为空”的问题。
 *  - 对绝大多数“产生副作用”的请求（POST/PUT/DELETE/PATCH）在请求完成后自动记录。
 *
 * 说明：
 * 1) 现有某些 Servlet 已经手工调用 OperationLogger.log(...)；为了避免重复，
 *    OperationLogger 会在成功写入后设置 request attribute（REQ_ATTR_LOG_WRITTEN）。
 * 2) 本过滤器仅做兜底：如果该标记存在，则跳过记录。
 */
public class OperationLogFilter implements Filter {

    private static final Set<String> MUTATION_METHODS = new HashSet<>(Arrays.asList(
            "POST", "PUT", "DELETE", "PATCH"
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        StatusCaptureResponseWrapper resp = new StatusCaptureResponseWrapper((HttpServletResponse) response);

        // 静态资源不记录
        if (isStaticResource(req)) {
            chain.doFilter(request, resp);
            return;
        }

        String method = safeUpper(req.getMethod());
        boolean candidate = MUTATION_METHODS.contains(method);

        // 先执行主流程
        chain.doFilter(request, resp);

        // 仅兜底记录“可能产生副作用”的请求
        if (!candidate) {
            return;
        }

        // 过滤器兜底日志去重：如果业务代码已经记录过日志，则跳过
        Object written = req.getAttribute(OperationLogger.REQ_ATTR_LOG_WRITTEN);
        if (Boolean.TRUE.equals(written)) {
            return;
        }

        // 仅记录成功或重定向（Post-Redirect-Get）
        int status = resp.getStatus();
        if (!(status >= 200 && status < 400)) {
            return;
        }

        String path = normalizePath(req);
        String module = resolveModule(path);
        String action = resolveAction(method, path);
        String detail = buildDetail(req, path);
        OperationLogger.log(req, module, action, detail);
    }

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void destroy() { }

    private static String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri != null && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri == null ? "" : uri;
    }

    private static String resolveModule(String path) {
        if (path == null) return "";
        // /admin/*
        if (path.startsWith("/admin/users")) return "USER";
        if (path.startsWith("/admin/permissions")) return "PERMISSION";
        if (path.startsWith("/admin/logs")) return "LOG";
        if (path.startsWith("/admin/news")) return "NEWS";
        if (path.startsWith("/admin/journals")) return "JOURNAL";
        if (path.startsWith("/admin/system")) return "SYSTEM";
        if (path.startsWith("/admin/editorial")) return "EDITORIAL";

        // 业务流程
        if (path.startsWith("/manuscripts")) return "MANUSCRIPT";
        if (path.startsWith("/auth")) return "AUTH";
        if (path.startsWith("/dashboard")) return "DASHBOARD";
        if (path.startsWith("/reviewer")) return "REVIEW";
        if (path.startsWith("/editor")) return "EDITOR";
        if (path.startsWith("/eic")) return "EIC";
        if (path.startsWith("/eoadmin")) return "EO_ADMIN";

        // 兜底：取第一个路径段
        String p = path.startsWith("/") ? path.substring(1) : path;
        int idx = p.indexOf('/');
        return (idx > 0 ? p.substring(0, idx) : p).toUpperCase(Locale.ROOT);
    }

    private static String resolveAction(String method, String path) {
        if (path == null) path = "";
        // 取最后一个 path segment，兼容 /xxx/* 的 action
        String p = path;
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        int last = p.lastIndexOf('/');
        String seg = last >= 0 ? p.substring(last + 1) : p;

        if (seg.isEmpty()) {
            return method;
        }
        return seg + " (" + method + ")";
    }

    private static String buildDetail(HttpServletRequest req, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("uri=").append(path);

        Map<String, String[]> pm = req.getParameterMap();
        if (pm == null || pm.isEmpty()) {
            return sb.toString();
        }

        // 过滤敏感字段，避免密码/令牌等进入日志
        List<String> keys = new ArrayList<>(pm.keySet());
        Collections.sort(keys);

        int appended = 0;
        for (String k : keys) {
            if (k == null) continue;
            String lower = k.toLowerCase(Locale.ROOT);
            if (lower.contains("password") || lower.contains("pwd") || lower.contains("token")) {
                continue;
            }
            String[] vs = pm.get(k);
            if (vs == null) continue;

            if (appended == 0) sb.append(", params={");
            if (appended > 0) sb.append(", ");
            sb.append(k).append("=");

            sb.append(joinAndTrim(vs, 120));
            appended++;
            if (appended >= 20) {
                sb.append(", ...");
                break;
            }
        }

        if (appended > 0) sb.append("}");
        return sb.toString();
    }

    private static String joinAndTrim(String[] vs, int maxLenPerValue) {
        if (vs == null || vs.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vs.length; i++) {
            if (i > 0) sb.append("|");
            String v = vs[i] == null ? "" : vs[i];
            v = v.replaceAll("\\s+", " ").trim();
            if (v.length() > maxLenPerValue) {
                v = v.substring(0, maxLenPerValue) + "...";
            }
            sb.append(v);
        }
        return sb.toString();
    }

    private static boolean isStaticResource(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        String path = (ctx != null && !ctx.isEmpty() && uri != null && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
        if (path == null) return false;

        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/static/") || lower.startsWith("/assets/")) return true;

        return lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".map")
                || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".svg") || lower.endsWith(".ico")
                || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") || lower.endsWith(".eot");
    }

    /**
     * 捕获 response status（兼容 sendRedirect / sendError / setStatus）。
     */
    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {

        private int status = HttpServletResponse.SC_OK;

        StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.status = HttpServletResponse.SC_FOUND;
            super.sendRedirect(location);
        }

        @Override
        public int getStatus() {
            return this.status;
        }
    }
}
