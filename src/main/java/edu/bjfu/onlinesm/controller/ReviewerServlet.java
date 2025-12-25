package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.mail.MailNotifications; // 添加导入

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
     *  - 接受后仍然停留在"待评审稿件列表"页面。
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
     *  - 填写拒绝理由；
     *  - 拒绝后同样返回"待评审稿件列表"，该记录将不再出现。
     */
    private void handleDeclineInvitation(HttpServletRequest req,
                                         HttpServletResponse resp)
            throws IOException, ServletException {
        String reviewIdStr = req.getParameter("reviewId");
        String rejectionReason = req.getParameter("rejectionReason");
        
        if (reviewIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少审稿记录 ID。");
            return;
        }
        
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            // 如果没有拒绝理由，可以提供一个默认值
            rejectionReason = "时间冲突，无法审稿";
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            User current = getCurrentUser(req);
            
            // 保存拒绝理由到数据库 - 现在传递3个参数
            reviewDAO.declineInvitation(reviewId, current.getUserId(), rejectionReason.trim());
            
            // 发送邮件通知编辑
            try {
                // 创建邮件通知实例
                MailNotifications notifications = new MailNotifications(new UserDAO(), new ManuscriptDAO(), reviewDAO);
                // 发送拒绝邀请通知给编辑
                notifications.onReviewerDeclined(reviewId);
            } catch (Exception e) {
                // 邮件发送失败不影响主流程，只记录日志
                System.err.println("Failed to send declined invitation email: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 简化：不在Servlet中通知编辑，可以改为异步处理或日志记录
            System.out.println("审稿人 " + current.getFullName() + 
                             " (ID: " + current.getUserId() + 
                             ") 拒绝了审稿邀请 " + reviewId + 
                             "，理由: " + rejectionReason);
            
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
        
        // 给编辑的意见（富文本）
        String confidentialToEditor = req.getParameter("confidentialToEditor");
        
        // 给作者的意见（富文本）
        String commentsToAuthor = req.getParameter("commentsToAuthor");
        // 兼容旧字段
        if (commentsToAuthor == null || commentsToAuthor.trim().isEmpty()) {
        commentsToAuthor = req.getParameter("content");
        }
        
        // 隐藏的关键评价字段
        String keyEvaluation = req.getParameter("keyEvaluation");
        if (keyEvaluation == null) keyEvaluation = "";
        
        // 多维评分
        String s1 = req.getParameter("scoreOriginality");
        String s2 = req.getParameter("scoreSignificance");
        String s3 = req.getParameter("scoreMethodology");
        String s4 = req.getParameter("scorePresentation");
        String overallScoreStr = req.getParameter("score");  // 使用自动计算的总体分
        
        // 参数验证
        if (reviewIdStr == null || recommendation == null || 
        confidentialToEditor == null || commentsToAuthor == null ||
        s1 == null || s2 == null || s3 == null || s4 == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数");
        return;
        }
        
        try {
        int reviewId = Integer.parseInt(reviewIdStr);
        Double scoreOriginality = Double.parseDouble(s1.trim());
        Double scoreSignificance = Double.parseDouble(s2.trim());
        Double scoreMethodology = Double.parseDouble(s3.trim());
        Double scorePresentation = Double.parseDouble(s4.trim());
        Double scoreOverall = overallScoreStr != null ? 
                 Double.parseDouble(overallScoreStr.trim()) : 
                 (scoreOriginality + scoreSignificance + scoreMethodology + scorePresentation) / 4.0;
        
        // 分数范围校验
        checkScoreRange(scoreOriginality);
        checkScoreRange(scoreSignificance);
        checkScoreRange(scoreMethodology);
        checkScoreRange(scorePresentation);
        checkScoreRange(scoreOverall);
        
        // 提交评审
        reviewDAO.submitReviewV2(
                  reviewId,
                current.getUserId(),
                commentsToAuthor.trim(),          // 给作者的意见
                confidentialToEditor.trim(),      // 给编辑的保密意见
                keyEvaluation.trim(),             // 关键评价
                scoreOverall,                     // 总体分
                scoreOriginality,                 // 原创性评分
                scoreSignificance,                // 重要性评分
                scoreMethodology,                 // 方法学评分
                scorePresentation,                // 呈现质量评分
                recommendation.trim());           // 推荐结论
        
        // 清除本地草稿
        req.getSession().setAttribute("successMsg", "评审意见已成功提交！");
        resp.sendRedirect(req.getContextPath() + "/reviewer/history");
        
        } catch (NumberFormatException e) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数格式错误：请确保评分为数字");
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