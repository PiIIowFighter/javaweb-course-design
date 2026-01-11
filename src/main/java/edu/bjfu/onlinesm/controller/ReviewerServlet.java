package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.mail.MailNotifications; // 添加导入
import edu.bjfu.onlinesm.util.MenuPermissionGuard;
import edu.bjfu.onlinesm.util.PermissionCatalog;
import edu.bjfu.onlinesm.util.HtmlSanitizer;
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
import edu.bjfu.onlinesm.util.PaginationUtil;

/**
 * 审稿人端页面路由控制器。
 */
@WebServlet(name = "ReviewerServlet", urlPatterns = {"/reviewer/*"})
public class ReviewerServlet extends HttpServlet {

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final UserDAO userDAO = new UserDAO();

    // 通知（邮件/站内）。注意：通知发送失败不应影响主流程。
    private final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);

    // ==================== GET：页面展示 ====================

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/dashboard";
        }

        switch (path) {
            case "/dashboard":
                // 审稿人工作台首页：允许拥有任一审稿人入口权限的账号访问（跨角色授予入口）
                if (!ensureAnyReviewerEntry(req, resp)) return;
                req.getRequestDispatcher("/WEB-INF/jsp/reviewer/reviewer_dashboard.jsp")
                        .forward(req, resp);
                break;

            case "/assigned":
                // 待评审稿件列表
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleAssignedList(req, resp);
                break;

            case "/history":
                // 历史评审记录
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_HISTORY)) return;
                handleHistory(req, resp);
                break;

            case "/reviewForm":
                // 填写某条审稿记录的评审意见
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleReviewForm(req, resp);
                break;

            case "/invitation":
                // 查看邀请详情（摘要等），再决定是否接受/拒绝
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleInvitationDetail(req, resp);
                break;

            case "/manuscript":
                // 查看稿件详情（审稿人匿名视图，仅 ACCEPTED/SUBMITTED）
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleManuscriptDetail(req, resp);
                break;

            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ==================== POST：提交评审 ====================

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getPathInfo();
        if (path == null) {
            path = "/submit";
        }

        switch (path) {
            case "/submit":
                // 提交评审意见
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleSubmitReview(req, resp);
                break;
            case "/accept":
                // 接受审稿邀请
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleAcceptInvitation(req, resp);
                break;
            case "/decline":
                // 拒绝审稿邀请
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
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

            // 通知编辑：审稿人已接受邀请（站内 + 邮件）。通知失败不应影响主流程。
            try {
                inAppNotifications.onReviewerResponded(reviewId, true);
            } catch (Exception ignore) {
            }
            try {
                mailNotifications.onReviewerResponded(reviewId, true);
            } catch (Exception ignore) {
            }

            resp.sendRedirect(req.getContextPath() + "/reviewer/assigned");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("接受审稿邀请时数据库出错", e);
        }
    }

    /**
     * 审稿人拒绝审稿邀请：
     *  - 记录拒绝理由与时间，并将该邀请置为非活动状态（不再出现在“待评审稿件列表”）；
     *  - 通知编辑（邮件/站内）；
     *  - 后续如果需要再次邀请同一审稿人，ReviewDAO.inviteReviewer 会在插入前清理未提交的历史记录，避免插入失败。
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
        
        // 清理用户输入，避免在邮件/站内消息中出现不必要的 HTML。
        if (rejectionReason != null) {
            rejectionReason = rejectionReason.trim();
            // 去掉所有 HTML 标签（拒绝理由按纯文本处理）
            rejectionReason = rejectionReason.replaceAll("(?s)<[^>]*>", "");
        }
        if (rejectionReason == null || rejectionReason.isEmpty()) {
            // 没有填写时给一个默认值，保证后续通知可读
            rejectionReason = "时间冲突，无法审稿";
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            User current = getCurrentUser(req);

            // 1) 先在 Reviews 里记录拒绝理由/时间，并将状态置为非活跃（不再出现在待审列表）
            reviewDAO.declineInvitation(reviewId, current.getUserId(), rejectionReason.trim());

            // 2) 通知编辑（邮件/站内）
            // 说明：为兼容旧库（缺少 RejectionReason 列）场景，这里把拒绝理由一并传入，确保通知中可见。
            mailNotifications.onReviewerDeclined(reviewId, rejectionReason);
            inAppNotifications.onReviewerResponded(reviewId, false, rejectionReason);
            
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

    /**
     * 查看稿件详情（匿名视图）：
     *  - 仅允许当前审稿人查看自己 ACCEPTED/SUBMITTED 的审稿记录；
     *  - 不展示作者信息；
     *  - 文件下载仅指向脱密稿（由 /files/preview 在服务端强制执行）。
     */
    private void handleManuscriptDetail(HttpServletRequest req,
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
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看该稿件详情。");
                return;
            }

            if (!("ACCEPTED".equals(review.getStatus()) || "SUBMITTED".equals(review.getStatus()))) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "该审稿记录当前状态不支持查看稿件详情。");
                return;
            }

            Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
            if (m == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应稿件。");
                return;
            }

            req.setAttribute("review", review);
            req.setAttribute("manuscript", m);
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/manuscript_detail.jsp")
                    .forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("加载稿件详情时数据库出错", e);
        }
    }

    private void handleAssignedList(HttpServletRequest req,
                                    HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req);
        try {
            List<Review> list =
                    reviewDAO.findByReviewerAndStatus(current.getUserId(), "UNDER_REVIEW");
            PaginationUtil.apply(req, list, "reviews");
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
        PaginationUtil.apply(req, list, "reviews");
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
    if (isBlank(reviewIdStr)) {
        // 兼容：有的表单传 id
        reviewIdStr = req.getParameter("id");
    }

    String recommendation = req.getParameter("recommendation");

    // 给编辑的意见（富文本）
    String confidentialToEditor = req.getParameter("confidentialToEditor");

    // 给作者的意见（富文本）
    String commentsToAuthor = req.getParameter("commentsToAuthor");
    // 兼容旧字段
    if (isBlankHtml(commentsToAuthor)) {
        commentsToAuthor = req.getParameter("content");
    }

    // 隐藏的关键评价字段（可选）
    String keyEvaluation = req.getParameter("keyEvaluation");
    if (keyEvaluation == null) keyEvaluation = "";

    // === 必填校验 ===
    if (isBlank(reviewIdStr)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 reviewId 参数");
        return;
    }
    if (isBlank(recommendation)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请选择推荐结论（recommendation 为必填）");
        return;
    }
    if (isBlankHtml(confidentialToEditor)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请填写给编辑的保密意见（confidentialToEditor 为必填）");
        return;
    }
    if (isBlankHtml(commentsToAuthor)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请填写给作者的意见（commentsToAuthor 为必填）");
        return;
    }

    // === 富文本基础消毒（防 XSS / 非预期标签）===
    confidentialToEditor = HtmlSanitizer.sanitizeBasic(confidentialToEditor);
    commentsToAuthor = HtmlSanitizer.sanitizeBasic(commentsToAuthor);
    keyEvaluation = HtmlSanitizer.sanitizeBasic(keyEvaluation);

    try {
        int reviewId = Integer.parseInt(reviewIdStr.trim());

        // === 评分（0-10 整数） ===
        Double scoreOriginality = parseScoreInt(req.getParameter("scoreOriginality"), "原创性");
        Double scoreSignificance = parseScoreInt(req.getParameter("scoreSignificance"), "重要性/影响力");
        Double scoreMethodology = parseScoreInt(req.getParameter("scoreMethodology"), "方法/技术质量");
        Double scorePresentation = parseScoreInt(req.getParameter("scorePresentation"), "表达/结构");

        double sum = scoreOriginality + scoreSignificance + scoreMethodology + scorePresentation;
        int cnt = 4;

        // 新增维度（表单已提供 5 项：实验/文献/结论/诚信/实用性）。
        // 以前版本只用于计算总体分，但没有落库，导致编辑端/主编端查看详情时分项分值与审稿人填写不一致。
        // 这里同时用于计算总体分，并在 submit 时一并写入 dbo.Reviews。
        String pExp = req.getParameter("scoreExperimentation");
        String pLit = req.getParameter("scoreLiteratureReview");
        String pCon = req.getParameter("scoreConclusions");
        String pInt = req.getParameter("scoreAcademicIntegrity");
        String pPra = req.getParameter("scorePracticality");

        Double scoreExperimentation = null;
        Double scoreLiteratureReview = null;
        Double scoreConclusions = null;
        Double scoreAcademicIntegrity = null;
        Double scorePracticality = null;

        boolean hasAnyNew =
                !isBlank(pExp) || !isBlank(pLit) || !isBlank(pCon) || !isBlank(pInt) || !isBlank(pPra);

        if (hasAnyNew) {
            scoreExperimentation = parseScoreInt(pExp, "实验/数据分析");
            scoreLiteratureReview = parseScoreInt(pLit, "文献综述");
            scoreConclusions = parseScoreInt(pCon, "结论与讨论");
            scoreAcademicIntegrity = parseScoreInt(pInt, "学术规范性");
            scorePracticality = parseScoreInt(pPra, "实用性");

            sum += scoreExperimentation + scoreLiteratureReview + scoreConclusions + scoreAcademicIntegrity + scorePracticality;
            cnt += 5;
        }

        // === 总体分锁死：忽略前端传入的 score，统一服务端按维度均值重算 ===
        Double scoreOverall = Math.round((sum / cnt) * 10.0) / 10.0; // 1 位小数
        checkScoreRange(scoreOverall);

        // === 提交评审 ===
        // 新版：同时写入 9 个分项维度，避免编辑端查看“审稿意见详情”时分项分值错误/缺失
        reviewDAO.submitReviewV3(
                reviewId,
                current.getUserId(),
                commentsToAuthor == null ? "" : commentsToAuthor.trim(),          // 给作者的意见
                confidentialToEditor == null ? "" : confidentialToEditor.trim(),  // 给编辑的保密意见
                keyEvaluation == null ? "" : keyEvaluation.trim(),                // 关键评价（可空）
                scoreOverall,                     // 总体分（服务端重算）
                scoreOriginality,                 // 原创性评分
                scoreSignificance,                // 重要性评分
                scoreMethodology,                 // 方法学评分
                scorePresentation,                // 呈现质量评分
                scoreExperimentation,             // 实验/数据分析
                scoreLiteratureReview,            // 文献综述
                scoreConclusions,                 // 结论与讨论
                scoreAcademicIntegrity,           // 学术规范性
                scorePracticality,                // 实用性
                recommendation.trim());           // 推荐结论

        req.getSession().setAttribute("successMsg", "评审意见已成功提交！");
        resp.sendRedirect(req.getContextPath() + "/reviewer/history");

    } catch (NumberFormatException e) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数格式错误：请确保评分为数字且 reviewId 为整数");
    } catch (IllegalArgumentException e) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    } catch (SQLException e) {
        throw new ServletException("提交评审意见时数据库出错", e);
    }
    }

    private Double parseScoreInt(String s, String fieldLabel) {
    if (isBlank(s)) {
        throw new IllegalArgumentException("请填写评分：" + fieldLabel);
    }
    double v;
    try {
        v = Double.parseDouble(s.trim());
    } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("评分【" + fieldLabel + "】必须为数字（0-10 整数）。");
    }
    if (v < 0 || v > 10) {
        throw new IllegalArgumentException("评分【" + fieldLabel + "】必须在 0~10 范围内。");
    }
    if (Math.floor(v) != v) {
        throw new IllegalArgumentException("评分【" + fieldLabel + "】必须为整数（0-10）。");
    }
    return v;
}


    private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
}


    private boolean isBlankHtml(String html) {
    if (isBlank(html)) return true;
    String text = stripHtml(html);
    return text.trim().isEmpty();
}


    private String stripHtml(String html) {
    if (html == null) return "";
    String t = html;
    // 去掉 script/style，防止误判
    t = t.replaceAll("(?is)<script.*?>.*?</script>", " ");
    t = t.replaceAll("(?is)<style.*?>.*?</style>", " ");
    // 去掉标签
    t = t.replaceAll("(?s)<[^>]*>", " ");
    // 常见空白实体
    t = t.replace("&nbsp;", " ");
    // 合并空白
    t = t.replaceAll("\\s+", " ");
    return t;
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
    private boolean requireMenu(HttpServletRequest req, HttpServletResponse resp, String permKey)
            throws IOException, ServletException {
        return MenuPermissionGuard.require(req, resp, permKey);
    }

    /**
     * 审稿人模块“首页/仪表盘”允许只要拥有任一审稿人入口权限即可访问（跨角色授予入口）。
     */
    private boolean ensureAnyReviewerEntry(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        User u = getCurrentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }

        boolean ok = MenuPermissionGuard.has(req, PermissionCatalog.MENU_REVIEWER_ASSIGNED)
                || MenuPermissionGuard.has(req, PermissionCatalog.MENU_REVIEWER_HISTORY);

        if (!ok) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("error", "当前账号无权限访问审稿人模块。");
            req.getRequestDispatcher("/WEB-INF/jsp/error/access_denied.jsp").forward(req, resp);
            return false;
        }
        return true;
    }
}