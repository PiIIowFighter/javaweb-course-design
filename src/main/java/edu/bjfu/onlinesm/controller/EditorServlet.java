package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.dao.ManuscriptFundingDAO;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.NotificationDAO;
import edu.bjfu.onlinesm.dao.EditorSuggestionDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.Notification;
import edu.bjfu.onlinesm.model.EditorSuggestion;
import edu.bjfu.onlinesm.dao.FormalCheckResultDAO;
import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.service.FormalCheckService;
import edu.bjfu.onlinesm.service.PlagiarismCheckService;
import edu.bjfu.onlinesm.util.OperationLogger;
import edu.bjfu.onlinesm.dao.ManuscriptAssignmentDAO;
import edu.bjfu.onlinesm.dao.ManuscriptRecommendedReviewerDAO;
import edu.bjfu.onlinesm.dao.ManuscriptFundingDAO;
import edu.bjfu.onlinesm.dao.ManuscriptAuthorDAO;
import edu.bjfu.onlinesm.dao.ManuscriptVersionDAO;
import edu.bjfu.onlinesm.model.ManuscriptRecommendedReviewer;
import edu.bjfu.onlinesm.model.ManuscriptAuthor;
import edu.bjfu.onlinesm.model.ManuscriptVersion;
import edu.bjfu.onlinesm.util.mail.MailNotifications;
import edu.bjfu.onlinesm.util.mail.MailService;
import edu.bjfu.onlinesm.util.notify.InAppNotifications;
import edu.bjfu.onlinesm.util.UploadPathUtil;
import edu.bjfu.onlinesm.util.PdfTextUtil;
import edu.bjfu.onlinesm.util.MenuPermissionGuard;
import edu.bjfu.onlinesm.util.FileTextUtil;
import edu.bjfu.onlinesm.util.PermissionCatalog;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import org.json.JSONObject;
import edu.bjfu.onlinesm.util.PaginationUtil;


public abstract class EditorServlet extends HttpServlet {

