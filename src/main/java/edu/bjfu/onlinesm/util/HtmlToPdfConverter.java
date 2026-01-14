package edu.bjfu.onlinesm.util;

import com.lowagie.text.pdf.BaseFont;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class HtmlToPdfConverter {
    
    
    public static void convert(String htmlContent, File outputFile) throws Exception {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("HTML content cannot be null or empty");
        }
        
        
        String xhtml = wrapAsXhtml(htmlContent);
        
        
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (OutputStream os = new FileOutputStream(outputFile)) {
            ITextRenderer renderer = new ITextRenderer();
            
            
            registerCjkFonts(renderer);

            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(os);
            
            try {
                renderer.finishPDF();
            } catch (Throwable ignore) {
                
            }
        }
    }
    
    
    private static String wrapAsXhtml(String htmlContent) {
        
        String cleanedHtml = cleanHtmlForXhtml(htmlContent);
        
        
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

    
    private static void registerCjkFonts(ITextRenderer renderer) {
        if (renderer == null) return;
        List<String> candidates = new ArrayList<>();

        String os = System.getProperty("os.name", "").toLowerCase();

        
        if (os.contains("win")) {
            candidates.add("C:/Windows/Fonts/simsun.ttc,0");
            candidates.add("C:/Windows/Fonts/simsun.ttf");
            candidates.add("C:/Windows/Fonts/simhei.ttf");
            candidates.add("C:/Windows/Fonts/msyh.ttc,0");
            candidates.add("C:/Windows/Fonts/msyh.ttf");
            candidates.add("C:/Windows/Fonts/simkai.ttf");
        }

        
        if (os.contains("mac")) {
            candidates.add("/System/Library/Fonts/STHeiti Medium.ttc");
            candidates.add("/System/Library/Fonts/STHeiti Light.ttc");
            candidates.add("/System/Library/Fonts/PingFang.ttc");
        }

        
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
                
                renderer.getFontResolver().addFont(p, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Throwable ignore) {
                
            }
        }
    }
    
    
    private static String cleanHtmlForXhtml(String html) {
        if (html == null) {
            return "";
        }
        
        String result = html;
        
        
        result = result.replaceAll("<br\\s*>", "<br/>");
        result = result.replaceAll("<BR\\s*>", "<br/>");
        
        
        result = result.replaceAll("<hr\\s*>", "<hr/>");
        result = result.replaceAll("<HR\\s*>", "<hr/>");
        
        
        result = result.replaceAll("<img([^>]*)(?<!/)>", "<img$1/>");
        result = result.replaceAll("<IMG([^>]*)(?<!/)>", "<img$1/>");
        
        
        result = result.replaceAll("\\s+class=\"[^\"]*\"", "");
        result = result.replaceAll("\\s+style=\"[^\"]*\"", "");
        
        
        result = result.replaceAll("<p>\\s*</p>", "<p>&#160;</p>");
        
        
        
        
        return result;
    }
    
    
    public static boolean isEmptyHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return true;
        }
        
        
        String textOnly = htmlContent.replaceAll("<[^>]*>", "").trim();
        
        
        textOnly = textOnly.replaceAll("&nbsp;", "").replaceAll("&#160;", "").trim();
        
        return textOnly.isEmpty();
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

