package edu.bjfu.onlinesm.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 从“文件”中提取可用于计数字数的纯文本。
 * <p>
 * 支持：PDF（走 {@link PdfTextUtil}）、HTML/HTM、TXT。
 * 其它类型返回空字符串。
 */
public class FileTextUtil {

    private FileTextUtil() {}

    public static String extractText(File file) {
        if (file == null || !file.exists() || !file.isFile()) return "";

        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            return PdfTextUtil.extractText(file);
        }
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            String html = readTextBestEffort(file);
            return htmlToPlainText(html);
        }
        if (name.endsWith(".txt")) {
            return readTextBestEffort(file);
        }
        return "";
    }

    /**
     * 尽量读取文本：先 UTF-8，失败再 GBK（兼容 Windows/旧环境）。
     */
    private static String readTextBestEffort(File file) {
        String t = readText(file, StandardCharsets.UTF_8);
        if (t != null && !t.trim().isEmpty()) return t;
        // 常见中文环境
        t = readText(file, Charset.forName("GBK"));
        return t == null ? "" : t;
    }

    private static String readText(File file, Charset charset) {
        try (InputStream in = new FileInputStream(file);
             Reader r = new InputStreamReader(in, charset);
             BufferedReader br = new BufferedReader(r)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 极简 HTML -> 纯文本：去掉脚本/样式块与标签，并做一些常见实体替换。
     *
     * <p>注意：此方法仅用于“计数字数”，不追求完整 HTML 解析正确性。</p>
     */
    private static String htmlToPlainText(String html) {
        if (html == null || html.trim().isEmpty()) return "";
        String s = html;

        // 去掉 script/style（不区分大小写）
        s = s.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        s = s.replaceAll("(?is)<style[^>]*>.*?</style>", " ");

        // br/p/div/li 等常见块级标签用换行替换，避免文字粘连
        s = s.replaceAll("(?i)<\\s*(br|p|div|li|tr|td|th|h[1-6])[^>]*>", "\n");
        s = s.replaceAll("(?i)</\\s*(p|div|li|tr|td|th|h[1-6])\\s*>", "\n");

        // 去除其它标签
        s = s.replaceAll("(?is)<[^>]+>", " ");

        // 常见实体
        s = s.replace("&nbsp;", " ");
        s = s.replace("&amp;", "&");
        s = s.replace("&lt;", "<");
        s = s.replace("&gt;", ">");
        s = s.replace("&quot;", "\"");
        s = s.replace("&#39;", "'");

        // 压缩多余空白
        s = s.replaceAll("[ \t\r\f]+", " ");
        s = s.replaceAll("\n{3,}", "\n\n");

        return s.trim();
    }
}
