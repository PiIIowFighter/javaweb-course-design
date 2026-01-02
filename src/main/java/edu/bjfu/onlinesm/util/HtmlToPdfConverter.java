package edu.bjfu.onlinesm.util;

import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.*;

/**
 * HTML 转 PDF 工具类
 * 使用 Flying Saucer 将 HTML 内容转换为 PDF 文件
 */
public class HtmlToPdfConverter {
    
    /**
     * 将 HTML 内容转换为 PDF 文件
     * @param htmlContent HTML 内容（Quill 编辑器输出）
     * @param outputFile 输出的 PDF 文件
     * @throws Exception 转换失败时抛出异常
     */
    public static void convert(String htmlContent, File outputFile) throws Exception {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("HTML content cannot be null or empty");
        }
        
        // 包装为完整的 XHTML 文档
        String xhtml = wrapAsXhtml(htmlContent);
        
        // 确保父目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (OutputStream os = new FileOutputStream(outputFile)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(os);
        }
    }
    
    /**
     * 将 HTML 片段包装为完整的 XHTML 文档
     * @param htmlContent HTML 片段
     * @return 完整的 XHTML 文档
     */
    private static String wrapAsXhtml(String htmlContent) {
        // 清理 Quill 编辑器可能产生的非 XHTML 兼容标签
        String cleanedHtml = cleanHtmlForXhtml(htmlContent);
        
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
               "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
               "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
               "<head>\n" +
               "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n" +
               "  <style type=\"text/css\">\n" +
               "    body { font-family: SimSun, STSong, Arial, sans-serif; font-size: 12pt; " +
               "           line-height: 1.6; padding: 40px; }\n" +
               "    h1 { font-size: 18pt; margin-top: 1em; margin-bottom: 0.5em; }\n" +
               "    h2 { font-size: 16pt; margin-top: 1em; margin-bottom: 0.5em; }\n" +
               "    h3 { font-size: 14pt; margin-top: 1em; margin-bottom: 0.5em; }\n" +
               "    p { margin: 0.5em 0; text-align: justify; }\n" +
               "    ul, ol { margin: 0.5em 0; padding-left: 2em; }\n" +
               "    li { margin: 0.3em 0; }\n" +
               "    strong, b { font-weight: bold; }\n" +
               "    em, i { font-style: italic; }\n" +
               "    u { text-decoration: underline; }\n" +
               "    s { text-decoration: line-through; }\n" +
               "    sub { vertical-align: sub; font-size: smaller; }\n" +
               "    sup { vertical-align: super; font-size: smaller; }\n" +
               "  </style>\n" +
               "</head>\n" +
               "<body>\n" +
               cleanedHtml +
               "\n</body>\n</html>";
    }
    
    /**
     * 清理 HTML 使其兼容 XHTML 规范
     * @param html 原始 HTML
     * @return 清理后的 HTML
     */
    private static String cleanHtmlForXhtml(String html) {
        if (html == null) {
            return "";
        }
        
        String result = html;
        
        // 将 <br> 转换为 <br/>
        result = result.replaceAll("<br\\s*>", "<br/>");
        result = result.replaceAll("<BR\\s*>", "<br/>");
        
        // 将 <hr> 转换为 <hr/>
        result = result.replaceAll("<hr\\s*>", "<hr/>");
        result = result.replaceAll("<HR\\s*>", "<hr/>");
        
        // 将 <img ...> 转换为 <img ... />
        result = result.replaceAll("<img([^>]*)(?<!/)>", "<img$1/>");
        result = result.replaceAll("<IMG([^>]*)(?<!/)>", "<img$1/>");
        
        // 移除可能导致问题的属性
        result = result.replaceAll("\\s+class=\"[^\"]*\"", "");
        result = result.replaceAll("\\s+style=\"[^\"]*\"", "");
        
        // 处理空段落
        result = result.replaceAll("<p>\\s*</p>", "<p>&#160;</p>");
        
        // 转义特殊字符（如果未转义）
        // 注意：不要转义已经是实体的内容
        
        return result;
    }
    
    /**
     * 检查 HTML 内容是否为空（仅包含空白或空标签）
     * @param htmlContent HTML 内容
     * @return 如果内容为空返回 true
     */
    public static boolean isEmptyHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return true;
        }
        
        // 移除所有 HTML 标签后检查是否还有内容
        String textOnly = htmlContent.replaceAll("<[^>]*>", "").trim();
        
        // 检查是否只有空白字符或 &nbsp;
        textOnly = textOnly.replaceAll("&nbsp;", "").replaceAll("&#160;", "").trim();
        
        return textOnly.isEmpty();
    }
}
