package edu.bjfu.onlinesm.util;

import java.io.File;

/**
 * 统一管理本系统中所有上传文件的物理存储路径。
 *
 * <p>当前约定：所有 upload 文件存放在与 src 目录同级的 upload 目录下，例如：</p>
 * <pre>
 *   project-root/
 *     src/
 *     upload/   <-- 所有上传文件都在这里按子目录归类存放
 * </pre>
 */
public final class UploadPathUtil {

    /** 统一的上传根目录的绝对路径，例如 D:/path/to/project/upload */
    private static final String BASE_DIR;

    static {
        // 以启动时的工作目录为项目根目录（IDE 下通常就是包含 src 的目录）
        String userDir = System.getProperty("user.dir");
        File baseDir = new File(userDir, "upload").getAbsoluteFile();
        if (!baseDir.exists()) {
            // 尽量创建目录，失败也不抛出异常，后续写入时再暴露问题
            baseDir.mkdirs();
        }
        BASE_DIR = baseDir.getAbsolutePath();
    }

    private UploadPathUtil() {
    }

    /**
     * 返回统一的上传根目录绝对路径。
     */
    public static String getBaseDir() {
        return BASE_DIR;
    }

    /**
     * 返回统一的上传根目录 File 对象。
     */
    public static File getBaseDirFile() {
        return new File(BASE_DIR);
    }

    /**
     * 确保并返回某个子目录，例如 "profile"、"manuscripts"、"journal" 等。
     */
    public static File ensureSubDir(String subDir) {
        File dir = new File(BASE_DIR, subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
