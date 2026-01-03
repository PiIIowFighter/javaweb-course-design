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
 * 前台：编委会完整名单页面（/editorial-board）。
 * 首页仍可展示部分编委，点击“查看详情”跳转到本页。
 */
@WebServlet(name = "PublicEditorialBoardPageServlet", urlPatterns = {"/editorial-board-page"})
public class PublicEditorialBoardPageServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Journal journal = journalDAO.findPrimary();
            req.setAttribute("journal", journal);

            List<EditorialBoardMember> board;
            if (journal != null && journal.getJournalId() != null) {
                board = editorialBoardDAO.findByJournal(journal.getJournalId());
            } else {
                board = Collections.emptyList();
            }
            req.setAttribute("boardMembers", board);

            req.setAttribute("pageTitle", "期刊编委会");
            req.getRequestDispatcher("/WEB-INF/jsp/public/editorial_board.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载编委会信息失败", e);
        }
    }
}
