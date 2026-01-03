package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.util.UploadPathUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 公共头像访问（给首页编委会等公开页面使用）。
 *
 * 访问示例：
 *   /public/avatar?userId=123
 *
 * 读取路径：
 *  - {base}/avatars/user_{userId}_avatar.*
 *  - 兼容旧目录：/var/lib/tomcat9/upload/profile/user_{userId}_avatar.*
 *
 * 若找不到头像则返回 /static/img/default-avatar.svg。
 */
@WebServlet(name = "PublicAvatarServlet", urlPatterns = {"/public/avatar"})
public class PublicAvatarServlet extends HttpServlet {

    private static final String LEGACY_PROFILE_SUB_DIR = "profile";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer userId = parseInt(req.getParameter("userId"));
        if (userId == null) {
            streamDefaultAvatar(req, resp);
            return;
        }

        File avatarDir = UploadPathUtil.getAvatarDir(getServletContext()).toFile();
        File legacyProfileDir = new File(UploadPathUtil.getLegacyBaseDir(getServletContext()), LEGACY_PROFILE_SUB_DIR);

        File file = findFileWithPrefix(avatarDir, "user_" + userId + "_avatar");
        if (file == null) {
            file = findFileWithPrefix(legacyProfileDir, "user_" + userId + "_avatar");
        }

        if (file == null || !file.exists() || !file.isFile()) {
            streamDefaultAvatar(req, resp);
            return;
        }

        String ext = getExtension(file.getName()).toLowerCase();
        String contentType = "image/jpeg";
        if (".png".equals(ext)) {
            contentType = "image/png";
        } else if (".gif".equals(ext)) {
            contentType = "image/gif";
        } else if (".webp".equals(ext)) {
            contentType = "image/webp";
        }

        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=3600");
        resp.setContentLengthLong(file.length());

        try (InputStream in = new BufferedInputStream(new FileInputStream(file));
             OutputStream out = new BufferedOutputStream(resp.getOutputStream())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
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

    private Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private File findFileWithPrefix(File dir, String prefix) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f != null && f.isFile() && f.getName() != null && f.getName().startsWith(prefix)) {
                return f;
            }
        }
        return null;
    }
}
