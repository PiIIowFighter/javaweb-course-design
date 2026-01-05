package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.MenuPermissionGuard;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 主编（EDITOR_IN_CHIEF）相关功能入口。
 *
 * URL 保持兼容：仍使用 /editor/... 下的各模块路径。
 */
@WebServlet(
        name = "ChiefEditorServlet",
        urlPatterns = {
                "/editor/overview/*",
                "/editor/desk/*",
                "/editor/toAssign/*",
                "/editor/reviewers/*",
                "/editor/finalDecision/*",
                "/editor/special/*"
        }
)
public class ChiefEditorServlet extends EditorServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String oldPath = buildOldStylePath(req);
        String requiredPerm = requiredMenuPermission(oldPath);
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        String sp = req.getServletPath();

        try {
            switch (sp) {
                case "/editor/overview":
                    handleChiefOverview(req, resp, current);
                    return;
                case "/editor/desk":
                    handleDeskList(req, resp, current);
                    return;
                case "/editor/toAssign":
                    handleToAssignList(req, resp, current);
                    return;
                case "/editor/reviewers":
                    handleReviewerPoolPage(req, resp, current);
                    return;
                case "/editor/finalDecision":
                    handleFinalDecisionList(req, resp, current);
                    return;
                case "/editor/special":
                    handleChiefSpecialPage(req, resp, current);
                    return;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理主编功能 GET 请求时访问数据库出错", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String oldPath = buildOldStylePath(req);
        String requiredPerm = requiredMenuPermission(oldPath);
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        String sp = req.getServletPath();
        try {
            switch (sp) {
                case "/editor/desk":
                    handleDeskDecisionPost(req, resp, current);
                    return;
                case "/editor/toAssign":
                    handleAssignEditorPost(req, resp, current);
                    return;
                case "/editor/finalDecision":
                    handleFinalDecisionPost(req, resp, current);
                    return;
                case "/editor/reviewers":
                    handleReviewerPoolPost(req, resp, current);
                    return;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理主编功能 POST 请求时访问数据库出错", e);
        }
    }

    private String buildOldStylePath(HttpServletRequest req) {
        // /editor/desk + null -> /desk
        String sp = req.getServletPath();
        String pi = req.getPathInfo();
        if (pi == null) pi = "";
        if (sp != null && sp.startsWith("/editor")) {
            sp = sp.substring("/editor".length());
        }
        if (sp == null) sp = "";
        return sp + pi;
    }
}
