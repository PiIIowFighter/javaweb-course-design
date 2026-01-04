package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 统一的“菜单入口权限”校验工具。
 * 用于 Filter / Servlet 中快速判断：只要用户拥有入口权限，就允许访问功能页面。
 */
public final class MenuPermissionGuard {

    private static final MenuPermissionService menuPermissionService = new MenuPermissionService();

    private MenuPermissionGuard() {
    }

    public static boolean require(HttpServletRequest req, HttpServletResponse resp, String permKey)
            throws IOException, ServletException {

        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }

        // SESSION 里没有权限时，兜底加载一次（避免“直接访问 URL”时菜单还没初始化）
        if (session.getAttribute(MenuPermissionService.SESSION_MENU_PERMS) == null) {
            menuPermissionService.loadIntoSession(session, current);
        }

        if (!menuPermissionService.hasPermission(session, current, permKey)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("error", "当前账号无权限访问该功能入口：" + permKey);
            req.getRequestDispatcher("/WEB-INF/jsp/error/access_denied.jsp").forward(req, resp);
            return false;
        }
        return true;
    }

    public static boolean has(HttpServletRequest req, String permKey) {
        if (permKey == null || permKey.trim().isEmpty()) return false;
        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) return false;

        if (session.getAttribute(MenuPermissionService.SESSION_MENU_PERMS) == null) {
            menuPermissionService.loadIntoSession(session, current);
        }

        return menuPermissionService.hasPermission(session, current, permKey);
    }
}
