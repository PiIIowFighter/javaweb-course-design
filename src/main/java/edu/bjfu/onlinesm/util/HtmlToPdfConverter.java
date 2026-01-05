package edu.bjfu.onlinesm.util;

import com.lowagie.text.pdf.BaseFont;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
            // 关键：注册 CJK 字体，避免中文/日文等内容在 PDF 中渲染为空白
            // （FlyingSaucer + iText 2.x 在未注册字体时，可能出现“PDF 有页但无文字”的情况）
            registerCjkFonts(renderer);

            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(os);
            // 某些版本需要显式 finish，否则可能出现内容丢失/不完整
            try {
                renderer.finishPDF();
            } catch (Throwable ignore) {
                // 兼容老版本 FlyingSaucer
            }
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
        
        // 注意：不要使用外部 DTD（某些环境下解析器会尝试联网拉取 DTD，导致渲染异常/空白）
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
     * 注册常见 CJK 字体（尽量跨平台）。
     * 目标：让 cover letter 中的中文/日文/韩文等字符能够正确渲染到 PDF。
     */
    private static void registerCjkFonts(ITextRenderer renderer) {
        if (renderer == null) return;
        List<String> candidates = new ArrayList<>();

        String os = System.getProperty("os.name", "").toLowerCase();

        // Windows（本地开发最常见）
        if (os.contains("win")) {
            candidates.add("C:/Windows/Fonts/simsun.ttc,0");
            candidates.add("C:/Windows/Fonts/simsun.ttf");
            candidates.add("C:/Windows/Fonts/simhei.ttf");
            candidates.add("C:/Windows/Fonts/msyh.ttc,0");
            candidates.add("C:/Windows/Fonts/msyh.ttf");
            candidates.add("C:/Windows/Fonts/simkai.ttf");
        }

        // macOS
        if (os.contains("mac")) {
            candidates.add("/System/Library/Fonts/STHeiti Medium.ttc");
            candidates.add("/System/Library/Fonts/STHeiti Light.ttc");
            candidates.add("/System/Library/Fonts/PingFang.ttc");
        }

        // Linux（常见服务器环境）
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/truetype/arphic/ukai.ttc");
        candidates.add("/usr/share/fonts/truetype/arphic/uming.ttc");
        candidates.add("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
        candidates.add("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc");

        for (String p : candidates) {
            if (p == null || p.trim().isEmpty()) continue;
            try {
                // iText 2.x：IDENTITY_H 以支持 Unicode；EMBEDDED 以避免客户端缺字体
                renderer.getFontResolver().addFont(p, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Throwable ignore) {
                // 忽略单个字体注册失败
            }
        }
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