    protected final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    protected final UserDAO userDAO = new UserDAO();
    protected final ReviewDAO reviewDAO = new ReviewDAO();
    protected final NotificationDAO notificationDAO = new NotificationDAO();
    protected final EditorSuggestionDAO editorSuggestionDAO = new EditorSuggestionDAO();
    protected final ManuscriptAssignmentDAO assignmentDAO = new ManuscriptAssignmentDAO();
    protected final ManuscriptRecommendedReviewerDAO recommendedReviewerDAO = new ManuscriptRecommendedReviewerDAO();
    protected final ManuscriptAuthorDAO manuscriptAuthorDAO = new ManuscriptAuthorDAO();
    protected final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();
    protected final FormalCheckResultDAO formalCheckResultDAO = new FormalCheckResultDAO();
    protected final FormalCheckService formalCheckService = new FormalCheckService();
    protected final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    protected final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final ManuscriptFundingDAO fundingDAO = new ManuscriptFundingDAO();

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }


        
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/dashboard";
        }

        
        String requiredPerm = requiredMenuPermission(path);
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        try {
            switch (path) {
                case "/dashboard":
                    resp.sendRedirect(req.getContextPath() + "/dashboard");
                    break;

                case "/formalCheck":
                    
                    handleFormalCheckList(req, resp, current);
                    break;

                case "/formalCheck/review":
                    
                    handleFormalCheckReviewPage(req, resp, current);
                    break;

                case "/formalCheck/history":
                    
                    handleFormalCheckHistoryPage(req, resp, current);
                    break;

                case "/formalCheck/history/detail":
                    
                    handleFormalCheckHistoryDetailPage(req, resp, current);
                    break;

                case "/desk":
                    
                    handleDeskList(req, resp, current);
                    break;

                case "/toAssign":
                    
                    handleToAssignList(req, resp, current);
                    break;

                case "/withEditor":
                    
                    handleWithEditorList(req, resp, current);
                    break;

                case "/underReview":
                    
                    handleUnderReviewList(req, resp, current);
                    break;

                case "/finalDecision":
                    
                    handleFinalDecisionList(req, resp, current);
                    break;

                case "/recommend":
                    
                    handleEditorRecommendPage(req, resp, current);
                    break;

                case "/recommend/detail":
                    
                    handleRecommendManuscriptDetailPage(req, resp, current);
                    break;

                case "/review/monitor":
                    
                    handleReviewMonitorPage(req, resp, current);
                    break;

                case "/review/remindForm":
                    
                    handleReviewRemindFormPage(req, resp, current);
                    break;

                case "/review/detail":
                    
                    handleEditorReviewDetailPage(req, resp, current);
                    break;

                case "/review/select":
                    
                    handleReviewSelectPage(req, resp, current);
                    break;

                case "/authorComm":
                    
                    handleAuthorCommList(req, resp, current);
                    break;

                case "/author/message":
                    
                    handleAuthorMessagePage(req, resp, current);
                    break;

                case "/reviewers":
                    
                    handleReviewerPoolPage(req, resp, current);
                    break;

                case "/overview":
                    
                    handleChiefOverview(req, resp, current);
                    break;

                case "/special":
                    
                    handleChiefSpecialPage(req, resp, current);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("查询稿件列表时访问数据库出错", e);
        }
    }

    
    protected void handleFormalCheckList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list = manuscriptDAO.findByStatuses("SUBMITTED", "FORMAL_CHECK");
        PaginationUtil.apply(req, list, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_list.jsp")
                .forward(req, resp);
    }

    protected void handleFormalCheckReviewPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!MenuPermissionGuard.has(req, PermissionCatalog.MENU_EO_FORMAL_CHECK)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以进入形式审查页面。");
            return;
        }

        String midStr = req.getParameter("manuscriptId");
        if (midStr == null || midStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/editor/formalCheck");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(midStr.trim());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 参数无效。");
            return;
        }

        Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
        if (manuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("SUBMITTED".equalsIgnoreCase(manuscript.getCurrentStatus())) {
            manuscript.setCurrentStatus("FORMAL_CHECK");
        }

        FormalCheckResult latest = formalCheckResultDAO.findByManuscriptId(manuscriptId);

        
        
        

int pdfPageCount = 0;
try {
    ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);

    
    if (currentVer != null) {
        String pdfPath = null;
        if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
            pdfPath = currentVer.getFileOriginalPath();
        } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
            pdfPath = currentVer.getFileAnonymousPath();
        }
        if (pdfPath != null) {
            pdfPageCount = PdfTextUtil.extractPageCount(new File(pdfPath));
        }
    }
} catch (Exception ignore) {
    pdfPageCount = 0;
}


        int abstractCount = 0;
        try {
            abstractCount = formalCheckService.computeAbstractCount(manuscript.getAbstractText());
        } catch (Exception ignore) {
            abstractCount = 0;
        }

        req.setAttribute("manuscript", manuscript);
        req.setAttribute("formalCheckResult", latest);
        req.setAttribute("pdfPageCount", pdfPageCount);
        req.setAttribute("bodyCount", pdfPageCount); 
        req.setAttribute("abstractCount", abstractCount);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_review.jsp").forward(req, resp);
    }

    protected void handleFormalCheckHistoryPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!MenuPermissionGuard.has(req, PermissionCatalog.MENU_EO_FORMAL_HISTORY)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以查看审查历史。");
            return;
        }

        List<FormalCheckResult> history = formalCheckResultDAO.findByReviewerId(current.getUserId());
        java.util.Map<Integer, Manuscript> manuscriptMap = new java.util.HashMap<>();
        if (history != null) {
            for (FormalCheckResult r : history) {
                if (r == null || r.getManuscriptId() == null) continue;
                int mid = r.getManuscriptId();
                if (!manuscriptMap.containsKey(mid)) {
                    try {
                        Manuscript m = manuscriptDAO.findById(mid);
                        if (m != null) manuscriptMap.put(mid, m);
                    } catch (Exception ignore) {
                        
                    }
                }
            }
        }

        PaginationUtil.apply(req, history, "history");
        req.setAttribute("manuscriptMap", manuscriptMap);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_history.jsp").forward(req, resp);
    }

    protected void handleFormalCheckHistoryDetailPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!MenuPermissionGuard.has(req, PermissionCatalog.MENU_EO_FORMAL_HISTORY)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以查看审查记录详情。");
            return;
        }

        String idStr = req.getParameter("checkId");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/editor/formalCheck/history");
            return;
        }

        int checkId;
        try {
            checkId = Integer.parseInt(idStr.trim());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "checkId 参数无效。");
            return;
        }

        FormalCheckResult result = formalCheckResultDAO.findById(checkId);
        Manuscript manuscript = null;
        if (result != null && result.getManuscriptId() != null) {
            try {
                manuscript = manuscriptDAO.findById(result.getManuscriptId());
            } catch (Exception ignore) {
            }
        }

        req.setAttribute("result", result);
        req.setAttribute("manuscript", manuscript);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_history_detail.jsp").forward(req, resp);
    }




    protected void handleDeskList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> deskList = manuscriptDAO.findByStatuses("DESK_REVIEW_INITIAL");
        PaginationUtil.apply(req, deskList, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/desk_list.jsp")
                .forward(req, resp);
    }

    protected void handleToAssignList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> toAssignList = manuscriptDAO.findByStatuses("TO_ASSIGN");
        PaginationUtil.apply(req, toAssignList, "manuscripts");
        
        if ("EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            List<User> editors = userDAO.findByRoleCode("EDITOR");
            req.setAttribute("editorList", editors);

            
            Map<Integer, List<User>> recommendedEditorsMap = new HashMap<>();
            for (Manuscript m : toAssignList) {
                recommendedEditorsMap.put(m.getManuscriptId(), rankEditorsByResearchArea(editors, m));
            }
            req.setAttribute("recommendedEditorsMap", recommendedEditorsMap);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/editor/to_assign_list.jsp")
                .forward(req, resp);
    }



    
    protected void handlePickEditorPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (current == null || !"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "仅主编可使用该功能。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少参数 manuscriptId。");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(idStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
        if (manuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        String q = req.getParameter("q");
        if (q != null) q = q.trim();
        String areaParam = req.getParameter("area");
        String area = (areaParam == null) ? null : areaParam.trim();

        
        boolean autoFillArea = false;
        if (areaParam == null) {
            String msArea = manuscript.getSubjectArea();
            if (msArea != null && !msArea.trim().isEmpty()) {
                area = msArea.trim();
                autoFillArea = true;
            }
        }
        if (area == null) area = "";

        String mo = req.getParameter("matchOnly");
        boolean matchOnly = "1".equals(mo) || "true".equalsIgnoreCase(mo) || "on".equalsIgnoreCase(mo);

        
        String msArea = manuscript.getSubjectArea();
        String msKw = manuscript.getKeywords();
        Set<String> msTokens = tokenizeKeywords((msArea == null ? "" : msArea) + " " + (msKw == null ? "" : msKw));

        
        List<User> editors = userDAO.findByRoleCode("EDITOR");
        List<User> filtered = new ArrayList<>();
        Map<Integer, Integer> scoreMap = new HashMap<>();

        String qLower = (q == null) ? "" : q.toLowerCase();
        String areaLower = area.toLowerCase();

        for (User e : editors) {
            if (e == null) continue;

            
            String st = e.getStatus();
            if (st != null && !"ACTIVE".equalsIgnoreCase(st)) continue;

            
            if (q != null && !q.isEmpty()) {
                if (!containsIgnoreCase(e.getFullName(), qLower)
                        && !containsIgnoreCase(e.getUsername(), qLower)
                        && !containsIgnoreCase(e.getEmail(), qLower)
                        && !containsIgnoreCase(e.getAffiliation(), qLower)
                        && !containsIgnoreCase(e.getResearchArea(), qLower)) {
                    continue;
                }
            }

            
            if (!area.isEmpty()) {
                String ra = e.getResearchArea();
                String raLower = (ra == null) ? "" : ra.toLowerCase();
                if (!raLower.contains(areaLower)) {
                    
                    Set<String> areaTokens = tokenizeKeywords(area);
                    if (!areaTokens.isEmpty()) {
                        boolean hit = false;
                        for (String t : areaTokens) {
                            if (t == null) continue;
                            String tl = t.toLowerCase();
                            if (!tl.isEmpty() && raLower.contains(tl)) {
                                hit = true;
                                break;
                            }
                        }
                        if (!hit) continue;
                    } else {
                        continue;
                    }
                }
            }

            int score = editorMatchScore(e, manuscript.getSubjectArea(), msTokens);
            scoreMap.put(e.getUserId(), score);

            if (matchOnly && score <= 0) continue;

            filtered.add(e);
        }

        
        filtered.sort((a, b) -> {
            int sa = scoreMap.getOrDefault(a.getUserId(), 0);
            int sb = scoreMap.getOrDefault(b.getUserId(), 0);
            if (sa != sb) return Integer.compare(sb, sa);

            boolean ha = a.getResearchArea() != null && !a.getResearchArea().trim().isEmpty();
            boolean hb = b.getResearchArea() != null && !b.getResearchArea().trim().isEmpty();
            if (ha != hb) return hb ? 1 : -1;

            return Integer.compare(a.getUserId(), b.getUserId());
        });

        
        PaginationUtil.apply(req, filtered, "editors");

        req.setAttribute("manuscript", manuscript);
        req.setAttribute("filterQ", q == null ? "" : q);
        req.setAttribute("filterArea", area);
        req.setAttribute("filterMatchOnly", matchOnly);
        req.setAttribute("autoFillArea", autoFillArea);
        req.setAttribute("editorScoreMap", scoreMap);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/pick_editor.jsp")
                .forward(req, resp);
    }

    private boolean containsIgnoreCase(String s, String qLower) {
        if (qLower == null || qLower.isEmpty()) return true;
        if (s == null) return false;
        return s.toLowerCase().contains(qLower);
    }


    protected void handleWithEditorList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        
        List<Manuscript> list;
        if ("EDITOR".equals(current.getRoleCode())) {
            list = manuscriptDAO.findByStatusesForEditor(current.getUserId(), "WITH_EDITOR");
        } else {
            list = manuscriptDAO.findByStatuses("WITH_EDITOR");
        }

        
        Map<Integer, edu.bjfu.onlinesm.model.ManuscriptAssignment> latestAssignments = new HashMap<>();
        Map<Integer, User> editors = new HashMap<>();
        for (Manuscript m : list) {
            int mid = m.getManuscriptId();
            try {
                edu.bjfu.onlinesm.model.ManuscriptAssignment ma = assignmentDAO.findLatestByManuscript(mid);
                if (ma != null) {
                    latestAssignments.put(mid, ma);
                    int eid = ma.getEditorId();
                    if (!editors.containsKey(eid)) {
                        User u = userDAO.findById(eid);
                        if (u != null) editors.put(eid, u);
                    }
                } else {
                    
                    Integer eid = manuscriptDAO.findCurrentEditorId(mid);
                    if (eid != null && !editors.containsKey(eid)) {
                        User u = userDAO.findById(eid);
                        if (u != null) editors.put(eid, u);
                    }
                }
            } catch (Exception ignore) {
                
            }
        }

        PaginationUtil.apply(req, list, "manuscripts");
        req.setAttribute("latestAssignments", latestAssignments);
        req.setAttribute("editors", editors);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/with_editor_list.jsp")
                .forward(req, resp);
    }

    protected void handleUnderReviewList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {
        
        
        reviewDAO.promoteAllUnderReviewManuscriptsIfReady();

        
        
        List<Manuscript> underReviewList;
        if ("EDITOR".equals(current.getRoleCode())) {
            underReviewList = manuscriptDAO.findByStatusesForEditor(current.getUserId(), "UNDER_REVIEW");
        } else {
            underReviewList = manuscriptDAO.findByStatuses("UNDER_REVIEW");
        }

        req.setAttribute("underReviewList", underReviewList);

        
        PaginationUtil.apply(req, underReviewList, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/under_review_list.jsp")
                .forward(req, resp);
    }

    
    protected void handleEditorRecommendPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {

            
            List<Manuscript> ready = manuscriptDAO.findByStatuses("EDITOR_RECOMMENDATION");

            
            if ("EDITOR".equals(current.getRoleCode())) {
                List<Manuscript> filtered = new ArrayList<>();
                for (Manuscript m : ready) {
                    Integer editorId = manuscriptDAO.findCurrentEditorId(m.getManuscriptId());
                    if (java.util.Objects.equals(editorId, current.getUserId())) {
                        filtered.add(m);
                    }
                }
                ready = filtered;
            }

            req.setAttribute("readyList", ready);
            req.getRequestDispatcher("/WEB-INF/jsp/editor/recommend_list.jsp")
                    .forward(req, resp);
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (!java.util.Objects.equals(editorId, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        
        List<Review> all = reviewDAO.findByManuscript(manuscriptId);
        List<Review> submitted = new ArrayList<>();
        Map<String, Integer> stats = new HashMap<>();
        for (Review r : all) {
            if ("SUBMITTED".equalsIgnoreCase(r.getStatus())) {
                submitted.add(r);
                String rec = r.getRecommendation();
                if (rec == null) rec = "UNKNOWN";
                stats.put(rec, stats.getOrDefault(rec, 0) + 1);
            }
        }

        EditorSuggestion existing = editorSuggestionDAO.findByManuscriptId(manuscriptId);

        req.setAttribute("manuscript", m);
        req.setAttribute("submittedReviews", submitted);
        req.setAttribute("recommendStats", stats);
        req.setAttribute("editorSuggestion", existing);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/recommend_form.jsp")
                .forward(req, resp);
    }

protected void handleFinalDecisionList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> finalList = manuscriptDAO.findByStatuses(
                "EDITOR_RECOMMENDATION",
                "FINAL_DECISION_PENDING",
                "ACCEPTED",
                "REJECTED"
        );

        
        List<Integer> ids = new ArrayList<>();
        for (Manuscript m : finalList) {
            ids.add(m.getManuscriptId());
        }
        Map<Integer, EditorSuggestion> suggestionMap = editorSuggestionDAO.findByManuscriptIds(ids);
        req.setAttribute("suggestionMap", suggestionMap);

        PaginationUtil.apply(req, finalList, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/final_decision_list.jsp")
                .forward(req, resp);
    }

    
    protected void handleRecommendManuscriptDetailPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {
    	
        String midStr = req.getParameter("manuscriptId");
        if (midStr == null || midStr.trim().isEmpty()) {
            midStr = req.getParameter("id");
        }
        if (midStr == null || midStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/editor/recommend");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(midStr);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 参数无效。");
            return;
        }

        Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
        if (manuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (!java.util.Objects.equals(editorId, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "您无权查看该稿件的详情。");
                return;
            }
        }

        
        List<ManuscriptAuthor> authors = manuscriptAuthorDAO.findByManuscriptId(manuscriptId);

        
        ManuscriptVersion currentVersion = versionDAO.findCurrentByManuscriptId(manuscriptId);
        FormalCheckResult formalCheckResult = formalCheckResultDAO.findByManuscriptId(manuscriptId);

        
        List<Review> reviews = reviewDAO.findByManuscript(manuscriptId);
        List<ManuscriptRecommendedReviewer> recommendedReviewers = recommendedReviewerDAO.findByManuscriptId(manuscriptId);

        req.setAttribute("manuscript", manuscript);
        req.setAttribute("authors", authors);
        req.setAttribute("currentVersion", currentVersion);
        req.setAttribute("formalCheckResult", formalCheckResult);
        req.setAttribute("reviews", reviews);
        req.setAttribute("recommendedReviewers", recommendedReviewers);
        req.setAttribute("fundings", fundingDAO.findByManuscriptId(manuscriptId));

        req.getRequestDispatcher("/WEB-INF/jsp/editor/recommend_manuscript_detail.jsp")
                .forward(req, resp);
    }

    
    protected void handleEditorManuscriptDetailPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            
            manuscriptIdStr = req.getParameter("id");
        }
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        List<ManuscriptAuthor> authors = manuscriptAuthorDAO.findByManuscriptId(manuscriptId);
        List<ManuscriptRecommendedReviewer> recReviewers = recommendedReviewerDAO.findByManuscriptId(manuscriptId);
        List<Review> reviews = reviewDAO.findByManuscript(manuscriptId);

        Map<Integer, User> reviewerMap = new HashMap<>();
        for (Review r : reviews) {
            int rid = r.getReviewerId();
            if (!reviewerMap.containsKey(rid)) {
                User u = userDAO.findById(rid);
                if (u != null) {
                    reviewerMap.put(rid, u);
                }
            }
        }

        
        String backToUrl = req.getContextPath() + req.getServletPath() + "/detail?manuscriptId=" + manuscriptId;

        req.setAttribute("manuscript", m);
        req.setAttribute("authors", authors);
        req.setAttribute("fundings", fundingDAO.findByManuscriptId(manuscriptId));
        req.setAttribute("recommendedReviewers", recReviewers);
        req.setAttribute("reviews", reviews);
        req.setAttribute("reviewerMap", reviewerMap);
        req.setAttribute("backToUrl", backToUrl);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/editor_manuscript_detail.jsp")
                .forward(req, resp);
    }

    
    protected void handleExternalInviteReviewerPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        String backToUrl = req.getParameter("backTo");
        if (backToUrl == null || backToUrl.trim().isEmpty()) {
            
            String detailSp;
            if ("WITH_EDITOR".equals(m.getCurrentStatus())) {
                detailSp = "/editor/withEditor";
            } else if ("UNDER_REVIEW".equals(m.getCurrentStatus())) {
                detailSp = "/editor/underReview";
            } else {
                detailSp = "/editor/withEditor";
            }
            backToUrl = req.getContextPath() + detailSp + "/detail?manuscriptId=" + manuscriptId;
        }

        
        String selectUrl = req.getContextPath() + "/editor/review/select?manuscriptId=" + manuscriptId;
        selectUrl = appendQueryParam(selectUrl, "backTo", URLEncoder.encode(backToUrl, "UTF-8"));

        req.setAttribute("manuscript", m);
        req.setAttribute("backToUrl", backToUrl);
        req.setAttribute("selectUrl", selectUrl);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_invite_external.jsp")
                .forward(req, resp);
    }


    
    protected void handleReviewerPoolPage (HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String reviewerKeyword = req.getParameter("reviewerKeyword");
        if (reviewerKeyword == null) reviewerKeyword = "";
        String kw = reviewerKeyword.trim().toLowerCase();

        List<User> reviewers = userDAO.findByRoleCode("REVIEWER");
        if (!kw.isEmpty() && reviewers != null) {
            List<User> filtered = new ArrayList<>();
            for (User u : reviewers) {
                if (u == null) continue;
                String un = u.getUsername();
                String em = u.getEmail();
                String fn = u.getFullName();
                if ((un != null && un.toLowerCase().contains(kw))
                        || (em != null && em.toLowerCase().contains(kw))
                        || (fn != null && fn.toLowerCase().contains(kw))) {
                    filtered.add(u);
                }
            }
            reviewers = filtered;
        }

        
        req.setAttribute("msg", req.getParameter("msg"));
        req.setAttribute("error", req.getParameter("error"));

        
        PaginationUtil.apply(req, reviewers, "reviewers");

        
        req.setAttribute("reviewerKeyword", reviewerKeyword);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_pool.jsp")
                .forward(req, resp);
    }

    
    protected void handleChiefOverview(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list = manuscriptDAO.findAllForChief();
        PaginationUtil.apply(req, list, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/chief_overview.jsp")
                .forward(req, resp);
    }

    
    protected void handleChiefSpecialPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        
        List<Manuscript> list = manuscriptDAO.findByStatuses(
                "EDITOR_RECOMMENDATION",
                "FINAL_DECISION_PENDING",
                "REVISION",
                "ACCEPTED",
                "REJECTED"
        );
        PaginationUtil.apply(req, list, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/chief_special.jsp")
                .forward(req, resp);
    }

    protected User getCurrentUser(HttpServletRequest req) {
        Object obj = req.getSession().getAttribute("currentUser");
        if (obj instanceof User) {
            return (User) obj;
        }
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) {
            path = "";
        }

        
        String requiredPerm = requiredMenuPermission(path);
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        try {
            switch (path) {
                case "/formalCheck":
                case "/formalCheck/autoCheck":
                    
                    handleFormalCheckPost(req, resp, current);
                    break;

                case "/desk":
                    
                    handleDeskDecisionPost(req, resp, current);
                    break;

                case "/toAssign":
                    
                    handleAssignEditorPost(req, resp, current);
                    break;

                case "/finalDecision":
                    
                    handleFinalDecisionPost(req, resp, current);
                    break;

                case "/reviewers":
                    
                    handleReviewerPoolPost(req, resp, current);
                    break;

                case "/review/invite":
                    
                    handleInviteReviewerPost(req, resp, current);
                    break;

                case "/review/inviteExternal":
                    
                    handleInviteExternalReviewerPost(req, resp, current);
                    break;

                case "/review/remind":
                    
                    handleRemindReviewerPost(req, resp, current);
                    break;

                case "/review/remindCustom":
                    
                    handleRemindReviewerCustomPost(req, resp, current);
                    break;

                case "/review/cancel":
                    
                    handleCancelReviewerPost(req, resp, current);
                    break;

                case "/author/message":
                    
                    handleSendAuthorMessagePost(req, resp, current);
                    break;

                case "/review/autoRemindNow":
                    
                    handleAutoRemindNowPost(req, resp, current);
                    break;

                case "/recommend":
                    
                    handleEditorRecommendPost(req, resp, current);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理编辑操作时访问数据库出错", e);
        }
    }
    
    
    protected void handleInviteReviewerPost(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          User current)
            throws IOException, SQLException {

        

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String[] reviewerIdParams = req.getParameterValues("reviewerIds");
        String dueDateStr = req.getParameter("dueDate"); 
        String backTo = req.getParameter("backTo");      

        
        if (reviewerIdParams == null || reviewerIdParams.length == 0) {
            String singleReviewerId = req.getParameter("reviewerId");
            if (singleReviewerId != null && !singleReviewerId.trim().isEmpty()) {
                reviewerIdParams = new String[]{singleReviewerId.trim()};
            }
        }

        if (manuscriptIdStr == null || reviewerIdParams == null || reviewerIdParams.length == 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        LocalDateTime dueAt = null;
        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            LocalDate d = LocalDate.parse(dueDateStr.trim());
            
            dueAt = d.atTime(23, 59, 59);
        }

        
        Set<Integer> declinedReviewerIds = new HashSet<>();
        Set<Integer> alreadyAssignedReviewerIds = new HashSet<>();
        try {
            List<Review> existing = reviewDAO.findByManuscript(manuscriptId);
            for (Review r : existing) {
                if (r == null) continue;
                String st = r.getStatus();
                boolean isDeclined = "DECLINED".equals(st)
                        || ("EXPIRED".equals(st) && (r.getDeclinedAt() != null
                        || (r.getRejectionReason() != null && !r.getRejectionReason().trim().isEmpty())));
                if (isDeclined) {
                    declinedReviewerIds.add(r.getReviewerId());
                } else {
                    alreadyAssignedReviewerIds.add(r.getReviewerId());
                }
            }
        } catch (Exception ignore) {
            
        }

        int invitedCount = 0;
        List<Integer> skippedDecline = new ArrayList<>();
        List<Integer> skippedAssigned = new ArrayList<>();

        
        for (String reviewerIdStr : reviewerIdParams) {
            int reviewerId = Integer.parseInt(reviewerIdStr.trim());

            if (declinedReviewerIds.contains(reviewerId)) {
                skippedDecline.add(reviewerId);
                continue;
            }
            if (alreadyAssignedReviewerIds.contains(reviewerId)) {
                skippedAssigned.add(reviewerId);
                continue;
            }

            
            int reviewId = reviewDAO.inviteReviewerReturnId(manuscriptId, reviewerId, dueAt);

            
            inAppNotifications.onReviewerInvited(reviewId);
            mailNotifications.onReviewerInvited(reviewId);
            invitedCount++;
        }

        
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m != null && "WITH_EDITOR".equals(m.getCurrentStatus())) {
            manuscriptDAO.updateStatusWithHistory(manuscriptId, "UNDER_REVIEW", "SEND_TO_REVIEW", current.getUserId(), "送外审");
        }

        
        String base = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        if (invitedCount == 0) {
            String err = URLEncoder.encode("所选审稿人均不可邀请（可能已分配或已拒绝）。", "UTF-8");
            resp.sendRedirect(appendQueryParam(base, "inviteErr", err));
            return;
        }

        String msg = URLEncoder.encode("已向所选审稿人发送邀请（" + invitedCount + "人）。", "UTF-8");
        String target = appendQueryParam(base, "inviteMsg", msg);

        if (!skippedDecline.isEmpty()) {
            String err = URLEncoder.encode("部分审稿人已拒绝邀请（已拒绝：" + skippedDecline.size() + "人），已自动跳过。","UTF-8");
            target = appendQueryParam(target, "inviteErr", err);
        } else if (!skippedAssigned.isEmpty()) {
            String err = URLEncoder.encode("部分审稿人已被分配/已评审（已跳过：" + skippedAssigned.size() + "人）。", "UTF-8");
            target = appendQueryParam(target, "inviteErr", err);
        }

        resp.sendRedirect(target);
    }

    
