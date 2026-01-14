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

        
        String oldPath = buildOldStylePath(req);
        String requiredPerm = requiredMenuPermission(oldPath);
        
        
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

    
    private String buildOldStylePath(HttpServletRequest req) {
        String sp = req.getServletPath();
        String pi = req.getPathInfo();
        if (pi == null) pi = "";
        
        if (sp != null && sp.startsWith("/editor")) {
            sp = sp.substring("/editor".length());
        }
        if (sp == null || sp.trim().isEmpty()) sp = "";
        return sp + pi;
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

