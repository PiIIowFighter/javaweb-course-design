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


@WebServlet(name = "PermissionAdminServlet", urlPatterns = {"/admin/permissions/*"})
public class PermissionAdminServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final MenuPermissionDAO menuPermissionDAO = new MenuPermissionDAO();
    private final MenuPermissionService menuPermissionService = new MenuPermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        
        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        
        if (!MenuPermissionGuard.has(req, PermissionCatalog.ADMIN_PERMISSIONS)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限访问权限管理。");
            return;
        }

        
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) path = "/list";

        if (!"/list".equals(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            
            List<User> users = userDAO.findSelectableUsers();
            req.setAttribute("users", users);

            
            int selectedUserId = parseIntOrDefault(req.getParameter("userId"),
                    users != null && !users.isEmpty() ? users.get(0).getUserId() : current.getUserId());

            User selectedUser = userDAO.findById(selectedUserId);
            if (selectedUser == null && users != null && !users.isEmpty()) {
                selectedUserId = users.get(0).getUserId();
                selectedUser = userDAO.findById(selectedUserId);
            }
            req.setAttribute("selectedUser", selectedUser);

            
            Set<String> assigned = new HashSet<>();
            Set<String> lockedKeys = new HashSet<>();
            req.setAttribute("readOnly", false); 

            if (selectedUser != null) {
                String role = selectedUser.getRoleCode();

                
                assigned = menuPermissionDAO.findPermissionsByUser(selectedUser.getUserId());

                
                if (assigned == null || assigned.isEmpty()) {
                    assigned = menuPermissionService.defaultMenuPermissions(role);
                    menuPermissionDAO.addPermissionsForUser(selectedUser.getUserId(), assigned);
                }

                
                
                
                if (role != null && "SUPER_ADMIN".equalsIgnoreCase(role)) {
                    assigned = menuPermissionService.defaultMenuPermissions(role);
                    lockedKeys.addAll(menuPermissionService.allMenuPermissionKeys());
                    req.setAttribute("readOnly", true);
                }
            }

            req.setAttribute("assigned", assigned);
            java.util.Map<String, Boolean> assignedMap = new java.util.HashMap<>();
            for (String k : assigned) { assignedMap.put(k, Boolean.TRUE); }
            req.setAttribute("assignedMap", assignedMap);
            java.util.Map<String, Boolean> lockedMap = new java.util.HashMap<>();
            for (String k : lockedKeys) { lockedMap.put(k, Boolean.TRUE); }
            req.setAttribute("lockedMap", lockedMap);
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

        if (!MenuPermissionGuard.has(req, PermissionCatalog.ADMIN_PERMISSIONS)) {
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


                        
            if (target.getRoleCode() != null && "SUPER_ADMIN".equalsIgnoreCase(target.getRoleCode())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "SUPER_ADMIN 不允许修改入口权限。");
                return;
            }

String[] keys = req.getParameterValues("permissions");
            Set<String> newSet = new HashSet<>();
            if (keys != null) {
                for (String k : keys) {
                    if (k != null && !k.trim().isEmpty()) newSet.add(k.trim());
                }
            }

            
            if (target.getRoleCode() != null && "SUPER_ADMIN".equalsIgnoreCase(target.getRoleCode())) {
                newSet.add(PermissionCatalog.ADMIN_PERMISSIONS);
            }

            menuPermissionDAO.setPermissionsForUser(userId, newSet);

            
            if (current.getUserId() != null && current.getUserId() == userId) {
                menuPermissionService.loadIntoSession(session, current);
            }

            
            
            if (current.getUserId() != null && current.getUserId() == userId) {
                session.removeAttribute(MenuPermissionService.SESSION_MENU_PERMS);
                session.removeAttribute(MenuPermissionService.SESSION_MENU_PERM_MAP);
                menuPermissionService.loadIntoSession(session, current);
            }

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

/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

