package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.CallForPaperDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.model.CallForPaper;
import edu.bjfu.onlinesm.model.Journal;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 前台 Call for papers 页面：展示征稿通知列表与详情。
 */
@WebServlet(name = "PublicCallForPaperServlet", urlPatterns = {"/calls"})
public class PublicCallForPaperServlet extends HttpServlet {

    private final JournalDAO journalDAO = new JournalDAO();
    private final CallForPaperDAO callDAO = new CallForPaperDAO();

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
            throw new ServletException("加载 Call for papers 失败", e);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Journal j = journalDAO.findPrimary();
        Integer journalId = j != null ? j.getJournalId() : null;
        req.setAttribute("journal", j);

        List<CallForPaper> calls = callDAO.listPublished(journalId, 50);
        req.setAttribute("calls", calls);

        req.getRequestDispatcher("/WEB-INF/jsp/public/calls.jsp").forward(req, resp);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Integer id = parseInt(req.getParameter("id"));
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 call id 参数");
            return;
        }

        CallForPaper call = callDAO.findById(id);
        if (call == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该征稿通知");
            return;
        }

        req.setAttribute("call", call);
        req.getRequestDispatcher("/WEB-INF/jsp/public/call_detail.jsp").forward(req, resp);
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
