package edu.bjfu.onlinesm.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * PDF 文本提取工具（尽量避免硬依赖）。
 *
 * 运行时优先使用 PDFBox（若项目已包含相关 jar）。
 * 如果没有 PDFBox，则尝试 iText（若存在 parser 包）。
 *
 * 注意：若环境缺少 PDF 解析库，将返回空字符串，调用方应据此判定“无法自动统计字数”。
 */
public class PdfTextUtil {

    private PdfTextUtil() {}

    public static String extractText(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || !pdfFile.isFile()) return "";

        // 1) PDFBox
        String t = tryPdfBox(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        // 2) iText 5.x
        t = tryIText5(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        // 3) iText 2.x (com.lowagie.*)
        t = tryLowagie(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        return "";
    }

    private static String tryPdfBox(File pdfFile) {
        // 兼容 PDFBox 2.x 与 1.8.x
        // 2.x: org.apache.pdfbox.text.PDFTextStripper
        // 1.8: org.apache.pdfbox.util.PDFTextStripper
        // 两者 API 很接近，但包名不同。

        // 1) PDFBox 2.x
        String t = tryPdfBox2(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        // 2) PDFBox 1.8.x
        t = tryPdfBox18(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        return "";
    }

    private static String tryPdfBox2(File pdfFile) {
        try {
            Class<?> pdDocumentCls = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdfTextStripperCls = Class.forName("org.apache.pdfbox.text.PDFTextStripper");

            Method loadMethod = pdDocumentCls.getMethod("load", File.class);
            Object document = loadMethod.invoke(null, pdfFile);

            Object stripper = pdfTextStripperCls.getConstructor().newInstance();
            Method getTextMethod = pdfTextStripperCls.getMethod("getText", pdDocumentCls);
            String text = (String) getTextMethod.invoke(stripper, document);

            // close
            Method closeMethod = pdDocumentCls.getMethod("close");
            closeMethod.invoke(document);

            return text == null ? "" : text;
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static String tryPdfBox18(File pdfFile) {
        try {
            Class<?> pdDocumentCls = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdfTextStripperCls = Class.forName("org.apache.pdfbox.util.PDFTextStripper");

            // PDDocument.load(File) 在 1.8/2.x 都存在
            Method loadMethod = pdDocumentCls.getMethod("load", File.class);
            Object document = loadMethod.invoke(null, pdfFile);

            Object stripper = pdfTextStripperCls.getConstructor().newInstance();
            Method getTextMethod = pdfTextStripperCls.getMethod("getText", pdDocumentCls);
            String text = (String) getTextMethod.invoke(stripper, document);

            Method closeMethod = pdDocumentCls.getMethod("close");
            closeMethod.invoke(document);

            return text == null ? "" : text;
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static String tryIText5(File pdfFile) {
        try {
            Class<?> readerCls = Class.forName("com.itextpdf.text.pdf.PdfReader");
            Class<?> parserCls = Class.forName("com.itextpdf.text.pdf.parser.PdfTextExtractor");
            Class<?> strategyCls = Class.forName("com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy");

            Constructor<?> readerCtor = readerCls.getConstructor(String.class);
            Object reader = readerCtor.newInstance(pdfFile.getAbsolutePath());

            Method getNumberOfPages = readerCls.getMethod("getNumberOfPages");
            int pages = (Integer) getNumberOfPages.invoke(reader);

            Object strategy = strategyCls.getConstructor().newInstance();
            Method getTextFromPage = parserCls.getMethod("getTextFromPage", readerCls, int.class,
                    Class.forName("com.itextpdf.text.pdf.parser.TextExtractionStrategy"));

            StringBuilder sb = new StringBuilder();
            for (int p = 1; p <= pages; p++) {
                Object pageText = getTextFromPage.invoke(null, reader, p, strategy);
                if (pageText != null) sb.append(pageText.toString()).append('\n');
            }

            Method close = readerCls.getMethod("close");
            close.invoke(reader);

            return sb.toString();
        } catch (Throwable ignore) {
            return "";
        }
    }

    private static String tryLowagie(File pdfFile) {
        try {
            Class<?> readerCls = Class.forName("com.lowagie.text.pdf.PdfReader");

            // 某些版本的 iText 2.x 有 parser 包；没有的话会抛异常
            Class<?> parserCls = Class.forName("com.lowagie.text.pdf.parser.PdfTextExtractor");
            Class<?> strategyCls = Class.forName("com.lowagie.text.pdf.parser.SimpleTextExtractionStrategy");
            Class<?> strategyIface = Class.forName("com.lowagie.text.pdf.parser.TextExtractionStrategy");

            Object reader = readerCls.getConstructor(String.class).newInstance(pdfFile.getAbsolutePath());
            int pages = (Integer) readerCls.getMethod("getNumberOfPages").invoke(reader);

            Object strategy = strategyCls.getConstructor().newInstance();
            Method getTextFromPage = parserCls.getMethod("getTextFromPage", readerCls, int.class, strategyIface);

            StringBuilder sb = new StringBuilder();
            for (int p = 1; p <= pages; p++) {
                Object pageText = getTextFromPage.invoke(null, reader, p, strategy);
                if (pageText != null) sb.append(pageText.toString()).append('\n');
            }

            readerCls.getMethod("close").invoke(reader);
            return sb.toString();
        } catch (Throwable ignore) {
            return "";
        }
    }
}
