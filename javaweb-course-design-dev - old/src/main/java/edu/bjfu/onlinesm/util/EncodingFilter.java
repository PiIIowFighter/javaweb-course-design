package edu.bjfu.onlinesm.util;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class EncodingFilter implements Filter {

    private static boolean isStaticResource(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;
        if (path == null) return false;

        String lower = path.toLowerCase();
        // 你项目里静态资源一般在 /static 下；再用后缀兜底，避免资源不在 /static 时也被误伤
        if (lower.startsWith("/static/") || lower.startsWith("/assets/")) return true;

        return lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".map")
                || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".svg") || lower.endsWith(".ico")
                || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf") || lower.endsWith(".eot");
    }

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        // ✅ 静态资源放行：不要强行 setContentType，否则 CSS/JS 会被浏览器按 MIME 拒绝
        if (isStaticResource(req)) {
            chain.doFilter(request, response);
            return;
        }

        // 表单参数编码
        request.setCharacterEncoding("UTF-8");

        // 先执行后续，让 JSP/Servlet 自己设置 content-type
        chain.doFilter(request, response);

        // 对没设置 content-type 的响应再兜底
        if (response instanceof HttpServletResponse) {
            HttpServletResponse resp = (HttpServletResponse) response;
            if (!resp.isCommitted() && resp.getContentType() == null) {
                resp.setContentType("text/html; charset=UTF-8");
            }
        }
    }

    @Override
    public void destroy() { }
}
