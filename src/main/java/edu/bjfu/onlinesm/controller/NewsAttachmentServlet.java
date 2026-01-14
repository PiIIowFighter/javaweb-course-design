package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.util.UploadPathUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

@WebServlet(name = "NewsAttachmentServlet", urlPatterns = {"/news/attachment"})
public class NewsAttachmentServlet extends HttpServlet {

    private final NewsDAO newsDAO = new NewsDAO();

    
    private static final String BASE_UPLOAD_DIR = UploadPathUtil.getBaseDirPath();
    private static final String NEWS_SUB_DIR = "news";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 id 参数");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法的 id 参数");
            return;
        }

        News news;
        try {
            news = newsDAO.findById(id);
        } catch (Exception e) {
            throw new ServletException("读取新闻信息失败", e);
        }

        if (news == null || news.getAttachmentPath() == null || news.getAttachmentPath().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "该新闻没有附件");
            return;
        }

        
        if (!news.isPublished()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "未发布的新闻附件不允许访问");
            return;
        }

        File baseDir = new File(BASE_UPLOAD_DIR, NEWS_SUB_DIR);
        File file = new File(baseDir, news.getAttachmentPath());

        if (!file.exists() || !file.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "附件文件不存在");
            return;
        }

        String fileName = file.getName();
        resp.setContentType(getServletContext().getMimeType(fileName));
        if (resp.getContentType() == null) {
            resp.setContentType("application/octet-stream");
        }

        
        String encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedName + "\"");

        try (InputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
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

