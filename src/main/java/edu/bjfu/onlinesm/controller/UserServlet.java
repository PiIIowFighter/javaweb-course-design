package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.RoleDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.OperationLogger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 用户管理控制器：
 *  - 用户列表；
 *  - 管理员创建各类后台用户（除 SUPER_ADMIN 自身外）；
 *  - 编辑 / 删除 / 封禁 / 解封；
 *  - 重置密码（简单重置为 123456）。
 *
 * 仅允许具有“用户管理”权限的后台用户访问（由 AdminAuthzFilter 控制）。
 *
 * URL 约定：
 *  GET  /admin/users/list              用户列表
 *  GET  /admin/users/edit?userId=xx     编辑用户表单
 *  GET  /admin/users/add               新增用户表单
 *
 *  POST /admin/users/status            修改状态（封禁/解封）
 *  POST /admin/users/resetPassword     重置密码
 *  POST /admin/users/add               保存新增用户
 *  POST /admin/users/update            保存编辑
 *  POST /admin/users/delete            删除用户
 */
@WebServlet(name = "UserServlet", urlPatterns = {"/admin/users/*"})
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/list";
        }

        try {
            switch (path) {
                case "/list":
                    String role = req.getParameter("roleCode");
                    List<User> users;
                    if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
                        users = userDAO.findByRoleCode(role.trim().toUpperCase());
                        req.setAttribute("selectedRole", role.trim().toUpperCase());
                    } else {
                        users = userDAO.findAll();
                        req.setAttribute("selectedRole", "ALL");
                    }
                    req.setAttribute("users", users);
                    req.setAttribute("roles", roleDAO.findAllRoleCodes());
                    req.getRequestDispatcher("/WEB-INF/jsp/admin/user/user_list.jsp").forward(req, resp);
                    break;
                case "/edit":
                    String idStr = req.getParameter("userId");
                    if (idStr == null || idStr.trim().isEmpty()) {
                        resp.sendRedirect(req.getContextPath() + "/admin/users/list");
                        return;
                    }
                    int userId = Integer.parseInt(idStr);
                    User u = userDAO.findById(userId);
                    req.setAttribute("user", u);
                    req.setAttribute("roles", roleDAO.findAllRoleCodes());
                    req.getRequestDispatcher("/WEB-INF/jsp/admin/user/user_form.jsp").forward(req, resp);
                    break;
                case "/add":
                    req.setAttribute("roles", roleDAO.findAllRoleCodes());
                    req.getRequestDispatcher("/WEB-INF/jsp/admin/user/user_add.jsp").forward(req, resp);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("查询用户列表时访问数据库出错", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/status";
        }

        try {
            switch (path) {
                case "/status":
                    handleChangeStatus(req);
                    break;
                case "/resetPassword":
                    handleResetPassword(req);
                    break;
                case "/add":
                    handleCreateUser(req);
                    break;
                case "/update":
                    handleUpdateUser(req);
                    break;
                case "/delete":
                    handleDeleteUser(req);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        } catch (SQLException e) {
            throw new ServletException("修改用户信息时访问数据库出错", e);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/users/list");
    }

    private void handleChangeStatus(HttpServletRequest req) throws SQLException {
        String userIdStr = req.getParameter("userId");
        String targetStatus = req.getParameter("targetStatus");
        if (userIdStr == null || targetStatus == null) {
            return;
        }
        int userId = Integer.parseInt(userIdStr);

        // 约束：超级管理员是最高权限用户，不能被任何人修改（包括封禁/解封）。
        User target = userDAO.findById(userId);
        if (isSuperAdmin(target)) {
            return;
        }

        userDAO.updateStatus(userId, targetStatus);
        OperationLogger.log(req, "USER", "修改用户状态", "userId=" + userId + ", status=" + targetStatus);
    }

    private void handleResetPassword(HttpServletRequest req) throws SQLException {
        String userIdStr = req.getParameter("userId");
        if (userIdStr == null) {
            return;
        }
        int userId = Integer.parseInt(userIdStr);

        // 约束：超级管理员是最高权限用户，不能被任何人修改（包括重置密码）。
        User target = userDAO.findById(userId);
        if (isSuperAdmin(target)) {
            return;
        }

        // 简单重置为 123456，实际系统中应由管理员输入或发送重置链接
        userDAO.resetPassword(userId, "123456");
        OperationLogger.log(req, "USER", "重置密码", "userId=" + userId + ", newPassword=123456");
    }
    private void handleCreateUser(HttpServletRequest req) throws SQLException {
        // 当前登录用户（用于“只有超级管理员才能创建系统管理员账号”等约束）
        User current = getCurrentUser(req);
        String currentRole = current == null ? "" : (current.getRoleCode() == null ? "" : current.getRoleCode().trim().toUpperCase());

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String roleCode = req.getParameter("roleCode");
        String status = req.getParameter("status");

        if (username == null || username.trim().isEmpty() || roleCode == null || roleCode.trim().isEmpty()) {
            // 必要字段缺失，直接返回，不做任何处理
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            password = "123456";
        }

        String normalizedRole = roleCode == null ? "" : roleCode.trim().toUpperCase();

        // 约束 1：禁止任何人创建 SUPER_ADMIN。
        if ("SUPER_ADMIN".equalsIgnoreCase(normalizedRole)) {
            return;
        }

        // 约束 2：作者/审稿人属于外部用户，一般不允许通过后台“创建”；应通过注册功能自行创建账号。
        // （课程设计演示用账号可由初始化 SQL 脚本插入）
        if ("AUTHOR".equalsIgnoreCase(normalizedRole) || "REVIEWER".equalsIgnoreCase(normalizedRole)) {
            return;
        }

        // 约束 3：只有 SUPER_ADMIN 才能创建 SYSTEM_ADMIN（系统管理员）账号。
        if ("SYSTEM_ADMIN".equalsIgnoreCase(normalizedRole) && !"SUPER_ADMIN".equalsIgnoreCase(currentRole)) {
            return;
        }

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPasswordHash(password.trim());
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setStatus(status == null || status.isEmpty() ? "ACTIVE" : status);

        userDAO.createUserWithRole(newUser, normalizedRole);
        OperationLogger.log(req, "USER", "新增用户", "username=" + newUser.getUsername() + ", role=" + normalizedRole);
    }

    private void handleUpdateUser(HttpServletRequest req) throws SQLException {
        String userIdStr = req.getParameter("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) return;

        int userId = Integer.parseInt(userIdStr);

        // 当前登录用户（用于角色提升等约束）
        User current = getCurrentUser(req);
        String currentRole = current == null ? "" : (current.getRoleCode() == null ? "" : current.getRoleCode().trim().toUpperCase());

        // 约束：超级管理员是最高权限用户，不能被任何人修改（包括角色/状态/资料）。
        User target = userDAO.findById(userId);
        if (isSuperAdmin(target)) {
            return;
        }

        // 约束：禁止把任何用户提升为 SUPER_ADMIN。
        String incomingRole = req.getParameter("roleCode");
        if (incomingRole != null && "SUPER_ADMIN".equalsIgnoreCase(incomingRole.trim())) {
            return;
        }

        // 约束：只有 SUPER_ADMIN 才能把用户角色设置为 SYSTEM_ADMIN。
        if (incomingRole != null
                && "SYSTEM_ADMIN".equalsIgnoreCase(incomingRole.trim())
                && !"SUPER_ADMIN".equalsIgnoreCase(currentRole)) {
            return;
        }

        User u = new User();
        u.setUserId(userId);
        u.setEmail(req.getParameter("email"));
        u.setFullName(req.getParameter("fullName"));
        u.setAffiliation(req.getParameter("affiliation"));
        u.setResearchArea(req.getParameter("researchArea"));
        u.setStatus(req.getParameter("status"));
        u.setRoleCode(incomingRole);
        userDAO.adminUpdateUser(u);
        OperationLogger.log(req, "USER", "更新用户信息", "userId=" + userId + ", role=" + u.getRoleCode() + ", status=" + u.getStatus());
    }

    private void handleDeleteUser(HttpServletRequest req) throws SQLException {
        String userIdStr = req.getParameter("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) return;

        int userId = Integer.parseInt(userIdStr);
        // 防止误删自己
        User current = getCurrentUser(req);
        if (current != null && current.getUserId() != null && current.getUserId() == userId) {
            return;
        }
        User target = userDAO.findById(userId);
        if (isSuperAdmin(target)) {
            // 不允许删除超级管理员
            return;
        }
        userDAO.deleteUser(userId);
        OperationLogger.log(req, "USER", "删除用户", "userId=" + userId);
    }

    private boolean isSuperAdmin(User u) {
        return u != null && u.getRoleCode() != null && "SUPER_ADMIN".equalsIgnoreCase(u.getRoleCode());
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }
}