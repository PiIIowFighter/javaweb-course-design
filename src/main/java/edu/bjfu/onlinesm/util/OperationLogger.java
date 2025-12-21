package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.dao.OperationLogDAO;
import edu.bjfu.onlinesm.model.OperationLog;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;

/**
 * 统一写入 dbo.OperationLogs 的工具。
 */
public final class OperationLogger {

    private static final OperationLogDAO DAO = new OperationLogDAO();

    private OperationLogger() {
    }

    public static void log(HttpServletRequest req, String module, String action, String detail) {
        try {
            OperationLog log = new OperationLog();
            User u = getCurrentUser(req);
            if (u != null) {
                log.setActorUserId(u.getUserId());
                log.setActorUsername(u.getUsername());
            }
            log.setModule(module == null ? "" : module);
            log.setAction(action == null ? "" : action);
            log.setDetail(detail);
            log.setIp(getClientIp(req));
            DAO.insert(log);
        } catch (SQLException ignore) {
            // 日志写入失败不影响主流程（避免管理员操作因日志失败导致 500）
        }
    }

    private static User getCurrentUser(HttpServletRequest req) {
        if (req == null) return null;
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private static String getClientIp(HttpServletRequest req) {
        if (req == null) return null;
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.trim().isEmpty()) {
            String[] parts = xf.split(",");
            return parts.length > 0 ? parts[0].trim() : xf.trim();
        }
        return req.getRemoteAddr();
    }
}
