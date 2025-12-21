package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 后台 /admin/* 统一鉴权过滤器：
 *  1) 未登录 -> 跳转登录页；
 *  2) 已登录 -> 根据 URL 判断所需权限点；
 *  3) 无权限 -> 403 并展示 access_denied.jsp。
 */
@WebFilter(filterName = "AdminAuthzFilter", urlPatterns = {"/admin/*"})
public class AdminAuthzFilter implements Filter {

    private final PermissionService permissionService = new PermissionService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String roleCode = current.getRoleCode() == null ? "" : current.getRoleCode();
        String path = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        String required = requiredPermission(path);
        if (required == null) {
            // 未配置的 /admin 路由默认允许（避免误伤课程设计其它管理页面）
            chain.doFilter(request, response);
            return;
        }

        if (!permissionService.hasPermission(roleCode, required)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("error", "当前账号无权限访问该模块：" + required);
            req.getRequestDispatcher("/WEB-INF/jsp/error/access_denied.jsp").forward(req, resp);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 根据 URL 前缀映射到权限点。
     */
    private String requiredPermission(String path) {
        if (path == null) return null;
        // 关键模块
        if (path.startsWith("/admin/users")) return PermissionCatalog.ADMIN_USERS;
        if (path.startsWith("/admin/permissions")) return PermissionCatalog.ADMIN_PERMISSIONS;
        if (path.startsWith("/admin/logs")) return PermissionCatalog.ADMIN_LOGS;
        if (path.startsWith("/admin/system")) return PermissionCatalog.ADMIN_SYSTEM;
        // 现有模块（保持一致的权限控制）
        if (path.startsWith("/admin/journals")) return PermissionCatalog.ADMIN_JOURNALS;
        if (path.startsWith("/admin/editorial")) return PermissionCatalog.ADMIN_EDITORIAL;
        if (path.startsWith("/admin/news")) return PermissionCatalog.ADMIN_NEWS;
        return null;
    }
}
