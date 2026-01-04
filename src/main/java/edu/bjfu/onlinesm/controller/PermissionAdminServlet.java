package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.MenuPermissionDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.MenuPermissionGuard;
import edu.bjfu.onlinesm.util.MenuPermissionService;
import edu.bjfu.onlinesm.util.PermissionCatalog;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限管理（按“菜单入口”粒度，用户级）。
 *
 * 说明：
 *  - 超级管理员/系统管理员（或被授予 ADMIN_PERMISSIONS 的用户）可为任意用户勾选入口权限；
 *  - 入口权限决定：工作台/侧边栏显示 + 功能页面访问权限（跨角色授予）。
 */
@WebServlet(name = "PermissionAdminServlet", urlPatterns = {"/admin/permissions/*"})
public class PermissionAdminServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final MenuPermissionDAO menuPermissionDAO = new MenuPermissionDAO();
    private final MenuPermissionService menuPermissionService = new MenuPermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 仅已登录
        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // 权限：必须拥有“权限管理”入口
        if (!MenuPermissionGuard.has(req, PermissionCatalog.ADMIN_PERMISSIONS)
                && !"SUPER_ADMIN".equals(current.getRoleCode())
                && !"SYSTEM_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限访问权限管理。");
            return;
        }

        // 默认 /list
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) path = "/list";

        if (!"/list".equals(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            // 用户下拉列表
            List<User> users = userDAO.findSelectableUsers();
            req.setAttribute("users", users);

            // 选择目标用户
            int selectedUserId = parseIntOrDefault(req.getParameter("userId"),
                    users != null && !users.isEmpty() ? users.get(0).getUserId() : current.getUserId());

            User selectedUser = userDAO.findById(selectedUserId);
            if (selectedUser == null && users != null && !users.isEmpty()) {
                selectedUserId = users.get(0).getUserId();
                selectedUser = userDAO.findById(selectedUserId);
            }
            req.setAttribute("selectedUser", selectedUser);

            // 已勾选权限
            Set<String> assigned = new HashSet<>();
            if (selectedUser != null) {
                String role = selectedUser.getRoleCode();
                if ("SUPER_ADMIN".equals(role)) {
                    assigned = menuPermissionService.allMenuPermissionKeys();
                    req.setAttribute("readOnly", true);
                } else {
                    assigned = menuPermissionDAO.findPermissionsByUser(selectedUser.getUserId());
                    // 若无记录：首次初始化默认入口（与旧侧边栏一致）
                    if (assigned == null || assigned.isEmpty()) {
                        assigned = menuPermissionService.defaultMenuPermissions(role);
                        menuPermissionDAO.addPermissionsForUser(selectedUser.getUserId(), assigned);
                    }
                    req.setAttribute("readOnly", false);
                }
            }

            req.setAttribute("assigned", assigned);
            java.util.Map<String, Boolean> assignedMap = new java.util.HashMap<>();
            for (String k : assigned) { assignedMap.put(k, Boolean.TRUE); }
            req.setAttribute("assignedMap", assignedMap);
            req.setAttribute("permissions", PermissionCatalog.all());
            req.setAttribute("success", req.getParameter("success"));

            req.getRequestDispatcher("/WEB-INF/jsp/admin/permission/permission_list.jsp").forward(req, resp);

        } catch (SQLException e) {
            throw new ServletException("加载权限管理数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        if (!MenuPermissionGuard.has(req, PermissionCatalog.ADMIN_PERMISSIONS)
                && !"SUPER_ADMIN".equals(current.getRoleCode())
                && !"SYSTEM_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限访问权限管理。");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) path = "/save";

        if (!"/save".equals(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        int userId = parseIntOrDefault(req.getParameter("userId"), -1);
        if (userId <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 userId");
            return;
        }

        try {
            User target = userDAO.findById(userId);
            if (target == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "目标用户不存在");
                return;
            }

            if ("SUPER_ADMIN".equals(target.getRoleCode())) {
                // 超级管理员固定拥有全部入口，不允许修改
                // 不在 URL 参数中携带中文，避免出现“????”编码问题
                resp.sendRedirect(req.getContextPath() + "/admin/permissions/list?userId=" + userId + "&success=2");
                return;
            }

            String[] keys = req.getParameterValues("permissions");
            Set<String> newSet = new HashSet<>();
            if (keys != null) {
                for (String k : keys) {
                    if (k != null && !k.trim().isEmpty()) newSet.add(k.trim());
                }
            }

            menuPermissionDAO.setPermissionsForUser(userId, newSet);

            // 如果改的是当前用户 -> 立即刷新 session 中的菜单权限，保证侧边栏/工作台即时生效
            if (current.getUserId() != null && current.getUserId() == userId) {
                menuPermissionService.loadIntoSession(session, current);
            }

            // success=1 表示保存成功（避免 URL 中文编码导致页面显示 ????）
            resp.sendRedirect(req.getContextPath() + "/admin/permissions/list?userId=" + userId + "&success=1");
        } catch (SQLException e) {
            throw new ServletException("保存权限失败: " + e.getMessage(), e);
        }
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
