package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.EditorialBoardDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.model.Journal;

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
 * 关于期刊（About the journal）。
 */
@WebServlet(name = "PublicJournalServlet", urlPatterns = {"/about"})
public class PublicJournalServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Journal journal = journalDAO.findPrimary();
            req.setAttribute("journal", journal);

            if (journal != null && journal.getJournalId() != null) {
                List<EditorialBoardMember> board = editorialBoardDAO.findByJournal(journal.getJournalId(), 0);
                req.setAttribute("boardMembers", board);
            } else {
                req.setAttribute("boardMembers", Collections.emptyList());
            }

            req.getRequestDispatcher("/WEB-INF/jsp/public/journal_about.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载期刊信息失败", e);
        }
    }
}
