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

    /**
     * 在同一次请求中避免“手工记录 + 过滤器兜底记录”导致的重复日志。
     * 只要本次请求已经成功写入过日志，则设置该标记。
     */
    public static final String REQ_ATTR_LOG_WRITTEN = "__oplog_written";

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

            // 标记：本次请求已经成功写入过操作日志（用于过滤器去重）
            if (req != null) {
                req.setAttribute(REQ_ATTR_LOG_WRITTEN, Boolean.TRUE);
            }
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
