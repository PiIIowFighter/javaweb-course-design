package edu.bjfu.onlinesm.util;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


public class UploadPathUtil {

    private static final String DEFAULT_BASE_DIR = "/var/lib/tomcat9/uploads";
    private static final String DEFAULT_AVATAR_SUBDIR = "avatars";
    private static final String DEFAULT_RESUME_SUBDIR = "resumes";

    private static volatile Properties cached;

    private static Properties loadProps(ServletContext ctx) {
        if (cached != null) return cached;
        synchronized (UploadPathUtil.class) {
            if (cached != null) return cached;
            Properties p = new Properties();

            
            String cfg = System.getProperty("upload.config");
            if (cfg == null || cfg.trim().isEmpty()) cfg = System.getenv("UPLOAD_CONFIG");
            if (cfg != null && !cfg.trim().isEmpty()) {
                File f = new File(cfg.trim());
                if (f.isFile()) {
                    try (InputStream in = new FileInputStream(f)) {
                        p.load(in);
                    } catch (IOException ignored) {
                        
                    }
                }
            }

            
            if (p.isEmpty()) {
                File etc = new File("/etc/Online_SMsystem/upload.properties");
                if (etc.isFile()) {
                    try (InputStream in = new FileInputStream(etc)) {
                        p.load(in);
                    } catch (IOException ignored) {
                    }
                }
            }

            
            if (p.isEmpty()) {
                try (InputStream in = UploadPathUtil.class.getClassLoader().getResourceAsStream("upload.properties")) {
                    if (in != null) {
                        p.load(in);
                    }
                } catch (IOException ignored) {
                }
            }

            
            overlay(p, "upload.baseDir", System.getProperty("upload.baseDir"));
            overlay(p, "upload.baseDir", System.getenv("UPLOAD_BASE_DIR"));
            overlay(p, "upload.avatarSubDir", System.getProperty("upload.avatarSubDir"));
            overlay(p, "upload.avatarSubDir", System.getenv("UPLOAD_AVATAR_SUBDIR"));
            overlay(p, "upload.resumeSubDir", System.getProperty("upload.resumeSubDir"));
            overlay(p, "upload.resumeSubDir", System.getenv("UPLOAD_RESUME_SUBDIR"));

            cached = p;
            return p;
        }
    }

    private static void overlay(Properties p, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            p.setProperty(key, value.trim());
        }
    }

    
    public static String getBaseDir() {
        return getBaseDirFile().getAbsolutePath();
    }

    
    public static String getBaseDirPath() {
        return getBaseDir();
    }

    public static File getBaseDirFile() {
        String base = System.getProperty("upload.baseDir");
        if (base == null || base.trim().isEmpty()) base = System.getenv("UPLOAD_BASE_DIR");
        if (base == null || base.trim().isEmpty()) base = DEFAULT_BASE_DIR;
        return ensureDir(Paths.get(base.trim())).toFile();
    }

    public static Path getBaseDir(ServletContext ctx) {
        Properties p = loadProps(ctx);
        String base = p.getProperty("upload.baseDir");
        if (base == null || base.trim().isEmpty()) base = DEFAULT_BASE_DIR;
        return ensureDir(Paths.get(base.trim()));
    }

    public static Path getAvatarDir(ServletContext ctx) {
        Properties p = loadProps(ctx);
        String sub = p.getProperty("upload.avatarSubDir");
        if (sub == null || sub.trim().isEmpty()) return ensureDir(getBaseDir(ctx).resolve(DEFAULT_AVATAR_SUBDIR));
        return ensureDir(getBaseDir(ctx).resolve(sub.trim()));
    }

    public static Path getResumeDir(ServletContext ctx) {
        Properties p = loadProps(ctx);
        String sub = p.getProperty("upload.resumeSubDir");
        if (sub == null || sub.trim().isEmpty()) return ensureDir(getBaseDir(ctx).resolve(DEFAULT_RESUME_SUBDIR));
        return ensureDir(getBaseDir(ctx).resolve(sub.trim()));
    }

    
    public static File getLegacyBaseDir(ServletContext ctx) {
        
        return new File("/var/lib/tomcat9/upload");
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            
        }
        return dir;
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

