package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 审稿人端页面路由控制器。
 */
@WebServlet(name = "ReviewerServlet", urlPatterns = {"/reviewer/*"})
public class ReviewerServlet extends HttpServlet {

    private final ReviewDAO reviewDAO = new ReviewDAO();

    // ==================== GET：页面展示 ====================

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        if (!ensureLoggedIn(req, resp)) {
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/dashboard";
        }

        switch (path) {
            case "/dashboard":
                // 审稿人工作台首页
                req.getRequestDispatcher("/WEB-INF/jsp/reviewer/reviewer_dashboard.jsp")
                        .forward(req, resp);
                break;

            case "/assigned":
                // 待评审稿件列表
                handleAssignedList(req, resp);
                break;

            case "/history":
                // 历史评审记录
                handleHistory(req, resp);
                break;

            case "/reviewForm":
                // 填写某条审稿记录的评审意见
                handleReviewForm(req, resp);
                break;

            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ==================== POST：提交评审 ====================

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {

        if (!ensureLoggedIn(req, resp)) {
            return;
        }

        String path = req.getPathInfo();
        if (path == null) {
            path = "/submit";
        }

        switch (path) {
            case "/submit":
                // 提交评审意见
                handleSubmitReview(req, resp);
                break;
            case "/accept":
                // 接受审稿邀请
                handleAcceptInvitation(req, resp);
                break;
            case "/decline":
                // 拒绝审稿邀请
                handleDeclineInvitation(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ==================== 具体处理方法 ====================

    /**
     * 待评审稿件列表：
     * 查询当前审稿人、稿件状态为 UNDER_REVIEW 的审稿记录。
     */

    /**
     * 审稿人接受审稿邀请：
     *  - 根据 reviewId 更新 dbo.Reviews.Status = 'ACCEPTED'；
     *  - 接受后仍然停留在“待评审稿件列表”页面。
     */
    private void handleAcceptInvitation(HttpServletRequest req,
                                        HttpServletResponse resp)
            throws IOException, ServletException {
        String reviewIdStr = req.getParameter("reviewId");
        if (reviewIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少审稿记录 ID。");
            return;
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            reviewDAO.acceptInvitation(reviewId);
            resp.sendRedirect(req.getContextPath() + "/reviewer/assigned");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("接受审稿邀请时数据库出错", e);
        }
    }

    /**
     * 审稿人拒绝审稿邀请：
     *  - 根据 reviewId 将 dbo.Reviews.Status 更新为 'DECLINED'；
     *  - 拒绝后同样返回“待评审稿件列表”，该记录将不再出现。
     */
    private void handleDeclineInvitation(HttpServletRequest req,
                                         HttpServletResponse resp)
            throws IOException, ServletException {
        String reviewIdStr = req.getParameter("reviewId");
        if (reviewIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少审稿记录 ID。");
            return;
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            reviewDAO.declineInvitation(reviewId);
            resp.sendRedirect(req.getContextPath() + "/reviewer/assigned");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("拒绝审稿邀请时数据库出错", e);
        }
    }


    private void handleAssignedList(HttpServletRequest req,
                                    HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req);
        try {
            List<Review> list =
                    reviewDAO.findByReviewerAndStatus(current.getUserId(), "UNDER_REVIEW");
            req.setAttribute("reviews", list);
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/assigned_list.jsp")
                    .forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载待评审稿件列表时数据库出错", e);
        }
    }

    /**
     * 历史评审记录（Status = SUBMITTED）。
     */
    private void handleHistory(HttpServletRequest req,
                               HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req);
        try {
            List<Review> list =
                    reviewDAO.findHistoryByReviewer(current.getUserId());
            req.setAttribute("reviews", list);
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/review_history.jsp")
                    .forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载历史评审记录时数据库出错", e);
        }
    }

    /**
     * 打开填写评审意见的表单。
     */
    private void handleReviewForm(HttpServletRequest req,
                                  HttpServletResponse resp)
            throws ServletException, IOException {

        String reviewIdStr = req.getParameter("id");
        if (reviewIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少审稿记录 ID。");
            return;
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            req.setAttribute("reviewId", reviewId);
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/review_form.jsp")
                    .forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        }
    }

    /**
     * 审稿人提交评审意见。
     */
    private void handleSubmitReview(HttpServletRequest req,
                                    HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req); // 现在没用到，保留以后可做校验

        String reviewIdStr    = req.getParameter("reviewId");
        String content        = req.getParameter("content");
        String scoreStr       = req.getParameter("score");
        String recommendation = req.getParameter("recommendation");

        if (reviewIdStr == null
                || content == null || content.trim().isEmpty()
                || recommendation == null || recommendation.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            Double score = null;
            if (scoreStr != null && !scoreStr.trim().isEmpty()) {
                score = Double.parseDouble(scoreStr.trim());
            }

            reviewDAO.submitReview(reviewId,
                    content.trim(),
                    score,
                    recommendation.trim());

            // 提交后跳转到历史评审记录
            resp.sendRedirect(req.getContextPath() + "/reviewer/history");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数格式错误。");
        } catch (SQLException e) {
            throw new ServletException("提交评审意见时数据库出错", e);
        }
    }

    // ==================== 工具方法 ====================

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null
                ? (User) session.getAttribute("currentUser")
                : null;
    }

    private boolean ensureLoggedIn(HttpServletRequest req,
                                   HttpServletResponse resp) throws IOException {
        if (getCurrentUser(req) == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }
        return true;
    }
}
