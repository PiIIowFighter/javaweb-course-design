package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.dao.NotificationDAO;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 在顶栏展示“未读通知数”。
 *
 * 说明：
 * - 不做消息对话串，只做单向通知；
 * - 对已登录用户，在每次请求时写入 requestScope.unreadNotificationCount；
 * - 静态资源请求跳过。
 */
public class NotificationBadgeFilter implements Filter {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String path = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        if (path.startsWith("/static/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current != null) {
            try {
                int unread = notificationDAO.countUnread(current.getUserId());
                req.setAttribute("unreadNotificationCount", unread);
            } catch (Exception ignored) {
                req.setAttribute("unreadNotificationCount", 0);
            }
        }

        chain.doFilter(request, response);
    }
}
