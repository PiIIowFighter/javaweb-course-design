package edu.bjfu.onlinesm.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 编辑委员会管理模块控制器。
 * 目前仅负责页面跳转。
 */
@WebServlet(name = "EditorialBoardServlet", urlPatterns = {"/admin/editorial/*"})
public class EditorialBoardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLoggedIn(req, resp)) {
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/jsp/admin/editorial/editorial_board_list.jsp").forward(req, resp);
        } else if ("/edit".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/jsp/admin/editorial/editorial_board_form.jsp").forward(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLoggedIn(req, resp)) {
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/editorial/list");
    }

    private boolean ensureLoggedIn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getSession().getAttribute("currentUser") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }
        return true;
    }
}
