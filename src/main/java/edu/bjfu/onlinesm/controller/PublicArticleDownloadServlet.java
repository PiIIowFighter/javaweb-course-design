package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.ManuscriptVersionDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.ManuscriptVersion;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


@WebServlet(name = "PublicArticleDownloadServlet", urlPatterns = {"/articles/download"})
public class PublicArticleDownloadServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer id = parseInt(firstNonEmpty(req.getParameter("id"), req.getParameter("manuscriptId")));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少论文 id 参数。\n");
            return;
        }

        try {
            
            Manuscript m = manuscriptDAO.findAcceptedById(id);
            if (m == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该论文（或该稿件未处于 ACCEPTED 状态）。");
                return;
            }

            ManuscriptVersion v = versionDAO.findCurrentByManuscriptId(id);
            if (v == null || isBlank(v.getFileOriginalPath())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该论文尚未上传全文附件。");
                return;
            }

            File file = new File(v.getFileOriginalPath());
            if (!file.exists() || !file.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在：" + v.getFileOriginalPath());
                return;
            }

            
            try {
                manuscriptDAO.incrementDownloadCount(id);
            } catch (Exception ignore) {
            }

            String contentType = guessContentType(file);
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType(contentType);
            resp.setHeader("X-Content-Type-Options", "nosniff");

            boolean inline = contentType.startsWith("application/pdf")
                    || contentType.startsWith("text/html")
                    || contentType.startsWith("image/");

            String fileName = file.getName();
            String safeName = fileName.replace("\"", "");
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");

            
            String disposition = (inline ? "inline" : "attachment")
                    + "; filename=\"" + safeName + "\""
                    + "; filename*=UTF-8''" + encoded;
            resp.setHeader("Content-Disposition", disposition);
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
            throw new ServletException("下载论文失败", e);
        }
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        if (b != null && !b.trim().isEmpty()) return b;
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
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
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (name.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
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

