package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.News;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 前台 - 新闻列表与详情。
 *
 * URL：
 *   GET /news/list
 *   GET /news/detail?id=1
 */
@WebServlet(name = "PublicNewsServlet", urlPatterns = {"/news/*"})
public class PublicNewsServlet extends HttpServlet {

    private final NewsDAO newsDAO = new NewsDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            showList(req, resp);
            return;
        }
        if ("/detail".equals(path)) {
            showDetail(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<News> list = newsDAO.findPublishedAll();
            req.setAttribute("newsList", list);
        } catch (SQLException e) {
            req.setAttribute("newsLoadError", e.getMessage());
        }
        req.setAttribute("pageTitle", "新闻");
        req.getRequestDispatcher("/WEB-INF/jsp/public/news_list.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少参数 id");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数 id 非法");
            return;
        }

        try {
            News news = newsDAO.findById(id);
            if (news == null || !news.isPublished()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("news", news);
        } catch (SQLException e) {
            throw new ServletException("读取新闻详情失败", e);
        }

        req.setAttribute("pageTitle", "新闻详情");
        req.getRequestDispatcher("/WEB-INF/jsp/public/news_detail.jsp").forward(req, resp);
    }
}
