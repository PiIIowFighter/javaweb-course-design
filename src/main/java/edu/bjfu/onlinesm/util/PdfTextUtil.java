package edu.bjfu.onlinesm.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class PdfTextUtil {

    private PdfTextUtil() {}

    public static String extractText(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || !pdfFile.isFile()) return "";

        
        String t = tryPdfBox(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        
        t = tryIText5(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        
        t = tryLowagie(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        return "";
    }


public static int extractPageCount(File pdfFile) {
    if (pdfFile == null || !pdfFile.exists() || !pdfFile.isFile()) return 0;

    
    try {
        Class<?> pdDocumentCls = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Method loadMethod = pdDocumentCls.getMethod("load", File.class);
        Object document = loadMethod.invoke(null, pdfFile);
        try {
            Method getNumberOfPages = pdDocumentCls.getMethod("getNumberOfPages");
            int pages = ((Number) getNumberOfPages.invoke(document)).intValue();
            
            try { Method closeMethod = pdDocumentCls.getMethod("close"); closeMethod.invoke(document); } catch (Exception ignore) {}
            return pages;
        } finally {
            try { Method closeMethod = pdDocumentCls.getMethod("close"); closeMethod.invoke(document); } catch (Exception ignore) {}
        }
    } catch (Throwable ignore) {}

    
    try {
        Class<?> readerCls = Class.forName("com.itextpdf.text.pdf.PdfReader");
        Constructor<?> ctor = readerCls.getConstructor(String.class);
        Object reader = ctor.newInstance(pdfFile.getAbsolutePath());
        Method getNumberOfPages = readerCls.getMethod("getNumberOfPages");
        int pages = ((Number) getNumberOfPages.invoke(reader)).intValue();
        try { Method close = readerCls.getMethod("close"); close.invoke(reader); } catch (Exception ignore2) {}
        return pages;
    } catch (Throwable ignore) {}

    
    try {
        Class<?> readerCls = Class.forName("com.lowagie.text.pdf.PdfReader");
        Constructor<?> ctor = readerCls.getConstructor(String.class);
        Object reader = ctor.newInstance(pdfFile.getAbsolutePath());
        Method getNumberOfPages = readerCls.getMethod("getNumberOfPages");
        int pages = ((Number) getNumberOfPages.invoke(reader)).intValue();
        try { Method close = readerCls.getMethod("close"); close.invoke(reader); } catch (Exception ignore2) {}
        return pages;
    } catch (Throwable ignore) {}

    return 0;
}

    private static String tryPdfBox(File pdfFile) {
        
        
        
        

        
        String t = tryPdfBox2(pdfFile);
        if (t != null && !t.trim().isEmpty()) return t;

        
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

