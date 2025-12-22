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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 前台 - 文章与专刊（Issues）。
 *
 * 说明：当前数据库中尚未建模“卷/期/专刊”以及 “Issue-Article 关联”。
 * 因此该模块先提供：
 *  - Issue 列表：Latest / Special / All（静态占位）
 *  - Issue 详情：占位展示 + 复用“最近录用稿件”作为文章列表示例
 *
 * 后续可扩展：dbo.Issues / dbo.SpecialIssues / dbo.IssueArticles ...
 */
@WebServlet(name = "PublicIssueServlet", urlPatterns = {"/issues", "/issues/*"})
public class PublicIssueServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();

    public static class IssueItem {
        private final int issueId;
        private final String title;
        private final String type;   // latest | special | all
        private final String coverNote;

        public IssueItem(int issueId, String title, String type, String coverNote) {
            this.issueId = issueId;
            this.title = title;
            this.type = type;
            this.coverNote = coverNote;
        }

        public int getIssueId() {
            return issueId;
        }

        public String getTitle() {
            return title;
        }

        public String getType() {
            return type;
        }

        public String getCoverNote() {
            return coverNote;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String view = trim(req.getParameter("view"));
        if (view == null) view = "list";

        if ("detail".equalsIgnoreCase(view)) {
            showDetail(req, resp);
        } else {
            showList(req, resp);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = trim(req.getParameter("type"));
        if (type == null) type = "latest";
        type = type.toLowerCase();
        if (!"latest".equals(type) && !"special".equals(type) && !"all".equals(type)) {
            type = "latest";
        }

        List<IssueItem> issues = buildPlaceholderIssues();
        if (!"all".equals(type)) {
            List<IssueItem> filtered = new ArrayList<>();
            for (IssueItem i : issues) {
                if (type.equals(i.getType())) filtered.add(i);
            }
            issues = filtered;
        }

        req.setAttribute("type", type);
        req.setAttribute("issues", issues);
        req.setAttribute("isPlaceholder", true);
        req.setAttribute("pageTitle", "文章与专刊");
        req.getRequestDispatcher("/WEB-INF/jsp/public/issues.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少参数 id");
            return;
        }

        IssueItem issue = null;
        for (IssueItem i : buildPlaceholderIssues()) {
            if (i.getIssueId() == id) {
                issue = i;
                break;
            }
        }
        if (issue == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 占位：当前没有 Issue->Articles 关联，先用最新录用稿件作为示例
        List<Manuscript> articles = Collections.emptyList();
        try {
            articles = manuscriptDAO.findLatestAccepted(12);
        } catch (SQLException e) {
            req.setAttribute("articleLoadError", e.getMessage());
        }

        req.setAttribute("issue", issue);
        req.setAttribute("articles", articles);
        req.setAttribute("isPlaceholder", true);
        req.setAttribute("pageTitle", "Issue详情");
        req.getRequestDispatcher("/WEB-INF/jsp/public/issue_detail.jsp").forward(req, resp);
    }

    private List<IssueItem> buildPlaceholderIssues() {
        List<IssueItem> list = new ArrayList<>();
        list.add(new IssueItem(1, "Latest Issue · Vol. 1 No. 1 (2025)", "latest",
                "占位：尚未建模卷期，后续可用 dbo.Issues 表驱动"));
        list.add(new IssueItem(2, "Latest Issue · Vol. 1 No. 2 (2025)", "latest",
                "占位：用于展示进入 Issue 后的文章列表"));
        list.add(new IssueItem(101, "Special Issue · AI for Forestry", "special",
                "占位：专刊/专题需新增 SpecialIssues 相关表"));
        list.add(new IssueItem(102, "Special Issue · RAG Systems & Evaluation", "special",
                "占位：后续可绑定征稿通知与文章集合"));
        return list;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer parseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
