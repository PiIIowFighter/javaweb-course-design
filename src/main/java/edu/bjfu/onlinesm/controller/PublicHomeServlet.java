package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.EditorialBoardDAO;
import edu.bjfu.onlinesm.dao.CallForPaperDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.NewsDAO;
import edu.bjfu.onlinesm.model.CallForPaper;
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


@WebServlet(name = "PublicHomeServlet", urlPatterns = {"/home"})
public class PublicHomeServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();
    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final NewsDAO newsDAO = new NewsDAO();
    private final CallForPaperDAO callDAO = new CallForPaperDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Journal journal = journalDAO.findPrimary();
            req.setAttribute("journal", journal);

            List<EditorialBoardMember> boardMembers = Collections.emptyList();
if (journal != null && journal.getJournalId() != null) {
    
    boardMembers = editorialBoardDAO.findByJournal(journal.getJournalId(), 6);

    
    
    if (boardMembers == null || boardMembers.isEmpty()) {
        List<EditorialBoardMember> all = editorialBoardDAO.findAll();
        if (all != null && !all.isEmpty()) {
            
            int jid = journal.getJournalId();
            List<EditorialBoardMember> sameJournal = new java.util.ArrayList<>();
            for (EditorialBoardMember m : all) {
                if (m.getJournalId() == jid) sameJournal.add(m);
            }
            List<EditorialBoardMember> source = !sameJournal.isEmpty() ? sameJournal : all;
            int n = Math.min(6, source.size());
            boardMembers = source.subList(0, n);
        }
    }
}
req.setAttribute("boardMembers", boardMembers);

            List<Manuscript> latestAccepted = manuscriptDAO.findLatestAccepted(8);
            req.setAttribute("latestPublished", latestAccepted);

            List<News> newsList = newsDAO.findPublishedTopN(6);
            req.setAttribute("newsList", newsList);

            
            if (journal != null && journal.getJournalId() != null) {
                List<CallForPaper> calls = callDAO.listPublished(journal.getJournalId(), 6);
                req.setAttribute("callForPapers", calls);
            } else {
                req.setAttribute("callForPapers", Collections.emptyList());
            }

            req.getRequestDispatcher("/WEB-INF/jsp/public/home.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载首页数据失败", e);
        }
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

