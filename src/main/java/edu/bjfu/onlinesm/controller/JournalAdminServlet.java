package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.*;
import edu.bjfu.onlinesm.model.*;
import edu.bjfu.onlinesm.util.OperationLogger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 超级管理员：期刊管理（按板块分组管理 Journals / JournalPages / Issues / CallForPapers）。
 *
 * URL 设计：
 *  - /admin/journals/list
 *  - /admin/journals/basic/edit|save|delete
 *  - /admin/journals/pages/list|edit|save|delete
 *  - /admin/journals/issues/list|edit|save|delete
 *  - /admin/journals/calls/list|edit|save|delete
 */
@WebServlet(name = "JournalAdminServlet", urlPatterns = {"/admin/journals/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 30 * 1024 * 1024,
        maxRequestSize = 60 * 1024 * 1024
)
public class JournalAdminServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final JournalPageDAO journalPageDAO = new JournalPageDAO();
    private final IssueDAO issueDAO = new IssueDAO();
    private final CallForPaperDAO callDAO = new CallForPaperDAO();

    // 与 NewsAdminServlet 保持一致
    private static final String BASE_UPLOAD_DIR = UploadPathUtil.getBaseDirPath();
    private static final String JOURNAL_SUB_DIR = "journal";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = normalize(req.getPathInfo(), "/list");

        try {
            switch (path) {
                case "/list":
                    handleJournalList(req, resp);
                    break;

                // 基本信息
                case "/basic/edit":
                    handleBasicEdit(req, resp);
                    break;

                // 页面
                case "/pages/list":
                    handlePagesList(req, resp);
                    break;
                case "/pages/edit":
                    handlePagesEdit(req, resp);
                    break;

                // 期次
                case "/issues/list":
                    handleIssuesList(req, resp);
                    break;
                case "/issues/edit":
                    handleIssuesEdit(req, resp);
                    break;

                // 征稿
                case "/calls/list":
                    handleCallsList(req, resp);
                    break;
                case "/calls/edit":
                    handleCallsEdit(req, resp);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("期刊管理模块处理失败：" + path, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = normalize(req.getPathInfo(), "/basic/save");

        try {
            switch (path) {
                case "/basic/save":
                    handleBasicSave(req, resp);
                    break;
                case "/basic/delete":
                    handleBasicDelete(req, resp);
                    break;

                case "/pages/save":
                    handlePagesSave(req, resp);
                    break;
                case "/pages/delete":
                    handlePagesDelete(req, resp);
                    break;

                case "/issues/save":
                    handleIssuesSave(req, resp);
                    break;
                case "/issues/delete":
                    handleIssuesDelete(req, resp);
                    break;

                case "/calls/save":
                    handleCallsSave(req, resp);
                    break;
                case "/calls/delete":
                    handleCallsDelete(req, resp);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("期刊管理模块处理失败：" + path, e);
        }
    }

    // -------------------- list/dashboard --------------------

    private void handleJournalList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        List<Journal> journals = journalDAO.listAll();
        req.setAttribute("journals", journals);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/journal_list.jsp").forward(req, resp);
    }

    // -------------------- basic (dbo.Journals) --------------------

    private void handleBasicEdit(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        Journal journal = null;
        if (journalId != null) {
            journal = journalDAO.findById(journalId);
        }
        if (journal == null) {
            journal = new Journal();
        }
        req.setAttribute("journal", journal);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/journal_form.jsp").forward(req, resp);
    }

    private void handleBasicSave(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        User current = getCurrentUser(req);

        Integer journalId = intParam(req, "journalId");
        String name = req.getParameter("name");
        String issn = req.getParameter("issn");
        String timeline = req.getParameter("timeline");
        String description = req.getParameter("description");

        Double impactFactor = null;
        String ifStr = req.getParameter("impactFactor");
        if (ifStr != null && !ifStr.trim().isEmpty()) {
            try {
                impactFactor = Double.parseDouble(ifStr.trim());
            } catch (Exception ignored) {
            }
        }

        Journal j = new Journal();
        j.setJournalId(journalId);
        j.setName(name);
        j.setIssn(issn);
        j.setTimeline(timeline);
        j.setDescription(description);
        j.setImpactFactor(impactFactor);

        if (j.getJournalId() == null) {
            int newId = journalDAO.insert(j, current != null ? current.getUserId() : null);
            OperationLogger.log(req, "JOURNAL", "新增期刊", "journalId=" + newId);
        } else {
            journalDAO.update(j);
            OperationLogger.log(req, "JOURNAL", "修改期刊", "journalId=" + j.getJournalId());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
    }

    private void handleBasicDelete(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId != null) {
            journalDAO.delete(journalId);
            OperationLogger.log(req, "JOURNAL", "删除期刊", "journalId=" + journalId);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
    }

    // -------------------- pages (dbo.JournalPages) --------------------

    private void handlePagesList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }
        Journal journal = journalDAO.findById(journalId);
        List<JournalPage> pages = journalPageDAO.listByJournal(journalId);

        req.setAttribute("journal", journal);
        req.setAttribute("pages", pages);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/page_list.jsp").forward(req, resp);
    }

    private void handlePagesEdit(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }
        Integer id = intParam(req, "id");

        Journal journal = journalDAO.findById(journalId);
        JournalPage page = null;
        if (id != null) {
            page = journalPageDAO.findById(id);
        }
        if (page == null) {
            page = new JournalPage();
            page.setJournalId(journalId);
        }

        req.setAttribute("journal", journal);
        req.setAttribute("page", page);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/page_form.jsp").forward(req, resp);
    }

    private void handlePagesSave(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException, ServletException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }

        Integer pageId = intParam(req, "pageId");
        String pageKey = req.getParameter("pageKey");
        String title = req.getParameter("title");
        String content = req.getParameter("content");

        JournalPage existing = (pageId != null) ? journalPageDAO.findById(pageId) : null;

        // 上传：封面图 + 附件
        String coverPath = (existing != null) ? existing.getCoverImagePath() : null;
        String attachmentPath = (existing != null) ? existing.getAttachmentPath() : null;

        coverPath = handleUpload(req, "coverImage", "pages", "page_cover_", coverPath, true);
        attachmentPath = handleUpload(req, "attachment", "pages", "page_att_", attachmentPath, false);

        JournalPage page = new JournalPage();
        page.setPageId(pageId);
        page.setJournalId(journalId);
        page.setPageKey(pageKey);
        page.setTitle(title);
        page.setContent(content);
        page.setCoverImagePath(coverPath);
        page.setAttachmentPath(attachmentPath);
        page.setUpdatedAt(LocalDateTime.now());

        if (page.getPageId() == null) {
            int newId = journalPageDAO.insert(page);
            OperationLogger.log(req, "JOURNAL", "新增期刊页面", "pageId=" + newId + ", journalId=" + journalId);
        } else {
            journalPageDAO.update(page);
            OperationLogger.log(req, "JOURNAL", "修改期刊页面", "pageId=" + page.getPageId() + ", journalId=" + journalId);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/journals/pages/list?journalId=" + journalId);
    }

    private void handlePagesDelete(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer journalId = intParam(req, "journalId");
        Integer id = intParam(req, "id");
        if (id != null) {
            journalPageDAO.delete(id);
            OperationLogger.log(req, "JOURNAL", "删除期刊页面", "pageId=" + id + ", journalId=" + journalId);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/journals/pages/list?journalId=" + (journalId != null ? journalId : ""));
    }

    // -------------------- issues (dbo.Issues) --------------------

    private void handleIssuesList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }
        Journal journal = journalDAO.findById(journalId);
        List<Issue> issues = issueDAO.listAllByJournal(journalId);

        req.setAttribute("journal", journal);
        req.setAttribute("issues", issues);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/issue_list.jsp").forward(req, resp);
    }

    private void handleIssuesEdit(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }
        Integer id = intParam(req, "id");

        Journal journal = journalDAO.findById(journalId);
        Issue issue = null;
        if (id != null) {
            issue = issueDAO.findById(id);
        }
        if (issue == null) {
            issue = new Issue();
            issue.setJournalId(journalId);
            issue.setIssueType("LATEST");
            issue.setPublished(false);
        }

        req.setAttribute("journal", journal);
        req.setAttribute("issue", issue);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/issue_form.jsp").forward(req, resp);
    }

    private void handleIssuesSave(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException, ServletException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }

        Integer issueId = intParam(req, "issueId");
        Issue existing = (issueId != null) ? issueDAO.findById(issueId) : null;

        String issueType = req.getParameter("issueType");
        String title = req.getParameter("title");
        Integer volume = intParam(req, "volume");
        Integer number = intParam(req, "number");
        Integer year = intParam(req, "year");
        String guestEditors = req.getParameter("guestEditors");
        String description = req.getParameter("description");

        boolean published = "true".equalsIgnoreCase(req.getParameter("published")) || "on".equalsIgnoreCase(req.getParameter("published"));
        LocalDate publishDate = null;
        String publishDateStr = req.getParameter("publishDate");
        if (publishDateStr != null && !publishDateStr.isEmpty()) {
            try {
                publishDate = LocalDate.parse(publishDateStr);
            } catch (Exception ignored) {
            }
        }

        String coverPath = (existing != null) ? existing.getCoverImagePath() : null;
        String attachmentPath = (existing != null) ? existing.getAttachmentPath() : null;

        coverPath = handleUpload(req, "coverImage", "issues", "issue_cover_", coverPath, true);
        attachmentPath = handleUpload(req, "attachment", "issues", "issue_att_", attachmentPath, false);

        Issue issue = new Issue();
        issue.setIssueId(issueId);
        issue.setJournalId(journalId);
        issue.setIssueType(issueType);
        issue.setTitle(title);
        issue.setVolume(volume);
        issue.setNumber(number);
        issue.setYear(year);
        issue.setGuestEditors(guestEditors);
        issue.setDescription(description);
        issue.setPublished(published);
        issue.setPublishDate(publishDate);
        issue.setCoverImagePath(coverPath);
        issue.setAttachmentPath(attachmentPath);

        if (issue.getIssueId() == null) {
            int newId = issueDAO.insertAdmin(issue);
            OperationLogger.log(req, "JOURNAL", "新增期次/专刊", "issueId=" + newId + ", journalId=" + journalId);
        } else {
            issueDAO.updateAdmin(issue);
            OperationLogger.log(req, "JOURNAL", "修改期次/专刊", "issueId=" + issue.getIssueId() + ", journalId=" + journalId);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/journals/issues/list?journalId=" + journalId);
    }

    private void handleIssuesDelete(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer journalId = intParam(req, "journalId");
        Integer id = intParam(req, "id");
        if (id != null) {
            issueDAO.deleteAdmin(id);
            OperationLogger.log(req, "JOURNAL", "删除期次/专刊", "issueId=" + id + ", journalId=" + journalId);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/journals/issues/list?journalId=" + (journalId != null ? journalId : ""));
    }

    // -------------------- calls (dbo.CallForPapers) --------------------

    private void handleCallsList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }
        Journal journal = journalDAO.findById(journalId);
        List<CallForPaper> calls = callDAO.listAllByJournal(journalId);

        req.setAttribute("journal", journal);
        req.setAttribute("calls", calls);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/call_list.jsp").forward(req, resp);
    }

    private void handleCallsEdit(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }
        Integer id = intParam(req, "id");

        Journal journal = journalDAO.findById(journalId);
        CallForPaper call = null;
        if (id != null) {
            call = callDAO.findById(id);
        }
        if (call == null) {
            call = new CallForPaper();
            call.setJournalId(journalId);
            call.setPublished(true);
        }

        req.setAttribute("journal", journal);
        req.setAttribute("call", call);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/call_form.jsp").forward(req, resp);
    }

    private void handleCallsSave(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException, ServletException {
        Integer journalId = intParam(req, "journalId");
        if (journalId == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
            return;
        }

        Integer callId = intParam(req, "callId");
        CallForPaper existing = (callId != null) ? callDAO.findById(callId) : null;

        String title = req.getParameter("title");
        String content = req.getParameter("content");
        boolean published = "true".equalsIgnoreCase(req.getParameter("published")) || "on".equalsIgnoreCase(req.getParameter("published"));

        LocalDate startDate = parseDate(req.getParameter("startDate"));
        LocalDate deadline = parseDate(req.getParameter("deadline"));
        LocalDate endDate = parseDate(req.getParameter("endDate"));

        String coverPath = (existing != null) ? existing.getCoverImagePath() : null;
        String attachmentPath = (existing != null) ? existing.getAttachmentPath() : null;

        coverPath = handleUpload(req, "coverImage", "calls", "call_cover_", coverPath, true);
        attachmentPath = handleUpload(req, "attachment", "calls", "call_att_", attachmentPath, false);

        CallForPaper call = new CallForPaper();
        call.setCallId(callId);
        call.setJournalId(journalId);
        call.setTitle(title);
        call.setContent(content);
        call.setPublished(published);
        call.setStartDate(startDate);
        call.setDeadline(deadline);
        call.setEndDate(endDate);
        call.setCoverImagePath(coverPath);
        call.setAttachmentPath(attachmentPath);
        // createdAt 由 DB 默认

        if (call.getCallId() == null) {
            int newId = callDAO.insertAdmin(call);
            OperationLogger.log(req, "JOURNAL", "新增征稿通知", "callId=" + newId + ", journalId=" + journalId);
        } else {
            callDAO.updateAdmin(call);
            OperationLogger.log(req, "JOURNAL", "修改征稿通知", "callId=" + call.getCallId() + ", journalId=" + journalId);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/journals/calls/list?journalId=" + journalId);
    }

    private void handleCallsDelete(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer journalId = intParam(req, "journalId");
        Integer id = intParam(req, "id");
        if (id != null) {
            callDAO.deleteAdmin(id);
            OperationLogger.log(req, "JOURNAL", "删除征稿通知", "callId=" + id + ", journalId=" + journalId);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/journals/calls/list?journalId=" + (journalId != null ? journalId : ""));
    }

    // -------------------- helpers --------------------

    private String handleUpload(HttpServletRequest req,
                                String partName,
                                String subDir,
                                String filenamePrefix,
                                String oldStoredName,
                                boolean isImage) throws IOException, ServletException {
        Part part = null;
        try {
            part = req.getPart(partName);
        } catch (IllegalStateException ex) {
            // 上传超出限制等异常：忽略该文件，避免影响主体保存
            return oldStoredName;
        }
        if (part == null || part.getSize() <= 0) {
            return oldStoredName;
        }

        File baseDir = new File(new File(BASE_UPLOAD_DIR, JOURNAL_SUB_DIR), subDir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new ServletException("无法创建上传目录：" + baseDir.getAbsolutePath());
        }

        String submittedName = part.getSubmittedFileName();
        String ext = "";
        if (submittedName != null) {
            int dot = submittedName.lastIndexOf('.');
            if (dot >= 0) ext = submittedName.substring(dot);
        }

        // 图片文件：尽量保持为常见扩展名
        if (isImage && ext.isEmpty()) {
            ext = ".png";
        }

        String storedName = filenamePrefix + System.currentTimeMillis() + ext;
        File target = new File(baseDir, storedName);
        part.write(target.getAbsolutePath());
        return storedName;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer intParam(HttpServletRequest req, String name) {
        String s = req.getParameter(name);
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalize(String path, String defaultPath) {
        if (path == null || path.isEmpty() || "/".equals(path)) return defaultPath;
        return path;
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }
}
