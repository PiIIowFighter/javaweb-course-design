package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.model.Manuscript;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 前台论文列表（Articles & Issues）...
 */
@WebServlet(name = "PublicArticleServlet", urlPatterns = {"/articles"})
public class PublicArticleServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String view = req.getParameter("view");
        if (view == null || view.trim().isEmpty()) {
            view = "list";
        }

        try {
            if ("detail".equalsIgnoreCase(view)) {
                showDetail(req, resp);
            } else {
                showList(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException("读取论文列表失败", e);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        String type = req.getParameter("type");
        if (type == null || type.trim().isEmpty()) {
            type = "latest";
        }
        type = type.trim().toLowerCase();

        // 当前数据库中没有 citations/downloads/popularity 的统计字段；先只实现 latest
        if ("latest".equals(type)) {
            List<Manuscript> list = manuscriptDAO.findLatestAccepted(50);
            req.setAttribute("articles", list);
        } else {
            req.setAttribute("articles", java.util.Collections.emptyList());
            req.setAttribute("notImplemented", true);
        }

        req.setAttribute("type", type);
        req.getRequestDispatcher("/WEB-INF/jsp/public/articles.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少论文 id 参数。");
            return;
        }

        Manuscript m = manuscriptDAO.findAcceptedById(id);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该论文（或该稿件未处于 ACCEPTED 状态）。");
            return;
        }

        req.setAttribute("article", m);
        req.getRequestDispatcher("/WEB-INF/jsp/public/article_detail.jsp").forward(req, resp);
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
