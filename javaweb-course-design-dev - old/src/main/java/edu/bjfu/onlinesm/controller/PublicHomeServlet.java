package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.EditorialBoardDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.model.Manuscript;
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
 * 前台首页：展示期刊介绍、论文列表、新闻、征稿通知。
 */
@WebServlet(name = "PublicHomeServlet", urlPatterns = {"/home"})
public class PublicHomeServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final NewsDAO newsDAO = new NewsDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Journal journal = journalDAO.findPrimary();
            req.setAttribute("journal", journal);

            if (journal != null && journal.getJournalId() != null) {
                List<EditorialBoardMember> board = editorialBoardDAO.findByJournal(journal.getJournalId(), 6);
                req.setAttribute("boardMembers", board);
            } else {
                req.setAttribute("boardMembers", Collections.emptyList());
            }

            List<Manuscript> latestAccepted = manuscriptDAO.findLatestAccepted(8);
            req.setAttribute("latestPublished", latestAccepted);

            List<News> newsList = newsDAO.findPublishedTopN(6);
            req.setAttribute("newsList", newsList);

            // 征稿通知（Call for papers / Special issue）目前数据库中无对应表：先给空列表
            req.setAttribute("callForPapers", Collections.emptyList());

            req.getRequestDispatcher("/WEB-INF/jsp/public/home.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载首页数据失败", e);
        }
    }
}
