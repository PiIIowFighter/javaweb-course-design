package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.ManuscriptFundingDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.mail.MailNotifications; 
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


@WebServlet(name = "ReviewerServlet", urlPatterns = {"/reviewer/*"})
public class ReviewerServlet extends HttpServlet {

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ManuscriptFundingDAO fundingDAO = new ManuscriptFundingDAO();
    private final UserDAO userDAO = new UserDAO();

    
    private final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);

    

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/dashboard";
        }

        switch (path) {
            case "/dashboard":
                
                if (!ensureAnyReviewerEntry(req, resp)) return;
                req.getRequestDispatcher("/WEB-INF/jsp/reviewer/reviewer_dashboard.jsp")
                        .forward(req, resp);
                break;

            case "/assigned":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleAssignedList(req, resp);
                break;

            case "/history":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_HISTORY)) return;
                handleHistory(req, resp);
                break;

            case "/reviewForm":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleReviewForm(req, resp);
                break;

            case "/invitation":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleInvitationDetail(req, resp);
                break;

            case "/manuscript":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleManuscriptDetail(req, resp);
                break;

            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getPathInfo();
        if (path == null) {
            path = "/submit";
        }

        switch (path) {
            case "/submit":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleSubmitReview(req, resp);
                break;
            case "/accept":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleAcceptInvitation(req, resp);
                break;
            case "/decline":
                
                if (!requireMenu(req, resp, PermissionCatalog.MENU_REVIEWER_ASSIGNED)) return;
                handleDeclineInvitation(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    

    

    
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

    
    private void handleDeclineInvitation(HttpServletRequest req,
                                         HttpServletResponse resp)
            throws IOException, ServletException {
        String reviewIdStr = req.getParameter("reviewId");
        String rejectionReason = req.getParameter("rejectionReason");
        
        if (reviewIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少审稿记录 ID。");
            return;
        }
        
        
        if (rejectionReason != null) {
            rejectionReason = rejectionReason.trim();
            
            rejectionReason = rejectionReason.replaceAll("(?s)<[^>]*>", "");
        }
        if (rejectionReason == null || rejectionReason.isEmpty()) {
            
            rejectionReason = "时间冲突，无法审稿";
        }

        try {
            int reviewId = Integer.parseInt(reviewIdStr);
            User current = getCurrentUser(req);

            
            reviewDAO.declineInvitation(reviewId, current.getUserId(), rejectionReason.trim());

            
            
            mailNotifications.onReviewerDeclined(reviewId, rejectionReason);
            inAppNotifications.onReviewerResponded(reviewId, false, rejectionReason);
            
            
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
            req.setAttribute("fundings", fundingDAO.findByManuscriptId(m.getManuscriptId()));
            req.setAttribute("fundings", fundingDAO.findByManuscriptId(m.getManuscriptId()));
            req.getRequestDispatcher("/WEB-INF/jsp/reviewer/invitation_detail.jsp")
                    .forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "审稿记录 ID 非法。");
        } catch (SQLException e) {
            throw new ServletException("加载邀请详情时数据库出错", e);
        }
    }

    
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

    
    private void handleSubmitReview(HttpServletRequest req,
                                HttpServletResponse resp)
        throws ServletException, IOException {

    User current = getCurrentUser(req);

    String reviewIdStr = req.getParameter("reviewId");
    if (isBlank(reviewIdStr)) {
        
        reviewIdStr = req.getParameter("id");
    }

    String recommendation = req.getParameter("recommendation");

    
    String confidentialToEditor = req.getParameter("confidentialToEditor");

    
    String commentsToAuthor = req.getParameter("commentsToAuthor");
    
    if (isBlankHtml(commentsToAuthor)) {
        commentsToAuthor = req.getParameter("content");
    }

    
    String keyEvaluation = req.getParameter("keyEvaluation");
    if (keyEvaluation == null) keyEvaluation = "";

    
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

    
    confidentialToEditor = HtmlSanitizer.sanitizeBasic(confidentialToEditor);
    commentsToAuthor = HtmlSanitizer.sanitizeBasic(commentsToAuthor);
    keyEvaluation = HtmlSanitizer.sanitizeBasic(keyEvaluation);

    try {
        int reviewId = Integer.parseInt(reviewIdStr.trim());

        
        Double scoreOriginality = parseScoreInt(req.getParameter("scoreOriginality"), "原创性");
        Double scoreSignificance = parseScoreInt(req.getParameter("scoreSignificance"), "重要性/影响力");
        Double scoreMethodology = parseScoreInt(req.getParameter("scoreMethodology"), "方法/技术质量");
        Double scorePresentation = parseScoreInt(req.getParameter("scorePresentation"), "表达/结构");

        double sum = scoreOriginality + scoreSignificance + scoreMethodology + scorePresentation;
        int cnt = 4;

        
        
        
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

        
        Double scoreOverall = Math.round((sum / cnt) * 10.0) / 10.0; 
        checkScoreRange(scoreOverall);

        
        
        reviewDAO.submitReviewV3(
                reviewId,
                current.getUserId(),
                commentsToAuthor == null ? "" : commentsToAuthor.trim(),          
                confidentialToEditor == null ? "" : confidentialToEditor.trim(),  
                keyEvaluation == null ? "" : keyEvaluation.trim(),                
                scoreOverall,                     
                scoreOriginality,                 
                scoreSignificance,                
                scoreMethodology,                 
                scorePresentation,                
                scoreExperimentation,             
                scoreLiteratureReview,            
                scoreConclusions,                 
                scoreAcademicIntegrity,           
                scorePracticality,                
                recommendation.trim());           


        
        try {
            inAppNotifications.onReviewSubmitted(reviewId);
        } catch (Exception ignore) {
        }

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
    
    t = t.replaceAll("(?is)<script.*?>.*?</script>", " ");
    t = t.replaceAll("(?is)<style.*?>.*?</style>", " ");
    
    t = t.replaceAll("(?s)<[^>]*>", " ");
    
    t = t.replace("&nbsp;", " ");
    
    t = t.replaceAll("\\s+", " ");
    return t;
}


    private void checkScoreRange(Double v) {
        if (v == null || v < 0 || v > 10) {
            throw new IllegalArgumentException("评分必须在 0~10 范围内。");
        }
    }

    

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

