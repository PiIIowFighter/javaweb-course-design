package edu.bjfu.onlinesm.util;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * 将“已构造好的 XHTML（包含 style/class/inline style）”直接渲染为 PDF。
 *
 * <p>注意：项目现有 {@link HtmlToPdfConverter} 会在清理阶段移除 class/style 属性，
 * 这会导致查重报告这类需要复杂排版的 HTML 丢失样式。
 * 本工具不做清理，专用于系统生成的查重报告 PDF。</p>
 */
public class RawHtmlToPdfConverter {

    /**
     * 将 XHTML 文本渲染为 PDF。
     * @param xhtml 完整 XHTML（必须含 html/head/body）
     * @param outputFile 输出 PDF 文件
     */
    public static void convert(String xhtml, File outputFile) throws Exception {
        if (xhtml == null || xhtml.trim().isEmpty()) {
            throw new IllegalArgumentException("XHTML content cannot be null or empty");
        }

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
}
