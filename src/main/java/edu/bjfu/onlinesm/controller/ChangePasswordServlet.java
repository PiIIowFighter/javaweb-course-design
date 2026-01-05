package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 修改密码：
 *  GET  /profile/changePassword  显示修改密码页面
 *  POST /profile/changePassword  校验旧密码、新密码，更新成功后提示“请重新登录”
 */
@WebServlet(name = "ChangePasswordServlet", urlPatterns = {"/profile/changePassword"})
public class ChangePasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/jsp/user/change_password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String oldPassword = trim(req.getParameter("oldPassword"));
        String newPassword = trim(req.getParameter("newPassword"));
        String confirmNewPassword = trim(req.getParameter("confirmNewPassword"));

        if (isEmpty(oldPassword) || isEmpty(newPassword) || isEmpty(confirmNewPassword)) {
            req.setAttribute("error", "旧密码、新密码和确认新密码均不能为空。");
            req.getRequestDispatcher("/WEB-INF/jsp/user/change_password.jsp").forward(req, resp);
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            req.setAttribute("error", "新密码与再次输入的新密码不一致。");
            req.getRequestDispatcher("/WEB-INF/jsp/user/change_password.jsp").forward(req, resp);
            return;
        }

        // 可选：与注册逻辑一致，限制最小长度
        if (newPassword.length() < 8) {
            req.setAttribute("error", "新密码长度至少为 8 位，请重新输入。");
            req.getRequestDispatcher("/WEB-INF/jsp/user/change_password.jsp").forward(req, resp);
            return;
        }

        try {
            int userId = current.getUserId();
            int updated = userDAO.updatePasswordIfMatch(userId, oldPassword, newPassword);
            if (updated <= 0) {
                req.setAttribute("error", "修改失败：旧密码不正确。");
                req.getRequestDispatcher("/WEB-INF/jsp/user/change_password.jsp").forward(req, resp);
                return;
            }

            // 修改成功：清理 session，并提示重新登录
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            req.setAttribute("message", "密码修改成功，请重新登入。");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
        } catch (SQLException e) {
            req.setAttribute("error", "修改失败：数据库错误 - " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/user/change_password.jsp").forward(req, resp);
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
