package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.UploadPathUtil;
import edu.bjfu.onlinesm.util.MenuPermissionGuard;
import edu.bjfu.onlinesm.util.PermissionCatalog;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Part;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import edu.bjfu.onlinesm.util.PaginationUtil;


@WebServlet(name = "NewsAdminServlet", urlPatterns = {"/admin/news/*"})
@MultipartConfig
public class NewsAdminServlet extends HttpServlet {

    private final NewsDAO newsDAO = new NewsDAO();

    private static final String BASE_UPLOAD_DIR = UploadPathUtil.getBaseDirPath();
    private static final String NEWS_SUB_DIR = "news";
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (!MenuPermissionGuard.require(req, resp, PermissionCatalog.ADMIN_NEWS)) {
            return;
        }

String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            try {
                String keyword = req.getParameter("keyword");
                String fromStr = req.getParameter("fromDate");
                String toStr = req.getParameter("toDate");

                LocalDate fromDate = null;
                LocalDate toDate = null;
                if (fromStr != null && !fromStr.isEmpty()) {
                    try {
                        fromDate = LocalDate.parse(fromStr);
                    } catch (Exception ignored) {
                    }
                }
                if (toStr != null && !toStr.isEmpty()) {
                    try {
                        toDate = LocalDate.parse(toStr);
                    } catch (Exception ignored) {
                    }
                }

                List<News> list = newsDAO.search(keyword, fromDate, toDate);
                PaginationUtil.apply(req, list, "newsList");
            } catch (SQLException e) {
                throw new ServletException("查询新闻/公告列表失败", e);
            }
            req.getRequestDispatcher("/WEB-INF/jsp/admin/news/news_list.jsp").forward(req, resp);
        } else if ("/edit".equals(path)) {
            showEditForm(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (!MenuPermissionGuard.require(req, resp, PermissionCatalog.ADMIN_NEWS)) {
            return;
        }

String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/save";
        }

        try {
            switch (path) {
                case "/save":
                    handleSave(req, current);
                    break;
                case "/delete":
                    handleDelete(req);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        } catch (SQLException e) {
            throw new ServletException("保存或删除新闻/公告失败", e);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/news/list");
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idStr = req.getParameter("id");
        News news = null;

        if (idStr != null && !idStr.isEmpty()) {
            try {
                int id = Integer.parseInt(idStr);
                news = newsDAO.findById(id);
            } catch (NumberFormatException ignore) {
                
            } catch (SQLException e) {
                throw new ServletException("按 ID 查询新闻失败", e);
            }
        }

        if (news == null) {
            news = new News();
            news.setPublished(true); 
        }

        req.setAttribute("news", news);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/news/news_form.jsp").forward(req, resp);
    }

    private void handleSave(HttpServletRequest req, User current) throws SQLException, IOException, ServletException {
        String idStr = req.getParameter("id");
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String publishedParam = req.getParameter("published");
        String publishDateStr = req.getParameter("publishDate");

        boolean isPublished = "true".equalsIgnoreCase(publishedParam)
                || "on".equalsIgnoreCase(publishedParam); 

        Integer id = null;
        if (idStr != null && !idStr.isEmpty()) {
            try {
                id = Integer.parseInt(idStr);
            } catch (NumberFormatException ignored) {
                id = null;
            }
        }

        News existing = null;
        if (id != null) {
            existing = newsDAO.findById(id);
        }

        
        LocalDateTime publishDateTime = null;
        if (publishDateStr != null && !publishDateStr.isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(publishDateStr);
                publishDateTime = d.atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        
        String attachmentPath = (existing != null) ? existing.getAttachmentPath() : null;
        Part attachmentPart = null;
        try {
            attachmentPart = req.getPart("attachment");
        } catch (IllegalStateException ex) {
            
        }

        if (attachmentPart != null && attachmentPart.getSize() > 0) {
            File baseDir = new File(BASE_UPLOAD_DIR, NEWS_SUB_DIR);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new ServletException("无法创建新闻附件上传目录：" + baseDir.getAbsolutePath());
            }

            String submittedName = attachmentPart.getSubmittedFileName();
            String ext = "";
            if (submittedName != null) {
                int dot = submittedName.lastIndexOf('.');
                if (dot >= 0) {
                    ext = submittedName.substring(dot);
                }
            }

            String storedName = "news_" + System.currentTimeMillis() + ext;
            File target = new File(baseDir, storedName);
            attachmentPart.write(target.getAbsolutePath());
            attachmentPath = storedName;
        }

        News news = new News();
        if (existing != null && existing.getNewsId() != null) {
            news.setNewsId(existing.getNewsId());
        }
        news.setTitle(title);
        news.setContent(content);
        news.setPublished(isPublished);
        news.setAuthorId(current.getUserId());
        news.setAttachmentPath(attachmentPath);

        
        
        
        
        if (isPublished) {
            if (publishDateTime != null) {
                
                news.setPublishedAt(publishDateTime);
            } else if (existing != null && existing.getPublishedAt() != null) {
                
                news.setPublishedAt(existing.getPublishedAt());
            } else {
                
                news.setPublishedAt(null);
            }
        } else {
            if (publishDateTime != null) {
                
                news.setPublishedAt(publishDateTime);
            } else if (existing != null && existing.getPublishedAt() != null) {
                
                news.setPublishedAt(existing.getPublishedAt());
            } else {
                
                news.setPublishedAt(null);
            }
        }

        if (news.getNewsId() == null) {
            newsDAO.insert(news);
        } else {
            newsDAO.update(news);
        }
    }


    private void handleDelete(HttpServletRequest req) throws SQLException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            return;
        }
        try {
            int id = Integer.parseInt(idStr);
            newsDAO.delete(id);
        } catch (NumberFormatException ignore) {
            
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
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