protected void handleInviteExternalReviewerPost(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              User current)
        throws IOException, SQLException {

    String manuscriptIdStr = req.getParameter("manuscriptId");
    String dueDateStr = req.getParameter("dueDate"); 
    String backTo = req.getParameter("backTo");      

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    String fullName = req.getParameter("fullName");
    String email = req.getParameter("email");
    String affiliation = req.getParameter("affiliation");
    String researchArea = req.getParameter("researchArea");

    username = username == null ? "" : username.trim();
    password = password == null ? "" : password.trim();
    fullName = fullName == null ? "" : fullName.trim();
    email = email == null ? "" : email.trim();
    affiliation = affiliation == null ? "" : affiliation.trim();
    researchArea = researchArea == null ? "" : researchArea.trim();

    int manuscriptId;
    try {
        manuscriptId = Integer.parseInt((manuscriptIdStr == null ? "" : manuscriptIdStr.trim()));
    } catch (Exception e) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
        return;
    }

    if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
        String base = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        String msg = URLEncoder.encode("用户名/初始密码/邮箱均不能为空。", "UTF-8");
        resp.sendRedirect(appendQueryParam(base, "inviteErr", msg));
        return;
    }

    
    if ("EDITOR".equals(current.getRoleCode())) {
        Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
        if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
            return;
        }
    }

    
    LocalDateTime dueAt = null;
    if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
        LocalDate d = LocalDate.parse(dueDateStr.trim());
        dueAt = d.atTime(23, 59, 59);
    }

    
    User existed = userDAO.findByUsername(username);
    if (existed != null) {
        String base = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        String msg = URLEncoder.encode("用户名已存在，请更换用户名后再邀请。", "UTF-8");
        resp.sendRedirect(appendQueryParam(base, "inviteErr", msg));
        return;
    }

    
    User reviewer = new User();
    reviewer.setUsername(username);
    reviewer.setPasswordHash(password); 
    reviewer.setFullName(fullName.isEmpty() ? username : fullName);
    reviewer.setEmail(email);
    reviewer.setAffiliation(affiliation);
    reviewer.setResearchArea(researchArea);
    reviewer.setStatus("ACTIVE");

    User createdReviewer = userDAO.createUserWithRole(reviewer, "REVIEWER");
    int newReviewerId = createdReviewer.getUserId();
    reviewer.setUserId(newReviewerId);

    
    mailNotifications.onInviteNewReviewer(reviewer, password);

    
    int reviewId = reviewDAO.inviteReviewerReturnId(manuscriptId, newReviewerId, dueAt);
    inAppNotifications.onReviewerInvited(reviewId);
    mailNotifications.onReviewerInvited(reviewId);

    
    Manuscript m = manuscriptDAO.findById(manuscriptId);
    if (m != null && "WITH_EDITOR".equals(m.getCurrentStatus())) {
        manuscriptDAO.updateStatusWithHistory(manuscriptId, "UNDER_REVIEW", "SEND_TO_REVIEW", current.getUserId(), "送外审");
    }

    String base = (backTo != null && !backTo.trim().isEmpty())
            ? backTo.trim()
            : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
    String msg = URLEncoder.encode("外部审稿人账号已创建，并已发送邮件邀请。", "UTF-8");
    resp.sendRedirect(appendQueryParam(base, "inviteMsg", msg));
}


    protected void handleRemindReviewerPost(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          User current)
            throws IOException, SQLException {

        String reviewIdStr     = req.getParameter("reviewId");
        String manuscriptIdStr = req.getParameter("manuscriptId");
        String backTo          = req.getParameter("backTo");

        if (reviewIdStr == null || manuscriptIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int reviewId     = Integer.parseInt(reviewIdStr);
        int manuscriptId = Integer.parseInt(manuscriptIdStr);

        
        reviewDAO.remind(reviewId);

        
        String target = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        resp.sendRedirect(target);
    }

    

    
    protected void handleReviewMonitorPage(HttpServletRequest req,
                                         HttpServletResponse resp,
                                         User current)
            throws ServletException, IOException, SQLException {

        
        int overdueDays  = parseIntOrDefault(req.getParameter("overdueDays"), 7);
        int cooldownDays = parseIntOrDefault(req.getParameter("cooldownDays"), 3);
        int limit        = parseIntOrDefault(req.getParameter("limit"), 50);

        
        List<Review> overdue = reviewDAO.findOverdueForAutoRemind(overdueDays, cooldownDays, limit);

        
        Map<Integer, String> titleMap = new HashMap<>();
        for (Review r : overdue) {
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            if (m != null) {
                titleMap.put(r.getReviewId(), m.getTitle());
            }
        }

        
        String message = (String) req.getSession().getAttribute("monitorMessage");
        if (message != null) {
            req.setAttribute("monitorMessage", message);
            req.getSession().removeAttribute("monitorMessage");
        }

        req.setAttribute("overdueReviews", overdue);
        req.setAttribute("monitorTitles", titleMap);
        req.setAttribute("monitorOverdueDays", overdueDays);
        req.setAttribute("monitorCooldownDays", cooldownDays);
        req.setAttribute("monitorLimit", limit);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/review_monitor.jsp")
                .forward(req, resp);
    }

    
    
    protected int parseIntOrDefault(String s, int defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    
    
    protected void handleReviewRemindFormPage(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            User current)
            throws ServletException, IOException, SQLException {

        String reviewIdStr = req.getParameter("reviewId");
        if (reviewIdStr == null || reviewIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 reviewId 参数。");
            return;
        }

        int reviewId = Integer.parseInt(reviewIdStr.trim());
        Review review = reviewDAO.findById(reviewId);
        if (review == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的审稿记录。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
        User reviewer = userDAO.findById(review.getReviewerId());

        
        String defaultText = "请尽快提交您的审稿意见，本稿件的截止日期已过。";

        req.setAttribute("review", review);
        req.setAttribute("reviewManuscript", m);
        req.setAttribute("reviewReviewer", reviewer);
        req.setAttribute("defaultRemindText", defaultText);
        req.setAttribute("back", req.getParameter("back"));

        req.getRequestDispatcher("/WEB-INF/jsp/editor/review_remind_form.jsp")
                .forward(req, resp);
    }

    
    protected void handleEditorReviewDetailPage(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              User current)
            throws ServletException, IOException, SQLException {

        String reviewIdStr = req.getParameter("reviewId");
        if (reviewIdStr == null || reviewIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 reviewId 参数。\n");
            return;
        }

        int reviewId;
        try {
            reviewId = Integer.parseInt(reviewIdStr.trim());
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "reviewId 参数格式不正确。\n");
            return;
        }

        Review review = reviewDAO.findById(reviewId);
        if (review == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的审稿记录。\n");
            return;
        }

        Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的稿件。\n");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(m.getManuscriptId());
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。\n");
                return;
            }
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("review", review);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/review_detail.jsp")
                .forward(req, resp);
    }

    
    protected void handleRemindReviewerCustomPost(HttpServletRequest req,
                                                HttpServletResponse resp,
                                                User current)
            throws IOException, SQLException {

        String reviewIdStr     = req.getParameter("reviewId");
        String manuscriptIdStr = req.getParameter("manuscriptId");
        String back            = req.getParameter("back");
        String message         = req.getParameter("message");

        if (reviewIdStr == null || manuscriptIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int reviewId     = Integer.parseInt(reviewIdStr.trim());
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        
        reviewDAO.remindChecked(reviewId);
        
        mailNotifications.onReviewerRemindCustom(reviewId, message);
        
        inAppNotifications.onReviewerRemind(reviewId);

        String ctx = req.getContextPath();
        if ("monitor".equals(back)) {
            resp.sendRedirect(ctx + "/editor/review/monitor");
        } else {
            resp.sendRedirect(ctx + "/manuscripts/detail?id=" + manuscriptId);
        }
    }

    
    protected void handleAutoRemindNowPost(HttpServletRequest req,
                                         HttpServletResponse resp,
                                         User current)
            throws IOException, SQLException {

        int overdueDays  = parseIntOrDefault(req.getParameter("overdueDays"), 7);
        int cooldownDays = parseIntOrDefault(req.getParameter("cooldownDays"), 3);
        int limit        = parseIntOrDefault(req.getParameter("limit"), 50);

        List<Review> overdue = reviewDAO.findOverdueForAutoRemind(overdueDays, cooldownDays, limit);

        int success = 0;
        for (Review r : overdue) {
            try {
                reviewDAO.remindChecked(r.getReviewId());
                mailNotifications.onReviewerRemind(r.getReviewId());
                inAppNotifications.onReviewerRemind(r.getReviewId());
                success++;
            } catch (Exception ignore) {
                
            }
        }

        String summary = "按当前规则筛选到 " + overdue.size()
                + " 条逾期审稿任务，成功发送催审邮件 " + success + " 封。";

        req.getSession().setAttribute("monitorMessage", summary);

            resp.sendRedirect(req.getContextPath()
                    + "/editor/review/monitor?overdueDays=" + overdueDays
                    + "&cooldownDays=" + cooldownDays
                    + "&limit=" + limit);
    }


    protected void handleEditorRecommendPost(HttpServletRequest req,
                                           HttpServletResponse resp,
                                           User current)
            throws IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String suggestionCode  = req.getParameter("suggestion");
        String summary         = req.getParameter("summary");

        if (manuscriptIdStr == null || suggestionCode == null || suggestionCode.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (!java.util.Objects.equals(editorId, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }
        String code = suggestionCode.trim().toUpperCase();
        String decisionText;
        switch (code) {
            case "ACCEPT":
                decisionText = "Suggest Acceptance";
                break;
            case "MINOR_REVISION":
                decisionText = "Suggest Acceptance after Minor Revision";
                break;
            case "MAJOR_REVISION":
                decisionText = "Suggest Major Revision";
                break;
            case "REJECT":
                decisionText = "Suggest Reject";
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "未知建议类型。");
                return;
        }

        
        EditorSuggestion s = new EditorSuggestion();
        s.setManuscriptId(manuscriptId);
        s.setEditorId(current.getUserId());
        s.setSuggestion(code);
        s.setSummary(summary);
        editorSuggestionDAO.upsert(s);

        
        manuscriptDAO.updateStatusWithHistory(
                manuscriptId,
                "FINAL_DECISION_PENDING",
                "EDITOR_RECOMMENDATION_SUBMIT",
                current.getUserId(),
                "编辑提交建议：" + decisionText
        );

        
        
        

        
        inAppNotifications.onEditorRecommendationSubmitted(manuscriptId, current, decisionText, summary);

        
        resp.sendRedirect(req.getContextPath() + "/editor/recommend?msg=提交成功");
    }

    
    
    protected void handleFormalCheckPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以执行形式审查/格式检查操作。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        if (idStr == null || op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);

        switch (op) {
            case "start":
                
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "FORMAL_CHECK", "FORMAL_CHECK_START", current.getUserId(), "编辑部开始进行形式审查");
                inAppNotifications.onFormalCheckStarted(manuscriptId);
                resp.sendRedirect(req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId);
                return;
            case "autoCheck":
                handleAutoCheck(req, resp, manuscriptId);
                return;
            case "plagiarismCheck":
                handlePlagiarismCheck(req, resp, manuscriptId);
                return;
            case "submit":
                handleSubmitFormalCheck(req, resp, current, manuscriptId);
                return;
            case "returnForRevision":
                handleReturnForRevision(req, resp, current, manuscriptId);
                return;
            case "approve":
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "DESK_REVIEW_INITIAL", "FORMAL_CHECK_PASS", current.getUserId(), "形式审查通过");
                inAppNotifications.onFormalCheckPassed(manuscriptId);
                break;
            case "return":
                String issues = req.getParameter("issues");
                if (issues != null) issues = issues.trim();
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(),
                        (issues == null || issues.isEmpty()) ? "形式审查未通过，已退回修改" : issues);

                String guideUrl = req.getParameter("guideUrl");
                mailNotifications.onFormalCheckReturn(manuscriptId, issues, guideUrl);
                inAppNotifications.onFormalCheckReturn(manuscriptId, issues);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        resp.sendRedirect(req.getContextPath() + "/editor/formalCheck");
    }

    protected void handleAutoCheck(HttpServletRequest req, HttpServletResponse resp, int manuscriptId)
            throws SQLException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                out.print(jsonResponse.toString());
                return;
            }

            
            
            
            User current = getCurrentUser(req);
            boolean eoAdminOnlyPdf = (current != null && "EO_ADMIN".equals(current.getRoleCode()));

            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);
            String bodyText = "";
            int pdfPageCount = 0;

            if (!eoAdminOnlyPdf) {
                
                if (currentVer != null) {
                    String coverPath = currentVer.getCoverLetterPath();
                    if (coverPath != null && !coverPath.trim().isEmpty()) {
                        bodyText = FileTextUtil.extractText(new File(coverPath));
                    }
                }
            }

            
            if (currentVer != null && (eoAdminOnlyPdf || formalCheckService.computeBodyCount(bodyText) == 0)) {
                String pdfPath = null;
                if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileOriginalPath();
                } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileAnonymousPath();
                }
                if (pdfPath != null) {
                    File pdfFile = new File(pdfPath);
                    bodyText = FileTextUtil.extractText(pdfFile);
                    pdfPageCount = PdfTextUtil.extractPageCount(pdfFile);
                }
            }

            
            java.util.List<ManuscriptAuthor> authors = null;
            try {
                authors = manuscriptAuthorDAO.findByManuscriptId(manuscriptId);
            } catch (Exception ignore) {
                authors = null;
            }

            FormalCheckResult result = formalCheckService.performAutomaticChecks(manuscript, bodyText, authors, pdfPageCount);
            int bodyCount = pdfPageCount;
            int abstractCount = formalCheckService.computeAbstractCount(manuscript.getAbstractText());

            
            jsonResponse.put("success", true);
            jsonResponse.put("authorInfoValid", result.getAuthorInfoValid() != null ? result.getAuthorInfoValid().toString() : "");
            jsonResponse.put("abstractWordCountValid", result.getAbstractWordCountValid() != null ? result.getAbstractWordCountValid().toString() : "");
            jsonResponse.put("bodyWordCountValid", result.getBodyWordCountValid() != null ? result.getBodyWordCountValid().toString() : "");
            jsonResponse.put("keywordsValid", result.getKeywordsValid() != null ? result.getKeywordsValid().toString() : "");
            jsonResponse.put("bodyCount", bodyCount);
            jsonResponse.put("pdfPageCount", pdfPageCount);
            jsonResponse.put("abstractCount", abstractCount);
            String bodyOk = Boolean.TRUE.equals(result.getBodyWordCountValid()) ? "通过" : (Boolean.FALSE.equals(result.getBodyWordCountValid()) ? "不通过" : "未检查");
            jsonResponse.put("message", "自动检查完成：正文字数 " + bodyCount + "（3000-8000，" + bodyOk + "），摘要字数 " + abstractCount + "");
            
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "自动检查失败：" + e.getMessage());
        }

        out.print(jsonResponse.toString());
    }

    protected void handlePlagiarismCheck(HttpServletRequest req, HttpServletResponse resp, int manuscriptId)
            throws SQLException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                out.print(jsonResponse.toString());
                return;
            }

            
            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);
            String pdfPath = null;
            if (currentVer != null) {
                if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileOriginalPath();
                } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileAnonymousPath();
                }
            }
            String bodyText = "";
            if (pdfPath != null) {
                bodyText = PdfTextUtil.extractText(new File(pdfPath));
            }

            PlagiarismCheckService.PlagiarismReport plagiarismReport =
                    formalCheckService.performPlagiarismCheck(manuscript, bodyText);
            
            jsonResponse.put("success", true);
            jsonResponse.put("similarityScore", plagiarismReport.getSimilarityScore());
            jsonResponse.put("highSimilarity", plagiarismReport.isHighSimilarity());
            String reportUrl = plagiarismReport.getReportUrl();
            if (reportUrl != null && reportUrl.startsWith("/")) {
                reportUrl = req.getContextPath() + reportUrl;
            }
            jsonResponse.put("reportUrl", reportUrl);
            jsonResponse.put("message", "查重完成");
            
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "查重失败：" + e.getMessage());
        }

        out.print(jsonResponse.toString());
    }

    protected void handleSubmitFormalCheck(HttpServletRequest req, HttpServletResponse resp, User current, int manuscriptId)
            throws SQLException, IOException {

        
        
        
        String ajax = req.getParameter("ajax");
        boolean wantJson = "1".equals(ajax) || "true".equalsIgnoreCase(ajax);

        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                if (wantJson) {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().print(jsonResponse.toString());
                } else {
                    resp.sendRedirect(req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId);
                }
                return;
            }

            FormalCheckResult result = new FormalCheckResult();
            result.setManuscriptId(manuscriptId);
            result.setReviewerId(current.getUserId());
            result.setCheckTime(LocalDateTime.now());

            String authorInfoValidStr = req.getParameter("authorInfoValid");
            if (authorInfoValidStr != null && !authorInfoValidStr.isEmpty()) {
                result.setAuthorInfoValid(Boolean.valueOf(authorInfoValidStr));
            }

            String abstractWordCountValidStr = req.getParameter("abstractWordCountValid");
            if (abstractWordCountValidStr != null && !abstractWordCountValidStr.isEmpty()) {
                result.setAbstractWordCountValid(Boolean.valueOf(abstractWordCountValidStr));
            }

            String bodyWordCountValidStr = req.getParameter("bodyWordCountValid");
            if (bodyWordCountValidStr != null && !bodyWordCountValidStr.isEmpty()) {
                result.setBodyWordCountValid(Boolean.valueOf(bodyWordCountValidStr));
            }

            String keywordsValidStr = req.getParameter("keywordsValid");
            if (keywordsValidStr != null && !keywordsValidStr.isEmpty()) {
                result.setKeywordsValid(Boolean.valueOf(keywordsValidStr));
            }

            String footnoteNumberingValidStr = req.getParameter("footnoteNumberingValid");
            if (footnoteNumberingValidStr != null && !footnoteNumberingValidStr.isEmpty()) {
                result.setFootnoteNumberingValid(Boolean.valueOf(footnoteNumberingValidStr));
            }

            String figureTableFormatValidStr = req.getParameter("figureTableFormatValid");
            if (figureTableFormatValidStr != null && !figureTableFormatValidStr.isEmpty()) {
                result.setFigureTableFormatValid(Boolean.valueOf(figureTableFormatValidStr));
            }

            String referenceFormatValidStr = req.getParameter("referenceFormatValid");
            if (referenceFormatValidStr != null && !referenceFormatValidStr.isEmpty()) {
                result.setReferenceFormatValid(Boolean.valueOf(referenceFormatValidStr));
            }

            
