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
 * 编辑部管理员（EO_ADMIN）相关功能入口。
 *
 * URL 保持兼容：/editor/formalCheck/*
 */
@WebServlet(name = "EditorialOfficeServlet", urlPatterns = {"/editor/formalCheck/*"})
public class EditorialOfficeServlet extends EditorServlet {

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

        String pi = req.getPathInfo();
        if (pi == null || "/".equals(pi)) pi = "/";

        try {
            switch (pi) {
                case "/":
                    handleFormalCheckList(req, resp, current);
                    return;
                case "/review":
                    handleFormalCheckReviewPage(req, resp, current);
                    return;
                case "/history":
                    handleFormalCheckHistoryPage(req, resp, current);
                    return;
                case "/history/detail":
                    handleFormalCheckHistoryDetailPage(req, resp, current);
                    return;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理形式审查 GET 请求时访问数据库出错", e);
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

        try {
            // 该模块所有 POST 都由 handleFormalCheckPost 统一处理，通过 op 参数区分（autoCheck/plagiarismCheck/submit/...）
            handleFormalCheckPost(req, resp, current);
        } catch (SQLException e) {
            throw new ServletException("处理形式审查 POST 请求时访问数据库出错", e);
        }
    }

    private String buildOldStylePath(HttpServletRequest req) {
        // /editor/formalCheck + /history -> /formalCheck/history
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
