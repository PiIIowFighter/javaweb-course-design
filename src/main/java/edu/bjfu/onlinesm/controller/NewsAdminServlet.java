package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletException;
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
public class NewsAdminServlet extends HttpServlet {

    private final NewsDAO newsDAO = new NewsDAO();

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
                List<News> list = newsDAO.findAll();
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

    private void handleSave(HttpServletRequest req, User current) throws SQLException {
        String idStr = req.getParameter("id");
        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String publishedParam = req.getParameter("published");

        boolean isPublished = "true".equalsIgnoreCase(publishedParam)
                || "on".equalsIgnoreCase(publishedParam); // checkbox 提交时可能是 on

        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setPublished(isPublished);
        news.setAuthorId(current.getUserId());

        if (idStr == null || idStr.isEmpty()) {
            // 新增
            newsDAO.insert(news);
        } else {
            try {
                news.setNewsId(Integer.parseInt(idStr));
            } catch (NumberFormatException e) {
                // id 非法，当作新增
            }
            if (news.getNewsId() == null) {
                newsDAO.insert(news);
            } else {
                newsDAO.update(news);
            }
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
