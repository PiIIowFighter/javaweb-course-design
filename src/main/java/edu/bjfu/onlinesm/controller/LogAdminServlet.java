package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.OperationLogDAO;
import edu.bjfu.onlinesm.model.OperationLog;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 系统日志查询模块控制器。
 */
@WebServlet(name = "LogAdminServlet", urlPatterns = {"/admin/logs/*"})
public class LogAdminServlet extends HttpServlet {

    private final OperationLogDAO logDAO = new OperationLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLoggedIn(req, resp)) {
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            String keyword = req.getParameter("keyword");
            try {
                List<OperationLog> logs = logDAO.findRecent(200, keyword);
                req.setAttribute("logs", logs);
                req.setAttribute("keyword", keyword);
            } catch (SQLException e) {
                throw new ServletException("读取操作日志失败", e);
            }
            req.getRequestDispatcher("/WEB-INF/jsp/admin/logs/log_list.jsp").forward(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLoggedIn(req, resp)) {
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/logs/list");
    }

    private boolean ensureLoggedIn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getSession().getAttribute("currentUser") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }
        return true;
    }
}
