package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import edu.bjfu.onlinesm.util.UploadPathUtil;
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


@WebServlet(
        name = "ProfileServlet",
        urlPatterns = {"/profile", "/profile/avatar", "/profile/resume"}
)


@MultipartConfig
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    
    private static final String LEGACY_PROFILE_SUB_DIR = "profile";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        
        if ("/profile/avatar".equals(servletPath)) {
            streamAvatar(req, resp);
            return;
        }

        
        if ("/profile/resume".equals(servletPath)) {
            streamResume(req, resp);
            return;
        }

        
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

        int userId = current.getUserId();

        
        String username = trim(req.getParameter("username"));
        String email = trim(req.getParameter("email"));
        String fullName = trim(req.getParameter("fullName"));
        String affiliation = trim(req.getParameter("affiliation"));
        String researchArea = trim(req.getParameter("researchArea"));

        try {
            boolean usernameChanged = false;

            
            if (username == null || username.trim().isEmpty()) {
                User fresh = userDAO.findById(userId);
                req.setAttribute("user", fresh);
                String[] paths = resolveProfileFiles(req, userId);
                req.setAttribute("avatarPath", paths[0]);
                req.setAttribute("resumePath", paths[1]);
                req.setAttribute("error", "用户名不能为空。");
                req.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(req, resp);
                return;
            }

            String normalizedUsername = username.trim();
            if (current.getUsername() == null || !normalizedUsername.equals(current.getUsername())) {
                User existed = userDAO.findByUsername(normalizedUsername);
                if (existed != null && existed.getUserId() != null && existed.getUserId() != userId) {
                    User fresh = userDAO.findById(userId);
                    req.setAttribute("user", fresh);
                    String[] paths = resolveProfileFiles(req, userId);
                    req.setAttribute("avatarPath", paths[0]);
                    req.setAttribute("resumePath", paths[1]);
                    req.setAttribute("error", "修改失败：用户名已存在，请换一个用户名。");
                    req.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(req, resp);
                    return;
                }
                userDAO.updateUsername(userId, normalizedUsername);
                usernameChanged = true;
            }

            
            User toUpdate = new User();
            toUpdate.setUserId(userId);
            toUpdate.setEmail(email);
            toUpdate.setFullName(fullName);
            toUpdate.setAffiliation(affiliation);
            toUpdate.setResearchArea(researchArea);
            userDAO.updateProfile(toUpdate);

            
            saveProfileFiles(req, userId);

            
            User fresh = userDAO.findById(userId);
            req.getSession(true).setAttribute("currentUser", fresh);

            req.setAttribute("user", fresh);
            String[] paths = resolveProfileFiles(req, userId);
            req.setAttribute("avatarPath", paths[0]);
            req.setAttribute("resumePath", paths[1]);
            req.setAttribute("message", usernameChanged ? "信息更新成功，用户名已更新。" : "信息更新成功。");

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
        
        
        
        
        
        File avatarDir = UploadPathUtil.getAvatarDir(getServletContext()).toFile();
        File resumeDir = UploadPathUtil.getResumeDir(getServletContext()).toFile();

        Part avatarPart = null;
        Part resumePart = null;
        try {
            avatarPart = req.getPart("avatar");
        } catch (IllegalStateException | IOException | ServletException e) {
            
        }
        try {
            resumePart = req.getPart("resume");
        } catch (IllegalStateException | IOException | ServletException e) {
            
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
            
            file = findFileWithPrefix(legacyProfileDir, "user_" + current.getUserId() + "_avatar");
        }
        if (file == null || !file.exists()) {
            
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

/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

