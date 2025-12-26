package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.IssueDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.model.Issue;
import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.model.Manuscript;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * 前台 Issues 页面：Latest Issues / Special Issues / All Issues。
 */
@WebServlet(name = "PublicIssueServlet", urlPatterns = {"/issues"})
public class PublicIssueServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final IssueDAO issueDAO = new IssueDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String view = req.getParameter("view");
        if (view == null || view.trim().isEmpty()) view = "list";

        try {
            if ("detail".equalsIgnoreCase(view)) {
                showDetail(req, resp);
            } else {
                showList(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException("加载 Issues 失败", e);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Journal j = journalDAO.findPrimary();
        Integer journalId = j != null ? j.getJournalId() : null;
        req.setAttribute("journal", j);

        String type = req.getParameter("type");
        if (type == null || type.trim().isEmpty()) type = "latest";
        type = type.trim().toLowerCase();

        List<Issue> issues = issueDAO.listPublished(journalId, type, 50);
        req.setAttribute("issues", issues);
        req.setAttribute("type", type);

        req.getRequestDispatcher("/WEB-INF/jsp/public/issues.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 issue id 参数");
            return;
        }

        Issue issue = issueDAO.findById(id);
        if (issue == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该 Issue");
            return;
        }

        List<Manuscript> articles;
        try {
            articles = issueDAO.listIssueArticles(id);
        } catch (SQLException ex) {
            // 关联表不存在时不报 500，页面给提示即可
            articles = Collections.emptyList();
            req.setAttribute("articleLoadError", ex.getMessage());
        }

        req.setAttribute("issue", issue);
        req.setAttribute("articles", articles);
        req.getRequestDispatcher("/WEB-INF/jsp/public/issue_detail.jsp").forward(req, resp);
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
