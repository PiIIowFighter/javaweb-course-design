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
@WebServlet(name = "PublicStaticPageServlet", urlPatterns = {"/publish", "/guide", "/guide/*", "/calls"})
public class PublicStaticPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sp = req.getServletPath();
        if ("/publish".equals(sp)) {
            req.setAttribute("pageTitle", "论文发表");
            req.getRequestDispatcher("/WEB-INF/jsp/public/publish.jsp").forward(req, resp);
            return;
        }

        if ("/calls".equals(sp)) {
            req.setAttribute("pageTitle", "征稿通知");
            req.getRequestDispatcher("/WEB-INF/jsp/public/calls.jsp").forward(req, resp);
            return;
        }

        // /guide 与 /guide/*
        if ("/guide".equals(sp)) {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || "/".equals(pathInfo) || "".equals(pathInfo)) {
                req.setAttribute("pageTitle", "用户指南");
                req.getRequestDispatcher("/WEB-INF/jsp/public/guide.jsp").forward(req, resp);
                return;
            }

            switch (pathInfo) {
                case "/writing":
                    req.setAttribute("pageTitle", "Writing");
                    req.getRequestDispatcher("/WEB-INF/jsp/public/guide_writing.jsp").forward(req, resp);
                    return;
                case "/formatting":
                    req.setAttribute("pageTitle", "Formatting");
                    req.getRequestDispatcher("/WEB-INF/jsp/public/guide_formatting.jsp").forward(req, resp);
                    return;
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
