package edu.bjfu.onlinesm.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


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

    
    private static String readTextBestEffort(File file) {
        String t = readText(file, StandardCharsets.UTF_8);
        if (t != null && !t.trim().isEmpty()) return t;
        
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

    
    private static String htmlToPlainText(String html) {
        if (html == null || html.trim().isEmpty()) return "";
        String s = html;

        
        s = s.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        s = s.replaceAll("(?is)<style[^>]*>.*?</style>", " ");

        
        s = s.replaceAll("(?i)<\\s*(br|p|div|li|tr|td|th|h[1-6])[^>]*>", "\n");
        s = s.replaceAll("(?i)</\\s*(p|div|li|tr|td|th|h[1-6])\\s*>", "\n");

        
        s = s.replaceAll("(?is)<[^>]+>", " ");

        
        s = s.replace("&nbsp;", " ");
        s = s.replace("&amp;", "&");
        s = s.replace("&lt;", "<");
        s = s.replace("&gt;", ">");
        s = s.replace("&quot;", "\"");
        s = s.replace("&#39;", "'");

        
        s = s.replaceAll("[ \t\r\f]+", " ");
        s = s.replaceAll("\n{3,}", "\n\n");

        return s.trim();
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

