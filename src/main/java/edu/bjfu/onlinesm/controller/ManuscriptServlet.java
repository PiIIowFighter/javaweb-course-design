package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.*;
import edu.bjfu.onlinesm.model.*;
import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.mail.MailNotifications;
import edu.bjfu.onlinesm.util.notify.InAppNotifications;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import edu.bjfu.onlinesm.util.UploadPathUtil;

/**
 * 投稿/稿件列表/详情控制器
 *
 * 新增（2025课设投稿模块补全）：
 *  - 保存草稿（DRAFT）与最终提交（SUBMITTED）
 *  - 投稿元数据：研究主题 SubjectArea、作者列表（多作者）、资助信息 FundingInfo
 *  - 文件上传：Manuscript、Cover Letter（支持富文本 Cover Letter）
 *  - 推荐审稿人
 *  - 文件预览（通过 ManuscriptFilePreviewServlet 提供）
 *
 * 说明：邮件通知未在本项目中引入 JavaMail，当前只在页面提示“已提交/已保存草稿”。如需真实发送邮件，可后续扩展。
 */
@WebServlet(name = "ManuscriptServlet", urlPatterns = {"/manuscripts/*"})
@MultipartConfig
public class ManuscriptServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);

    private final JournalDAO journalDAO = new JournalDAO();
    private final ManuscriptAuthorDAO authorDAO = new ManuscriptAuthorDAO();
    private final ManuscriptRecommendedReviewerDAO recommendedReviewerDAO = new ManuscriptRecommendedReviewerDAO();
    private final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();
    private final ManuscriptAssignmentDAO assignmentDAO = new ManuscriptAssignmentDAO();
    /** 与 ProfileServlet 保持一致的上传根目录 */
    private static final String UPLOAD_BASE_DIR = UploadPathUtil.getBaseDir();
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
                case "/detail":
                    handleDetail(req, resp, current);
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

    /**
     * 投稿页面（新建或编辑草稿）。
     */
    private void handleSubmitForm(HttpServletRequest req, HttpServletResponse resp, User current, Manuscript draft)
            throws ServletException, IOException, SQLException {

        List<Journal> journals = journalDAO.findAll();
        req.setAttribute("journals", journals);
        req.setAttribute("manuscript", draft);
        if (draft != null) {
            req.setAttribute("authors", authorDAO.findByManuscriptId(draft.getManuscriptId()));
            req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(draft.getManuscriptId()));
            req.setAttribute("currentVersion", versionDAO.findCurrentByManuscriptId(draft.getManuscriptId()));
        }
        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_submit.jsp").forward(req, resp);
    }

    /**
     * 投稿表单回显（基于当前请求中已解析出的临时数据），用于校验失败时不丢失用户输入。
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

        // 若是编辑草稿时校验失败，可继续展示当前版本信息
        if (manuscript != null && manuscript.getManuscriptId() != null) {
            req.setAttribute("currentVersion", versionDAO.findCurrentByManuscriptId(manuscript.getManuscriptId()));
        }

        req.getRequestDispatcher("/WEB-INF/jsp/author/manuscript_submit.jsp").forward(req, resp);
    }

    /**
     * 检查“推荐审稿人”是否存在不完整行：只要某行被填写（姓名/邮箱/理由任一不为空），则必须同时提供姓名与邮箱。
     * 返回首个错误提示；若无错误返回 null。
     */
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

            boolean allEmpty = (n == null || n.isEmpty()) && (e == null || e.isEmpty()) && (r == null || r.isEmpty());
            if (allEmpty) continue;

            if (n == null || n.isEmpty() || e == null || e.isEmpty()) {
                return "推荐审稿人第 " + (i + 1) + " 行信息不完整：已填写内容但“姓名/邮箱”未同时填写，请补全或删除该行。";
            }
        }
        return null;
    }

    /**
     * 编辑草稿：仅允许作者编辑自己的 DRAFT。
     * GET /manuscripts/edit?id=xxx
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

        req.setAttribute("manuscript", m);
        req.setAttribute("authors", authorDAO.findByManuscriptId(manuscriptId));
        req.setAttribute("recommendedReviewers", recommendedReviewerDAO.findByManuscriptId(manuscriptId));
        req.setAttribute("currentVersion", versionDAO.findCurrentByManuscriptId(manuscriptId));

        // 1）加载审稿记录，供“当前审稿记录”表格使用
        List<Review> reviewList = reviewDAO.findByManuscript(manuscriptId);
        req.setAttribute("reviews", reviewList);

        // 2）如果当前用户是编辑 / 主编 / 编辑部管理员，就加载审稿人库
        String role = current.getRoleCode();
        if ("EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role)) {
            List<User> reviewerUsers = userDAO.findByRoleCode("REVIEWER");
            req.setAttribute("reviewers", reviewerUsers);
        }
        
        // 3）如果当前用户是编辑（或主编），加载最新一条主编给该编辑的指派建议
        if ("EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role)) {
            ManuscriptAssignment chiefAssignment =
                    assignmentDAO.findLatestByManuscriptAndEditor(manuscriptId, current.getUserId());
            req.setAttribute("chiefAssignment", chiefAssignment);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/manuscript/detail.jsp").forward(req, resp);
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
            String coverLetterHtml = trim(req.getParameter("coverLetterHtml"));

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

                // 课程设计：若没有单独生成匿名稿，默认先把匿名稿路径指向同一份文件
                String fileAnonymousPath = (fileOriginalPath != null ? fileOriginalPath : null);

                // Cover Letter 富文本直接存储到数据库字段
                String coverLetterHtmlToSave = coverLetterHtml;

                // 未重新上传文件时，沿用上一版的附件路径
                if (prevCurrent != null) {
                    if (fileOriginalPath == null || fileOriginalPath.trim().isEmpty()) {
                        fileOriginalPath = prevCurrent.getFileOriginalPath();
                    }
                    if (fileAnonymousPath == null || fileAnonymousPath.trim().isEmpty()) {
                        fileAnonymousPath = prevCurrent.getFileAnonymousPath();
                    }
                    // 如果没有新的 Cover Letter 内容，沿用上一版
                    if (coverLetterHtmlToSave == null || coverLetterHtmlToSave.trim().isEmpty()) {
                        coverLetterHtmlToSave = prevCurrent.getCoverLetterHtml();
                    }
                    // ResponseLetter 暂未在投稿页面提供上传入口，若上一版存在则沿用
                    if (v.getResponseLetterPath() == null) {
                        v.setResponseLetterPath(prevCurrent.getResponseLetterPath());
                    }
                }

                v.setFileOriginalPath(fileOriginalPath);
                v.setFileAnonymousPath(fileAnonymousPath);
                v.setCoverLetterHtml(coverLetterHtmlToSave);

                versionDAO.markAllNotCurrent(conn, manuscriptId);
                versionDAO.insert(conn, v);

                conn.commit();
            }

            String msg;
            String group;
            if (isFinalSubmit) {
                String code = genManuscriptCode(manuscriptId);
                msg = "投稿已提交，稿件 ID：" + code;
                // 1.1 投稿提交成功：发送“投稿确认邮件”
                mailNotifications.onSubmissionSuccess(current, m, code);
                // 站内通知
                inAppNotifications.onSubmissionSuccess(current, m, code);
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
            toUpdate.setAuthorList(joinAuthorNames(authors));

            // Resubmit 也是“提交”性质操作：若填写了推荐审稿人，则必须姓名+邮箱齐全。
            String recRowError = findFirstIncompleteRecommendedReviewerRow(req);
            if (recRowError != null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, recRowError);
                return;
            }

            Part manuscriptFile = safeGetPart(req, "manuscriptFile");
            String coverLetterHtml = trim(req.getParameter("coverLetterHtml"));

            try (Connection conn = DbUtil.getConnection()) {
                conn.setAutoCommit(false);

                manuscriptDAO.updateAndResubmit(conn, toUpdate, fromStatus);

                authorDAO.deleteByManuscriptId(conn, manuscriptId);
                authorDAO.insertBatch(conn, manuscriptId, authors);

                recommendedReviewerDAO.deleteByManuscriptId(conn, manuscriptId);
                recommendedReviewerDAO.insertBatch(conn, manuscriptId, recs);

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

                // 课程设计：若没有单独生成匿名稿，默认先把匿名稿路径指向同一份文件
                String fileAnonymousPath = (fileOriginalPath != null ? fileOriginalPath : null);

                // Cover Letter 富文本直接存储到数据库字段
                String coverLetterHtmlToSave = coverLetterHtml;

                v.setFileOriginalPath(fileOriginalPath);
                v.setFileAnonymousPath(fileAnonymousPath);
                v.setCoverLetterHtml(coverLetterHtmlToSave);

                // 未重新上传文件时，沿用上一版的附件路径
                if (prevCurrent != null) {
                    if (v.getFileOriginalPath() == null || v.getFileOriginalPath().trim().isEmpty()) {
                        v.setFileOriginalPath(prevCurrent.getFileOriginalPath());
                    }
                    if (v.getFileAnonymousPath() == null || v.getFileAnonymousPath().trim().isEmpty()) {
                        v.setFileAnonymousPath(prevCurrent.getFileAnonymousPath());
                    }
                    // 如果没有新的 Cover Letter 内容，沿用上一版
                    if (v.getCoverLetterHtml() == null || v.getCoverLetterHtml().trim().isEmpty()) {
                        v.setCoverLetterHtml(prevCurrent.getCoverLetterHtml());
                    }
                    // ResponseLetter 暂未在投稿页面提供上传入口，若上一版存在则沿用
                    if (v.getResponseLetterPath() == null || v.getResponseLetterPath().trim().isEmpty()) {
                        v.setResponseLetterPath(prevCurrent.getResponseLetterPath());
                    }
                }

                versionDAO.markAllNotCurrent(conn, manuscriptId);
                versionDAO.insert(conn, v);

                conn.commit();
            }

            String msg = "已重新提交（Resubmit），稿件 ID：" + genManuscriptCode(manuscriptId);
            resp.sendRedirect(req.getContextPath() + "/manuscripts/list?group=processing&msg=" + URLEncoder.encode(msg, "UTF-8"));
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
        String group = trim(req.getParameter("group"));
        if (group == null || group.isEmpty()) {
            group = "processing"; // 默认展示处理中稿件
        }
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

        int page = 1;
        try {
            String pageStr = req.getParameter("page");
            if (pageStr != null && !pageStr.isEmpty()) {
                page = Integer.parseInt(pageStr);
            }
        } catch (NumberFormatException ignore) {
        }
        if (page < 1) {
            page = 1;
        }

        int pageSize = 10;
        try {
            String pageSizeStr = req.getParameter("pageSize");
            if (pageSizeStr != null && !pageSizeStr.isEmpty()) {
                pageSize = Integer.parseInt(pageSizeStr);
            }
        } catch (NumberFormatException ignore) {
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }

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

        // 5. 分页
        int total = filtered.size();
        int pageCount = (total + pageSize - 1) / pageSize;
        if (page > pageCount && pageCount > 0) {
            page = pageCount;
        }
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Manuscript> pageList;
        if (fromIndex >= 0 && fromIndex < toIndex) {
            pageList = filtered.subList(fromIndex, toIndex);
        } else {
            pageList = filtered;
        }

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

        req.setAttribute("manuscripts", pageList);
        req.setAttribute("totalCount", total);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", pageSize);
        req.setAttribute("pageCount", pageCount);
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