String checkResult = req.getParameter("checkResult");
if (checkResult != null) checkResult = checkResult.trim().toUpperCase();

boolean hasInvalid = false;
if (result.getAuthorInfoValid() != null && !result.getAuthorInfoValid()) hasInvalid = true;
if (result.getAbstractWordCountValid() != null && !result.getAbstractWordCountValid()) hasInvalid = true;
if (result.getBodyWordCountValid() != null && !result.getBodyWordCountValid()) hasInvalid = true;
if (result.getKeywordsValid() != null && !result.getKeywordsValid()) hasInvalid = true;
if (result.getFootnoteNumberingValid() != null && !result.getFootnoteNumberingValid()) hasInvalid = true;
if (result.getFigureTableFormatValid() != null && !result.getFigureTableFormatValid()) hasInvalid = true;
if (result.getReferenceFormatValid() != null && !result.getReferenceFormatValid()) hasInvalid = true;




boolean userSelected = "PASS".equals(checkResult) || "FAIL".equals(checkResult);
if (!userSelected) {
    checkResult = hasInvalid ? "FAIL" : "PASS";
}

result.setCheckResult(checkResult);

            String feedback = req.getParameter("feedback");
            if (feedback == null || feedback.trim().isEmpty()) {
                feedback = formalCheckService.generateFeedback(result);
            }
            result.setFeedback(feedback);

            formalCheckResultDAO.save(result);


            if ("PASS".equals(checkResult)) {
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "DESK_REVIEW_INITIAL", "FORMAL_CHECK_PASS", current.getUserId(),
                        "形式审查通过");
                inAppNotifications.onFormalCheckPassed(manuscriptId);
            } else if ("FAIL".equals(checkResult)) {
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(),
                        feedback);
                String guideUrl = req.getContextPath() + "/static/guides/format_guide.pdf";
                mailNotifications.onFormalCheckReturn(manuscriptId, feedback, guideUrl);
                inAppNotifications.onFormalCheckReturn(manuscriptId, feedback);
            }

            jsonResponse.put("success", true);
            jsonResponse.put("message", "形式审查结果已提交");

        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "提交失败：" + e.getMessage());
        }

        if (wantJson) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print(jsonResponse.toString());
        } else {
            
            if (jsonResponse.optBoolean("success", false)) {
                resp.sendRedirect(req.getContextPath() + "/editor/formalCheck/history");
            } else {
                
                resp.sendRedirect(req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId);
            }
        }
    }

    protected void handleReturnForRevision(HttpServletRequest req, HttpServletResponse resp, User current, int manuscriptId)
            throws SQLException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                out.print(jsonResponse.toString());
                return;
            }

            String feedback = req.getParameter("feedback");
            if (feedback == null || feedback.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "请填写反馈意见");
                out.print(jsonResponse.toString());
                return;
            }

            FormalCheckResult result = new FormalCheckResult();
            result.setManuscriptId(manuscriptId);
            result.setReviewerId(current.getUserId());
            result.setCheckTime(LocalDateTime.now());
            result.setCheckResult("FAIL");
            result.setFeedback(feedback);

            formalCheckResultDAO.save(result);

            manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(), feedback);

            String guideUrl = req.getContextPath() + "/static/guides/format_guide.pdf";
            mailNotifications.onFormalCheckReturn(manuscriptId, feedback, guideUrl);
            inAppNotifications.onFormalCheckReturn(manuscriptId, feedback);

            jsonResponse.put("success", true);
            jsonResponse.put("message", "已退回修改，邮件已发送给作者，站内消息已同步发送");
            
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "操作失败：" + e.getMessage());
        }

        out.print(jsonResponse.toString());
    }

    
    protected void handleDeskDecisionPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        if (idStr == null || op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);
        switch (op) {
            case "deskAccept":
                
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "TO_ASSIGN", "DESK_REVIEW_ACCEPT", current.getUserId(), "案头初审通过");

                
                inAppNotifications.onDeskAccepted(manuscriptId);
                break;
            case "deskReject":
                
                String rejectReason = req.getParameter("rejectReason");
                if (rejectReason == null || rejectReason.trim().isEmpty()) {
                    req.getSession().setAttribute("errorMsg", "退稿理由不能为空。请填写退稿理由后再提交。");
                    resp.sendRedirect(req.getContextPath() + "/editor/desk");
                    return;
                }
                rejectReason = rejectReason.trim();

                manuscriptDAO.deskRejectWithReason(manuscriptId, current.getUserId(), rejectReason);

                
                inAppNotifications.onDeskRejected(manuscriptId, rejectReason);
                mailNotifications.onDeskRejected(manuscriptId, rejectReason);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        resp.sendRedirect(req.getContextPath() + "/editor/desk");
    }

    
    protected void handleAssignEditorPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        String idStr = req.getParameter("manuscriptId");
        String editorIdStr = req.getParameter("editorId");
        
        String chiefComment = req.getParameter("chiefComment");

        if (idStr == null || editorIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);
        int editorId = Integer.parseInt(editorIdStr);

        
        String historyRemark = (chiefComment == null || chiefComment.trim().isEmpty())
                ? "主编指派责任编辑"
                : ("主编指派责任编辑：" + chiefComment.trim());
        manuscriptDAO.assignEditorWithHistory(manuscriptId, editorId, current.getUserId(), historyRemark);

        
        assignmentDAO.createAssignment(
                manuscriptId,
                editorId,
                current.getUserId(),  
                chiefComment
        );

        
        inAppNotifications.onEditorAssigned(manuscriptId, current, editorId, chiefComment);
        mailNotifications.onEditorAssigned(manuscriptId, current, editorId, chiefComment);

        
        inAppNotifications.onEditorAssignedToAuthor(manuscriptId, current, editorId);

        resp.sendRedirect(req.getContextPath() + "/editor/toAssign");
    }


    
    protected void handleFinalDecisionPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        if (idStr == null || op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);

        
        Manuscript currentManuscript = manuscriptDAO.findById(manuscriptId);
        if (currentManuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }

        if ("rescind".equals(op)) {
            
            String st = currentManuscript.getCurrentStatus();
            if (!"ACCEPTED".equals(st) && !"REJECTED".equals(st) && !"REVISION".equals(st)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "只有已做出最终决定的稿件才可以撤销决策。");
                return;
            }
            manuscriptDAO.rescindDecision(manuscriptId);
            resp.sendRedirect(req.getContextPath() + "/editor/special");
            return;
        }

        if ("retract".equals(op)) {
            
            if ("ARCHIVED".equals(currentManuscript.getCurrentStatus())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "该稿件已经归档/撤稿，无需重复操作。");
                return;
            }
            manuscriptDAO.retractManuscript(manuscriptId);

            
            inAppNotifications.onRetract(manuscriptId);
            mailNotifications.onRetract(manuscriptId);

            resp.sendRedirect(req.getContextPath() + "/editor/special");
            return;
        }

        
        String decision;
        String newStatus;
        String decisionText;
        switch (op) {
            case "accept":
                decision = "ACCEPT";
                newStatus = "ACCEPTED";
                decisionText = "录用";
                break;
            case "reject":
                decision = "REJECT";
                newStatus = "REJECTED";
                decisionText = "退稿";
                break;
            case "revision":
                decision = "REVISION";
                newStatus = "REVISION";
                decisionText = "修改后再审";
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        manuscriptDAO.updateFinalDecision(manuscriptId, decision, newStatus);

        
        inAppNotifications.onFinalDecision(manuscriptId, decisionText);
        mailNotifications.onFinalDecision(manuscriptId, decisionText);
        resp.sendRedirect(req.getContextPath() + "/editor/finalDecision");
    }

    
    protected void handleReviewerPoolPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        
        String op = req.getParameter("op");
        if (op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        String redirectMsg = null;

        if ("create".equals(op) || "invite".equals(op)) {
            
            String username = trim(req.getParameter("username"));
            String password = trim(req.getParameter("password"));
            String fullName = trim(req.getParameter("fullName"));
            String email = trim(req.getParameter("email"));
            String affiliation = trim(req.getParameter("affiliation"));
            String researchArea = trim(req.getParameter("researchArea"));

            if (username == null || username.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "用户名/初始密码/邮箱不能为空。");
                return;
            }

            
            User existed = userDAO.findByUsername(username);
            if (existed != null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "用户名已存在，请更换后重试。");
                return;
            }

            User reviewer = new User();
            reviewer.setUsername(username);
            reviewer.setPasswordHash(password); 
            reviewer.setFullName((fullName == null || fullName.isEmpty()) ? username : fullName);
            reviewer.setEmail(email);
            reviewer.setAffiliation(affiliation);
            reviewer.setResearchArea(researchArea);
            reviewer.setStatus("ACTIVE");

            User created = userDAO.createUserWithRole(reviewer, "REVIEWER");
            reviewer.setUserId(created.getUserId());

            
            inAppNotifications.onInviteNewReviewer(reviewer);
            mailNotifications.onInviteNewReviewer(reviewer, password);

            redirectMsg = "已创建审稿人账号并发送邀请：" + reviewer.getUsername();

        } else {
            
            String userIdStr = req.getParameter("userId");
            if (userIdStr == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少用户 ID 参数。");
                return;
            }
            int userId = Integer.parseInt(userIdStr);

            if ("approve".equals(op)) {
                
                userDAO.updateStatus(userId, "ACTIVE");
                redirectMsg = "已审核通过：ID=" + userId;
            } else if ("disable".equals(op)) {
                
                userDAO.updateStatus(userId, "DISABLED");
                redirectMsg = "已禁用：ID=" + userId;
            } else if ("enable".equals(op)) {
                
                userDAO.updateStatus(userId, "ACTIVE");
                redirectMsg = "已启用：ID=" + userId;
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
            }
        }

        String target = req.getContextPath() + "/editor/reviewers";
        if (redirectMsg != null && !redirectMsg.trim().isEmpty()) {
            target += "?msg=" + URLEncoder.encode(redirectMsg, "UTF-8");
        }
        resp.sendRedirect(target);
    }

    
    private String trim(String s) {
        return s == null ? null : s.trim();
    }


    


    

    
    protected void handleReviewSelectPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        
        String backToUrl = req.getParameter("backTo");
        if (backToUrl == null || backToUrl.trim().isEmpty()) {
            String detailSp;
            if ("WITH_EDITOR".equals(m.getCurrentStatus())) {
                detailSp = "/editor/withEditor";
            } else if ("UNDER_REVIEW".equals(m.getCurrentStatus())) {
                detailSp = "/editor/underReview";
            } else {
                detailSp = "/editor/withEditor";
            }
            backToUrl = req.getContextPath() + detailSp + "/detail?manuscriptId=" + manuscriptId;
        }

        
        String reviewerKeyword = req.getParameter("reviewerKeyword");
        Integer minCompleted = null;
        Integer minAvgScore = null;
        boolean onlyMatch = false;

        try {
            String mc = req.getParameter("minCompleted");
            if (mc != null && !mc.trim().isEmpty()) minCompleted = Integer.parseInt(mc.trim());
        } catch (Exception ignore) {}
        try {
            String mas = req.getParameter("minAvgScore");
            if (mas != null && !mas.trim().isEmpty()) minAvgScore = Integer.parseInt(mas.trim());
        } catch (Exception ignore) {}

        
        String onlyMatchParam = req.getParameter("onlyMatch");
        if (onlyMatchParam != null) {
            String v = onlyMatchParam.trim();
            onlyMatch = "1".equals(v) || "true".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
        }

        
        List<User> reviewers = userDAO.searchReviewerPool(reviewerKeyword, minCompleted, minAvgScore, 200);

        
        Map<Integer, Integer> reviewerMatchScore = new HashMap<>();
        reviewers = rankReviewersByResearchArea(reviewers, m, reviewerMatchScore);

        
        if (onlyMatch && reviewers != null && !reviewers.isEmpty()) {
            List<User> filtered = new ArrayList<>();
            for (User u : reviewers) {
                if (u == null || u.getUserId() == null) continue;
                int sc = reviewerMatchScore.getOrDefault(u.getUserId(), 0);
                if (sc > 0) filtered.add(u);
            }
            reviewers = filtered;
        }


        
        
        
        Set<Integer> assignedReviewerIds = new HashSet<>();
        Set<Integer> declinedReviewerIds = new HashSet<>();
        List<Review> existing = reviewDAO.findByManuscript(manuscriptId);
        for (Review r : existing) {
            if (r == null) continue;
            String st = r.getStatus();

            
            boolean isDeclined = "DECLINED".equals(st)
                    || ("EXPIRED".equals(st) && (r.getDeclinedAt() != null
                    || (r.getRejectionReason() != null && !r.getRejectionReason().trim().isEmpty())));

            if (isDeclined) {
                declinedReviewerIds.add(r.getReviewerId());
            } else {
                assignedReviewerIds.add(r.getReviewerId());
            }
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("backToUrl", backToUrl);
        
        req.setAttribute("reviewerMatchScore", reviewerMatchScore);
        req.setAttribute("onlyMatch", onlyMatch);
        
        req.setAttribute("reviewers", reviewers);
        req.setAttribute("assignedReviewerIds", assignedReviewerIds);
        req.setAttribute("declinedReviewerIds", declinedReviewerIds);
        req.setAttribute("reviewerKeyword", reviewerKeyword);
        req.setAttribute("minCompleted", minCompleted);
        req.setAttribute("minAvgScore", minAvgScore);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_select_pool.jsp")
                .forward(req, resp);
    }

    
    protected void handleCancelReviewerPost(HttpServletRequest req, HttpServletResponse resp, User current)
        throws IOException, SQLException {

    String reviewIdStr = req.getParameter("reviewId");
    String manuscriptIdStr = req.getParameter("manuscriptId");
    if (reviewIdStr == null || manuscriptIdStr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
        return;
    }

    int reviewId = Integer.parseInt(reviewIdStr.trim());
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

    Review r = reviewDAO.findById(reviewId);
    if (r == null || r.getManuscriptId() != manuscriptId) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应审稿记录。");
        return;
    }

    
    if ("EDITOR".equals(current.getRoleCode())) {
        Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
        if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
            return;
        }
    }

    
    int updated = reviewDAO.cancelAssignment(reviewId);

    
    if (updated > 0) {
        
        notificationDAO.create(
                r.getReviewerId(),
                current.getUserId(),
                "SYSTEM",
                "REVIEW_CANCEL",
                "审稿任务已解除",
                "编辑已解除该稿件的审稿任务。如有疑问请联系编辑部。",
                manuscriptId
        );

        OperationLogger.log(req, "EDITOR", "解除审稿人",
                "解除审稿人 reviewId=" + reviewId + ", manuscriptId=" + manuscriptId);
    }

    
    
    
    boolean rolledBack = false;
    try {
        Manuscript curM = manuscriptDAO.findById(manuscriptId);
        if (curM != null && "UNDER_REVIEW".equalsIgnoreCase(curM.getCurrentStatus())) {
            
            List<Review> still = reviewDAO.findByManuscript(manuscriptId);
            int effectiveCnt = 0;
            for (Review rr : still) {
                String st = rr.getStatus();
                if ("INVITED".equalsIgnoreCase(st)
                        || "ACCEPTED".equalsIgnoreCase(st)
                        || "SUBMITTED".equalsIgnoreCase(st)) {
                    effectiveCnt++;
                }
            }
            if (effectiveCnt <= 0) {
                manuscriptDAO.updateStatusWithHistory(
                        manuscriptId,
                        "WITH_EDITOR",
                        "ROLLBACK_NO_REVIEWER",
                        current.getUserId(),
                        "撤回审稿人后已无在审分配，退回责任编辑阶段"
                );
                rolledBack = true;
            }
        }
    } catch (Exception ignore) {
        
    }

    
    String msg;
    if (updated > 0) {
        msg = rolledBack
                ? "已撤回该审稿人分配，稿件已无在审审稿人，已退回责任编辑阶段。"
                : "已撤回该审稿人分配。";
    } else {
        msg = "撤回未生效：该审稿人当前不可撤回（可能已提交或已撤回）。";
    }

    String ctx = req.getContextPath();
    String target = null;

    
    String backTo = req.getParameter("backTo");
    if (backTo != null) {
        backTo = backTo.trim();
        if (!backTo.isEmpty()) {
            if (backTo.startsWith(ctx + "/")) {
                target = backTo;
            } else if (backTo.startsWith("/")) {
                target = ctx + backTo;
            }
        }
    }

    
    if (target == null) {
        String ref = req.getHeader("Referer");
        if (ref != null) {
            int idx = ref.indexOf(ctx + "/");
            if (idx >= 0) {
                target = ref.substring(idx);
            }
        }
    }

    
    if (target == null) {
        target = ctx + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers";
    }

    
    String encoded = URLEncoder.encode(msg, "UTF-8");
    int hash = target.indexOf('#');
    String frag = "";
    if (hash >= 0) {
        frag = target.substring(hash);
        target = target.substring(0, hash);
    }
    target = target + (target.contains("?") ? "&" : "?") + "cancelMsg=" + encoded + frag;

    resp.sendRedirect(target);
}

    

    
    protected void handleAuthorCommList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list;
        if ("EDITOR".equals(current.getRoleCode())) {
            list = manuscriptDAO.findByStatusesForEditor(
                    current.getUserId(),
                    
                    "UNDER_REVIEW",
                    "WITH_EDITOR",
                    "EDITOR_RECOMMENDATION"
            );
        } else {
            
            list = manuscriptDAO.findByStatuses(
                    "UNDER_REVIEW",
                    "WITH_EDITOR",
                    "EDITOR_RECOMMENDATION"
            );
        }

        Map<Integer, Integer> countMap = new HashMap<>();
        for (Manuscript m : list) {
            int cnt = notificationDAO.countByManuscriptAndCategory(m.getManuscriptId(), "AUTHOR_MESSAGE");
            countMap.put(m.getManuscriptId(), cnt);
        }

        PaginationUtil.apply(req, list, "manuscripts");
        req.setAttribute("commCountMap", countMap);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/author_comm_list.jsp").forward(req, resp);
    }

    
    protected void handleAuthorMessagePage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        User author = null;
        if (m.getSubmitterId() != null) {
            author = userDAO.findById(m.getSubmitterId());
        }

        List<Notification> history = notificationDAO.listByManuscriptAndCategory(
                manuscriptId,
                "AUTHOR_MESSAGE",
                null,
                200,
                true
        );

        Map<Integer, User> userMap = new HashMap<>();
        
        userMap.put(current.getUserId(), current);
        if (author != null) userMap.put(author.getUserId(), author);
        for (Notification n : history) {
            Integer cb = n.getCreatedByUserId();
            if (cb != null && !userMap.containsKey(cb)) {
                User u = userDAO.findById(cb);
                if (u != null) userMap.put(cb, u);
            }
            int ru = n.getRecipientUserId();
            if (!userMap.containsKey(ru)) {
                User u = userDAO.findById(ru);
                if (u != null) userMap.put(ru, u);
            }
        }

        List<User> chiefs = userDAO.findByRoleCode("EDITOR_IN_CHIEF");

        String flash = (String) req.getSession().getAttribute("authorMessageFlash");
        if (flash != null) {
            req.setAttribute("authorMessageFlash", flash);
            req.getSession().removeAttribute("authorMessageFlash");
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("authorUser", author);
        req.setAttribute("history", history);
        req.setAttribute("userMap", userMap);
        req.setAttribute("chiefEditors", chiefs);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/author_message.jsp").forward(req, resp);
    }

    
    protected void handleSendAuthorMessagePost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String title = req.getParameter("title");
        String content = req.getParameter("content");

        boolean sendSystem = req.getParameter("sendSystem") != null;
        boolean sendEmail = req.getParameter("sendEmail") != null;
        boolean ccChief = req.getParameter("ccChief") != null;

        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        if (content == null || content.trim().isEmpty()) {
            req.getSession().setAttribute("authorMessageFlash", "消息内容不能为空。");
            resp.sendRedirect(req.getContextPath() + "/editor/author/message?manuscriptId=" + manuscriptId);
            return;
        }

        if (!sendSystem && !sendEmail) {
            
            sendSystem = true;
        }

        if (title == null) title = "";
        title = title.trim();
        if (title.isEmpty()) {
            title = "稿件沟通（稿件ID=" + manuscriptId + "）";
        }

        User author = null;
        if (m.getSubmitterId() != null) {
            author = userDAO.findById(m.getSubmitterId());
        }
        if (author == null) {
            req.getSession().setAttribute("authorMessageFlash", "未找到作者用户，无法发送。");
            resp.sendRedirect(req.getContextPath() + "/editor/author/message?manuscriptId=" + manuscriptId);
            return;
        }

        List<User> chiefs = userDAO.findByRoleCode("EDITOR_IN_CHIEF");

        
        if (sendSystem) {
            notificationDAO.create(
                    author.getUserId(),
                    current.getUserId(),
                    "SYSTEM",
                    "AUTHOR_MESSAGE",
                    title,
                    content,
                    manuscriptId
            );
            if (ccChief && chiefs != null) {
                for (User ce : chiefs) {
                    if (ce == null) continue;
                    notificationDAO.create(
                            ce.getUserId(),
                            current.getUserId(),
                            "SYSTEM",
                            "AUTHOR_MESSAGE",
                            "[抄送] " + title,
                            "（抄送主编）\n\n" + content,
                            manuscriptId
                    );
                }
            }
        }

        
        boolean emailOk = true;
        if (sendEmail) {
            String subject = "[OnlineSM] " + title;
            StringBuilder body = new StringBuilder();
            body.append("稿件ID：").append(manuscriptId).append("\n");
            body.append("稿件标题：").append(m.getTitle() == null ? "" : m.getTitle()).append("\n");
            body.append("发件人：").append(current.getFullName() == null ? current.getUsername() : current.getFullName()).append("\n\n");
            body.append(content);

            try {
                MailService.sendText(author.getEmail(), subject, body.toString());
                if (ccChief && chiefs != null) {
                    for (User ce : chiefs) {
                        if (ce == null || ce.getEmail() == null) continue;
                        MailService.sendText(ce.getEmail(), "[抄送] " + subject, body.toString());
                    }
                }
            } catch (Exception ex) {
                emailOk = false;
            }
        }

        OperationLogger.log(req, "EDITOR", "发送作者消息", "发送消息给作者 manuscriptId=" + manuscriptId + ", sendSystem=" + sendSystem + ", sendEmail=" + sendEmail + ", ccChief=" + ccChief);

        String okText = "消息已发送" + (sendEmail && !emailOk ? "（邮件发送失败，仅站内消息生效）" : "。");
        req.getSession().setAttribute("authorMessageFlash", okText);
        resp.sendRedirect(req.getContextPath() + "/editor/author/message?manuscriptId=" + manuscriptId);
    }

    

    
    protected String appendQueryParam(String url, String key, String valueEncoded) {
        if (url == null) return null;
        String base = url;
        String fragment = "";
        int idx = url.indexOf('#');
        if (idx >= 0) {
            base = url.substring(0, idx);
            fragment = url.substring(idx);
        }
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + key + "=" + valueEncoded + fragment;
    }

    
    protected String appendQueryParamIfPresent(HttpServletRequest req, String url, String key) throws UnsupportedEncodingException {
        String v = req.getParameter(key);
        if (v == null || v.trim().isEmpty()) return url;
        String enc = URLEncoder.encode(v, "UTF-8");
        return appendQueryParam(url, key, enc);
    }


    
    
    
    private List<User> rankEditorsByResearchArea(List<User> editors, Manuscript manuscript) {
        if (editors == null) return Collections.emptyList();
        if (manuscript == null) return new ArrayList<>(editors);

        String msArea = manuscript.getSubjectArea();
        String msKeywords = manuscript.getKeywords();
        String msText = ((msArea == null ? "" : msArea) + " " + (msKeywords == null ? "" : msKeywords)).trim();
        Set<String> msTokens = tokenizeKeywords(msText);

        List<User> copy = new ArrayList<>(editors);
        copy.sort((a, b) -> {
            int sa = editorMatchScore(a, msText, msTokens);
            int sb = editorMatchScore(b, msText, msTokens);
            if (sa != sb) return Integer.compare(sb, sa); 
            boolean ha = a != null && a.getResearchArea() != null && !a.getResearchArea().trim().isEmpty();
            boolean hb = b != null && b.getResearchArea() != null && !b.getResearchArea().trim().isEmpty();
            if (ha != hb) return hb ? 1 : -1;
            String ua = a == null ? "" : String.valueOf(a.getUsername());
            String ub = b == null ? "" : String.valueOf(b.getUsername());
            return ua.compareToIgnoreCase(ub);
        });
        return copy;
    }

    private int editorMatchScore(User editor, String manuscriptSubjectArea, Set<String> manuscriptTokens) {
        if (editor == null) return 0;
        String ra = editor.getResearchArea();
        if (ra == null) ra = "";
        ra = ra.trim();
        if (ra.isEmpty()) return 0;

        int score = 0;

        
        Set<String> eTokens = tokenizeKeywords(ra);
        if (manuscriptTokens != null && !manuscriptTokens.isEmpty()) {
            for (String t : eTokens) {
                if (manuscriptTokens.contains(t)) score++;
            }
        }

        
        String ms = manuscriptSubjectArea == null ? "" : manuscriptSubjectArea.trim();
        if (!ms.isEmpty()) {
            String msLower = ms.toLowerCase();
            String raLower = ra.toLowerCase();
            if (msLower.contains(raLower) || raLower.contains(msLower)) score += 2;
        }

        return score;
    }


    
    
    
    
    private List<User> rankReviewersByResearchArea(List<User> reviewers, Manuscript manuscript, Map<Integer, Integer> scoreOut) {
        if (reviewers == null) return Collections.emptyList();
        if (manuscript == null) return new ArrayList<>(reviewers);

        String msArea = manuscript.getSubjectArea();
        String msKeywords = manuscript.getKeywords();
        String msText = ((msArea == null ? "" : msArea) + " " + (msKeywords == null ? "" : msKeywords)).trim();
        Set<String> msTokens = tokenizeKeywords(msText);

        List<User> copy = new ArrayList<>(reviewers);

        if (scoreOut != null) {
            scoreOut.clear();
            for (User u : copy) {
                if (u == null || u.getUserId() == null) continue;
                int sc = editorMatchScore(u, msText, msTokens); 
                scoreOut.put(u.getUserId(), sc);
            }
        }

        copy.sort((a, b) -> {
            int sa = editorMatchScore(a, msText, msTokens);
            int sb = editorMatchScore(b, msText, msTokens);
            if (sa != sb) return Integer.compare(sb, sa); 

            
            int ca = (a == null || a.getCompletedReviewCount() == null) ? 0 : a.getCompletedReviewCount();
            int cb = (b == null || b.getCompletedReviewCount() == null) ? 0 : b.getCompletedReviewCount();
            if (ca != cb) return Integer.compare(cb, ca);

            double aa = (a == null || a.getAvgReviewScore() == null) ? 0.0 : a.getAvgReviewScore();
            double ab = (b == null || b.getAvgReviewScore() == null) ? 0.0 : b.getAvgReviewScore();
            if (Double.compare(aa, ab) != 0) return Double.compare(ab, aa);

            boolean ha = a != null && a.getResearchArea() != null && !a.getResearchArea().trim().isEmpty();
            boolean hb = b != null && b.getResearchArea() != null && !b.getResearchArea().trim().isEmpty();
            if (ha != hb) return hb ? 1 : -1;

            String ua = a == null ? "" : String.valueOf(a.getUsername());
            String ub = b == null ? "" : String.valueOf(b.getUsername());
            return ua.compareToIgnoreCase(ub);
        });

        return copy;
    }

    private Set<String> tokenizeKeywords(String s) {
        if (s == null) return Collections.emptySet();
        String normalized = s.toLowerCase()
                .replaceAll("[\u3000\t\r\n]+", " ")
                .replaceAll("[，、；;|/\\\\]+", " ")   
                .replaceAll("[()（）\\[\\]{}<>《》“”\\\"'`]+", " ")
                .replaceAll("[:：·•—–\\-_=+]+", " ")
                .replaceAll("[,]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) return Collections.emptySet();

        String[] parts = normalized.split(" ");
        Set<String> set = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (t.length() == 1 && t.charAt(0) <= 127) continue; 
            set.add(t);
        }
        return set;
    }





    
    protected String requiredMenuPermission(String path) {
        if (path == null) return null;

        
        if (path.startsWith("/formalCheck/history")) return PermissionCatalog.MENU_EO_FORMAL_HISTORY;
        if (path.startsWith("/formalCheck/autoCheck")) return PermissionCatalog.MENU_EO_FORMAL_CHECK;
        if (path.startsWith("/formalCheck")) return PermissionCatalog.MENU_EO_FORMAL_CHECK;

        
        if (path.startsWith("/overview")) return PermissionCatalog.MENU_EIC_OVERVIEW;
        if (path.startsWith("/desk")) return PermissionCatalog.MENU_EIC_DESK;
        if (path.startsWith("/toAssign")) return PermissionCatalog.MENU_EIC_TO_ASSIGN;
        if (path.startsWith("/reviewers")) return PermissionCatalog.MENU_EIC_REVIEWERS;
        if (path.startsWith("/finalDecision")) return PermissionCatalog.MENU_EIC_FINAL_DECISION;
        if (path.startsWith("/special")) return PermissionCatalog.MENU_EIC_SPECIAL;

        
        if (path.startsWith("/withEditor")) return PermissionCatalog.MENU_EDITOR_TODO;
        if (path.startsWith("/underReview")) return PermissionCatalog.MENU_EDITOR_UNDER_REVIEW;
        if (path.startsWith("/recommend")) return PermissionCatalog.MENU_EDITOR_RECOMMEND;

        
        if (path.startsWith("/review/select") || path.startsWith("/review/externalInvite")
                || path.startsWith("/review/invite") || path.startsWith("/review/inviteExternal") || path.startsWith("/review/cancel")) {
            return PermissionCatalog.MENU_EDITOR_TODO;
        }
        
        if (path.startsWith("/review/remind") || path.startsWith("/review/remindCustom") || path.startsWith("/review/autoRemindNow")
                || path.startsWith("/review/monitor")) {
            return PermissionCatalog.MENU_EDITOR_REVIEW_MONITOR;
        }

        
        if (path.startsWith("/author/message") || path.startsWith("/authorComm")) {
            return PermissionCatalog.MENU_EDITOR_AUTHOR_COMM;
        }

        return null;
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

