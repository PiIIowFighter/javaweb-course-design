package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 统一“菜单入口权限”过滤器：
 *  1) 未登录 -> 跳转登录页；
 *  2) 已登录 -> 根据 URL 判断所需入口权限；
 *  3) 无权限 -> 403 并展示 access_denied.jsp。
 *
 * 说明：
 *  - 按“菜单入口”粒度做权限点；
 *  - 允许跨角色授予入口：只要有入口权限，就允许访问功能页面；
 *  - 默认权限在首次登录时已初始化（见 MenuPermissionService）。
 */
@WebFilter(filterName = "MenuAuthzFilter", urlPatterns = {"/admin/*", "/editor/*", "/reviewer/*", "/manuscripts/*"})
public class AdminAuthzFilter implements Filter {

    private final MenuPermissionService menuPermissionService = new MenuPermissionService();

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

        // 确保 session 中已有菜单权限缓存（允许用户直接输入 URL）
        if (session.getAttribute(MenuPermissionService.SESSION_MENU_PERMS) == null) {
            menuPermissionService.loadIntoSession(session, current);
        }

        String path = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        String required = requiredPermission(path);
        if (required == null) {
            chain.doFilter(request, response);
            return;
        }

        if (!menuPermissionService.hasPermission(session, current, required)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("error", "当前账号无权限访问该功能入口：" + required);
            req.getRequestDispatcher("/WEB-INF/jsp/error/access_denied.jsp").forward(req, resp);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 根据 URL 前缀映射到入口权限点。
     */
    private String requiredPermission(String path) {
        if (path == null) return null;

        // ====== Admin ======
        if (path.startsWith("/admin/users")) return PermissionCatalog.ADMIN_USERS;
        if (path.startsWith("/admin/permissions")) return PermissionCatalog.ADMIN_PERMISSIONS;
        if (path.startsWith("/admin/logs")) return PermissionCatalog.ADMIN_LOGS;
        if (path.startsWith("/admin/system/db")) return PermissionCatalog.ADMIN_DB_MAINTENANCE;
        if (path.startsWith("/admin/system")) return PermissionCatalog.ADMIN_SYSTEM;
        if (path.startsWith("/admin/journals")) return PermissionCatalog.ADMIN_JOURNALS;
        if (path.startsWith("/admin/editorial")) return PermissionCatalog.ADMIN_EDITORIAL;
        if (path.startsWith("/admin/news")) return PermissionCatalog.ADMIN_NEWS;

        // ====== Author ======
        if (path.startsWith("/manuscripts/submit")) return PermissionCatalog.MENU_AUTHOR_SUBMIT;
        if (path.startsWith("/manuscripts")) return PermissionCatalog.MENU_AUTHOR_MY_MANUSCRIPTS;

        // ====== Reviewer ======
        if (path.startsWith("/reviewer/history")) return PermissionCatalog.MENU_REVIEWER_HISTORY;
        if (path.startsWith("/reviewer")) return PermissionCatalog.MENU_REVIEWER_ASSIGNED;

        // ====== Editor / EIC / EO_ADMIN (all under /editor/*) ======
        if (path.startsWith("/editor/formalCheck/history")) return PermissionCatalog.MENU_EO_FORMAL_HISTORY;
        if (path.startsWith("/editor/formalCheck")) return PermissionCatalog.MENU_EO_FORMAL_CHECK;

        if (path.startsWith("/editor/overview")) return PermissionCatalog.MENU_EIC_OVERVIEW;
        if (path.startsWith("/editor/desk")) return PermissionCatalog.MENU_EIC_DESK;
        if (path.startsWith("/editor/toAssign")) return PermissionCatalog.MENU_EIC_TO_ASSIGN;
        if (path.startsWith("/editor/reviewers")) return PermissionCatalog.MENU_EIC_REVIEWERS;
        if (path.startsWith("/editor/finalDecision")) return PermissionCatalog.MENU_EIC_FINAL_DECISION;
        if (path.startsWith("/editor/special")) return PermissionCatalog.MENU_EIC_SPECIAL;

        if (path.startsWith("/editor/withEditor")) return PermissionCatalog.MENU_EDITOR_TODO;
        if (path.startsWith("/editor/underReview")) return PermissionCatalog.MENU_EDITOR_UNDER_REVIEW;
        if (path.startsWith("/editor/recommend")) return PermissionCatalog.MENU_EDITOR_RECOMMEND;
        if (path.startsWith("/editor/review/invite") || path.startsWith("/editor/review/inviteExternal") || path.startsWith("/editor/review/cancel")) return PermissionCatalog.MENU_EDITOR_TODO;
        if (path.startsWith("/editor/review/remind") || path.startsWith("/editor/review/remindCustom") || path.startsWith("/editor/review/autoRemindNow") || path.startsWith("/editor/review/monitor")) return PermissionCatalog.MENU_EDITOR_REVIEW_MONITOR;
        if (path.startsWith("/editor/author/message") || path.startsWith("/editor/authorComm")) return PermissionCatalog.MENU_EDITOR_AUTHOR_COMM;

        // 未覆盖的 /editor 子路径默认不拦截（避免误伤其他内部跳转）
        return null;
    }
}
