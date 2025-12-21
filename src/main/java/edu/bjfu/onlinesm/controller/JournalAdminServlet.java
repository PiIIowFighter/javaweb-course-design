package edu.bjfu.onlinesm.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 期刊管理模块控制器。
 * 目前仅负责在不同 URL 和 JSP 之间做简单跳转，不包含真实的数据库逻辑。
 */
@WebServlet(name = "JournalAdminServlet", urlPatterns = {"/admin/journals/*"})
public class JournalAdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLoggedIn(req, resp)) {
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/list".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/journal_list.jsp").forward(req, resp);
        } else if ("/edit".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/jsp/admin/journal/journal_form.jsp").forward(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!ensureLoggedIn(req, resp)) {
            return;
        }
        // 暂时不做真实的保存 / 删除逻辑，直接返回列表页面
        resp.sendRedirect(req.getContextPath() + "/admin/journals/list");
    }

    /** 简单校验是否已登录，未登录则跳转到登录页。 */
    private boolean ensureLoggedIn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getSession().getAttribute("currentUser") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }
        return true;
    }
}
