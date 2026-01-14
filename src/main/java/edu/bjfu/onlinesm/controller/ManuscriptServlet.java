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

/**
 * 投稿/稿件列表/详情控制器：
 *  - 保存草稿（DRAFT）与最终提交（SUBMITTED）
 *  - 投稿元数据：研究主题 SubjectArea、作者列表（多作者）、资助信息 FundingInfo
 *  - 文件上传：Manuscript、Cover Letter（支持富文本 Cover Letter）
 *  - 推荐审稿人
 *  - 文件预览（通过 ManuscriptFilePreviewServlet 提供）
 *
 */
@WebServlet(name = "ManuscriptServlet", urlPatterns = {"/manuscripts/*"})
@MultipartConfig
public class ManuscriptServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final UserDAO userDAO = new UserDAO();

    // 通知（站内 + 邮件）
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
    // 与 ProfileServlet 保持一致的上传根目录
    private final ManuscriptStatusHistoryDAO statusHistoryDAO = new ManuscriptStatusHistoryDAO();
    private final ManuscriptStageTimestampsDAO stageTimestampsDAO = new ManuscriptStageTimestampsDAO();
    // 与 ProfileServlet 保持一致的上传根目录
    private static final String UPLOAD_BASE_DIR = UploadPathUtil.getBaseDirPath();
    private static final String UPLOAD_MANUSCRIPT_DIR = UPLOAD_BASE_DIR + File.separator + "manuscripts";

    // 路由分发，处理前端请求
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 检查用户登陆状态
    	User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // 检查请求名，按名分发
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/list";
        }

        try {
            switch (path) {
                case "/list":
                    handleAuthorList(req, resp, current);			// 显示作者的稿件列表
                    break;
                case "/exportCsv":
                    handleExportCsv(req, resp, current);			// 导出稿件数据为 CSV
                    break;
                case "/submit":
                    handleSubmitForm(req, resp, current, null);		// 显示投稿表单页面
                    break;
                case "/edit":
                    handleEditDraft(req, resp, current);			// 编辑草稿状态的稿件
                    break;
                case "/resubmitEdit":
                    handleResubmitEditForm(req, resp, current);		// 显示"修改后重新提交"表单
                    break;
                case "/detail":
                    handleDetail(req, resp, current);				// 查看稿件详情
                    break;
                case "/track":
                    handleTrackStatus(req, resp, current);			// 追踪稿件状态时间线
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);	// 其他情况，只能404了
            }
        } catch (SQLException e) {
            throw new ServletException("访问数据库出错", e);
        }
    }

    
    // 处理表单提交
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 依旧检查用户登陆状态
    	User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // 只有两种情况：稿件首次提交和再次提交
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

    
    /*
     * 准备投稿表单页面所需的所有数据
     * 靠 draft 参数判断新建投稿还是编辑草稿
     */
    private void handleSubmitForm(HttpServletRequest req, HttpServletResponse resp, User current, Manuscript draft)
            throws ServletException, IOException, SQLException {
    	// 有点冗余，期刊已确定
        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        req.setAttribute("manuscript", draft);

        // 确定期刊
        Integer journalId = null;
        if (draft != null && draft.getJournalId() != null) {	// 草稿已有期刊
            journalId = draft.getJournalId();
        } else if (journals != null && !journals.isEmpty() && journals.get(0) != null) {
            journalId = journals.get(0).getJournalId();			// 默认数据库中记录的第一个期刊
        }

        // 编辑草稿模式特有信息
        if (draft != null) {
        	// 作者列表
            req.setAttribute("authors", authorDAO.findByManuscriptId(draft.getManuscriptId()));
            // 推荐审稿人
            req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(draft.getManuscriptId()));
            // 项目资助（多条）
            req.setAttribute("fundings", fundingDAO.findByManuscriptId(draft.getManuscriptId()));
            // 当前版本
            ManuscriptVersion cv = versionDAO.findCurrentByManuscriptId(draft.getManuscriptId());
            req.setAttribute("currentVersion", cv);

            // Cover Letter 多附件（仅作者/编辑可见；审稿人没有详情页权限）
            try {
                if (cv != null) {
                    req.setAttribute("coverAttachments",
                            fileDAO.findByManuscriptVersionAndType(draft.getManuscriptId(), cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
                }
            } catch (Exception ignore) {
                // 不影响主流程
            }
        }

        // 新建投稿：默认空资助列表（避免 JSP 空指针）
        if (draft == null) {
            req.setAttribute("fundings", java.util.Collections.emptyList());
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_submit.jsp").forward(req, resp);
    }

    /*
     * 稿件被退回或需要修改时，进入修改页面
     */
    private void handleResubmitEditForm(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {
    	// 权限检查
        if (!"AUTHOR".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有作者可以修改稿件");
            return;
        }

        // 获取稿件id
        Integer manuscriptId = parseInt(req.getParameter("id"));
        if (manuscriptId == null) {
            manuscriptId = parseInt(req.getParameter("manuscriptId"));
        }
        if (manuscriptId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少稿件ID");
            return;
        }

        // 从数据库中查找稿件，状态检查
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

        // 准备期刊和专刊数据（作者可能想要修改）
        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        if (m.getJournalId() != null) {
            req.setAttribute("journal", journalDAO.findById(m.getJournalId()));
        } else {
        }

        // 其他稿件信息
        req.setAttribute("manuscript", m);
        req.setAttribute("authors", authorDAO.findByManuscriptId(manuscriptId));
        req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(manuscriptId));
        // 项目资助（多条）
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

        // 退回/修回：向作者展示最近一次退回的修改意见（形式审查反馈）
        try {
            req.setAttribute("formalCheckResult", formalCheckResultDAO.findByManuscriptId(manuscriptId));
        } catch (Exception ignore) {
            // 不影响主流程
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_resubmit.jsp").forward(req, resp);
    }

    /*
     * 投稿表单回显（用于表单验证失败时保持现有数据）
     */
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
        // 项目资助（多条）：优先使用 request 中已有的回显值（表单校验失败时）
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

        // 版本信息
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

    /*
     * 重新提交投稿表单回显（用于表单验证失败时保持现有数据）
     */
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
        // 项目资助（多条）：优先使用 request 中已有的回显值（表单校验失败时）
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

        // 版本信息
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

        // 回显时也带上最近一次形式审查反馈（用于作者查看退回修改意见）
        if (manuscript != null && manuscript.getManuscriptId() != null) {
            try {
                req.setAttribute("formalCheckResult", formalCheckResultDAO.findByManuscriptId(manuscript.getManuscriptId()));
            } catch (Exception ignore) {
            }
        }
        
        // 修改时期刊信息不可更改
        if (manuscript != null && manuscript.getJournalId() != null) {
            req.setAttribute("journal", journalDAO.findById(manuscript.getJournalId()));
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_resubmit.jsp").forward(req, resp);
    }

    
    /*
     * 推荐审稿人是否完整
     * 返回错误提示，无错误返回 null
     */
    private String findFirstIncompleteRecommendedReviewerRow(HttpServletRequest req) {
        String[] names = req.getParameterValues("recReviewerName");
        String[] emails = req.getParameterValues("recReviewerEmail");
        String[] reasons = req.getParameterValues("recReviewerReason");

        // 没推荐审稿人
        if (names == null && emails == null && reasons == null) {
            return null;
        }

        // 推荐审稿人信息不完整
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

    /*
     * 编辑草稿页面
     */
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

        // 继续编辑草稿：对应 incomplete 分组
        resolveAndRememberGroup(req, m);
        handleSubmitForm(req, resp, current, m);
    }

    /**
     * 稿件详情页：作者可查看自己的稿件；编辑/主编/编辑部管理员可查看并进行对应操作。
     */
    private void handleDetail(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        // 结构图要求：审稿人不能直接查看稿件详情页（避免看到作者信息/决策历史等），
        // 必须通过 /reviewer/invitation 查看摘要并接受邀请后，再通过 /files/preview 下载稿件。
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

        // 作者只能查看自己的稿件
        if ("AUTHOR".equals(current.getRoleCode()) && !Objects.equals(current.getUserId(), m.getSubmitterId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看他人稿件详情。");
            return;
        }


        resolveAndRememberGroup(req, m);

        req.setAttribute("manuscript", m);

        // 期刊信息：详情页展示“期刊名称”，不直接展示 journalId
        try {
            if (m.getJournalId() != null) {
                req.setAttribute("journal", journalDAO.findById(m.getJournalId()));
            }
        } catch (Exception ignore) {
        }

        // 投稿人信息（提交人）
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

        // Cover Letter 多附件
        try {
            if (cv != null) {
                req.setAttribute("coverAttachments",
                        fileDAO.findByManuscriptVersionAndType(manuscriptId, cv.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT));
            }
        } catch (Exception ignore) {
        }

        FormalCheckResult formalCheckResult = formalCheckResultDAO.findByManuscriptId(manuscriptId);
        req.setAttribute("formalCheckResult", formalCheckResult);

        // ===== 形式审查页右侧“当前字数”展示 =====
        // 需求变更：编辑部管理员（EO_ADMIN）的形式审查正文字数仅统计稿件 PDF，不再统计 Cover Letter。
        int bodyCount = 0;
        int pdfPageCount = 0;
        int abstractCount = 0;
        try {
            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);

            String bodyText = "";

            boolean eoAdminOnlyPdf = "EO_ADMIN".equals(current.getRoleCode());

            if (!eoAdminOnlyPdf) {
                // 1) Cover Letter（历史逻辑：非 EO_ADMIN 仍可优先取 Cover Letter）
                if (currentVer != null) {
                    String coverPath = currentVer.getCoverLetterPath();
                    if (coverPath != null && !coverPath.trim().isEmpty()) {
                        bodyText = FileTextUtil.extractText(new File(coverPath));
                    }
                }
                bodyCount = formalCheckService.computeBodyCount(bodyText);
            }

            // 2) manuscript PDF（与 /files/preview?type=manuscript 一致：优先原稿，否则匿名稿）
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

        // 1）加载审稿记录，供“当前审稿记录”表格使用
        List<Review> reviewList = reviewDAO.findByManuscript(manuscriptId);
        req.setAttribute("reviews", reviewList);

        // 1.1）详情页已合并“审稿人选择”功能：用于禁用已分配的审稿人，避免重复邀请
        // 与 EditorServlet 的 /review/select 保持一致：INVITED / ACCEPTED 视为已分配（SUBMITTED 可再次邀请由业务决定）
        java.util.Set<Integer> assignedReviewerIds = new java.util.HashSet<>();
        for (Review r : reviewList) {
            if (r == null) continue;
            String st = r.getStatus();
            if ("INVITED".equals(st) || "ACCEPTED".equals(st)) {
                assignedReviewerIds.add(r.getReviewerId());
            }
        }
        req.setAttribute("assignedReviewerIds", assignedReviewerIds);

        // 2）如果当前用户是编辑 / 主编 / 编辑部管理员，就加载审稿人库（支持搜索与推荐）
        String role = current.getRoleCode();
        if ("EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role)) {

            // 2.1 审稿人搜索条件（来自稿件详情页顶部的搜索表单）
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
                // 非法数字直接忽略，视为未设置
            }
            try {
                if (minAvgScoreStr != null && !minAvgScoreStr.trim().isEmpty()) {
                    minAvgScore = Integer.parseInt(minAvgScoreStr.trim());
                }
            } catch (NumberFormatException ignore) {
                // 非法数字直接忽略，视为未设置
            }

            boolean hasSearch = (reviewerKeyword != null && !reviewerKeyword.trim().isEmpty())
                    || minCompleted != null
                    || minAvgScore != null;

            List<User> reviewerUsers;
            if (hasSearch) {
                // 根据搜索条件过滤审稿人库，最多返回 100 条，避免一次性加载过多
                reviewerUsers = userDAO.searchReviewerPool(reviewerKeyword, minCompleted, minAvgScore, 100);
            } else {
                // 未填写任何搜索条件时，保持原有行为：加载全部 REVIEWER 列表
                reviewerUsers = userDAO.findByRoleCode("REVIEWER");
            }
            req.setAttribute("reviewers", reviewerUsers);

            // 2.2 简单推荐算法：根据稿件的研究主题 / 关键词，在 ResearchArea 中做一次关键词匹配
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

        // 3）如果当前用户是编辑（或主编），加载最新一条主编给该编辑的指派建议
        if ("EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role)) {
            ManuscriptAssignment chiefAssignment =
                    assignmentDAO.findLatestByManuscriptAndEditor(manuscriptId, current.getUserId());
            req.setAttribute("chiefAssignment", chiefAssignment);
        }

        // 4）与作者沟通历史（时间线）：复用 Notifications 表
        NotificationDAO notificationDAO = new NotificationDAO();
        List<Notification> authorMessages;
        if ("AUTHOR".equals(role)) {
            authorMessages = notificationDAO.listByManuscriptAndCategory(manuscriptId, "AUTHOR_MESSAGE", current.getUserId(), 200, true);
        } else {
            // 编辑/主编/编辑部管理员：查看全部沟通记录（包括抄送主编）
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

        // ✅ 案头退稿理由：作者侧可见（来自状态历史 Remark）
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
    

    /**
     * 追踪稿件状态：显示时间线视图和状态变更历史
     * GET /manuscripts/track?id=xxx
     */
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

        // 作者只能查看自己的稿件状态
        if ("AUTHOR".equals(current.getRoleCode()) && !Objects.equals(current.getUserId(), m.getSubmitterId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看他人稿件状态。");
            return;
        }


        resolveAndRememberGroup(req, m);

        // 获取状态变更历史
        List<ManuscriptStatusHistory> historyList = statusHistoryDAO.findByManuscriptId(manuscriptId);

        // 获取阶段时间戳数据
        ManuscriptStageTimestamps stageTimestamps = stageTimestampsDAO.findByManuscriptId(manuscriptId);

        // 兼容修复：早期版本中“自动推进 UNDER_REVIEW -> EDITOR_RECOMMENDATION”只更新了 Manuscripts.Status，
        // 未写入 ManuscriptStageTimestamps.UnderReviewCompletedAt，导致作者时间线“外审阶段”不显示完成时间。
        // 这里按“最后一条已提交审稿意见的 SubmittedAt”做展示兜底（仅用于显示，不回写数据库）。
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
                // 兜底失败不应影响页面渲染
            }
        }
        
        // 从ManuscriptStageTimestamps生成完整的历史记录
        List<ManuscriptStatusHistory> completeHistoryList = buildCompleteHistoryList(
                manuscriptId, historyList, stageTimestamps, m);
        
        // 按时间排序
        completeHistoryList.sort((h1, h2) -> {
            if (h1.getChangeTime() == null && h2.getChangeTime() == null) return 0;
            if (h1.getChangeTime() == null) return 1;
            if (h2.getChangeTime() == null) return -1;
            return h1.getChangeTime().compareTo(h2.getChangeTime());
        });

        // 获取预计审稿周期
        String estimatedCycle = statusHistoryDAO.getEstimatedReviewCycle(m.getCurrentStatus());

        // 退回状态：补充最近一次形式审查反馈（用于作者查看修改意见）
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
    
    /**
     * 从ManuscriptStageTimestamps生成完整的历史记录列表
     * 结合数据库中的历史记录和时间戳数据，生成完整的状态变更历史
     */
    private List<ManuscriptStatusHistory> buildCompleteHistoryList(
            int manuscriptId,
            List<ManuscriptStatusHistory> dbHistoryList,
            ManuscriptStageTimestamps stageTimestamps,
            Manuscript manuscript) {
        
        List<ManuscriptStatusHistory> completeList = new ArrayList<>();
        
        // 定义状态流程顺序
        String[] statusFlow = {
            "DRAFT", "SUBMITTED", "FORMAL_CHECK", "DESK_REVIEW_INITIAL",
            "TO_ASSIGN", "WITH_EDITOR", "UNDER_REVIEW", 
            "EDITOR_RECOMMENDATION", "FINAL_DECISION_PENDING"
        };
        
        // 状态到事件类型的映射
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
        
        // 从时间戳生成历史记录
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
                    
                    // 确定fromStatus（上一个状态）
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
        
        // 合并数据库中的历史记录（如果时间戳中没有对应记录）
        // 使用Map来去重，以时间和状态为key
        Map<String, ManuscriptStatusHistory> historyMap = new HashMap<>();
        
        // 先添加时间戳生成的记录
        for (ManuscriptStatusHistory h : completeList) {
            String key = h.getChangeTime() + "_" + h.getToStatus();
            historyMap.put(key, h);
        }
        
        // 再添加数据库中的记录（如果不存在相同时间和状态的记录）
        for (ManuscriptStatusHistory h : dbHistoryList) {
            if (h.getChangeTime() != null) {
                String key = h.getChangeTime() + "_" + h.getToStatus();
                if (!historyMap.containsKey(key)) {
                    historyMap.put(key, h);
                } else {
                    // 如果存在，优先使用数据库中的记录（因为它有操作者信息）
                    historyMap.put(key, h);
                }
            }
        }
        
        // 如果没有历史记录，但稿件有提交时间，创建一个初始记录
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

    /**
     * 保存草稿 / 最终提交（投稿主流程）
     * POST /manuscripts/submit
     */
    private void handleSaveDraftOrSubmit(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException {

        if (!"AUTHOR".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有作者可以投稿。");
            return;
        }

        String action = trim(req.getParameter("action")); // saveDraft | submit
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

            // 项目资助（多条）：名称 / 级别 / 资助金额
            List<String> fundingErrors = new ArrayList<>();
            List<ManuscriptFunding> fundings = buildFundingsFromRequest(req, fundingErrors);
            // 回显用
            req.setAttribute("fundings", fundings);
            if (!fundingErrors.isEmpty()) {
                req.setAttribute("error", String.join("；", fundingErrors));
                forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                return;
            }
            // 兼容旧字段（Manuscripts.FundingInfo）：存汇总字符串
            String fundingSummary = buildFundingInfoSummary(fundings);
            if (fundingSummary != null && !fundingSummary.isEmpty()) {
                m.setFundingInfo(fundingSummary);
            } else {
                // 若完全未填写新表单，则保留旧参数 fundingInfo（兼容老页面）
                if (m.getFundingInfo() == null || m.getFundingInfo().trim().isEmpty()) {
                    m.setFundingInfo(null);
                }
            }


            // 推荐审稿人：任意一行如果被填写了（姓名/邮箱/理由任一不为空），则必须同时提供“姓名 + 邮箱”。
            // 数据库表 ManuscriptRecommendedReviewers 约束 FullName/Email NOT NULL。
            String recRowError = findFirstIncompleteRecommendedReviewerRow(req);
            if (isFinalSubmit && recRowError != null) {
                req.setAttribute("error", recRowError);
                forwardSubmitFormWithTempData(req, resp, m, authors, recs);
                return;
            }

            // 基本校验：最终提交至少要有标题和 1 个作者
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

            // 版本文件
            Part manuscriptFile = safeGetPart(req, "manuscriptFile");
            Part anonymousFile = safeGetPart(req, "anonymousFile");
            // Cover Letter 附件（支持多文件）
            List<Part> coverAttachmentParts = safeGetParts(req, "coverAttachments");
            String coverLetterHtml = trim(req.getParameter("coverLetterHtml"));

            // 最终提交时，必须上传稿件文件（新稿件或之前未上传过的情况）
            if (isFinalSubmit) {
                // 检查是否有之前上传的文件
                ManuscriptVersion prevVersion = null;
                if (manuscriptId != null) {
                    prevVersion = versionDAO.findCurrentByManuscriptId(manuscriptId);
                }
                boolean hasExistingManuscript = prevVersion != null && prevVersion.getFileOriginalPath() != null && !prevVersion.getFileOriginalPath().trim().isEmpty();
                boolean hasExistingAnonymous = prevVersion != null && prevVersion.getFileAnonymousPath() != null && !prevVersion.getFileAnonymousPath().trim().isEmpty();
                
                // 如果没有上传新文件，且之前也没有文件，则报错
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

            // PDF 格式验证
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

            // 作者列表冗余字段
            m.setAuthorList(joinAuthorNames(authors));

            try (Connection conn = DbUtil.getConnection()) {
                conn.setAutoCommit(false);

                if (existing == null) {
                    manuscriptDAO.insertWithStatus(conn, m, isFinalSubmit ? "SUBMITTED" : "DRAFT", isFinalSubmit);
                    manuscriptId = m.getManuscriptId();
                } else {
                    manuscriptDAO.updateMetadataAndStatus(conn, m, isFinalSubmit ? "SUBMITTED" : "DRAFT", isFinalSubmit);
                }

                // 作者 / 推荐审稿人：每次以“全量覆盖”方式保存
                authorDAO.deleteByManuscriptId(conn, manuscriptId);
                authorDAO.insertBatch(conn, manuscriptId, authors);

                recommendedReviewerDAO.deleteByManuscriptId(conn, manuscriptId);
                recommendedReviewerDAO.insertBatch(conn, manuscriptId, recs);

                // 项目资助（多条）：全量覆盖保存
                try {
                    fundingDAO.replaceByManuscriptId(conn, manuscriptId, fundings);
                } catch (SQLException e) {
                    // 若尚未执行建表脚本，避免整个投稿流程挂掉（但建议尽快执行 SQL Patch）
                    String msg = e.getMessage();
                    if (msg == null) throw e;
                    String lower = msg.toLowerCase();
                    if (!(lower.contains("invalid object name") && lower.contains("manuscriptfundings"))) {
                        throw e;
                    }
                }

                // 版本：每次保存草稿/提交都生成一个“当前版本”
                int nextVersionNumber = getNextVersionNumber(conn, manuscriptId);

                // 取上一版“当前版本”（用于沿用附件路径，避免未重新上传文件导致附件丢失）
                ManuscriptVersion prevCurrent = versionDAO.findCurrentByManuscriptId(conn, manuscriptId);

                

                // 保存文件（可为空）
                ManuscriptVersion v = new ManuscriptVersion();
                v.setManuscriptId(manuscriptId);
                v.setVersionNumber(nextVersionNumber);
                v.setCurrent(true);
                v.setCreatedBy(current.getUserId());

                File versionDir = new File(UPLOAD_MANUSCRIPT_DIR + File.separator + "MS_" + manuscriptId + File.separator + "v" + nextVersionNumber);
                if (!versionDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    versionDir.mkdirs();
                }

                String fileOriginalPath = null;
                if (manuscriptFile != null && manuscriptFile.getSize() > 0) {
                    fileOriginalPath = savePartToDir(manuscriptFile, versionDir, "manuscript_");
                }

                // 匿名手稿：独立上传
                String fileAnonymousPath = null;
                if (anonymousFile != null && anonymousFile.getSize() > 0) {
                    fileAnonymousPath = savePartToDir(anonymousFile, versionDir, "anonymous_");
                }

                // CoverLetter：保存富文本原文 + 尝试转 PDF
                String coverPath = null;
                String remark = null;
                String coverHtmlToStore = coverLetterHtml;
                if (coverLetterHtml != null && !coverLetterHtml.isEmpty() && !HtmlToPdfConverter.isEmptyHtml(coverLetterHtml)) {
                    try {
                        File coverPdfFile = new File(versionDir, "cover_letter.pdf");
                        HtmlToPdfConverter.convert(coverLetterHtml, coverPdfFile);
                        coverPath = coverPdfFile.getAbsolutePath();
                    } catch (Exception e) {
                        // 转换失败时保存原始 HTML 作为备份
                        String htmlPath = saveTextToFile(coverLetterHtml, new File(versionDir, "cover_letter.html"));
                        coverPath = htmlPath;
                        remark = "CoverLetter PDF 转换失败，已保存 HTML 原文";
                    }
                }

                // 未重新上传文件时，沿用上一版的附件路径
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
                    // CoverLetter HTML：若本次未提供（或为空），沿用上一版
                    if (coverHtmlToStore == null || coverHtmlToStore.trim().isEmpty() || HtmlToPdfConverter.isEmptyHtml(coverHtmlToStore)) {
                        coverHtmlToStore = prevCurrent.getCoverLetterHtml();
                    }
                    // ResponseLetter 暂未在投稿页面提供上传入口，若上一版存在则沿用
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

                // ========== Cover Letter 多附件保存（支持多文件） ==========
                try {
                    // 生成新版本时，默认沿用上一版附件（便于作者“只改正文/cover，不用每次重复上传附件”）
                    if (prevCurrent != null && prevCurrent.getVersionId() != null) {
                        fileDAO.copyByVersionAndType(conn, manuscriptId, prevCurrent.getVersionId(), v.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT);
                    }

                    if (coverAttachmentParts != null) {
                        File attachDir = new File(versionDir, "cover_attachments");
                        if (!attachDir.exists()) {
                            //noinspection ResultOfMethodCallIgnored
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
                    // 多附件不影响主流程（如失败仍可完成投稿）
                }

                conn.commit();
            }

            // 投稿提交成功：站内 + 邮件通知（不影响主流程）
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

    /**
     * 作者在 RETURNED / REVISION 状态下对稿件内容进行修改并重新提交（Resubmit）。
     * 与旧版相比：支持 SubjectArea/FundingInfo/AuthorList、作者列表、推荐审稿人与文件版本。
     */
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

        // mode: submit | draft（草稿保存不推进流程）
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

            // 项目资助（多条）：名称 / 级别 / 资助金额
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

            // submit 模式：若填写了推荐审稿人，则必须姓名+邮箱齐全（草稿模式下允许先不完整地填写）
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
            // Cover Letter 附件（支持多文件）
            List<Part> coverAttachmentParts = safeGetParts(req, "coverAttachments");
            String coverLetterHtml = trim(req.getParameter("coverLetterHtml"));

            // PDF 格式验证
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
                    // 不改变状态，仅保存元数据
                    manuscriptDAO.updateResubmitDraft(conn, toUpdate);
                } else {
                    manuscriptDAO.updateAndResubmit(conn, toUpdate, fromStatus);
                }

                authorDAO.deleteByManuscriptId(conn, manuscriptId);
                authorDAO.insertBatch(conn, manuscriptId, authors);

                recommendedReviewerDAO.deleteByManuscriptId(conn, manuscriptId);
                recommendedReviewerDAO.insertBatch(conn, manuscriptId, recs);

                // 项目资助（多条）：全量覆盖保存
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

                // 取上一版“当前版本”（用于沿用附件路径，避免未重新上传文件导致附件丢失）
                ManuscriptVersion prevCurrent = versionDAO.findCurrentByManuscriptId(conn, manuscriptId);

                ManuscriptVersion v = new ManuscriptVersion();
                v.setManuscriptId(manuscriptId);
                v.setVersionNumber(nextVersionNumber);
                v.setCurrent(true);
                v.setCreatedBy(current.getUserId());

                File versionDir = new File(UPLOAD_MANUSCRIPT_DIR + File.separator + "MS_" + manuscriptId + File.separator + "v" + nextVersionNumber);
                if (!versionDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    versionDir.mkdirs();
                }

                String fileOriginalPath = null;
                if (manuscriptFile != null && manuscriptFile.getSize() > 0) {
                    fileOriginalPath = savePartToDir(manuscriptFile, versionDir, "manuscript_");
                }

                // 匿名手稿：独立上传
                String fileAnonymousPath = null;
                if (anonymousFile != null && anonymousFile.getSize() > 0) {
                    fileAnonymousPath = savePartToDir(anonymousFile, versionDir, "anonymous_");
                }

                // CoverLetter：保存富文本原文 + 尝试转 PDF
                String coverPath = null;
                String remark = null;
                String coverHtmlToStore = coverLetterHtml;
                if (coverLetterHtml != null && !coverLetterHtml.isEmpty() && !HtmlToPdfConverter.isEmptyHtml(coverLetterHtml)) {
                    try {
                        File coverPdfFile = new File(versionDir, "cover_letter.pdf");
                        HtmlToPdfConverter.convert(coverLetterHtml, coverPdfFile);
                        coverPath = coverPdfFile.getAbsolutePath();
                    } catch (Exception e) {
                        // 转换失败时保存原始 HTML 作为备份
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

                // 未重新上传文件时，沿用上一版的附件路径
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
                    // CoverLetter HTML：若本次未提供（或为空），沿用上一版
                    if (v.getCoverLetterHtml() == null || v.getCoverLetterHtml().trim().isEmpty() || HtmlToPdfConverter.isEmptyHtml(v.getCoverLetterHtml())) {
                        v.setCoverLetterHtml(prevCurrent.getCoverLetterHtml());
                    }
                    // ResponseLetter 暂未在投稿页面提供上传入口，若上一版存在则沿用
                    if (v.getResponseLetterPath() == null || v.getResponseLetterPath().trim().isEmpty()) {
                        v.setResponseLetterPath(prevCurrent.getResponseLetterPath());
                    }
                    if (v.getRemark() == null || v.getRemark().trim().isEmpty()) {
                        v.setRemark(prevCurrent.getRemark());
                    }
                }

                versionDAO.markAllNotCurrent(conn, manuscriptId);
                versionDAO.insert(conn, v);

                // ========== Cover Letter 多附件保存（支持多文件） ==========
                try {
                    // 生成新版本时，默认沿用上一版附件
                    if (prevCurrent != null && prevCurrent.getVersionId() != null) {
                        fileDAO.copyByVersionAndType(conn, manuscriptId, prevCurrent.getVersionId(), v.getVersionId(), FileDAO.TYPE_COVER_ATTACHMENT);
                    }

                    if (coverAttachmentParts != null) {
                        File attachDir = new File(versionDir, "cover_attachments");
                        if (!attachDir.exists()) {
                            //noinspection ResultOfMethodCallIgnored
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
                    // 多附件不影响主流程
                }

                conn.commit();
            }

            // Resubmit 成功（submit 模式）：站内 + 邮件通知（不影响主流程）
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

    // ------------------------- 列表与导出（原有实现保留） -------------------------

    /**
     * 作者“我的稿件”列表视图：
     *  - 支持按状态分组（Incomplete / Processing / Revision / Decision）；
     *  - 支持按单一状态过滤、提交日期范围过滤；
     *  - 支持按提交时间排序以及分页显示；
     *  - 为导出 CSV 复用同一套过滤逻辑。
     */
    private void handleAuthorList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        // 1. 读取当前作者的所有稿件
        List<Manuscript> allList = manuscriptDAO.findBySubmitter(current.getUserId());

        // 2. 解析查询参数：分组、状态、日期范围、排序及分页
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
            group = "processing"; // 默认展示处理中稿件
        }
        group = group.toLowerCase();
        // 记住当前分组（用于详情/追踪/编辑等页面保持侧边栏高亮）
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

	        // 分页参数（统一工具类处理）
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

        // 3. 根据分组 / 状态 / 日期条件过滤
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

        // 4. 排序：当前主要支持按提交时间，若为空则按稿件编号
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

	        // 5. 分页（写入 manuscripts / page / pageSize / totalCount / pageCount / paginationPrefix）
	        PaginationUtil.apply(req, filtered, "manuscripts");

        // 6. 统计每个分组的数量，用于页面 Tab 显示
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

	        // manuscripts、分页元数据已由 PaginationUtil.apply 写入
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

    /**
     * 根据当前过滤条件导出 CSV 文件。
     * 路径：GET /manuscripts/exportCsv
     */
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

    /**
     * 分组过滤规则（对应作者列表 Tab）。
     */
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
    /**
     * 解析并记住当前“我的稿件”分组（incomplete/processing/revision/decision），
     * 用于详情/追踪/编辑/修回等页面保持侧边栏展开与高亮。
     *
     * 优先级：
     *  1) 参数 group（若重复出现取最后一个非空）
     *  2) request attribute "group"
     *  3) session "__msGroup"
     *  4) 若给定 manuscript，则根据状态 matchGroup(...) 推断
     *  5) 默认 processing
     */
    private String resolveAndRememberGroup(HttpServletRequest req, Manuscript manuscript) {
        HttpSession session = req.getSession();

        // 1) 参数 group（可能重复出现）
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

        // 2) request attribute
        if (group == null || group.isEmpty()) {
            Object gObj = req.getAttribute("group");
            if (gObj != null) group = String.valueOf(gObj).trim();
        }

        // 3) session 记忆
        if (group == null || group.isEmpty()) {
            Object gSess = session.getAttribute("__msGroup");
            if (gSess != null) group = String.valueOf(gSess).trim();
        }

        // 4) 根据状态推断
        if ((group == null || group.isEmpty()) && manuscript != null) {
            String st = manuscript.getCurrentStatus();
            if (matchGroup(st, "incomplete")) group = "incomplete";
            else if (matchGroup(st, "revision")) group = "revision";
            else if (matchGroup(st, "decision")) group = "decision";
            else group = "processing";
        }

        // 5) 默认值
        if (group == null || group.isEmpty()) group = "processing";
        group = group.toLowerCase();

        // 写回 request + session
        req.setAttribute("group", group);
        session.setAttribute("__msGroup", group);

        return group;
    }


    // ------------------------- 投稿表单解析/文件保存辅助 -------------------------

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

            // 名称必填（只要这一行填写了任意字段）
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
                    // 允许输入 1,000.00 这种格式
                    String normalized = amtStr.replace(",", "");
                    f.setFundingAmount(new java.math.BigDecimal(normalized));
                } catch (Exception ex) {
                    if (errors != null) {
                        errors.add("第 " + (i + 1) + " 行项目资助：资助金额格式不正确。");
                    }
                    // 解析失败则不加入列表，避免数据库报错
                    continue;
                }
            }

            list.add(f);
        }
        return list;
    }

    /**
     * 为兼容旧字段 Manuscript.FundingInfo（TEXT/NVARCHAR），将多条资助汇总成可读字符串。
     * 真实结构化数据以 ManuscriptFundings 表为准。
     */
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

        // 若未指定通讯作者但有作者，则默认第 1 位为通讯作者
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

            // 仅保存“姓名+邮箱”齐全的推荐审稿人行；不完整行交由上层（最终提交）校验提示，
            // 或在保存草稿时自动忽略，避免数据库 NOT NULL 约束导致 500。
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

    /**
     * 获取同名多文件上传的所有 Part。
     * 适用于 <input type="file" name="xxx" multiple>。
     */
    private List<Part> safeGetParts(HttpServletRequest req, String name) {
        List<Part> list = new ArrayList<>();
        try {
            Collection<Part> parts = req.getParts();
            if (parts == null) return list;
            for (Part p : parts) {
                if (p == null) continue;
                if (!name.equals(p.getName())) continue;
                // 多附件：只保留有内容的上传
                if (p.getSubmittedFileName() == null || p.getSubmittedFileName().trim().isEmpty()) continue;
                if (p.getSize() <= 0) continue;
                list.add(p);
            }
        } catch (Exception ignore) {
            // ignore
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
        // 简单输出为 UTF-8 HTML 文件
        java.nio.file.Files.write(dest.toPath(), html.getBytes(StandardCharsets.UTF_8));
        return dest.getAbsolutePath();
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 检查上传的文件是否为 PDF 格式
     * @param part 上传的文件
     * @return 如果是 PDF 文件返回 true
     */
    private boolean isPdfFile(Part part) {
        if (part == null || part.getSize() == 0) {
            return false;
        }
        
        // 检查文件扩展名
        String filename = part.getSubmittedFileName();
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            if (!lowerName.endsWith(".pdf")) {
                return false;
            }
        }
        
        // 检查 MIME 类型
        String contentType = part.getContentType();
        if (contentType != null) {
            return contentType.equalsIgnoreCase("application/pdf");
        }
        
        return true; // 如果无法确定，默认允许（依赖扩展名检查）
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

    // ------------------------- 通用辅助 -------------------------

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
