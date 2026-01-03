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
 * 前台编委会页面：从首页拆分出来，单独展示完整编委名单与简介。
 */
@WebServlet(name = "PublicEditorialBoardServlet", urlPatterns = {"/editorial-board"})
public class PublicEditorialBoardServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Journal journal = journalDAO.findPrimary();
            req.setAttribute("journal", journal);

            if (journal != null && journal.getJournalId() != null) {
                List<EditorialBoardMember> board = editorialBoardDAO.findByJournal(journal.getJournalId());
                req.setAttribute("boardMembers", board);
            } else {
                req.setAttribute("boardMembers", Collections.emptyList());
            }

            req.getRequestDispatcher("/WEB-INF/jsp/public/editorial_board.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载编委会页面失败", e);
        }
    }
}
