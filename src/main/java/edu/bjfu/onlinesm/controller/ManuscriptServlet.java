package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.*;
import edu.bjfu.onlinesm.model.*;
import edu.bjfu.onlinesm.service.FormalCheckService;
import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.UploadPathUtil;
import edu.bjfu.onlinesm.util.HtmlToPdfConverter;
import edu.bjfu.onlinesm.util.PdfTextUtil;
import edu.bjfu.onlinesm.util.FileTextUtil;
import edu.bjfu.onlinesm.util.PaginationUtil;
import edu.bjfu.onlinesm.util.mail.MailNotifications;
import edu.bjfu.onlinesm.util.notify.InAppNotifications;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@WebServlet(name = "ManuscriptServlet", urlPatterns = {"/manuscripts/*"})
@MultipartConfig
public class ManuscriptServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final UserDAO userDAO = new UserDAO();

    
    private final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);

    private final JournalDAO journalDAO = new JournalDAO();
    private final ManuscriptAuthorDAO authorDAO = new ManuscriptAuthorDAO();
    private final ManuscriptRecommendedReviewerDAO recommendedReviewerDAO = new ManuscriptRecommendedReviewerDAO();
    private final ManuscriptFundingDAO fundingDAO = new ManuscriptFundingDAO();
    private final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();
    private final FileDAO fileDAO = new FileDAO();
    private final ManuscriptAssignmentDAO assignmentDAO = new ManuscriptAssignmentDAO();
    private final FormalCheckResultDAO formalCheckResultDAO = new FormalCheckResultDAO();
    private final FormalCheckService formalCheckService = new FormalCheckService();
    
    private final ManuscriptStatusHistoryDAO statusHistoryDAO = new ManuscriptStatusHistoryDAO();
    private final ManuscriptStageTimestampsDAO stageTimestampsDAO = new ManuscriptStageTimestampsDAO();
    
    private static final String UPLOAD_BASE_DIR = UploadPathUtil.getBaseDirPath();
    private static final String UPLOAD_MANUSCRIPT_DIR = UPLOAD_BASE_DIR + File.separator + "manuscripts";

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
    	User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/list";
        }

        try {
            switch (path) {
                case "/list":
                    handleAuthorList(req, resp, current);			
                    break;
                case "/exportCsv":
                    handleExportCsv(req, resp, current);			
                    break;
                case "/submit":
                    handleSubmitForm(req, resp, current, null);		
                    break;
                case "/edit":
                    handleEditDraft(req, resp, current);			
                    break;
                case "/resubmitEdit":
                    handleResubmitEditForm(req, resp, current);		
                    break;
                case "/detail":
                    handleDetail(req, resp, current);				
                    break;
                case "/track":
                    handleTrackStatus(req, resp, current);			
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);	
            }
        } catch (SQLException e) {
            throw new ServletException("访问数据库出错", e);
        }
    }

    
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
    	User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/submit";
        }

        switch (path) {
            case "/submit":
                handleSaveDraftOrSubmit(req, resp, current);
                break;
            case "/resubmit":
                handleResubmit(req, resp, current);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    
    
    private void handleSubmitForm(HttpServletRequest req, HttpServletResponse resp, User current, Manuscript draft)
            throws ServletException, IOException, SQLException {
    	
        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        req.setAttribute("manuscript", draft);

        
        Integer journalId = null;
        if (draft != null && draft.getJournalId() != null) {	
            journalId = draft.getJournalId();
        } else if (journals != null && !journals.isEmpty() && journals.get(0) != null) {
            journalId = journals.get(0).getJournalId();			
        }

        
        if (draft != null) {
        	
            req.setAttribute("authors", authorDAO.findByManuscriptId(draft.getManuscriptId()));
            
            req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(draft.getManuscriptId()));
            
            req.setAttribute("fundings", fundingDAO.findByManuscriptId(draft.getManuscriptId()));
            
            ManuscriptVersion cv = versionDAO.findCurrentByManuscriptId(draft.getManuscriptId());
            req.setAttribute("currentVersion", cv);

            
            try {
                if (cv != null) {
                    req.setAttribute("coverAttachments",
                            fileDAO.findByManuscriptVersionAndType(draft.getManuscriptId(), cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
                }
            } catch (Exception ignore) {
                
            }
        }

        
        if (draft == null) {
            req.setAttribute("fundings", java.util.Collections.emptyList());
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_submit.jsp").forward(req, resp);
    }

    
    private void handleResubmitEditForm(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {
    	
        if (!"AUTHOR".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有作者可以修改稿件");
            return;
        }

        
        Integer manuscriptId = parseInt(req.getParameter("id"));
        if (manuscriptId == null) {
            manuscriptId = parseInt(req.getParameter("manuscriptId"));
        }
        if (manuscriptId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少稿件ID");
            return;
        }

        
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件");
            return;
        }
        if (!Objects.equals(m.getSubmitterId(), current.getUserId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只能修改自己的稿件");
            return;
        }
        if (!("RETURNED".equals(m.getCurrentStatus()) || "REVISION".equals(m.getCurrentStatus()))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "当前稿件状态不允许修改");
            return;
        }

        resolveAndRememberGroup(req, m);

        
        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        if (m.getJournalId() != null) {
            req.setAttribute("journal", journalDAO.findById(m.getJournalId()));
        } else {
        }

        
        req.setAttribute("manuscript", m);
        req.setAttribute("authors", authorDAO.findByManuscriptId(manuscriptId));
        req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(manuscriptId));
        
        req.setAttribute("fundings", fundingDAO.findByManuscriptId(manuscriptId));
        ManuscriptVersion cv = versionDAO.findCurrentByManuscriptId(manuscriptId);
        req.setAttribute("currentVersion", cv);
        try {
            if (cv != null) {
                req.setAttribute("coverAttachments",
                        fileDAO.findByManuscriptVersionAndType(manuscriptId, cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
            }
        } catch (Exception ignore) {
        }

        
        try {
            req.setAttribute("formalCheckResult", formalCheckResultDAO.findByManuscriptId(manuscriptId));
        } catch (Exception ignore) {
            
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_resubmit.jsp").forward(req, resp);
    }

    
    private void forwardSubmitFormWithTempData(HttpServletRequest req, HttpServletResponse resp,
                                              Manuscript manuscript,
                                              List<ManuscriptAuthor> authors,
                                              List<ManuscriptRecommendedReviewer> recommendedReviewers)
            throws ServletException, IOException, SQLException {

        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        req.setAttribute("manuscript", manuscript);
        req.setAttribute("authors", authors == null ? Collections.emptyList() : authors);
        req.setAttribute("recommendedReviewers", recommendedReviewers == null ? Collections.emptyList() : recommendedReviewers);
        
        if (req.getAttribute("fundings") == null) {
            if (manuscript != null && manuscript.getManuscriptId() != null) {
                try {
                    req.setAttribute("fundings", fundingDAO.findByManuscriptId(manuscript.getManuscriptId()));
                } catch (Exception ignore) {
                    req.setAttribute("fundings", Collections.emptyList());
                }
            } else {
                req.setAttribute("fundings", Collections.emptyList());
            }
        }

        
        if (manuscript != null && manuscript.getManuscriptId() != null) {
            ManuscriptVersion cv = versionDAO.findCurrentByManuscriptId(manuscript.getManuscriptId());
            req.setAttribute("currentVersion", cv);
            try {
                if (cv != null) {
                    req.setAttribute("coverAttachments",
                            fileDAO.findByManuscriptVersionAndType(manuscript.getManuscriptId(), cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
                }
            } catch (Exception ignore) {
            }
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_submit.jsp").forward(req, resp);
    }

    
    private void forwardResubmitFormWithTempData(HttpServletRequest req, HttpServletResponse resp,
                                                Manuscript manuscript,
                                                List<ManuscriptAuthor> authors,
                                                List<ManuscriptRecommendedReviewer> recommendedReviewers)
            throws ServletException, IOException, SQLException {

        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        req.setAttribute("manuscript", manuscript);
        req.setAttribute("authors", authors == null ? Collections.emptyList() : authors);
        req.setAttribute("recommendedReviewers", recommendedReviewers == null ? Collections.emptyList() : recommendedReviewers);
        
        if (req.getAttribute("fundings") == null) {
            if (manuscript != null && manuscript.getManuscriptId() != null) {
                try {
                    req.setAttribute("fundings", fundingDAO.findByManuscriptId(manuscript.getManuscriptId()));
                } catch (Exception ignore) {
                    req.setAttribute("fundings", Collections.emptyList());
                }
            } else {
                req.setAttribute("fundings", Collections.emptyList());
            }
        }

        
        if (manuscript != null && manuscript.getManuscriptId() != null) {
            ManuscriptVersion cv = versionDAO.findCurrentByManuscriptId(manuscript.getManuscriptId());
            req.setAttribute("currentVersion", cv);
            try {
                if (cv != null) {
                    req.setAttribute("coverAttachments",
                            fileDAO.findByManuscriptVersionAndType(manuscript.getManuscriptId(), cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
                }
            } catch (Exception ignore) {
            }
        }

        
        if (manuscript != null && manuscript.getManuscriptId() != null) {
            try {
                req.setAttribute("formalCheckResult", formalCheckResultDAO.findByManuscriptId(manuscript.getManuscriptId()));
            } catch (Exception ignore) {
            }
        }
        
        
        if (manuscript != null && manuscript.getJournalId() != null) {
            req.setAttribute("journal", journalDAO.findById(manuscript.getJournalId()));
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_resubmit.jsp").forward(req, resp);
    }

    
    
    private String findFirstIncompleteRecommendedReviewerRow(HttpServletRequest req) {
        String[] names = req.getParameterValues("recReviewerName");
        String[] emails = req.getParameterValues("recReviewerEmail");
        String[] reasons = req.getParameterValues("recReviewerReason");

        
        if (names == null && emails == null && reasons == null) {
            return null;
        }

        
        int max = 0;
        if (names != null) max = Math.max(max, names.length);
        if (emails != null) max = Math.max(max, emails.length);
        if (reasons != null) max = Math.max(max, reasons.length);

        for (int i = 0; i < max; i++) {
            String n = (names == null ? null : getArrayValue(names, i));
            String e = (emails == null ? null : getArrayValue(emails, i));
            String r = (reasons == null ? null : getArrayValue(reasons, i));

            boolean anyFilled = (n != null && !n.isEmpty()) || 
                               (e != null && !e.isEmpty()) || 
                               (r != null && !r.isEmpty());
            
            boolean complete = (n != null && !n.isEmpty()) && 
                              (e != null && !e.isEmpty());
            
            if (anyFilled && !complete) {
                return "推荐审稿人填写不完整，请重新填写。";
            }
        }
        return null;
    }

    
    private void handleEditDraft(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!"AUTHOR".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有作者可以编辑草稿。");
            return;
        }

        Integer id = parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少稿件 ID。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(id);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }
        if (!Objects.equals(m.getSubmitterId(), current.getUserId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只能编辑自己的草稿。");
            return;
        }
        if (!"DRAFT".equals(m.getCurrentStatus())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "仅 DRAFT 状态允许通过“继续编辑”进入投稿编辑页。");
            return;
        }

        
        resolveAndRememberGroup(req, m);
        handleSubmitForm(req, resp, current, m);
    }

    
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        
        
        if ("REVIEWER".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "审稿人无权访问稿件详情页，请在‘待评审稿件’中查看摘要并下载稿件。");
            return;
        }

        Integer manuscriptId = parseInt(req.getParameter("id"));
        if (manuscriptId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少稿件 ID 参数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }

        
        if ("AUTHOR".equals(current.getRoleCode()) && !Objects.equals(current.getUserId(), m.getSubmitterId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看他人稿件详情。");
            return;
        }


        resolveAndRememberGroup(req, m);

        req.setAttribute("manuscript", m);

        
        try {
            if (m.getJournalId() != null) {
                req.setAttribute("journal", journalDAO.findById(m.getJournalId()));
            }
        } catch (Exception ignore) {
        }

        
        try {
            if (m.getSubmitterId() != null) {
                req.setAttribute("submitter", userDAO.findById(m.getSubmitterId()));
            }
        } catch (Exception ignore) {
        }

        req.setAttribute("authors", authorDAO.findByManuscriptId(manuscriptId));
        req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(manuscriptId));
        ManuscriptVersion cv = versionDAO.findCurrentByManuscriptId(manuscriptId);
        req.setAttribute("currentVersion", cv);

        
        try {
            if (cv != null) {
                req.setAttribute("coverAttachments",
                        fileDAO.findByManuscriptVersionAndType(manuscriptId, cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
            }
        } catch (Exception ignore) {
        }

        FormalCheckResult formalCheckResult = formalCheckResultDAO.findByManuscriptId(manuscriptId);
        req.setAttribute("formalCheckResult", formalCheckResult);

        
        
        int bodyCount = 0;
        int pdfPageCount = 0;
        int abstractCount = 0;
        try {
            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);

            String bodyText = "";

            boolean eoAdminOnlyPdf = "EO_ADMIN".equals(current.getRoleCode());

            if (!eoAdminOnlyPdf) {
                
                if (currentVer != null) {
                    String coverPath = currentVer.getCoverLetterPath();
                    if (coverPath != null && !coverPath.trim().isEmpty()) {
                        bodyText = FileTextUtil.extractText(new File(coverPath));
                    }
                }
                bodyCount = formalCheckService.computeBodyCount(bodyText);
            }

            
            if (currentVer != null && (eoAdminOnlyPdf || bodyCount == 0)) {
                String pdfPath = null;
                if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileOriginalPath();
                } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileAnonymousPath();
                }
                if (pdfPath != null) {
                    File pdfFile = new File(pdfPath);
                    bodyText = FileTextUtil.extractText(pdfFile);
                    bodyCount = formalCheckService.computeBodyCount(bodyText);
                    pdfPageCount = PdfTextUtil.extractPageCount(pdfFile);
                }
            }
        } catch (Exception ignore) {
            bodyCount = 0;
        }
        try {
            abstractCount = formalCheckService.computeAbstractCount(m.getAbstractText());
        } catch (Exception ignore) {
            abstractCount = 0;
        }
        req.setAttribute("pdfPageCount", pdfPageCount);
        req.setAttribute("bodyCount", bodyCount);
        req.setAttribute("abstractCount", abstractCount);

        
        List<Review> reviewList = reviewDAO.findByManuscript(manuscriptId);
        req.setAttribute("reviews", reviewList);

        
        
        java.util.Set<Integer> assignedReviewerIds = new java.util.HashSet<>();
        for (Review r : reviewList) {
            if (r == null) continue;
            String st = r.getStatus();
            if ("INVITED".equals(st) || "ACCEPTED".equals(st)) {
                assignedReviewerIds.add(r.getReviewerId());
            }
        }
        req.setAttribute("assignedReviewerIds", assignedReviewerIds);

        
        String role = current.getRoleCode();
        if ("EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role)) {

            
            String reviewerKeyword = req.getParameter("reviewerKeyword");
            String minCompletedStr = req.getParameter("minCompleted");
            String minAvgScoreStr  = req.getParameter("minAvgScore");

            Integer minCompleted = null;
            Integer minAvgScore  = null;
            try {
                if (minCompletedStr != null && !minCompletedStr.trim().isEmpty()) {
                    minCompleted = Integer.parseInt(minCompletedStr.trim());
                }
            } catch (NumberFormatException ignore) {
                
            }
            try {
                if (minAvgScoreStr != null && !minAvgScoreStr.trim().isEmpty()) {
                    minAvgScore = Integer.parseInt(minAvgScoreStr.trim());
                }
            } catch (NumberFormatException ignore) {
                
            }

            boolean hasSearch = (reviewerKeyword != null && !reviewerKeyword.trim().isEmpty())
                    || minCompleted != null
                    || minAvgScore != null;

            List<User> reviewerUsers;
            if (hasSearch) {
                
                reviewerUsers = userDAO.searchReviewerPool(reviewerKeyword, minCompleted, minAvgScore, 100);
            } else {
                
                reviewerUsers = userDAO.findByRoleCode("REVIEWER");
            }
            req.setAttribute("reviewers", reviewerUsers);

            
            String suggestionKeyword = null;
            if (m.getSubjectArea() != null && !m.getSubjectArea().trim().isEmpty()) {
                suggestionKeyword = m.getSubjectArea().split("[,;，； ]")[0];
            } else if (m.getKeywords() != null && !m.getKeywords().trim().isEmpty()) {
                suggestionKeyword = m.getKeywords().split("[,;，； ]")[0];
            }

            if (suggestionKeyword != null && !suggestionKeyword.trim().isEmpty()) {
                List<User> suggested = userDAO.searchReviewerPool(suggestionKeyword, 1, null, 5);
                req.setAttribute("reviewerSuggestions", suggested);
                req.setAttribute("reviewerSuggestionKeyword", suggestionKeyword);
            }
        }

        
        if ("EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role)) {
            ManuscriptAssignment chiefAssignment =
                    assignmentDAO.findLatestByManuscriptAndEditor(manuscriptId, current.getUserId());
            req.setAttribute("chiefAssignment", chiefAssignment);
        }

        
        NotificationDAO notificationDAO = new NotificationDAO();
        List<Notification> authorMessages;
        if ("AUTHOR".equals(role)) {
            authorMessages = notificationDAO.listByManuscriptAndCategory(manuscriptId, "AUTHOR_MESSAGE", current.getUserId(), 200, true);
        } else {
            
            authorMessages = notificationDAO.listByManuscriptAndCategory(manuscriptId, "AUTHOR_MESSAGE", null, 200, true);
        }

        Map<Integer, User> authorMessageUserMap = new HashMap<>();
        authorMessageUserMap.put(current.getUserId(), current);
        if (m.getSubmitterId() != null) {
            User au = userDAO.findById(m.getSubmitterId());
            if (au != null) authorMessageUserMap.put(au.getUserId(), au);
        }
        for (Notification n : authorMessages) {
            Integer cb = n.getCreatedByUserId();
            if (cb != null && !authorMessageUserMap.containsKey(cb)) {
                User u = userDAO.findById(cb);
                if (u != null) authorMessageUserMap.put(cb, u);
            }
            int ru = n.getRecipientUserId();
            if (!authorMessageUserMap.containsKey(ru)) {
                User u = userDAO.findById(ru);
                if (u != null) authorMessageUserMap.put(ru, u);
            }
        }

        req.setAttribute("authorMessages", authorMessages);
        req.setAttribute("authorMessageUserMap", authorMessageUserMap);

        
        try {
            List<ManuscriptStatusHistory> history = statusHistoryDAO.findByManuscriptId(manuscriptId);
            String deskRejectReason = null;
            for (int i = history.size() - 1; i >= 0; i--) {
                ManuscriptStatusHistory h = history.get(i);
                if (h != null && "DESK_REJECT".equalsIgnoreCase(h.getEvent())) {
                    deskRejectReason = h.getRemark();
                    break;
                }
            }
            req.setAttribute("deskRejectReason", deskRejectReason);
        } catch (Exception ignore) {
        }

        req.getRequestDispatcher("/WEB-INF/jsp/manuscript/detail.jsp").forward(req, resp);
    }
    

    
    private void handleTrackStatus(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        Integer manuscriptId = parseInt(req.getParameter("id"));
        if (manuscriptId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少稿件 ID 参数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }

        
        if ("AUTHOR".equals(current.getRoleCode()) && !Objects.equals(current.getUserId(), m.getSubmitterId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看他人稿件状态。");
            return;
        }


        resolveAndRememberGroup(req, m);

        
        List<ManuscriptStatusHistory> historyList = statusHistoryDAO.findByManuscriptId(manuscriptId);

        
        ManuscriptStageTimestamps stageTimestamps = stageTimestampsDAO.findByManuscriptId(manuscriptId);

        
        
        
        if (stageTimestamps != null && stageTimestamps.getUnderReviewCompletedAt() == null) {
            try {
                List<Review> reviews = reviewDAO.findByManuscript(manuscriptId);
                LocalDateTime maxSubmittedAt = null;
                for (Review r : reviews) {
                    LocalDateTime t = r.getSubmittedAt();
                    if (t != null && (maxSubmittedAt == null || t.isAfter(maxSubmittedAt))) {
                        maxSubmittedAt = t;
                    }
                }
                if (maxSubmittedAt != null) {
                    stageTimestamps.setUnderReviewCompletedAt(maxSubmittedAt);
                }
            } catch (Exception ignore) {
                
            }
        }
        
        
        List<ManuscriptStatusHistory> completeHistoryList = buildCompleteHistoryList(
                manuscriptId, historyList, stageTimestamps, m);
        
        
        completeHistoryList.sort((h1, h2) -> {
            if (h1.getChangeTime() == null && h2.getChangeTime() == null) return 0;
            if (h1.getChangeTime() == null) return 1;
            if (h2.getChangeTime() == null) return -1;
            return h1.getChangeTime().compareTo(h2.getChangeTime());
        });

        
        String estimatedCycle = statusHistoryDAO.getEstimatedReviewCycle(m.getCurrentStatus());

        
        FormalCheckResult formalCheckResult = null;
        try {
            formalCheckResult = formalCheckResultDAO.findByManuscriptId(manuscriptId);
        } catch (Exception ignore) {
            formalCheckResult = null;
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("historyList", completeHistoryList);
        req.setAttribute("estimatedCycle", estimatedCycle);
        req.setAttribute("stageTimestamps", stageTimestamps);
        req.setAttribute("currentStatusDesc", ManuscriptStatusHistory.getStatusDescription(m.getCurrentStatus()));
        req.setAttribute("formalCheckResult", formalCheckResult);

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_track.jsp").forward(req, resp);
    }
    
    
    private List<ManuscriptStatusHistory> buildCompleteHistoryList(
            int manuscriptId,
            List<ManuscriptStatusHistory> dbHistoryList,
            ManuscriptStageTimestamps stageTimestamps,
            Manuscript manuscript) {
        
        List<ManuscriptStatusHistory> completeList = new ArrayList<>();
        
        
        String[] statusFlow = {
            "DRAFT", "SUBMITTED", "FORMAL_CHECK", "DESK_REVIEW_INITIAL",
            "TO_ASSIGN", "WITH_EDITOR", "UNDER_REVIEW", 
            "EDITOR_RECOMMENDATION", "FINAL_DECISION_PENDING"
        };
        
        
        Map<String, String> statusToEvent = new HashMap<>();
        statusToEvent.put("DRAFT", "DRAFT_COMPLETED");
        statusToEvent.put("SUBMITTED", "SUBMIT");
        statusToEvent.put("FORMAL_CHECK", "FORMAL_CHECK_START");
        statusToEvent.put("DESK_REVIEW_INITIAL", "FORMAL_CHECK_APPROVE");
        statusToEvent.put("TO_ASSIGN", "DESK_REVIEW_ACCEPT");
        statusToEvent.put("WITH_EDITOR", "ASSIGN_EDITOR");
        statusToEvent.put("UNDER_REVIEW", "SEND_TO_REVIEW");
        statusToEvent.put("EDITOR_RECOMMENDATION", "REVIEW_COMPLETED");
        statusToEvent.put("FINAL_DECISION_PENDING", "EDITOR_RECOMMENDATION_SUBMIT");
        
        
        if (stageTimestamps != null) {
            for (String status : statusFlow) {
                LocalDateTime completedAt = stageTimestamps.getCompletedAtByStatus(status);
                if (completedAt != null) {
                    ManuscriptStatusHistory history = new ManuscriptStatusHistory();
                    history.setManuscriptId(manuscriptId);
                    history.setToStatus(status);
                    history.setChangeTime(completedAt);
                    history.setEvent(statusToEvent.getOrDefault(status, "STATUS_CHANGE"));
                    history.setRemark("阶段完成");
                    
                    
                    int currentIndex = -1;
                    for (int i = 0; i < statusFlow.length; i++) {
                        if (statusFlow[i].equals(status)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    if (currentIndex > 0) {
                        history.setFromStatus(statusFlow[currentIndex - 1]);
                    }
                    
                    completeList.add(history);
                }
            }
        }
        
        
        
        Map<String, ManuscriptStatusHistory> historyMap = new HashMap<>();
        
        
        for (ManuscriptStatusHistory h : completeList) {
            String key = h.getChangeTime() + "_" + h.getToStatus();
            historyMap.put(key, h);
        }
        
        
        for (ManuscriptStatusHistory h : dbHistoryList) {
            if (h.getChangeTime() != null) {
                String key = h.getChangeTime() + "_" + h.getToStatus();
                if (!historyMap.containsKey(key)) {
                    historyMap.put(key, h);
                } else {
                    
                    historyMap.put(key, h);
                }
            }
        }
        
        
        if (historyMap.isEmpty() && manuscript.getSubmitTime() != null) {
            ManuscriptStatusHistory initial = new ManuscriptStatusHistory();
            initial.setManuscriptId(manuscriptId);
            initial.setToStatus(manuscript.getCurrentStatus());
            initial.setChangeTime(manuscript.getSubmitTime());
            initial.setEvent("SUBMIT");
            initial.setRemark("稿件提交");
            historyMap.put(initial.getChangeTime() + "_" + initial.getToStatus(), initial);
        }
        
        return new ArrayList<>(historyMap.values());
    }

    
    private void handleSaveDraftOrSubmit(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException {

        if (!"AUTHOR".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有作者可以投稿。");
            return;
        }

        String action = trim(req.getParameter("action")); 
        boolean isFinalSubmit = "submit".equalsIgnoreCase(action);

        Integer manuscriptId = parseInt(req.getParameter("manuscriptId"));
        Manuscript existing = null;

        try {
            if (manuscriptId != null) {
                existing = manuscriptDAO.findById(manuscriptId);
                if (existing == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
                    return;
                }
                if (!Objects.equals(existing.getSubmitterId(), current.getUserId())) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只能修改自己的稿件。");
                    return;
                }
                if (!"DRAFT".equals(existing.getCurrentStatus())) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "只有 DRAFT 状态允许继续编辑。其他状态请在详情页执行“待修改/Resubmit”。");
                    return;
                }
            }

            Manuscript m = buildManuscriptFromRequest(req, current);
            if (manuscriptId != null) {
                m.setManuscriptId(manuscriptId);
            }

            List<ManuscriptAuthor> authors = buildAuthorsFromRequest(req);
            List<ManuscriptRecommendedReviewer> recs = buildRecommendedReviewersFromRequest(req);

            
            List<String> fundingErrors = new ArrayList<>();
            List<ManuscriptFunding> fundings = buildFundingsFromRequest(req, fundingErrors);
            
            req.setAttribute("fundings", fundings);
            if (!fundingErrors.isEmpty()) {
                req.setAttribute("error", String.join("；", fundingErrors));
                forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                return;
            }
            
            String fundingSummary = buildFundingInfoSummary(fundings);
            if (fundingSummary != null && !fundingSummary.isEmpty()) {
                m.setFundingInfo(fundingSummary);
            } else {
                
                if (m.getFundingInfo() == null || m.getFundingInfo().trim().isEmpty()) {
                    m.setFundingInfo(null);
                }
            }


            
            
            String recRowError = findFirstIncompleteRecommendedReviewerRow(req);
            if (isFinalSubmit && recRowError != null) {
                req.setAttribute("error", recRowError);
                forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                return;
            }

            
            if (isFinalSubmit) {
                if (m.getTitle() == null || m.getTitle().isEmpty()) {
                    req.setAttribute("error", "稿件标题不能为空。");
                    forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                    return;
                }
                if (authors.isEmpty()) {
                    req.setAttribute("error", "最终提交前至少需要填写 1 位作者。");
                    forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                    return;
                }
            }

            
            Part manuscriptFile = safeGetPart(req, "manuscriptFile");
            Part anonymousFile = safeGetPart(req, "anonymousFile");
            
            List<Part> coverAttachmentParts = safeGetParts(req, "coverAttachments");
            String coverLetterHtml = trim(req.getParameter("coverLetterHtml"));

            
            if (isFinalSubmit) {
                
                ManuscriptVersion prevVersion = null;
                if (manuscriptId != null) {
                    prevVersion = versionDAO.findCurrentByManuscriptId(manuscriptId);
                }
                boolean hasExistingManuscript = prevVersion != null && prevVersion.getFileOriginalPath() != null && !prevVersion.getFileOriginalPath().trim().isEmpty();
                boolean hasExistingAnonymous = prevVersion != null && prevVersion.getFileAnonymousPath() != null && !prevVersion.getFileAnonymousPath().trim().isEmpty();
                
                
                if ((manuscriptFile == null || manuscriptFile.getSize() == 0) && !hasExistingManuscript) {
                    req.setAttribute("error", "最终提交前必须上传手稿文件（PDF 格式）。");
                    forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                    return;
                }
                if ((anonymousFile == null || anonymousFile.getSize() == 0) && !hasExistingAnonymous) {
                    req.setAttribute("error", "最终提交前必须上传匿名手稿文件（PDF 格式）。");
                    forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                    return;
                }
            }

            
            if (manuscriptFile != null && manuscriptFile.getSize() > 0 && !isPdfFile(manuscriptFile)) {
                req.setAttribute("error", "手稿文件必须是 PDF 格式。");
                forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                return;
            }
            if (anonymousFile != null && anonymousFile.getSize() > 0 && !isPdfFile(anonymousFile)) {
                req.setAttribute("error", "匿名手稿必须是 PDF 格式。");
                forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                return;
            }

            
            m.setAuthorList(joinAuthorNames(authors));

            try (Connection conn = DbUtil.getConnection()) {
                conn.setAutoCommit(false);

                if (existing == null) {
                    manuscriptDAO.insertWithStatus(conn, m, isFinalSubmit ? "SUBMITTED" : "DRAFT", isFinalSubmit);
                    manuscriptId = m.getManuscriptId();
                } else {
                    manuscriptDAO.updateMetadataAndStatus(conn, m, isFinalSubmit ? "SUBMITTED" : "DRAFT", isFinalSubmit);
                }

                
                authorDAO.deleteByManuscriptId(conn, manuscriptId);
                authorDAO.insertBatch(conn, manuscriptId, authors);

                recommendedReviewerDAO.deleteByManuscriptId(conn, manuscriptId);
                recommendedReviewerDAO.insertBatch(conn, manuscriptId, recs);

                
                try {
                    fundingDAO.replaceByManuscriptId(conn, manuscriptId, fundings);
                } catch (SQLException e) {
                    
                    String msg = e.getMessage();
                    if (msg == null) throw e;
                    String lower = msg.toLowerCase();
                    if (!(lower.contains("invalid object name") && lower.contains("manuscriptfundings"))) {
                        throw e;
                    }
                }

                
                int nextVersionNumber = getNextVersionNumber(conn, manuscriptId);

                
                ManuscriptVersion prevCurrent = versionDAO.findCurrentByManuscriptId(conn, manuscriptId);

                

                
                ManuscriptVersion v = new ManuscriptVersion();
                v.setManuscriptId(manuscriptId);
                v.setVersionNumber(nextVersionNumber);
                v.setCurrent(true);
                v.setCreatedBy(current.getUserId());

                File versionDir = new File(UPLOAD_MANUSCRIPT_DIR + File.separator + "MS_" + manuscriptId + File.separator + "v" + nextVersionNumber);
                if (!versionDir.exists()) {
                    
                    versionDir.mkdirs();
                }

                String fileOriginalPath = null;
                if (manuscriptFile != null && manuscriptFile.getSize() > 0) {
                    fileOriginalPath = savePartToDir(manuscriptFile, versionDir, "manuscript_");
                }

                
                String fileAnonymousPath = null;
                if (anonymousFile != null && anonymousFile.getSize() > 0) {
                    fileAnonymousPath = savePartToDir(anonymousFile, versionDir, "anonymous_");
                }

                
                String coverPath = null;
                String remark = null;
                String coverHtmlToStore = coverLetterHtml;
                if (coverLetterHtml != null && !coverLetterHtml.isEmpty() && !HtmlToPdfConverter.isEmptyHtml(coverLetterHtml)) {
                    try {
                        File coverPdfFile = new File(versionDir, "cover_letter.pdf");
                        HtmlToPdfConverter.convert(coverLetterHtml, coverPdfFile);
                        coverPath = coverPdfFile.getAbsolutePath();
                    } catch (Exception e) {
                        
                        String htmlPath = saveTextToFile(coverLetterHtml, new File(versionDir, "cover_letter.html"));
                        coverPath = htmlPath;
                        remark = "CoverLetter PDF 转换失败，已保存 HTML 原文";
                    }
                }

                
                if (prevCurrent != null) {
                    if (fileOriginalPath == null || fileOriginalPath.trim().isEmpty()) {
                        fileOriginalPath = prevCurrent.getFileOriginalPath();
                    }
                    if (fileAnonymousPath == null || fileAnonymousPath.trim().isEmpty()) {
                        fileAnonymousPath = prevCurrent.getFileAnonymousPath();
                    }
                    if (coverPath == null || coverPath.trim().isEmpty()) {
                        coverPath = prevCurrent.getCoverLetterPath();
                    }
                    
                    if (coverHtmlToStore == null || coverHtmlToStore.trim().isEmpty() || HtmlToPdfConverter.isEmptyHtml(coverHtmlToStore)) {
                        coverHtmlToStore = prevCurrent.getCoverLetterHtml();
                    }
                    
                    if (v.getResponseLetterPath() == null) {
                        v.setResponseLetterPath(prevCurrent.getResponseLetterPath());
                    }
                    if (remark == null || remark.trim().isEmpty()) {
                        remark = prevCurrent.getRemark();
                    }
                }

                v.setFileOriginalPath(fileOriginalPath);
                v.setFileAnonymousPath(fileAnonymousPath);
                v.setCoverLetterPath(coverPath);
                v.setCoverLetterHtml(coverHtmlToStore);
                v.setRemark(remark);

                versionDAO.markAllNotCurrent(conn, manuscriptId);
                versionDAO.insert(conn, v);

                
                try {
                    
                    if (prevCurrent != null && prevCurrent.getVersionId() != null) {
                        fileDAO.copyByVersionAndType(conn, manuscriptId, prevCurrent.getVersionId(), v.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT);
                    }

                    if (coverAttachmentParts != null) {
                        File attachDir = new File(versionDir, "cover_attachments");
                        if (!attachDir.exists()) {
                            
                            attachDir.mkdirs();
                        }
                        for (Part p : coverAttachmentParts) {
                            if (p == null || p.getSize() <= 0) continue;
                            String originalName = p.getSubmittedFileName();
                            if (originalName == null || originalName.trim().isEmpty()) continue;

                            String savedPath = savePartToDir(p, attachDir, "cover_attach_");

                            edu.bjfu.onlinesm.model.StoredFile sf = new edu.bjfu.onlinesm.model.StoredFile();
                            sf.setFileName(originalName);
                            sf.setFilePath(savedPath);
                            sf.setFileType(FileDAO.TYPE_COVER_ATTACHMENT);
                            sf.setFileSize(p.getSize());
                            sf.setUploaderId(current.getUserId());
                            sf.setManuscriptId(manuscriptId);
                            sf.setVersionId(v.getVersionId());
                            fileDAO.insert(conn, sf);
                        }
                    }
                } catch (Exception ignore) {
                    
                }

                conn.commit();
            }

            
            if (isFinalSubmit) {
                String manuscriptCode = genManuscriptCode(manuscriptId);
                inAppNotifications.onSubmissionSuccess(current, m, manuscriptCode);
                mailNotifications.onSubmissionSuccess(current, m, manuscriptCode);
            }

            String msg;
            String group;
            if (isFinalSubmit) {
                msg = "投稿已提交，稿件 ID：" + genManuscriptCode(manuscriptId);
                group = "processing";
            } else {
                msg = "草稿已保存，可随时继续编辑。";
                group = "incomplete";
            }

            resp.sendRedirect(req.getContextPath() + "/manuscripts/list?group=" + group + "&msg=" + URLEncoder.encode(msg, "UTF-8"));
        } catch (Exception e) {
            throw new ServletException("保存稿件失败", e);
        }
    }

    
    private void handleResubmit(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException {

        if (!"AUTHOR".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有作者可以执行 Resubmit。");
            return;
        }

        Integer manuscriptId = parseInt(req.getParameter("manuscriptId"));
        if (manuscriptId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少稿件 ID。");
            return;
        }

        
        String mode = trim(req.getParameter("mode"));
        if (mode == null || mode.isEmpty()) {
            mode = "submit";
        }

        try {
            Manuscript existing = manuscriptDAO.findById(manuscriptId);
            if (existing == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应稿件。");
                return;
            }

            if (!Objects.equals(existing.getSubmitterId(), current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只能对自己的稿件执行 Resubmit。");
                return;
            }

            String fromStatus = existing.getCurrentStatus();
            if (!"RETURNED".equals(fromStatus) && !"REVISION".equals(fromStatus)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "当前状态不允许 Resubmit 操作。");
                return;
            }

            Manuscript toUpdate = buildManuscriptFromRequest(req, current);
            toUpdate.setManuscriptId(manuscriptId);
            toUpdate.setSubmitterId(existing.getSubmitterId());

            List<ManuscriptAuthor> authors = buildAuthorsFromRequest(req);
            List<ManuscriptRecommendedReviewer> recs = buildRecommendedReviewersFromRequest(req);

            
            List<String> fundingErrors = new ArrayList<>();
            List<ManuscriptFunding> fundings = buildFundingsFromRequest(req, fundingErrors);
            req.setAttribute("fundings", fundings);
            if (!fundingErrors.isEmpty()) {
                req.setAttribute("error", String.join("；", fundingErrors));
                forwardResubmitFormWithTempData(req, resp, toUpdate, authors, recs);
                return;
            }
            String fundingSummary = buildFundingInfoSummary(fundings);
            if (fundingSummary != null && !fundingSummary.isEmpty()) {
                toUpdate.setFundingInfo(fundingSummary);
            } else {
                if (toUpdate.getFundingInfo() == null || toUpdate.getFundingInfo().trim().isEmpty()) {
                    toUpdate.setFundingInfo(null);
                }
            }

            toUpdate.setAuthorList(joinAuthorNames(authors));

            
            if ("submit".equalsIgnoreCase(mode)) {
                String recRowError = findFirstIncompleteRecommendedReviewerRow(req);
                if (recRowError != null) {
                    req.setAttribute("error", recRowError);
                    forwardResubmitFormWithTempData(req, resp, toUpdate, authors, recs);
                    return;
                }
            }

            Part manuscriptFile = safeGetPart(req, "manuscriptFile");
            Part anonymousFile = safeGetPart(req, "anonymousFile");
            
            List<Part> coverAttachmentParts = safeGetParts(req, "coverAttachments");
            String coverLetterHtml = trim(req.getParameter("coverLetterHtml"));

            
            if (manuscriptFile != null && manuscriptFile.getSize() > 0 && !isPdfFile(manuscriptFile)) {
                req.setAttribute("error", "手稿文件必须是 PDF 格式。");
                forwardResubmitFormWithTempData(req, resp, toUpdate, authors, recs);
                return;
            }
            if (anonymousFile != null && anonymousFile.getSize() > 0 && !isPdfFile(anonymousFile)) {
                req.setAttribute("error", "匿名手稿必须是 PDF 格式。");
                forwardResubmitFormWithTempData(req, resp, toUpdate, authors, recs);
                return;
            }

            try (Connection conn = DbUtil.getConnection()) {
                conn.setAutoCommit(false);

                if ("draft".equalsIgnoreCase(mode)) {
                    
                    manuscriptDAO.updateResubmitDraft(conn, toUpdate);
                } else {
                    manuscriptDAO.updateAndResubmit(conn, toUpdate, fromStatus);
                }

                authorDAO.deleteByManuscriptId(conn, manuscriptId);
                authorDAO.insertBatch(conn, manuscriptId, authors);

                recommendedReviewerDAO.deleteByManuscriptId(conn, manuscriptId);
                recommendedReviewerDAO.insertBatch(conn, manuscriptId, recs);

                
                try {
                    fundingDAO.replaceByManuscriptId(conn, manuscriptId, fundings);
                } catch (SQLException e) {
                    String msg = e.getMessage();
                    if (msg == null) throw e;
                    String lower = msg.toLowerCase();
                    if (!(lower.contains("invalid object name") && lower.contains("manuscriptfundings"))) {
                        throw e;
                    }
                }

                int nextVersionNumber = getNextVersionNumber(conn, manuscriptId);

                
                ManuscriptVersion prevCurrent = versionDAO.findCurrentByManuscriptId(conn, manuscriptId);

                ManuscriptVersion v = new ManuscriptVersion();
                v.setManuscriptId(manuscriptId);
                v.setVersionNumber(nextVersionNumber);
                v.setCurrent(true);
                v.setCreatedBy(current.getUserId());

                File versionDir = new File(UPLOAD_MANUSCRIPT_DIR + File.separator + "MS_" + manuscriptId + File.separator + "v" + nextVersionNumber);
                if (!versionDir.exists()) {
                    
                    versionDir.mkdirs();
                }

                String fileOriginalPath = null;
                if (manuscriptFile != null && manuscriptFile.getSize() > 0) {
                    fileOriginalPath = savePartToDir(manuscriptFile, versionDir, "manuscript_");
                }

                
                String fileAnonymousPath = null;
                if (anonymousFile != null && anonymousFile.getSize() > 0) {
                    fileAnonymousPath = savePartToDir(anonymousFile, versionDir, "anonymous_");
                }

                
                String coverPath = null;
                String remark = null;
                String coverHtmlToStore = coverLetterHtml;
                if (coverLetterHtml != null && !coverLetterHtml.isEmpty() && !HtmlToPdfConverter.isEmptyHtml(coverLetterHtml)) {
                    try {
                        File coverPdfFile = new File(versionDir, "cover_letter.pdf");
                        HtmlToPdfConverter.convert(coverLetterHtml, coverPdfFile);
                        coverPath = coverPdfFile.getAbsolutePath();
                    } catch (Exception e) {
                        
                        String htmlPath = saveTextToFile(coverLetterHtml, new File(versionDir, "cover_letter.html"));
                        coverPath = htmlPath;
                        remark = "CoverLetter PDF 转换失败，已保存 HTML 原文";
                    }
                }

                v.setFileOriginalPath(fileOriginalPath);
                v.setFileAnonymousPath(fileAnonymousPath);
                v.setCoverLetterPath(coverPath);
                v.setCoverLetterHtml(coverHtmlToStore);
                v.setRemark(remark);

                
                if (prevCurrent != null) {
                    if (v.getFileOriginalPath() == null || v.getFileOriginalPath().trim().isEmpty()) {
                        v.setFileOriginalPath(prevCurrent.getFileOriginalPath());
                    }
                    if (v.getFileAnonymousPath() == null || v.getFileAnonymousPath().trim().isEmpty()) {
                        v.setFileAnonymousPath(prevCurrent.getFileAnonymousPath());
                    }
                    if (v.getCoverLetterPath() == null || v.getCoverLetterPath().trim().isEmpty()) {
                        v.setCoverLetterPath(prevCurrent.getCoverLetterPath());
                    }
                    
                    if (v.getCoverLetterHtml() == null || v.getCoverLetterHtml().trim().isEmpty() || HtmlToPdfConverter.isEmptyHtml(v.getCoverLetterHtml())) {
                        v.setCoverLetterHtml(prevCurrent.getCoverLetterHtml());
                    }
                    
                    if (v.getResponseLetterPath() == null || v.getResponseLetterPath().trim().isEmpty()) {
                        v.setResponseLetterPath(prevCurrent.getResponseLetterPath());
                    }
                    if (v.getRemark() == null || v.getRemark().trim().isEmpty()) {
                        v.setRemark(prevCurrent.getRemark());
                    }
                }

                versionDAO.markAllNotCurrent(conn, manuscriptId);
                versionDAO.insert(conn, v);

                
                try {
                    
                    if (prevCurrent != null && prevCurrent.getVersionId() != null) {
                        fileDAO.copyByVersionAndType(conn, manuscriptId, prevCurrent.getVersionId(), v.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT);
                    }

                    if (coverAttachmentParts != null) {
                        File attachDir = new File(versionDir, "cover_attachments");
                        if (!attachDir.exists()) {
                            
                            attachDir.mkdirs();
                        }
                        for (Part p : coverAttachmentParts) {
                            if (p == null || p.getSize() <= 0) continue;
                            String originalName = p.getSubmittedFileName();
                            if (originalName == null || originalName.trim().isEmpty()) continue;

                            String savedPath = savePartToDir(p, attachDir, "cover_attach_");

                            edu.bjfu.onlinesm.model.StoredFile sf = new edu.bjfu.onlinesm.model.StoredFile();
                            sf.setFileName(originalName);
                            sf.setFilePath(savedPath);
                            sf.setFileType(FileDAO.TYPE_COVER_ATTACHMENT);
                            sf.setFileSize(p.getSize());
                            sf.setUploaderId(current.getUserId());
                            sf.setManuscriptId(manuscriptId);
                            sf.setVersionId(v.getVersionId());
                            fileDAO.insert(conn, sf);
                        }
                    }
                } catch (Exception ignore) {
                    
                }

                conn.commit();
            }

            
            if (!"draft".equalsIgnoreCase(mode)) {
                String manuscriptCode = genManuscriptCode(manuscriptId);
                inAppNotifications.onSubmissionSuccess(current, toUpdate, manuscriptCode);
                mailNotifications.onSubmissionSuccess(current, toUpdate, manuscriptCode);
            }

            if ("draft".equalsIgnoreCase(mode)) {
                String msg = "已保存修改稿草稿，稿件 ID：" + genManuscriptCode(manuscriptId);
                resp.sendRedirect(req.getContextPath() + "/manuscripts/resubmitEdit?id=" + manuscriptId + "&msg=" + URLEncoder.encode(msg, "UTF-8"));
            } else {
                String msg = "已重新提交（Resubmit），稿件 ID：" + genManuscriptCode(manuscriptId);
                resp.sendRedirect(req.getContextPath() + "/manuscripts/list?group=processing&msg=" + URLEncoder.encode(msg, "UTF-8"));
            }
        } catch (Exception e) {
            throw new ServletException("执行 Resubmit 失败", e);
        }
    }

    

    
    private void handleAuthorList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        
        List<Manuscript> allList = manuscriptDAO.findBySubmitter(current.getUserId());

        
        String group = null;
        String[] groupArr = req.getParameterValues("group");
        if (groupArr != null) {
            for (int i = groupArr.length - 1; i >= 0; i--) {
                String g = trim(groupArr[i]);
                if (g != null && !g.isEmpty()) {
                    group = g;
                    break;
                }
            }
        }
        if (group == null || group.isEmpty()) {
            group = "processing"; 
        }
        group = group.toLowerCase();
        
        req.getSession().setAttribute("__msGroup", group);
        String statusFilter = trim(req.getParameter("status"));
        String fromDateStr = trim(req.getParameter("fromDate"));
        String toDateStr = trim(req.getParameter("toDate"));
        String sort = trim(req.getParameter("sort"));
        if (sort == null || sort.isEmpty()) {
            sort = "submitTime";
        }
        String dir = trim(req.getParameter("dir"));
        if (!"asc".equalsIgnoreCase(dir)) {
            dir = "desc";
        }

	        
	        int page = PaginationUtil.getPage(req);
	        int pageSize = PaginationUtil.getPageSize(req);

        LocalDate fromDate = null;
        LocalDate toDate = null;
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        try {
            if (fromDateStr != null && !fromDateStr.isEmpty()) {
                fromDate = LocalDate.parse(fromDateStr, df);
            }
            if (toDateStr != null && !toDateStr.isEmpty()) {
                toDate = LocalDate.parse(toDateStr, df);
            }
        } catch (Exception ignore) {
            fromDate = null;
            toDate = null;
        }

        
        List<Manuscript> filtered = new ArrayList<>();
        for (Manuscript m : allList) {
            String status = m.getCurrentStatus();
            if (!matchGroup(status, group)) {
                continue;
            }
            if (statusFilter != null && !statusFilter.isEmpty() && !status.equals(statusFilter)) {
                continue;
            }
            LocalDateTime submitTime = m.getSubmitTime();
            if (fromDate != null) {
                if (submitTime == null || submitTime.toLocalDate().isBefore(fromDate)) {
                    continue;
                }
            }
            if (toDate != null) {
                if (submitTime == null || submitTime.toLocalDate().isAfter(toDate)) {
                    continue;
                }
            }
            filtered.add(m);
        }

        
        Comparator<Manuscript> comparator;
        if ("id".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparingInt(Manuscript::getManuscriptId);
        } else {
            comparator = Comparator.comparing(
                    Manuscript::getSubmitTime,
                    (a, b) -> {
                        if (a == null && b == null) return 0;
                        if (a == null) return -1;
                        if (b == null) return 1;
                        return a.compareTo(b);
                    }
            );
        }
        filtered.sort(comparator);
        if ("desc".equalsIgnoreCase(dir)) {
            Collections.reverse(filtered);
        }

	        
	        PaginationUtil.apply(req, filtered, "manuscripts");

        
        int countIncomplete = 0;
        int countProcessing = 0;
        int countRevision = 0;
        int countDecision = 0;
        for (Manuscript m : allList) {
            String status = m.getCurrentStatus();
            if (matchGroup(status, "incomplete")) {
                countIncomplete++;
            }
            if (matchGroup(status, "processing")) {
                countProcessing++;
            }
            if (matchGroup(status, "revision")) {
                countRevision++;
            }
            if (matchGroup(status, "decision")) {
                countDecision++;
            }
        }

	        
        req.setAttribute("group", group);
        req.setAttribute("statusFilter", statusFilter);
        req.setAttribute("fromDate", fromDateStr);
        req.setAttribute("toDate", toDateStr);
        req.setAttribute("sort", sort);
        req.setAttribute("dir", dir);

        req.setAttribute("countIncomplete", countIncomplete);
        req.setAttribute("countProcessing", countProcessing);
        req.setAttribute("countRevision", countRevision);
        req.setAttribute("countDecision", countDecision);

        String msg = trim(req.getParameter("msg"));
        req.setAttribute("msg", msg);

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_list.jsp").forward(req, resp);
    }

    
    private void handleExportCsv(HttpServletRequest req, HttpServletResponse resp, User current)
            throws IOException, SQLException {

        List<Manuscript> allList = manuscriptDAO.findBySubmitter(current.getUserId());

        String group = trim(req.getParameter("group"));
        if (group == null || group.isEmpty()) {
            group = "processing";
        }
        String statusFilter = trim(req.getParameter("status"));
        String fromDateStr = trim(req.getParameter("fromDate"));
        String toDateStr = trim(req.getParameter("toDate"));

        LocalDate fromDate = null;
        LocalDate toDate = null;
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        try {
            if (fromDateStr != null && !fromDateStr.isEmpty()) {
                fromDate = LocalDate.parse(fromDateStr, df);
            }
            if (toDateStr != null && !toDateStr.isEmpty()) {
                toDate = LocalDate.parse(toDateStr, df);
            }
        } catch (Exception ignore) {
            fromDate = null;
            toDate = null;
        }

        List<Manuscript> filtered = new ArrayList<>();
        for (Manuscript m : allList) {
            String status = m.getCurrentStatus();
            if (!matchGroup(status, group)) {
                continue;
            }
            if (statusFilter != null && !statusFilter.isEmpty() && !status.equals(statusFilter)) {
                continue;
            }
            LocalDateTime submitTime = m.getSubmitTime();
            if (fromDate != null) {
                if (submitTime == null || submitTime.toLocalDate().isBefore(fromDate)) {
                    continue;
                }
            }
            if (toDate != null) {
                if (submitTime == null || submitTime.toLocalDate().isAfter(toDate)) {
                    continue;
                }
            }
            filtered.add(m);
        }

        resp.setCharacterEncoding("GBK");
        resp.setContentType("text/csv; charset=GBK");
        resp.setHeader("Content-Disposition", "attachment; filename=\"manuscripts.csv\"");
        PrintWriter writer = resp.getWriter();

        writer.println("ManuscriptId,Title,Status,SubmitTime,Decision,DecisionTime");

        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Manuscript m : filtered) {
            String submitTimeStr = "";
            if (m.getSubmitTime() != null) {
                submitTimeStr = dt.format(m.getSubmitTime());
            }
            String decisionStr = m.getDecision() == null ? "" : m.getDecision();
            String decisionTimeStr = "";
            if (m.getFinalDecisionTime() != null) {
                decisionTimeStr = dt.format(m.getFinalDecisionTime());
            }

            writer.printf("%d,%s,%s,%s,%s,%s%n",
                    m.getManuscriptId(),
                    escapeCsv(m.getTitle()),
                    escapeCsv(m.getCurrentStatus()),
                    escapeCsv(submitTimeStr),
                    escapeCsv(decisionStr),
                    escapeCsv(decisionTimeStr));
        }
        writer.flush();
    }

    private String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    
    private boolean matchGroup(String status, String group) {
        if (status == null) {
            return false;
        }
        switch (group) {
            case "incomplete":
                return "DRAFT".equals(status) || "INCOMPLETE".equals(status);
            case "processing":
                return "SUBMITTED".equals(status)
                        || "FORMAL_CHECK".equals(status)
                        || "FORMAT_CHECK".equals(status)
                        || "DESK_REVIEW_INITIAL".equals(status)
                        || "TO_ASSIGN".equals(status)
                        || "WITH_EDITOR".equals(status)
                        || "REVIEWER_ASSIGNED".equals(status)
                        || "UNDER_REVIEW".equals(status)
                        || "EDITOR_RECOMMENDATION".equals(status)
                        || "FINAL_DECISION_PENDING".equals(status);
            case "revision":
                return "RETURNED".equals(status)
                        || "REVISION".equals(status)
                        || "REVISION_REQUESTED".equals(status);
            case "decision":
                return "ACCEPTED".equals(status) || "REJECTED".equals(status);
            default:
                return true;
        }
    }
    
    private String resolveAndRememberGroup(HttpServletRequest req, Manuscript manuscript) {
        HttpSession session = req.getSession();

        
        String group = null;
        String[] groupArr = req.getParameterValues("group");
        if (groupArr != null) {
            for (int i = groupArr.length - 1; i >= 0; i--) {
                String g = trim(groupArr[i]);
                if (g != null && !g.isEmpty()) {
                    group = g;
                    break;
                }
            }
        }

        
        if (group == null || group.isEmpty()) {
            Object gObj = req.getAttribute("group");
            if (gObj != null) group = String.valueOf(gObj).trim();
        }

        
        if (group == null || group.isEmpty()) {
            Object gSess = session.getAttribute("__msGroup");
            if (gSess != null) group = String.valueOf(gSess).trim();
        }

        
        if ((group == null || group.isEmpty()) && manuscript != null) {
            String st = manuscript.getCurrentStatus();
            if (matchGroup(st, "incomplete")) group = "incomplete";
            else if (matchGroup(st, "revision")) group = "revision";
            else if (matchGroup(st, "decision")) group = "decision";
            else group = "processing";
        }

        
        if (group == null || group.isEmpty()) group = "processing";
        group = group.toLowerCase();

        
        req.setAttribute("group", group);
        session.setAttribute("__msGroup", group);

        return group;
    }


    

    private Manuscript buildManuscriptFromRequest(HttpServletRequest req, User current) {
        Manuscript m = new Manuscript();
        m.setSubmitterId(current.getUserId());

        m.setTitle(trim(req.getParameter("title")));
        m.setAbstractText(trim(req.getParameter("abstract")));
        m.setKeywords(trim(req.getParameter("keywords")));
        m.setSubjectArea(trim(req.getParameter("subjectArea")));
        m.setFundingInfo(trim(req.getParameter("fundingInfo")));

        Integer journalId = parseInt(req.getParameter("journalId"));
        m.setJournalId(journalId);
        return m;
    }

    
    private List<ManuscriptFunding> buildFundingsFromRequest(HttpServletRequest req, List<String> errors) {
        String[] names = req.getParameterValues("fundingName");
        String[] levels = req.getParameterValues("fundingLevel");
        String[] amounts = req.getParameterValues("fundingAmount");

        int max = 0;
        if (names != null) max = Math.max(max, names.length);
        if (levels != null) max = Math.max(max, levels.length);
        if (amounts != null) max = Math.max(max, amounts.length);

        List<ManuscriptFunding> list = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            String n = getArrayValue(names, i);
            String lv = getArrayValue(levels, i);
            String amtStr = getArrayValue(amounts, i);

            boolean anyFilled = (n != null && !n.isEmpty())
                    || (lv != null && !lv.isEmpty())
                    || (amtStr != null && !amtStr.isEmpty());

            if (!anyFilled) continue;

            
            if (n == null || n.isEmpty()) {
                if (errors != null) {
                    errors.add("第 " + (i + 1) + " 行项目资助：名称不能为空。");
                }
                continue;
            }

            ManuscriptFunding f = new ManuscriptFunding();
            f.setFundingName(n);
            f.setFundingLevel(lv);

            if (amtStr != null && !amtStr.isEmpty()) {
                try {
                    
                    String normalized = amtStr.replace(",", "");
                    f.setFundingAmount(new java.math.BigDecimal(normalized));
                } catch (Exception ex) {
                    if (errors != null) {
                        errors.add("第 " + (i + 1) + " 行项目资助：资助金额格式不正确。");
                    }
                    
                    continue;
                }
            }

            list.add(f);
        }
        return list;
    }

    
    private String buildFundingInfoSummary(List<ManuscriptFunding> fundings) {
        if (fundings == null || fundings.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fundings.size(); i++) {
            ManuscriptFunding f = fundings.get(i);
            if (f == null) continue;
            if (i > 0) sb.append("；");
            sb.append(f.getFundingName() == null ? "" : f.getFundingName());
            if (f.getFundingLevel() != null && !f.getFundingLevel().trim().isEmpty()) {
                sb.append("（").append(f.getFundingLevel().trim()).append("）");
            }
            if (f.getFundingAmount() != null) {
                sb.append(" 金额=").append(f.getFundingAmount().toPlainString());
            }
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }

private List<ManuscriptAuthor> buildAuthorsFromRequest(HttpServletRequest req) {
        String[] names = req.getParameterValues("authorName");
        String[] affiliations = req.getParameterValues("authorAffiliation");
        String[] degrees = req.getParameterValues("authorDegree");
        String[] titles = req.getParameterValues("authorTitle");
        String[] positions = req.getParameterValues("authorPosition");
        String[] emails = req.getParameterValues("authorEmail");
        Integer correspondingIndex = parseInt(req.getParameter("correspondingIndex"));

        List<ManuscriptAuthor> list = new ArrayList<>();
        if (names == null) {
            return list;
        }

        for (int i = 0; i < names.length; i++) {
            String n = trim(names[i]);
            if (n == null || n.isEmpty()) {
                continue;
            }
            ManuscriptAuthor a = new ManuscriptAuthor();
            a.setAuthorOrder(i + 1);
            a.setFullName(n);
            a.setAffiliation(getArrayValue(affiliations, i));
            a.setDegree(getArrayValue(degrees, i));
            a.setTitle(getArrayValue(titles, i));
            a.setPosition(getArrayValue(positions, i));
            a.setEmail(getArrayValue(emails, i));
            a.setCorresponding(correspondingIndex != null && correspondingIndex == i);
            list.add(a);
        }

        
        if (!list.isEmpty() && list.stream().noneMatch(ManuscriptAuthor::isCorresponding)) {
            list.get(0).setCorresponding(true);
        }

        return list;
    }

    private List<ManuscriptRecommendedReviewer> buildRecommendedReviewersFromRequest(HttpServletRequest req) {
        String[] names = req.getParameterValues("recReviewerName");
        String[] emails = req.getParameterValues("recReviewerEmail");
        String[] reasons = req.getParameterValues("recReviewerReason");

        List<ManuscriptRecommendedReviewer> list = new ArrayList<>();
        if (names == null) {
            return list;
        }

        for (int i = 0; i < names.length; i++) {
            String n = trim(names[i]);
            String e = getArrayValue(emails, i);
            String r = getArrayValue(reasons, i);
            if ((n == null || n.isEmpty()) && (e == null || e.isEmpty()) && (r == null || r.isEmpty())) {
                continue;
            }

            
            
            if (n == null || n.isEmpty() || e == null || e.isEmpty()) {
                continue;
            }
            ManuscriptRecommendedReviewer rr = new ManuscriptRecommendedReviewer();
            rr.setFullName(n);
            rr.setEmail(e);
            rr.setReason(r);
            list.add(rr);
        }
        return list;
    }

    private String joinAuthorNames(List<ManuscriptAuthor> authors) {
        if (authors == null || authors.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (ManuscriptAuthor a : authors) {
            if (a.getFullName() == null || a.getFullName().isEmpty()) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append(a.getFullName());
        }
        return sb.toString();
    }

    private String getArrayValue(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length) return null;
        return trim(arr[idx]);
    }

    private Part safeGetPart(HttpServletRequest req, String name) {
        try {
            return req.getPart(name);
        } catch (Exception e) {
            return null;
        }
    }

    
    private List<Part> safeGetParts(HttpServletRequest req, String name) {
        List<Part> list = new ArrayList<>();
        try {
            Collection<Part> parts = req.getParts();
            if (parts == null) return list;
            for (Part p : parts) {
                if (p == null) continue;
                if (!name.equals(p.getName())) continue;
                
                if (p.getSubmittedFileName() == null || p.getSubmittedFileName().trim().isEmpty()) continue;
                if (p.getSize() <= 0) continue;
                list.add(p);
            }
        } catch (Exception ignore) {
            
        }
        return list;
    }

    private String savePartToDir(Part part, File dir, String prefix) throws IOException {
        String submittedName = part.getSubmittedFileName();
        String ext = "";
        if (submittedName != null && submittedName.contains(".")) {
            ext = submittedName.substring(submittedName.lastIndexOf('.'));
        }
        String filename = prefix + System.currentTimeMillis() + ext;
        filename = sanitizeFilename(filename);

        File dest = new File(dir, filename);
        part.write(dest.getAbsolutePath());
        return dest.getAbsolutePath();
    }

    private String saveTextToFile(String html, File dest) throws IOException {
        
        java.nio.file.Files.write(dest.toPath(), html.getBytes(StandardCharsets.UTF_8));
        return dest.getAbsolutePath();
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    
    private boolean isPdfFile(Part part) {
        if (part == null || part.getSize() == 0) {
            return false;
        }
        
        
        String filename = part.getSubmittedFileName();
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            if (!lowerName.endsWith(".pdf")) {
                return false;
            }
        }
        
        
        String contentType = part.getContentType();
        if (contentType != null) {
            return contentType.equalsIgnoreCase("application/pdf");
        }
        
        return true; 
    }

    private int getNextVersionNumber(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT ISNULL(MAX(VersionNumber), 0) AS MaxVer FROM dbo.ManuscriptVersions WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("MaxVer") + 1;
                }
            }
        }
        return 1;
    }

    private String genManuscriptCode(int manuscriptId) {
        int year = LocalDate.now().getYear();
        return String.format("MS-%d-%03d", year, manuscriptId);
    }

    

    private User getCurrentUser(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("currentUser");
    }

    private String trim(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
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

