package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.OperationLogDAO;
import edu.bjfu.onlinesm.model.OperationLog;
import edu.bjfu.onlinesm.util.DbUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统维护模块：
 *  - 查看系统运行状态（JVM、内存、数据库连通性等）；
 *  - 方便系统管理员快速排查。
 */
@WebServlet(name = "SystemAdminServlet", urlPatterns = {"/admin/system/*"})
public class SystemAdminServlet extends HttpServlet {

    private final OperationLogDAO logDAO = new OperationLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/status".equals(path)) {
            handleStatus(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleStatus(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // JVM/系统状态
        Runtime rt = Runtime.getRuntime();
        req.setAttribute("now", LocalDateTime.now());
        req.setAttribute("javaVersion", System.getProperty("java.version"));
        req.setAttribute("osName", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        req.setAttribute("userTimeZone", System.getProperty("user.timezone"));
        req.setAttribute("maxMemory", rt.maxMemory());
        req.setAttribute("totalMemory", rt.totalMemory());
        req.setAttribute("freeMemory", rt.freeMemory());
        req.setAttribute("processors", rt.availableProcessors());

        // DB 连通性
        boolean dbOk = false;
        String dbError = null;
        try (Connection conn = DbUtil.getConnection()) {
            dbOk = conn != null && !conn.isClosed();
        } catch (SQLException e) {
            dbOk = false;
            dbError = e.getMessage();
        }
        req.setAttribute("dbOk", dbOk);
        req.setAttribute("dbError", dbError);

        // 最近操作日志（用于快速排查）
        try {
            List<OperationLog> recent = logDAO.findRecent(30, null);
            req.setAttribute("recentLogs", recent);
        } catch (SQLException e) {
            // ignore
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/system/system_status.jsp").forward(req, resp);
    }
}
