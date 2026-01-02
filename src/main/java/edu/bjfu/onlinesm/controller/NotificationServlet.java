package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.NotificationDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Notification;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.OperationLogger;

import java.util.HashMap;
import java.util.Map;


import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

/**
 * 通知中心（单向站内通知）。
 *
 * - 所有登录用户可查看自己的通知、标记已读；
 * - 主编/编辑部管理员/系统管理员/超级管理员可发送“自定义通知”（单发）。
 */
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            handleList(req, resp, current);
            return;
        }
        if ("/view".equals(path)) {
            handleView(req, resp, current);
            return;
        }
        if ("/send".equals(path)) {
            handleSendForm(req, resp, current);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String path = req.getPathInfo();
        if ("/markRead".equals(path)) {
            handleMarkRead(req, resp, current);
            return;
        }
        if ("/markAllRead".equals(path)) {
            handleMarkAllRead(req, resp, current);
            return;
        }
        if ("/view".equals(path)) {
            handleView(req, resp, current);
            return;
        }
        if ("/send".equals(path)) {
            handleSend(req, resp, current);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException {

        String box = trim(req.getParameter("box"));
        if (box == null || box.isEmpty()) box = "inbox";
        box = box.toLowerCase();

        try {
            int unread = notificationDAO.countUnread(current.getUserId());
            req.setAttribute("unreadCount", unread);
            req.setAttribute("canSend", canSendManual(current));
            req.setAttribute("box", box);
            req.setAttribute("pageTitle", "通知中心");

            if ("sent".equals(box)) {
                List<Notification> sent = notificationDAO.listByCreator(current.getUserId(), 80);
                req.setAttribute("sentNotifications", sent);
                req.setAttribute("recipientNameMap", buildRecipientNameMap(sent));
            } else {
                List<Notification> inbox = notificationDAO.listByRecipient(current.getUserId(), 80);
                req.setAttribute("inboxNotifications", inbox);
            }

            req.getRequestDispatcher("/WEB-INF/jsp/common/notifications.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("读取通知失败", e);
        }
    }
    

        private void handleView(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendRedirect(req.getContextPath() + "/notifications");
            return;
        }

        String box = trim(req.getParameter("box"));
        if (box == null || box.isEmpty()) box = "inbox";
        box = box.toLowerCase();

        try {
            Notification n = notificationDAO.findById(id);
            if (n == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "通知不存在");
                return;
            }

            boolean isRecipient = Objects.equals(n.getRecipientUserId(), current.getUserId());
            boolean isCreator   = Objects.equals(n.getCreatedByUserId(), current.getUserId());

            if (!isRecipient && !isCreator) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限查看该通知");
                return;
            }

            if (!"inbox".equals(box) && !"sent".equals(box)) {
                box = isCreator ? "sent" : "inbox";
            }

            req.setAttribute("notification", n);
            req.setAttribute("box", box);
            req.setAttribute("isRecipient", isRecipient);
            req.setAttribute("isCreator", isCreator);
            req.setAttribute("unreadCount", notificationDAO.countUnread(current.getUserId()));
            req.setAttribute("canSend", canSendManual(current));
            req.setAttribute("pageTitle", "通知详情");

            // 发送者/接收者信息（用于详情页展示名称）
            if (n.getCreatedByUserId() != null) {
                try {
                    req.setAttribute("senderUser", userDAO.findById(n.getCreatedByUserId()));
                } catch (Exception ignore) {
                }
            }
            if (n.getRecipientUserId() != null) {
                try {
                    req.setAttribute("recipientUser", userDAO.findById(n.getRecipientUserId()));
                } catch (Exception ignore) {
                }
            }

            req.getRequestDispatcher("/WEB-INF/jsp/common/notification_detail.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("读取通知详情失败", e);
        }
    }

private void handleMarkRead(HttpServletRequest req, HttpServletResponse resp, User current) throws IOException {
        Integer id = parseInt(req.getParameter("id"));
        String box = trim(req.getParameter("box"));
        if (box == null || box.isEmpty()) box = "inbox";
        String redirect = trim(req.getParameter("redirect"));

        if (id != null) {
            try {
                Notification n = notificationDAO.findById(id);
                if (n != null && Objects.equals(n.getRecipientUserId(), current.getUserId())) {
                    notificationDAO.markRead(current.getUserId(), id);
                }

            } catch (SQLException ignore) {
            }
        }

        if ("view".equalsIgnoreCase(redirect) && id != null) {
            resp.sendRedirect(req.getContextPath() + "/notifications/view?id=" + id + "&box=" + url(box));
        } else {
            resp.sendRedirect(req.getContextPath() + "/notifications?box=" + url(box));
        }
    }

    private void handleMarkAllRead(HttpServletRequest req, HttpServletResponse resp, User current) throws IOException {
        try {
            notificationDAO.markAllRead(current.getUserId());
        } catch (SQLException ignore) {
        }
        resp.sendRedirect(req.getContextPath() + "/notifications");
    }

    private void handleSendForm(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException {
        if (!canSendManual(current)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限发送自定义通知");
            return;
        }
        try {
            req.setAttribute("users", userDAO.findSelectableUsers());
            req.setAttribute("pageTitle", "发送通知");
            req.getRequestDispatcher("/WEB-INF/jsp/common/notification_send.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("读取用户列表失败", e);
        }
    }

    private void handleSend(HttpServletRequest req, HttpServletResponse resp, User current) throws IOException, ServletException {
        if (!canSendManual(current)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限发送自定义通知");
            return;
        }

        Integer recipientId = parseInt(req.getParameter("recipientId"));
        String title = trim(req.getParameter("title"));
        String content = trim(req.getParameter("content"));

        if (recipientId == null || title == null || title.isEmpty()) {
            String msg = "发送失败：请选择接收人并填写标题";
            resp.sendRedirect(req.getContextPath() + "/notifications/send?msg=" + url(msg));
            return;
        }

        try {
            notificationDAO.create(recipientId, current.getUserId(), "MANUAL", "ADMIN", title, content, null);
            OperationLogger.log(req, "NOTIFICATION", "SEND_MANUAL", "发送通知给用户ID=" + recipientId + ", 标题=" + title);
            resp.sendRedirect(req.getContextPath() + "/notifications?box=sent&msg=" + url("已发送"));
        } catch (SQLException e) {
            throw new ServletException("发送通知失败", e);
        }
    }
    
    private Map<Integer, String> buildRecipientNameMap(List<Notification> list) throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        if (list == null) return map;

        for (Notification n : list) {
            if (n == null) continue;
            Integer rid = n.getRecipientUserId();
            if (rid == null) continue;
            if (map.containsKey(rid)) continue;

            User u = userDAO.findById(rid);
            if (u == null) {
                map.put(rid, String.valueOf(rid));
                continue;
            }
            String name = u.getFullName();
            if (name == null || name.trim().isEmpty()) name = u.getUsername();
            if (name == null || name.trim().isEmpty()) name = String.valueOf(rid);
            map.put(rid, name);
        }
        return map;
    }


    private boolean canSendManual(User u) {
        if (u == null) return false;
        String r = u.getRoleCode();
        return "EDITOR_IN_CHIEF".equals(r) || "EO_ADMIN".equals(r) || "SYSTEM_ADMIN".equals(r) || "SUPER_ADMIN".equals(r);
    }

    private User getCurrentUser(HttpServletRequest req) {
        Object obj = req.getSession().getAttribute("currentUser");
        return (obj instanceof User) ? (User) obj : null;
    }

    private Integer parseInt(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return null;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private String url(String s) {
    	try {
    	    return URLEncoder.encode(s == null ? "" : s, "UTF-8");
    	} catch (java.io.UnsupportedEncodingException e) {
    	    return "";
    	}

    }
}
