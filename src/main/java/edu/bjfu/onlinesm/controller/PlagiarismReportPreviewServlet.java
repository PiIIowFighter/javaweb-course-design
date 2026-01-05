package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.UploadPathUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLException;

/**
 * 查重报告预览/下载（PDF）
 *
 * URL: /files/plagiarismReport?manuscriptId=1&reportId=SIM-...
 *
 * 说明：报告文件由 PlagiarismCheckService 生成并保存在 upload 目录下：
 *   {baseDir}/plagiarism_reports/turnitin_{manuscriptId}_{reportId}.pdf
 */
@WebServlet(urlPatterns = {"/files/plagiarismReport"})
public class PlagiarismReportPreviewServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 登录校验
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        int manuscriptId = parseInt(req.getParameter("manuscriptId"), -1);
        String reportId = req.getParameter("reportId");

        if (manuscriptId <= 0 || reportId == null || reportId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少参数 manuscriptId / reportId");
            return;
        }

        // 权限：作者只能看自己的；审稿人禁止；其他角色默认允许
        try {
            Manuscript m = manuscriptDAO.findById(manuscriptId);
            if (m == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "稿件不存在");
                return;
            }

            String role = currentUser.getRoleCode();
            if ("REVIEWER".equalsIgnoreCase(role)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限查看");
                return;
            }
            if ("AUTHOR".equalsIgnoreCase(role)) {
                if (m.getSubmitterId() == null || !m.getSubmitterId().equals(currentUser.getUserId())) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限查看");
                    return;
                }
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "数据库错误：" + e.getMessage());
            return;
        }

        String safeId = reportId.replaceAll("[^A-Za-z0-9\\-]", "_");
        File baseDir = UploadPathUtil.getBaseDirFile();
        File pdf = new File(new File(baseDir, "plagiarism_reports"),
                "turnitin_" + manuscriptId + "_" + safeId + ".pdf");

        if (!pdf.exists() || !pdf.isFile()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "报告文件不存在（可能尚未生成或已被清理）");
            return;
        }

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"plagiarism_" + manuscriptId + ".pdf\"");
        resp.setContentLengthLong(pdf.length());

        try (InputStream in = new BufferedInputStream(new FileInputStream(pdf));
             OutputStream out = new BufferedOutputStream(resp.getOutputStream())) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
