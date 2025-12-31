package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.CallForPaperDAO;
import edu.bjfu.onlinesm.dao.IssueDAO;
import edu.bjfu.onlinesm.dao.JournalPageDAO;
import edu.bjfu.onlinesm.model.CallForPaper;
import edu.bjfu.onlinesm.model.Issue;
import edu.bjfu.onlinesm.model.JournalPage;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * 期刊资源访问（封面图 / 附件）：
 *  - /journal/asset?type=page_cover&id=1
 *  - /journal/asset?type=issue_attachment&id=2
 *
 * 文件实际存放：D:/upload/journal/{pages|issues|calls}/<storedName>
 */
@WebServlet(name = "JournalAssetServlet", urlPatterns = {"/journal/asset"})
public class JournalAssetServlet extends HttpServlet {

    private final JournalPageDAO journalPageDAO = new JournalPageDAO();
    private final IssueDAO issueDAO = new IssueDAO();
    private final CallForPaperDAO callDAO = new CallForPaperDAO();

    private static final String BASE_UPLOAD_DIR = UploadPathUtil.getBaseDirPath();
    private static final String BASE_UPLOAD_DIR = "D:/upload";
    private static final String JOURNAL_SUB_DIR = "journal";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        String idStr = req.getParameter("id");

        if (type == null || type.trim().isEmpty() || idStr == null || idStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 type 或 id 参数");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr.trim());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id 参数非法");
            return;
        }

        String subDir;
        String storedName;
        boolean forceDownload;

        try {
            switch (type) {
                case "page_cover": {
                    JournalPage p = journalPageDAO.findById(id);
                    if (p == null || p.getCoverImagePath() == null || p.getCoverImagePath().isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该页面没有封面图");
                        return;
                    }
                    subDir = "pages";
                    storedName = p.getCoverImagePath();
                    forceDownload = false;
                    break;
                }
                case "page_attachment": {
                    JournalPage p = journalPageDAO.findById(id);
                    if (p == null || p.getAttachmentPath() == null || p.getAttachmentPath().isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该页面没有附件");
                        return;
                    }
                    subDir = "pages";
                    storedName = p.getAttachmentPath();
                    forceDownload = true;
                    break;
                }
                case "issue_cover": {
                    Issue i = issueDAO.findById(id);
                    if (i == null || i.getCoverImagePath() == null || i.getCoverImagePath().isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该期次没有封面图");
                        return;
                    }
                    // 未登录用户：仅允许访问已发布期次资源
                    if (!isLoggedIn(req) && (i.getPublished() == null || !i.getPublished())) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "未发布的期次资源不允许访问");
                        return;
                    }
                    subDir = "issues";
                    storedName = i.getCoverImagePath();
                    forceDownload = false;
                    break;
                }
                case "issue_attachment": {
                    Issue i = issueDAO.findById(id);
                    if (i == null || i.getAttachmentPath() == null || i.getAttachmentPath().isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该期次没有附件");
                        return;
                    }
                    if (!isLoggedIn(req) && (i.getPublished() == null || !i.getPublished())) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "未发布的期次附件不允许访问");
                        return;
                    }
                    subDir = "issues";
                    storedName = i.getAttachmentPath();
                    forceDownload = true;
                    break;
                }
                case "call_cover": {
                    CallForPaper c = callDAO.findById(id);
                    if (c == null || c.getCoverImagePath() == null || c.getCoverImagePath().isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该征稿没有封面图");
                        return;
                    }
                    if (!isLoggedIn(req) && (c.getPublished() == null || !c.getPublished())) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "未发布的征稿资源不允许访问");
                        return;
                    }
                    subDir = "calls";
                    storedName = c.getCoverImagePath();
                    forceDownload = false;
                    break;
                }
                case "call_attachment": {
                    CallForPaper c = callDAO.findById(id);
                    if (c == null || c.getAttachmentPath() == null || c.getAttachmentPath().isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该征稿没有附件");
                        return;
                    }
                    if (!isLoggedIn(req) && (c.getPublished() == null || !c.getPublished())) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "未发布的征稿附件不允许访问");
                        return;
                    }
                    subDir = "calls";
                    storedName = c.getAttachmentPath();
                    forceDownload = true;
                    break;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "未知 type");
                    return;
            }
        } catch (SQLException e) {
            throw new ServletException("读取期刊资源信息失败", e);
        }

        File file = new File(new File(new File(BASE_UPLOAD_DIR, JOURNAL_SUB_DIR), subDir), storedName);
        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        String mime = getServletContext().getMimeType(file.getName());
        if (mime == null || mime.trim().isEmpty()) {
            mime = "application/octet-stream";
        }
        resp.setContentType(mime);
        resp.setContentLengthLong(file.length());

        String encoded = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        String cd;
        if (forceDownload) {
            cd = "attachment; filename*=UTF-8''" + encoded;
        } else {
            cd = "inline; filename*=UTF-8''" + encoded;
        }
        resp.setHeader("Content-Disposition", cd);

        try (InputStream in = new BufferedInputStream(new FileInputStream(file));
             OutputStream out = new BufferedOutputStream(resp.getOutputStream())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }

    private boolean isLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        User u = session != null ? (User) session.getAttribute("currentUser") : null;
        return u != null;
    }
}
