package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 统一工作台：不再按角色分流到各自 dashboard JSP。
 *
 * 说明：
 * - 工作台页面展示“所有入口”，但具体显示/隐藏由 sessionScope.menuPermMap 控制；
 * - 是否可访问功能页面由 MenuAuthzFilter / MenuPermissionGuard 控制。
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
        if (roleCode == null || roleCode.trim().isEmpty()) roleCode = "AUTHOR";

        // 给 header.jsp 使用的页面标题
        req.setAttribute("pageTitle", "工作台");

        // 统一跳转到公共工作台
        req.setAttribute("currentRoleCode", roleCode);
        req.getRequestDispatcher("/WEB-INF/jsp/common/workbench.jsp").forward(req, resp);
    }
}
