package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 根据当前登录用户的角色，跳转到不同的工作台 JSP。
 *
 * 角色代码建议与 dbo.Roles.RoleCode 对应：
 *  SUPER_ADMIN / SYSTEM_ADMIN / EDITOR_IN_CHIEF / EDITOR / REVIEWER / AUTHOR
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Object obj = req.getSession().getAttribute("currentUser");
        if (!(obj instanceof User)) {
            // 未登录则先去登录页面
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        User currentUser = (User) obj;
        String roleCode = currentUser.getRoleCode();
        if (roleCode == null || roleCode.trim().isEmpty()) {
            // 没有显式设置角色的话，默认视为作者
            roleCode = "AUTHOR";
        }

        String target;
        switch (roleCode) {
            case "SUPER_ADMIN":
                target = "/WEB-INF/jsp/admin/superadmin_dashboard.jsp";
                break;
            case "SYSTEM_ADMIN":
                target = "/WEB-INF/jsp/admin/admin_dashboard.jsp";
                break;
            case "EO_ADMIN":
                target = "/WEB-INF/jsp/admin/eo_admin_dashboard.jsp";
                break;
            case "EDITOR_IN_CHIEF":
                target = "/WEB-INF/jsp/editor/chief_editor_dashboard.jsp";
                break;
            case "EDITOR":
                target = "/WEB-INF/jsp/editor/editor_dashboard.jsp";
                break;
            case "REVIEWER":
                target = "/WEB-INF/jsp/reviewer/reviewer_dashboard.jsp";
                break;
            case "AUTHOR":
            default:
                target = "/WEB-INF/jsp/author/author_dashboard.jsp";
                break;
        }

        // 给 header.jsp 使用的页面标题，可选
        req.setAttribute("pageTitle", "工作台 - " + roleCode);
        req.getRequestDispatcher(target).forward(req, resp);
    }
}
