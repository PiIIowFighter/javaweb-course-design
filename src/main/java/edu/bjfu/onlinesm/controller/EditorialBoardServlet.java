package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.EditorialBoardDAO;
import edu.bjfu.onlinesm.dao.JournalDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.OperationLogger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 编辑委员会管理模块控制器：维护 dbo.EditorialBoard。
 *
 * URL:
 *  GET  /admin/editorial/list?journalId=1
 *  GET  /admin/editorial/add?journalId=1
 *  GET  /admin/editorial/edit?id=xx
 *  POST /admin/editorial/save
 *  POST /admin/editorial/delete
 */
@WebServlet(name = "EditorialBoardServlet", urlPatterns = {"/admin/editorial/*"})
public class EditorialBoardServlet extends HttpServlet {

    private final EditorialBoardDAO editorialBoardDAO = new EditorialBoardDAO();
    private final JournalDAO journalDAO = new JournalDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) path = "/list";

        try {
            switch (path) {
                case "/list":
                    handleList(req, resp);
                    return;
                case "/add":
                    handleForm(req, resp, null);
                    return;
                case "/edit":
                    String idStr = req.getParameter("id");
                    if (idStr == null || idStr.trim().isEmpty()) {
                        resp.sendRedirect(req.getContextPath() + "/admin/editorial/list");
                        return;
                    }
                    handleForm(req, resp, Integer.parseInt(idStr));
                    return;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("读取编委会数据失败", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) path = "/save";

        try {
            switch (path) {
                case "/save":
                    handleSave(req);
                    break;
                case "/delete":
                    handleDelete(req);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        } catch (SQLException e) {
            throw new ServletException("保存编委会数据失败", e);
        }

        String journalId = req.getParameter("journalId");
        String redirect = req.getContextPath() + "/admin/editorial/list";
        if (journalId != null && !journalId.trim().isEmpty()) {
            redirect += "?journalId=" + journalId.trim();
        }
        resp.sendRedirect(redirect);
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        List<Journal> journals = journalDAO.findAll();
        Integer selectedJournalId = parseInt(req.getParameter("journalId"));
        if (selectedJournalId == null && journals != null && !journals.isEmpty()) {
            selectedJournalId = journals.get(0).getJournalId();
        }

        req.setAttribute("journals", journals);
        req.setAttribute("selectedJournalId", selectedJournalId);
        req.setAttribute("members", editorialBoardDAO.findByJournalId(selectedJournalId));
        req.getRequestDispatcher("/WEB-INF/jsp/admin/editorial/editorial_board_list.jsp").forward(req, resp);
    }

    private void handleForm(HttpServletRequest req, HttpServletResponse resp, Integer boardMemberId)
            throws SQLException, ServletException, IOException {
        EditorialBoardMember member = null;
        if (boardMemberId != null) {
            member = editorialBoardDAO.findById(boardMemberId);
        }
        if (member == null) {
            member = new EditorialBoardMember();
            Integer journalId = parseInt(req.getParameter("journalId"));
            if (journalId != null) member.setJournalId(journalId);
        }

        req.setAttribute("member", member);
        req.setAttribute("journals", journalDAO.findAll());
        req.setAttribute("users", userDAO.findSelectableUsers());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/editorial/editorial_board_form.jsp").forward(req, resp);
    }

    private void handleSave(HttpServletRequest req) throws SQLException {
        Integer id = parseInt(req.getParameter("boardMemberId"));
        Integer userId = parseInt(req.getParameter("userId"));
        Integer journalId = parseInt(req.getParameter("journalId"));
        String position = safe(req.getParameter("position"));
        String section = safe(req.getParameter("section"));
        String bio = safe(req.getParameter("bio"));

        if (userId == null || journalId == null || position.isEmpty()) {
            return;
        }

        EditorialBoardMember m = new EditorialBoardMember();
        m.setBoardMemberId(id);
        m.setUserId(userId);
        m.setJournalId(journalId);
        m.setPosition(position);
        m.setSection(section);
        m.setBio(bio);

        if (id == null) {
            int newId = editorialBoardDAO.create(m);
            OperationLogger.log(req, "EDITORIAL", "新增编委会成员", "boardMemberId=" + newId + ", userId=" + userId + ", journalId=" + journalId);
        } else {
            editorialBoardDAO.update(m);
            OperationLogger.log(req, "EDITORIAL", "更新编委会成员", "boardMemberId=" + id + ", userId=" + userId + ", journalId=" + journalId);
        }
    }

    private void handleDelete(HttpServletRequest req) throws SQLException {
        Integer id = parseInt(req.getParameter("boardMemberId"));
        if (id == null) return;
        editorialBoardDAO.delete(id);
        OperationLogger.log(req, "EDITORIAL", "删除编委会成员", "boardMemberId=" + id);
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
