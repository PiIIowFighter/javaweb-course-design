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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import edu.bjfu.onlinesm.util.PaginationUtil;


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
            String keyword = safe(req.getParameter("keyword"));
            String actor = safe(req.getParameter("actor"));
            String module = safe(req.getParameter("module"));

            LocalDateTime from = parseDateTime(req.getParameter("from"));
            LocalDateTime to = parseDateTime(req.getParameter("to"));

            try {
                List<OperationLog> logs = logDAO.findByFilters(200,
                        keyword.isEmpty() ? null : keyword,
                        actor.isEmpty() ? null : actor,
                        module.isEmpty() ? null : module,
                        from,
                        to);
                PaginationUtil.apply(req, logs, "logs");
                req.setAttribute("keyword", keyword);
                req.setAttribute("actor", actor);
                req.setAttribute("module", module);
                req.setAttribute("from", safe(req.getParameter("from")));
                req.setAttribute("to", safe(req.getParameter("to")));
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

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    
    private static LocalDateTime parseDateTime(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        try {
            
            if (s.contains("T")) {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
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

