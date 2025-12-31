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

/**
 * Upload path resolver (server-friendly).
 *
 * Priority (high -> low):
 * 1) JVM system property: upload.baseDir / upload.config
 * 2) Environment variable: UPLOAD_BASE_DIR / UPLOAD_CONFIG
 * 3) External config file: /etc/Online_SMsystem/upload.properties (if exists)
 * 4) Classpath resource: upload.properties
 * 5) Default: /var/lib/tomcat9/uploads
 *
 * Note: returned paths are created if missing.
 */
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

            // 1) Try explicit external config path
            String cfg = System.getProperty("upload.config");
            if (cfg == null || cfg.trim().isEmpty()) cfg = System.getenv("UPLOAD_CONFIG");
            if (cfg != null && !cfg.trim().isEmpty()) {
                File f = new File(cfg.trim());
                if (f.isFile()) {
                    try (InputStream in = new FileInputStream(f)) {
                        p.load(in);
                    } catch (IOException ignored) {
                        // fall through
                    }
                }
            }

            // 2) If still empty, try /etc default
            if (p.isEmpty()) {
                File etc = new File("/etc/Online_SMsystem/upload.properties");
                if (etc.isFile()) {
                    try (InputStream in = new FileInputStream(etc)) {
                        p.load(in);
                    } catch (IOException ignored) {
                    }
                }
            }

            // 3) If still empty, classpath resource
            if (p.isEmpty()) {
                try (InputStream in = UploadPathUtil.class.getClassLoader().getResourceAsStream("upload.properties")) {
                    if (in != null) {
                        p.load(in);
                    }
                } catch (IOException ignored) {
                }
            }

            // 4) Overlay env/system property
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

    /**
     * Backward compatible API: some servlets call getBaseDir() without ServletContext.
     * This version uses only env/system properties + default (no classpath read).
     */
    public static String getBaseDir() {
        return getBaseDirFile().getAbsolutePath();
    }

    /**
     * Compatibility alias.
     * Some older controllers referenced this method name.
     */
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

    /**
     * Legacy directory used by older versions (/var/lib/tomcat9/upload).
     * Only used for reading old files; new uploads should go to uploads/.
     */
    public static File getLegacyBaseDir(ServletContext ctx) {
        // keep stable for server deployments
        return new File("/var/lib/tomcat9/upload");
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            // Let caller throw a friendly message if needed
        }
        return dir;
    }
}
