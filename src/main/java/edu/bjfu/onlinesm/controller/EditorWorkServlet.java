package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.MenuPermissionGuard;
import edu.bjfu.onlinesm.util.PermissionCatalog;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 责任编辑（EDITOR）相关功能入口。
 *
 * 说明：本项目早期将 EDITOR / EO_ADMIN / EDITOR_IN_CHIEF 的功能混杂在一个 /editor/* 的 Servlet 中。
 * 现按角色拆分：责任编辑功能统一由本 Servlet 处理。
 *
 * URL 仍保持兼容（不改 JSP 链接）：
 *  - /editor/withEditor
 *  - /editor/underReview
 *  - /editor/recommend
 *  - /editor/recommend/detail
 *  - /editor/review/*
 *  - /editor/authorComm
 *  - /editor/author/message
 */
@WebServlet(
        name = "EditorWorkServlet",
        urlPatterns = {
                "/editor/withEditor/*",
                "/editor/underReview/*",
                "/editor/recommend/*",
                "/editor/review/*",
                "/editor/authorComm/*",
                "/editor/author/*"
        }
)
public class EditorWorkServlet extends EditorServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // 兼容旧的 requiredMenuPermission(pathInfo) 规则：拼回旧风格 path（以 / 开头）
        String oldPath = buildOldStylePath(req);
        String requiredPerm = requiredMenuPermission(oldPath);
        // 主编在“终审/决策”模块中需要查看编辑建议与审稿汇总，但这些页面复用了 /editor/recommend... 路由。
        // 因此当主编访问 recommend 页面时，使用终审入口权限放行（只读由 JSP 控制）。
        if (requiredPerm != null
                && PermissionCatalog.MENU_EDITOR_RECOMMEND.equals(requiredPerm)
                && "EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            requiredPerm = PermissionCatalog.MENU_EIC_FINAL_DECISION;
        }
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        String sp = req.getServletPath();
        String pi = req.getPathInfo();
        if (pi == null) pi = "/";

        try {
            switch (sp) {
                case "/editor/withEditor":
                    if ("/".equals(pi)) {
                        handleWithEditorList(req, resp, current);
                        return;
                    }
                    if ("/detail".equals(pi)) {
                        handleEditorManuscriptDetailPage(req, resp, current);
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;

                case "/editor/underReview":
                    if ("/".equals(pi)) {
                        handleUnderReviewList(req, resp, current);
                        return;
                    }
                    if ("/detail".equals(pi)) {
                        handleEditorManuscriptDetailPage(req, resp, current);
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;

                case "/editor/recommend":
                    if ("/".equals(pi)) {
                        handleEditorRecommendPage(req, resp, current);
                        return;
                    }
                    if ("/detail".equals(pi)) {
                        handleRecommendManuscriptDetailPage(req, resp, current);
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;

                case "/editor/review":
                    switch (pi) {
                        case "/monitor":
                            handleReviewMonitorPage(req, resp, current);
                            return;
                        case "/remindForm":
                            handleReviewRemindFormPage(req, resp, current);
                            return;
                        case "/detail":
                            handleEditorReviewDetailPage(req, resp, current);
                            return;
                        case "/select":
                            handleReviewSelectPage(req, resp, current);
                            return;
                        case "/externalInvite":
                            handleExternalInviteReviewerPage(req, resp, current);
                            return;
                        default:
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                            return;
                    }

                case "/editor/authorComm":
                    handleAuthorCommList(req, resp, current);
                    return;

                case "/editor/author":
                    if ("/message".equals(pi)) {
                        handleAuthorMessagePage(req, resp, current);
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理编辑功能 GET 请求时访问数据库出错", e);
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
        String pi = req.getPathInfo();
        if (pi == null) pi = "/";

        try {
            switch (sp) {
                case "/editor/recommend":
                    // 提交编辑建议
                    handleEditorRecommendPost(req, resp, current);
                    return;

                case "/editor/review":
                    switch (pi) {
                        case "/invite":
                            handleInviteReviewerPost(req, resp, current);
                            return;
                        case "/inviteExternal":
                            handleInviteExternalReviewerPost(req, resp, current);
                            return;
                        case "/remind":
                            handleRemindReviewerPost(req, resp, current);
                            return;
                        case "/remindCustom":
                            handleRemindReviewerCustomPost(req, resp, current);
                            return;
                        case "/cancel":
                            handleCancelReviewerPost(req, resp, current);
                            return;
                        case "/autoRemindNow":
                            handleAutoRemindNowPost(req, resp, current);
                            return;
                        default:
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                            return;
                    }

                case "/editor/author":
                    if ("/message".equals(pi)) {
                        handleSendAuthorMessagePost(req, resp, current);
                        return;
                    }
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理编辑功能 POST 请求时访问数据库出错", e);
        }
    }

    /**
     * 将“当前模块化映射”的 servletPath/pathInfo 拼回旧的 /editor/* pathInfo 形态，
     * 复用基类中既有的 requiredMenuPermission(...) 规则。
     */
    private String buildOldStylePath(HttpServletRequest req) {
        String sp = req.getServletPath();
        String pi = req.getPathInfo();
        if (pi == null) pi = "";
        // /editor/withEditor -> /withEditor
        if (sp != null && sp.startsWith("/editor")) {
            sp = sp.substring("/editor".length());
        }
        if (sp == null || sp.trim().isEmpty()) sp = "";
        return sp + pi;
    }
}
