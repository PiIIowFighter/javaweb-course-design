package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.ManuscriptVersionDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.ManuscriptVersion;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

/**
 * 文件预览/下载：读取 dbo.ManuscriptVersions 中“当前版本”的文件路径并进行输出。
 *
 * URL 示例：
 *  /files/preview?manuscriptId=1&type=manuscript
 *  /files/preview?manuscriptId=1&type=cover
 *
 * 注意：本模块假设文件路径为服务器本地磁盘路径。
 */
@WebServlet(name = "ManuscriptFilePreviewServlet", urlPatterns = {"/files/preview"})
public class ManuscriptFilePreviewServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = (User) req.getSession().getAttribute("currentUser");
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        Integer manuscriptId = parseInt(req.getParameter("manuscriptId"));
        String type = trim(req.getParameter("type"));

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

            // 作者只能看自己的
            if ("AUTHOR".equals(current.getRoleCode()) && !Objects.equals(current.getUserId(), m.getSubmitterId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权查看他人稿件文件。");
                return;
            }

            ManuscriptVersion v = versionDAO.findCurrentByManuscriptId(manuscriptId);
            if (v == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件当前版本。");
                return;
            }

            String filePath = null;
            if ("manuscript".equalsIgnoreCase(type)) {
                filePath = v.getFileOriginalPath();
            } else if ("cover".equalsIgnoreCase(type)) {
                filePath = v.getCoverLetterPath();
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的 type：" + type);
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

            String contentType = guessContentType(file);
            resp.setContentType(contentType);
            resp.setCharacterEncoding("UTF-8");

            boolean inline = contentType.startsWith("application/pdf") || contentType.startsWith("text/html")
                    || contentType.startsWith("image/");
            String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + file.getName() + "\"";
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
