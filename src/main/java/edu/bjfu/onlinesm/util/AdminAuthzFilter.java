package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;


@WebFilter(filterName = "MenuAuthzFilter", urlPatterns = {"/admin/*", "/editor/*", "/reviewer/*", "/manuscripts/*"})
public class AdminAuthzFilter implements Filter {

    private final MenuPermissionService menuPermissionService = new MenuPermissionService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        
        if (session.getAttribute(MenuPermissionService.SESSION_MENU_PERMS) == null) {
            menuPermissionService.loadIntoSession(session, current);
        }

        String path = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        String[] required = requiredPermissions(path, req.getMethod());
        if (required == null || required.length == 0) {
            chain.doFilter(request, response);
            return;
        }

        boolean ok = false;
        for (String perm : required) {
            if (perm != null && menuPermissionService.hasPermission(session, current, perm)) {
                ok = true;
                break;
            }
        }

        if (!ok) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            StringBuilder sb = new StringBuilder();
            sb.append("当前账号无权限访问该功能入口（需要以下任一权限）：");
            for (int i = 0; i < required.length; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(required[i]);
            }
            req.setAttribute("error", sb.toString());
            req.getRequestDispatcher("/WEB-INF/jsp/error/access_denied.jsp").forward(req, resp);
            return;
        }

        chain.doFilter(request, response);
    }

        
    private String[] requiredPermissions(String path, String method) {
        if (path == null) return null;

        
        if (path.startsWith("/admin/users")) return new String[]{PermissionCatalog.ADMIN_USERS};
        if (path.startsWith("/admin/permissions")) return new String[]{PermissionCatalog.ADMIN_PERMISSIONS};
        if (path.startsWith("/admin/logs")) return new String[]{PermissionCatalog.ADMIN_LOGS};
        if (path.startsWith("/admin/system/db")) return new String[]{PermissionCatalog.ADMIN_DB_MAINTENANCE};
        if (path.startsWith("/admin/system")) return new String[]{PermissionCatalog.ADMIN_SYSTEM};
        if (path.startsWith("/admin/journals")) return new String[]{PermissionCatalog.ADMIN_JOURNALS};
        if (path.startsWith("/admin/editorial")) return new String[]{PermissionCatalog.ADMIN_EDITORIAL};
        if (path.startsWith("/admin/news")) return new String[]{PermissionCatalog.ADMIN_NEWS};

        
        if (path.startsWith("/manuscripts/submit")) return new String[]{PermissionCatalog.MENU_AUTHOR_SUBMIT};

        
        if (path.startsWith("/manuscripts/detail") || path.startsWith("/manuscripts/track")) {
            return new String[]{
                    PermissionCatalog.MENU_AUTHOR_MY_MANUSCRIPTS,
                    PermissionCatalog.MENU_EO_FORMAL_CHECK,
                    PermissionCatalog.MENU_EO_FORMAL_HISTORY,
                    PermissionCatalog.MENU_EDITOR_TODO,
                    PermissionCatalog.MENU_EDITOR_UNDER_REVIEW,
                    PermissionCatalog.MENU_EDITOR_RECOMMEND,
                    PermissionCatalog.MENU_EDITOR_REVIEW_MONITOR,
                    PermissionCatalog.MENU_EDITOR_AUTHOR_COMM,
                    PermissionCatalog.MENU_EIC_OVERVIEW,
                    PermissionCatalog.MENU_EIC_DESK,
                    PermissionCatalog.MENU_EIC_TO_ASSIGN,
                    PermissionCatalog.MENU_EIC_REVIEWERS,
                    PermissionCatalog.MENU_EIC_FINAL_DECISION,
                    PermissionCatalog.MENU_EIC_SPECIAL,
                    PermissionCatalog.ADMIN_JOURNALS,
                    PermissionCatalog.ADMIN_EDITORIAL,
                    PermissionCatalog.ADMIN_USERS
            };
        }

        if (path.startsWith("/manuscripts")) return new String[]{PermissionCatalog.MENU_AUTHOR_MY_MANUSCRIPTS};

        
        if (path.startsWith("/reviewer/history")) return new String[]{PermissionCatalog.MENU_REVIEWER_HISTORY};
        if (path.startsWith("/reviewer")) return new String[]{PermissionCatalog.MENU_REVIEWER_ASSIGNED};

        
        if (path.startsWith("/editor/formalCheck/history")) return new String[]{PermissionCatalog.MENU_EO_FORMAL_HISTORY};
        if (path.startsWith("/editor/formalCheck")) return new String[]{PermissionCatalog.MENU_EO_FORMAL_CHECK};

        if (path.startsWith("/editor/overview")) return new String[]{PermissionCatalog.MENU_EIC_OVERVIEW};
        if (path.startsWith("/editor/desk")) return new String[]{PermissionCatalog.MENU_EIC_DESK};
        if (path.startsWith("/editor/toAssign")) return new String[]{PermissionCatalog.MENU_EIC_TO_ASSIGN};
        if (path.startsWith("/editor/reviewers")) return new String[]{PermissionCatalog.MENU_EIC_REVIEWERS};
        if (path.startsWith("/editor/finalDecision")) return new String[]{PermissionCatalog.MENU_EIC_FINAL_DECISION};
        if (path.startsWith("/editor/special")) return new String[]{PermissionCatalog.MENU_EIC_SPECIAL};

        if (path.startsWith("/editor/withEditor")) return new String[]{PermissionCatalog.MENU_EDITOR_TODO};
        if (path.startsWith("/editor/underReview")) return new String[]{PermissionCatalog.MENU_EDITOR_UNDER_REVIEW};
        
        
        
        
        if (path.startsWith("/editor/recommend")) {
            if (method != null && method.equalsIgnoreCase("GET")) {
                return new String[]{
                        PermissionCatalog.MENU_EDITOR_RECOMMEND,
                        PermissionCatalog.MENU_EIC_FINAL_DECISION
                };
            }
            return new String[]{PermissionCatalog.MENU_EDITOR_RECOMMEND};
        }
        if (path.startsWith("/editor/review/invite") || path.startsWith("/editor/review/inviteExternal") || path.startsWith("/editor/review/cancel")) {
            return new String[]{PermissionCatalog.MENU_EDITOR_TODO};
        }
        if (path.startsWith("/editor/review/remind") || path.startsWith("/editor/review/remindCustom") || path.startsWith("/editor/review/autoRemindNow") || path.startsWith("/editor/review/monitor")) {
            return new String[]{PermissionCatalog.MENU_EDITOR_REVIEW_MONITOR};
        }
        if (path.startsWith("/editor/author/message") || path.startsWith("/editor/authorComm")) {
            return new String[]{PermissionCatalog.MENU_EDITOR_AUTHOR_COMM};
        }

        
        return null;
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

