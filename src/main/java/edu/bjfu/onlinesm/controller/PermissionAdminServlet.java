package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.PermissionDAO;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.OperationLogger;
import edu.bjfu.onlinesm.util.PermissionCatalog;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限管理控制器：为不同角色分配系统访问权限。
 *
 * URL:
 *  GET  /admin/permissions/list?roleCode=SYSTEM_ADMIN
 *  POST /admin/permissions/save
 */
@WebServlet(name = "PermissionAdminServlet", urlPatterns = {"/admin/permissions/*"})
public class PermissionAdminServlet extends HttpServlet {

    private final PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            handleList(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/save".equals(path)) {
            handleSave(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String roleCode = safe(req.getParameter("roleCode"));
        if (roleCode.isEmpty()) roleCode = "SYSTEM_ADMIN";
        roleCode = roleCode.toUpperCase();

        // 约束：超级管理员角色拥有最高权限，不能被任何人修改。
        boolean readOnly = "SUPER_ADMIN".equalsIgnoreCase(roleCode);

        try {
            List<String> roles = permissionDAO.findAllRoles();
            Set<String> current;
            if (readOnly) {
                // SUPER_ADMIN 默认拥有全部权限；为了前端展示，视为全部勾选。
                current = new HashSet<>();
                for (PermissionCatalog.Item it : PermissionCatalog.all()) {
                    current.add(it.getKey());
                }
            } else {
                current = permissionDAO.findPermissionsByRole(roleCode);
            }

            // JSP EL 兼容性：避免使用 "contains" 运算符，统一用 Map 取值。
            Map<String, Boolean> assignedMap = new HashMap<>();
            for (PermissionCatalog.Item it : PermissionCatalog.all()) {
                assignedMap.put(it.getKey(), current.contains(it.getKey()));
            }
            req.setAttribute("roles", roles);
            req.setAttribute("roleCode", roleCode);
            req.setAttribute("assignedMap", assignedMap);
            req.setAttribute("allPermissions", PermissionCatalog.all());
            req.setAttribute("readOnly", readOnly);
            req.setAttribute("msg", safe(req.getParameter("msg")));
            req.getRequestDispatcher("/WEB-INF/jsp/admin/permission/permission_list.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("读取权限数据失败", e);
        }
    }

    private void handleSave(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String roleCode = safe(req.getParameter("roleCode")).toUpperCase();

        // 约束：SUPER_ADMIN 为最高权限角色，任何人都不能修改其权限。
        if ("SUPER_ADMIN".equalsIgnoreCase(roleCode)) {
            resp.sendRedirect(req.getContextPath() + "/admin/permissions/list?roleCode=" + roleCode + "&msg=SUPER_ADMIN_READONLY");
            return;
        }

        String[] perms = req.getParameterValues("perm");

        Set<String> set = new HashSet<>();
        if (perms != null) {
            for (String p : perms) {
                if (p != null && !p.trim().isEmpty()) set.add(p.trim());
            }
        }

        try {
            permissionDAO.setPermissionsForRole(roleCode, set);
            OperationLogger.log(req, "PERMISSION", "更新角色权限", "roleCode=" + roleCode + ", perms=" + set);
            resp.sendRedirect(req.getContextPath() + "/admin/permissions/list?roleCode=" + roleCode);
        } catch (SQLException e) {
            throw new ServletException("保存权限失败", e);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
