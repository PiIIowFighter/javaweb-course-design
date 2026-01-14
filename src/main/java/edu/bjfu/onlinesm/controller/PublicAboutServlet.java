package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.EditorialBoardDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.dao.JournalPageDAO;
import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.model.JournalPage;
import edu.bjfu.onlinesm.model.News;

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
 * 前台 - 关于期刊
 *
 * URL：
 *   GET /about             -> aims（默认）
 *   GET /about/aims
 *   GET /about/board
 *   GET /about/insights
 *   GET /about/news
 *   GET /about/policies
 */
@WebServlet(name = "PublicAboutServlet", urlPatterns = {"/about/*"})
public class PublicAboutServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO boardDAO = new EditorialBoardDAO();
    private final NewsDAO newsDAO = new NewsDAO();
    private final JournalPageDAO journalPageDAO = new JournalPageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String tab = normalizeTab(req.getPathInfo());
        req.setAttribute("activeAboutTab", tab);

        // 期刊信息（主期刊）
        Journal journal = null;
        try {
            journal = journalDAO.findPrimary();
        } catch (SQLException e) {
            // ignore, fall back to null
        }
        req.setAttribute("journal", journal);

        Integer journalId = (journal == null) ? null : journal.getJournalId();

        // 单页 Tab：一次性加载所有板块数据，前端仅切换展示（不跳转页面）
        // aims
        try {
            JournalPage aimsPage = (journalId != null)
                    ? journalPageDAO.findByJournalAndKey(journalId, "aims")
                    : journalPageDAO.findFirstJournalByKey("aims");
            req.setAttribute("aimsPage", aimsPage);
            if (aimsPage == null) {
                req.setAttribute("aimsLoadError", "暂无内容。");
            }
        } catch (SQLException e) {
            req.setAttribute("aimsLoadError", e.getMessage());
        }

        // policies
        try {
            JournalPage policiesPage = (journalId != null)
                    ? journalPageDAO.findByJournalAndKey(journalId, "policies")
                    : journalPageDAO.findFirstJournalByKey("policies");
            req.setAttribute("policiesPage", policiesPage);
            if (policiesPage == null) {
                req.setAttribute("policiesLoadError", "暂无内容。");
            }
        } catch (SQLException e) {
            req.setAttribute("policiesLoadError", e.getMessage());
        }

        // 编委会
        List<EditorialBoardMember> members = Collections.emptyList();
        if (journalId != null) {
            try {
                members = boardDAO.findByJournal(journalId);
            } catch (SQLException e) {
                members = Collections.emptyList();
                req.setAttribute("boardLoadError", e.getMessage());
            }
        }
        req.setAttribute("boardMembers", members);

        // 新闻
        try {
            List<News> newsList = newsDAO.findPublishedAll();
            req.setAttribute("newsList", newsList);
        } catch (SQLException e) {
            req.setAttribute("newsLoadError", e.getMessage());
        }

        req.setAttribute("pageTitle", "关于期刊");
        req.getRequestDispatcher("/WEB-INF/jsp/public/about.jsp").forward(req, resp);
    }

    private String normalizeTab(String pathInfo) {
        if (pathInfo == null || "/".equals(pathInfo) || "".equals(pathInfo)) {
            return "aims";
        }
        String p = pathInfo;
        if (p.startsWith("/")) p = p.substring(1);
        if (p.contains("/")) p = p.substring(0, p.indexOf('/'));
        switch (p) {
            case "aims":
            case "board":
            case "insights":
            case "news":
            case "policies":
                return p;
            default:
                return "aims";
        }
    }
}
