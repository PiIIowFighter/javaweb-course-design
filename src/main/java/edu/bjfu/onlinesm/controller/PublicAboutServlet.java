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

        // aims / policies：从数据库读取页面内容（不再使用占位）
        if ("aims".equals(tab) || "policies".equals(tab)) {
            try {
                JournalPage page;
                if (journalId != null) {
                    page = journalPageDAO.findByJournalAndKey(journalId, tab);
                } else {
                    page = journalPageDAO.findFirstJournalByKey(tab);
                }

                if ("aims".equals(tab)) {
                    req.setAttribute("aimsPage", page);
                } else {
                    req.setAttribute("policiesPage", page);
                }

                if (page == null) {
                    req.setAttribute("pageLoadError", "未找到页面配置数据（JournalPages 中没有对应记录）。请先执行提供的 about_journal_seed.sql 初始化数据。");
                }
            } catch (SQLException e) {
                req.setAttribute("pageLoadError", e.getMessage());
            }
        }

        // 编委会
        if ("board".equals(tab)) {
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
        }

        // 新闻
        if ("news".equals(tab)) {
            try {
                List<News> newsList = newsDAO.findPublishedAll();
                req.setAttribute("newsList", newsList);
            } catch (SQLException e) {
                req.setAttribute("newsLoadError", e.getMessage());
            }
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
