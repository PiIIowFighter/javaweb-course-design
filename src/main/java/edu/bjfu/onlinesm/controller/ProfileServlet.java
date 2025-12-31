package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.sql.SQLException;
import edu.bjfu.onlinesm.util.UploadPathUtil;

/**
 * 个人信息管理：
 *  - GET  /profile  展示当前登录用户的基本信息；
 *  - POST /profile  更新基本信息并处理头像/简历附件上传。
 *
 * 头像和简历文件统一保存在 Web 应用路径下的 /upload/profile 目录中，
 * 文件命名规则：
 *  - 头像：user_{userId}_avatar.ext
 *  - 简历：user_{userId}_resume.ext
 */
@WebServlet(
        name = "ProfileServlet",
        urlPatterns = {"/profile", "/profile/avatar", "/profile/resume"}
)


@MultipartConfig
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    /**
     * 上传目录统一由 UploadPathUtil 管理：
     *  - 头像: {base}/avatars
     *  - 简历: {base}/resumes
     *
     * 同时为历史版本兼容：以前把头像/简历放到 {base}/profile 下，读取时会同时尝试该目录。
     */
    private static final String LEGACY_PROFILE_SUB_DIR = "profile";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        // 1) 处理头像预览
        if ("/profile/avatar".equals(servletPath)) {
            streamAvatar(req, resp);
            return;
        }

        // 2) 处理简历预览
        if ("/profile/resume".equals(servletPath)) {
            streamResume(req, resp);
            return;
        }

        // 3) 正常的个人信息页面 /profile
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }
        try {
            User fresh = userDAO.findById(current.getUserId());
            if (fresh == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到当前用户信息。");
                return;
            }
            req.setAttribute("user", fresh);

            String[] paths = resolveProfileFiles(req, fresh.getUserId());
            req.setAttribute("avatarPath", paths[0]);
            req.setAttribute("resumePath", paths[1]);

            req.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("加载个人信息时访问数据库出错", e);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String email = trim(req.getParameter("email"));
        String fullName = trim(req.getParameter("fullName"));
        String affiliation = trim(req.getParameter("affiliation"));
        String researchArea = trim(req.getParameter("researchArea"));

        try {
            User toUpdate = new User();
            toUpdate.setUserId(current.getUserId());
            toUpdate.setEmail(email);
            toUpdate.setFullName(fullName);
            toUpdate.setAffiliation(affiliation);
            toUpdate.setResearchArea(researchArea);
            userDAO.updateProfile(toUpdate);

            saveProfileFiles(req, current.getUserId());

            User fresh = userDAO.findById(current.getUserId());
            req.getSession(true).setAttribute("currentUser", fresh);

            req.setAttribute("user", fresh);
            String[] paths = resolveProfileFiles(req, fresh.getUserId());
            req.setAttribute("avatarPath", paths[0]);
            req.setAttribute("resumePath", paths[1]);
            req.setAttribute("message", "信息更新成功。");

            req.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("更新个人信息时访问数据库出错", e);
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null ? (User) session.getAttribute("currentUser") : null;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private void saveProfileFiles(HttpServletRequest req, int userId) throws IOException, ServletException {
        
        // 统一目录：
        // - 头像：{base}/avatars
        // - 简历：{base}/resumes
        // 目录创建/权限检查在 UploadPathUtil 内完成
        File avatarDir = UploadPathUtil.getAvatarDir(getServletContext()).toFile();
        File resumeDir = UploadPathUtil.getResumeDir(getServletContext()).toFile();

        Part avatarPart = null;
        Part resumePart = null;
        try {
            avatarPart = req.getPart("avatar");
        } catch (IllegalStateException | IOException | ServletException e) {
            // 单个文件上传失败忽略
        }
        try {
            resumePart = req.getPart("resume");
        } catch (IllegalStateException | IOException | ServletException e) {
            // 单个文件上传失败忽略
        }

        if (avatarPart != null && avatarPart.getSize() > 0) {
            String ext = getExtension(avatarPart.getSubmittedFileName());
            File dest = new File(avatarDir, "user_" + userId + "_avatar" + ext);
            avatarPart.write(dest.getAbsolutePath());
        }

        if (resumePart != null && resumePart.getSize() > 0) {
            String ext = getExtension(resumePart.getSubmittedFileName());
            File dest = new File(resumeDir, "user_" + userId + "_resume" + ext);
            resumePart.write(dest.getAbsolutePath());
        }
    }

    private void streamAvatar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        File avatarDir = UploadPathUtil.getAvatarDir(getServletContext()).toFile();
        File legacyProfileDir = new File(UploadPathUtil.getLegacyBaseDir(getServletContext()), LEGACY_PROFILE_SUB_DIR);

        File file = findFileWithPrefix(avatarDir, "user_" + current.getUserId() + "_avatar");
        if (file == null) {
            // 兼容旧目录：{legacyBase}/profile
            file = findFileWithPrefix(legacyProfileDir, "user_" + current.getUserId() + "_avatar");
        }
        if (file == null || !file.exists()) {
            // 没有上传头像：返回默认头像（避免 <img> 显示 alt 文本“用户头像”）
            streamDefaultAvatar(req, resp);
            return;
        }

        String ext = getExtension(file.getName()).toLowerCase();
        String contentType = "image/jpeg";
        if (".png".equals(ext)) {
            contentType = "image/png";
        } else if (".gif".equals(ext)) {
            contentType = "image/gif";
        }
        resp.setContentType(contentType);
        resp.setHeader("Content-Length", String.valueOf(file.length()));

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    
    private void streamDefaultAvatar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("image/svg+xml; charset=UTF-8");
        // 避免浏览器缓存旧的 404 结果
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        try (InputStream in = req.getServletContext().getResourceAsStream("/static/img/default-avatar.svg");
             OutputStream out = resp.getOutputStream()) {
            if (in == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    private void streamResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        File resumeDir = UploadPathUtil.getResumeDir(getServletContext()).toFile();
        // 兼容旧目录：/var/lib/tomcat9/uploads/profile
        File legacyProfileDir = new File(UploadPathUtil.getLegacyBaseDir(getServletContext()), LEGACY_PROFILE_SUB_DIR);

        File file = findFileWithPrefix(resumeDir, "user_" + current.getUserId() + "_resume");
        if (file == null) {
            file = findFileWithPrefix(legacyProfileDir, "user_" + current.getUserId() + "_resume");
        }
        if (file == null || !file.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String ext = getExtension(file.getName()).toLowerCase();
        String contentType = "application/pdf";
        if (".doc".equals(ext)) {
            contentType = "application/msword";
        } else if (".docx".equals(ext)) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        resp.setContentType(contentType);
        // inline: 浏览器内预览；attachment: 直接下载
        resp.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
        resp.setHeader("Content-Length", String.valueOf(file.length()));

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    private File findFileWithPrefix(File dir, String prefix) {
        if (dir == null || !dir.exists()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f.getName().startsWith(prefix)) {
                return f;
            }
        }
        return null;
    }

    
    
    private String[] resolveProfileFiles(HttpServletRequest req, int userId) {
        String contextPath = req.getContextPath();
        String avatarWebPath = contextPath + "/profile/avatar";
        String resumeWebPath = contextPath + "/profile/resume";
        return new String[]{avatarWebPath, resumeWebPath};
    }


    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            return fileName.substring(dot);
        }
        return "";
    }
    
    
}