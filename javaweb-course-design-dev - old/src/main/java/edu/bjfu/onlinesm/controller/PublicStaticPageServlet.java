package edu.bjfu.onlinesm.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 前台静态/占位页面...
 */
@WebServlet(name = "PublicStaticPageServlet", urlPatterns = {"/publish", "/guide"})
public class PublicStaticPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sp = req.getServletPath();
        if ("/publish".equals(sp)) {
            req.getRequestDispatcher("/WEB-INF/jsp/public/publish.jsp").forward(req, resp);
        } else if ("/guide".equals(sp)) {
            req.getRequestDispatcher("/WEB-INF/jsp/public/guide.jsp").forward(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
