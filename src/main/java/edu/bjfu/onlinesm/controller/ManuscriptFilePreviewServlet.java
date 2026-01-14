package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.ManuscriptVersionDAO;
import edu.bjfu.onlinesm.dao.FileDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.ManuscriptVersion;
import edu.bjfu.onlinesm.model.StoredFile;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.HtmlToPdfConverter;
import edu.bjfu.onlinesm.util.PdfTextUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.Objects;


@WebServlet(name = "ManuscriptFilePreviewServlet", urlPatterns = {"/files/preview"})
public class ManuscriptFilePreviewServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();
    private final FileDAO fileDAO = new FileDAO();


    
    private boolean reviewerHasAccess(int reviewerId, int manuscriptId) throws Exception {
        
        String sql = "SELECT TOP 1 1 FROM dbo.Reviews WHERE ManuscriptId=? AND ReviewerId=? AND Status IN ('ACCEPTED','SUBMITTED')";
        try (java.sql.Connection conn = edu.bjfu.onlinesm.util.DbUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setInt(2, reviewerId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = (User) req.getSession().getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        Integer manuscriptId = parseInt(req.getParameter("manuscriptId"));
        String type = trim(req.getParameter("type"));
        Integer fileId = parseInt(req.getParameter("fileId"));

        if (manuscriptId == null || type == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 或 type 参数。");
            return;
        }

        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
                return;
            }

            String role = current.getRoleCode();

            
            if ("AUTHOR".equals(role) && !Objects.equals(current.getUserId(), m.getSubmitterId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看他人稿件文件。");
                return;
            }

            
            if ("REVIEWER".equals(role)) {
                if (!reviewerHasAccess(current.getUserId(), manuscriptId)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看未分配给您的稿件文件。");
                    return;
                }
                if ("cover".equalsIgnoreCase(type)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "审稿人无权查看 Cover Letter。");
                    return;
                }
                if ("response".equalsIgnoreCase(type)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "审稿人无权查看 Response Letter。");
                    return;
                }
                if ("attachment".equalsIgnoreCase(type)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "审稿人无权查看投稿附件。");
                    return;
                }
                
                if ("original".equalsIgnoreCase(type)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "审稿人仅允许查看脱密稿（匿名稿）。");
                    return;
                }
            }

            ManuscriptVersion v = versionDAO.findCurrentByManuscriptId(manuscriptId);
            if (v == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件当前版本。");
                return;
            }

            String filePath = null;
            String downloadName = null;
            if ("attachment".equalsIgnoreCase(type)) {
                if (fileId == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 fileId 参数。");
                    return;
                }
                StoredFile sf = fileDAO.findById(fileId);
                if (sf == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到附件记录。");
                    return;
                }
                if (!Objects.equals(sf.getManuscriptId(), manuscriptId)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看该附件。");
                    return;
                }
                if (!FileDAO.TYPE_COVER_ATTACHMENT.equalsIgnoreCase(sf.getFileType())) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的附件类型。");
                    return;
                }
                
                if (sf.getVersionId() != null && v.getVersionId() != null && !Objects.equals(sf.getVersionId(), v.getVersionId())) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "仅允许查看当前版本附件。");
                    return;
                }
                filePath = sf.getFilePath();
                downloadName = sf.getFileName();
            } else if ("manuscript".equalsIgnoreCase(type)) {
                
                if ("REVIEWER".equals(role)) {
                    filePath = (v.getFileAnonymousPath() != null && !v.getFileAnonymousPath().trim().isEmpty())
                            ? v.getFileAnonymousPath()
                            : null;
                } else {
                    filePath = v.getFileOriginalPath();
                }
            } else if ("anonymous".equalsIgnoreCase(type)) {
                
                filePath = (v.getFileAnonymousPath() != null && !v.getFileAnonymousPath().trim().isEmpty())
                        ? v.getFileAnonymousPath()
                        : null;
            } else if ("original".equalsIgnoreCase(type)) {
                
                filePath = v.getFileOriginalPath();
            } else if ("cover".equalsIgnoreCase(type)) {
                filePath = v.getCoverLetterPath();
            } else if ("response".equalsIgnoreCase(type)) {
                filePath = v.getResponseLetterPath();
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的 type：" + type + "（支持 manuscript/anonymous/original/cover/response/attachment）");
                return;
            }

            if (filePath == null || filePath.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该文件尚未上传。");
                return;
            }

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在：" + filePath);
                return;
            }

            
            
            if ("cover".equalsIgnoreCase(type) && file.getName().toLowerCase().endsWith(".pdf")) {
                try {
                    String html = v.getCoverLetterHtml();
                    if (html != null && !html.trim().isEmpty() && !HtmlToPdfConverter.isEmptyHtml(html)) {
                        
                        long size = file.length();
                        if (size > 0 && size <= 2L * 1024 * 1024) {
                            String text = PdfTextUtil.extractText(file);
                            if (text == null || text.trim().isEmpty()) {
                                
                                HtmlToPdfConverter.convert(html, file);
                            }
                        }
                    }
                } catch (Throwable ignore) {
                    
                }
            }

            String contentType = guessContentType(file);
            resp.setContentType(contentType);
            resp.setCharacterEncoding("UTF-8");

            boolean inline = contentType.startsWith("application/pdf") || contentType.startsWith("text/html")
                    || contentType.startsWith("image/");
            String safeName = (downloadName == null || downloadName.trim().isEmpty()) ? file.getName() : downloadName;
            safeName = safeName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + safeName + "\"";
            resp.setHeader("Content-Disposition", disposition);
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setContentLengthLong(file.length());

            try (InputStream in = new BufferedInputStream(new FileInputStream(file));
                 OutputStream out = resp.getOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        } catch (Exception e) {
            throw new ServletException("读取文件失败", e);
        }
    }

    private String guessContentType(File file) {
        try {
            String ct = Files.probeContentType(file.toPath());
            if (ct != null) return ct;
        } catch (Exception ignored) {
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
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

