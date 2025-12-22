package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.DbUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FilePreviewServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1) 必须登录
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // 2) 路由：/files/preview
        String pathInfo = req.getPathInfo(); // 例如 "/preview"
        if (pathInfo == null || !"/preview".equals(pathInfo)) {
            resp.sendError(404);
            return;
        }

        // 3) 参数
        Integer manuscriptId = parseInt(req.getParameter("manuscriptId"));
        String type = req.getParameter("type"); // manuscript | cover
        if (manuscriptId == null || type == null) {
            resp.sendError(400, "缺少 manuscriptId 或 type 参数");
            return;
        }

        // 4) 取稿件，做权限校验（至少：作者本人；另外允许编辑/主编/编辑部管理员）
        Manuscript m;
        try {
            m = manuscriptDAO.findById(manuscriptId);
        } catch (SQLException e) {
            throw new ServletException("读取稿件失败", e);
        }
        if (m == null) {
            resp.sendError(404, "稿件不存在");
            return;
        }

        String role = current.getRoleCode();
        boolean isOwner = (m.getSubmitterId() == current.getUserId());
        boolean isStaff = "EDITOR".equals(role) || "EDITOR_IN_CHIEF".equals(role) || "EO_ADMIN".equals(role);

        // cover letter 一般不给审稿人看
        if ("cover".equalsIgnoreCase(type)) {
            if (!(isOwner || isStaff)) {
                resp.sendError(403, "无权限预览 Cover Letter");
                return;
            }
        } else if ("manuscript".equalsIgnoreCase(type)) {
            // manuscript：作者/工作人员允许
            // 如果你以后要给审稿人看“匿名稿”，可以在这里加 reviewer 逻辑
            if (!(isOwner || isStaff)) {
                resp.sendError(403, "无权限预览稿件文件");
                return;
            }
        } else {
            resp.sendError(400, "type 仅支持 manuscript / cover");
            return;
        }

        // 5) 从 dbo.ManuscriptVersions 取“当前版本”的文件路径
        String filePath;
        try {
            filePath = findCurrentVersionPath(manuscriptId, type);
        } catch (SQLException e) {
            throw new ServletException("读取版本文件路径失败", e);
        }

        if (filePath == null || filePath.trim().isEmpty()) {
            resp.sendError(404, "数据库中未记录该文件路径（可能你还没把上传文件写入 ManuscriptVersions）");
            return;
        }

        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) {
            resp.sendError(404, "文件不存在：" + filePath);
            return;
        }

        // 6) 输出文件流
        String contentType = guessContentType(f.getName());
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType(contentType);

        // pdf/html 走 inline，其他走附件下载（避免 docx 在浏览器里乱码）
        boolean inline = contentType.startsWith("application/pdf") || contentType.startsWith("text/html");
        String disposition = inline ? "inline" : "attachment";
        resp.setHeader("Content-Disposition", disposition + "; filename=\"" + f.getName() + "\"");
        resp.setContentLengthLong(f.length());

        try (InputStream in = new BufferedInputStream(new FileInputStream(f));
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }

    private Integer parseInt(String s) {
        try {
            return s == null ? null : Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 dbo.ManuscriptVersions 取当前版本的文件路径：
     *  - type=manuscript -> FileOriginalPath
     *  - type=cover      -> CoverLetterPath
     */
    private String findCurrentVersionPath(int manuscriptId, String type) throws SQLException {
        String col = "manuscript".equalsIgnoreCase(type) ? "FileOriginalPath" : "CoverLetterPath";
        String sql = "SELECT TOP 1 " + col +
                " FROM dbo.ManuscriptVersions " +
                " WHERE ManuscriptId=? AND IsCurrent=1 " +
                " ORDER BY VersionNumber DESC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
                return null;
            }
        }
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        try {
            String probe = Files.probeContentType(new File(filename).toPath());
            if (probe != null) return probe;
        } catch (Exception ignore) {}

        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        return "application/octet-stream";
    }
}
