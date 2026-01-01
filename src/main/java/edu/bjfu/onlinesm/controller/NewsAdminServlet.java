package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.UploadPathUtil;
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

/**
 * 新闻 / 公告管理模块控制器。
 *
 * URL 约定：
 *   GET  /admin/news/list        列表
 *   GET  /admin/news/edit        新建或编辑表单（带 id 为编辑）
 *
 *   POST /admin/news/save        保存（新增 / 修改）
 *   POST /admin/news/delete      删除
 *
 * 允许角色：SUPER_ADMIN / SYSTEM_ADMIN / EDITOR_IN_CHIEF / EO_ADMIN
 */
@WebServlet(name = "NewsAdminServlet", urlPatterns = {"/admin/news/*"})
@MultipartConfig
public class NewsAdminServlet extends HttpServlet {

    private final NewsDAO newsDAO = new NewsDAO();

    private static final String BASE_UPLOAD_DIR = UploadPathUtil.getBaseDirPath();
    private static final String NEWS_SUB_DIR = "news";
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (!isNewsAdmin(current)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有有权限的管理员才能管理新闻/公告。");
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
                req.setAttribute("newsList", list);
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
        if (!isNewsAdmin(current)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有有权限的管理员才能管理新闻/公告。");
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
                // 保持 news = null，走“新增”逻辑
            } catch (SQLException e) {
                throw new ServletException("按 ID 查询新闻失败", e);
            }
        }

        if (news == null) {
            news = new News();
            news.setPublished(true); // 新建默认“已发布”
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
                || "on".equalsIgnoreCase(publishedParam); // checkbox 提交时可能是 on

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

        // 解析发布日期：用于“定时发布”
        LocalDateTime publishDateTime = null;
        if (publishDateStr != null && !publishDateStr.isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(publishDateStr);
                publishDateTime = d.atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        // 处理附件上传：支持 PDF 等指南文件
        String attachmentPath = (existing != null) ? existing.getAttachmentPath() : null;
        Part attachmentPart = null;
        try {
            attachmentPart = req.getPart("attachment");
        } catch (IllegalStateException ex) {
            // 上传超出限制等异常，直接忽略附件，避免影响主体保存
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

        // 处理发布时间与可见性：
        // - 已发布：若表单指定了发布日期，则使用表单值；
        //           否则沿用原有发布时间；若原来也没有，则交给 DAO 默认当前时间；
        // - 未发布（不可见）：仍然保留已有发布时间，便于以后重新发布或做记录。
        if (isPublished) {
            if (publishDateTime != null) {
                // 表单显式指定发布日期：支持“定时发布”
                news.setPublishedAt(publishDateTime);
            } else if (existing != null && existing.getPublishedAt() != null) {
                // 未修改发布日期，则沿用原值
                news.setPublishedAt(existing.getPublishedAt());
            } else {
                // 新发布且未指定时间，交给 DAO 使用当前时间
                news.setPublishedAt(null);
            }
        } else {
            if (publishDateTime != null) {
                // 管理员在不可见状态下手动调整了发布日期，也予以保留
                news.setPublishedAt(publishDateTime);
            } else if (existing != null && existing.getPublishedAt() != null) {
                // 从“可见”切换为“不可见”时，保留之前的发布时间
                news.setPublishedAt(existing.getPublishedAt());
            } else {
                // 从未发布过的草稿且没有指定时间：保持为 null
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
            // id 非法，忽略
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }

    private boolean isNewsAdmin(User user) {
        if (user == null || user.getRoleCode() == null) {
            return false;
        }
        String rc = user.getRoleCode();
        return "SUPER_ADMIN".equals(rc)
                || "SYSTEM_ADMIN".equals(rc)
                || "EDITOR_IN_CHIEF".equals(rc)
                || "EO_ADMIN".equals(rc);   // 编辑部管理员
    }
}