package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.mail.MailNotifications;
import edu.bjfu.onlinesm.util.notify.InAppNotifications;

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
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final UserDAO userDAO = new UserDAO();
    private final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);

    // ==================== GET：页面展示 ====================

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        if (!ensureReviewer(req, resp)) {
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

            case "/invitation":
                // 查看邀请详情（摘要等），再决定是否接受/拒绝
                handleInvitationDetail(req, resp);
                break;

            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ==================== POST：提交评审 ====================

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {

        if (!ensureReviewer(req, resp)) {
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
            User current = getCurrentUser(req);
            reviewDAO.acceptInvitation(reviewId, current.getUserId());
            // 3.1 审稿人接受/拒绝邀请：通知编辑（按邮件实现）
            mailNotifications.onReviewerResponded(reviewId, true);
            // 站内通知
            inAppNotifications.onReviewerResponded(reviewId, true);
            resp.sendRedirect(req.getContextPath() + "/reviewer/assigned");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("接受审稿邀请时数据库出错", e);
        }
    }

    /**
     * 审稿人拒绝审稿邀请：
     *  - 先通知编辑（邮件/站内），再移除该邀请记录（不写入额外状态），因此后续可再次邀请；
     *  - 拒绝后返回“待评审稿件列表”，该记录将不再出现。
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
            User current = getCurrentUser(req);

            // 先通知编辑（邮件/站内），再移除邀请记录（拒绝后允许再次邀请）
            mailNotifications.onReviewerResponded(reviewId, false);
            inAppNotifications.onReviewerResponded(reviewId, false);

            reviewDAO.declineInvitation(reviewId, current.getUserId());
            resp.sendRedirect(req.getContextPath() + "/reviewer/assigned");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("拒绝审稿邀请时数据库出错", e);
        }
    }

    /**
     * 查看邀请详情：仅允许当前审稿人查看自己被邀请的记录（Status=INVITED）。
     * 该页仅展示：标题/摘要/关键词/研究主题/资助信息等（不展示作者信息与决策历史）。
     */
    private void handleInvitationDetail(HttpServletRequest req,
                                        HttpServletResponse resp)
            throws ServletException, IOException {

        String reviewIdStr = req.getParameter("id");
        if (reviewIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少审稿记录 ID。");
            return;
        }

        User current = getCurrentUser(req);
        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            Review review = reviewDAO.findById(reviewId);
            if (review == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到审稿记录。");
                return;
            }
            if (review.getReviewerId() != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看该审稿邀请。");
                return;
            }
            // 允许 INVITED/ACCEPTED 查看邀请页（ACCEPTED 也可回看摘要）；其他状态禁止
            if (!("INVITED".equals(review.getStatus()) || "ACCEPTED".equals(review.getStatus()))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "该审稿记录当前状态不支持查看邀请详情。");
                return;
            }

            Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
            if (m == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应稿件。");
                return;
            }

            req.setAttribute("review", review);
            req.setAttribute("manuscript", m);
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/invitation_detail.jsp")
                    .forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("加载邀请详情时数据库出错", e);
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
            User current = getCurrentUser(req);

            Review review = reviewDAO.findById(reviewId);
            if (review == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到审稿记录。");
                return;
            }
            if (review.getReviewerId() != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权访问该审稿记录。");
                return;
            }
            if (!"ACCEPTED".equals(review.getStatus())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请先在邀请详情页接受审稿邀请后再提交评审意见。");
                return;
            }

            Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
            if (m == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应稿件。");
                return;
            }

            req.setAttribute("review", review);
            req.setAttribute("manuscript", m);
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/review_form.jsp")
                    .forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("加载评审表单时数据库出错", e);
        }
    }

    /**
     * 审稿人提交评审意见。
     */
    private void handleSubmitReview(HttpServletRequest req,
                                    HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req);

        String reviewIdStr = req.getParameter("reviewId");
        String recommendation = req.getParameter("recommendation");

        // 给编辑的意见
        String confidentialToEditor = req.getParameter("confidentialToEditor");
        String keyEvaluation = req.getParameter("keyEvaluation");

        // 给作者的意见
        String commentsToAuthor = req.getParameter("content");

        // 多维评分
        String s1 = req.getParameter("scoreOriginality");
        String s2 = req.getParameter("scoreSignificance");
        String s3 = req.getParameter("scoreMethodology");
        String s4 = req.getParameter("scorePresentation");

        if (reviewIdStr == null
                || recommendation == null || recommendation.trim().isEmpty()
                || commentsToAuthor == null || commentsToAuthor.trim().isEmpty()
                || confidentialToEditor == null || confidentialToEditor.trim().isEmpty()
                || keyEvaluation == null || keyEvaluation.trim().isEmpty()
                || s1 == null || s2 == null || s3 == null || s4 == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数（多维评分/给编辑意见/关键评价/给作者意见/推荐结论）。");
            return;
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            Double scoreOriginality = Double.parseDouble(s1.trim());
            Double scoreSignificance = Double.parseDouble(s2.trim());
            Double scoreMethodology = Double.parseDouble(s3.trim());
            Double scorePresentation = Double.parseDouble(s4.trim());

            // 简单范围校验（前端也限制）：0~10
            checkScoreRange(scoreOriginality);
            checkScoreRange(scoreSignificance);
            checkScoreRange(scoreMethodology);
            checkScoreRange(scorePresentation);

            Double scoreOverall = (scoreOriginality + scoreSignificance + scoreMethodology + scorePresentation) / 4.0;

            reviewDAO.submitReviewV2(
                    reviewId,
                    current.getUserId(),
                    commentsToAuthor.trim(),
                    confidentialToEditor.trim(),
                    keyEvaluation.trim(),
                    scoreOverall,
                    scoreOriginality,
                    scoreSignificance,
                    scoreMethodology,
                    scorePresentation,
                    recommendation.trim());

            resp.sendRedirect(req.getContextPath() + "/reviewer/history");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数格式错误（reviewId 或评分必须为数字）。");
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("提交评审意见时数据库出错", e);
        }
    }

    private void checkScoreRange(Double v) {
        if (v == null || v < 0 || v > 10) {
            throw new IllegalArgumentException("评分必须在 0~10 范围内。");
        }
    }

    // ==================== 工具方法 ====================

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null
                ? (User) session.getAttribute("currentUser")
                : null;
    }

    private boolean ensureReviewer(HttpServletRequest req,
                                   HttpServletResponse resp) throws IOException {
        User u = getCurrentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }
        if (!"REVIEWER".equals(u.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有审稿人可以访问该模块。");
            return false;
        }
        return true;
    }
}
