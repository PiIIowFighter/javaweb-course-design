package edu.bjfu.onlinesm.service;

import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.ManuscriptAuthor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 形式审查自动检查服务
 *
 * 调整说明（v2026-01）：
 * 1) 作者信息：仅校验作者邮箱格式（不再要求机构邮箱）。
 * 2) 正文页数：以 PDF 页数为准，范围 8-10 页。
 * 3) 查重：由 PlagiarismCheckService 模拟（<20%），并可生成 PDF 报告链接。
 */
public class FormalCheckService {

    /** 简化邮箱校验（足够应付“格式正确”） */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    // 摘要字数要求：150-700（与详情页提示保持一致）
    private static final int MIN_ABSTRACT_WORDS = 150;
    private static final int MAX_ABSTRACT_WORDS = 700;

    // PDF 页数要求：8-10 页
    private static final int MIN_PDF_PAGES = 8;
    private static final int MAX_PDF_PAGES = 10;

    private static final int MIN_KEYWORDS = 3;
    private static final int MAX_KEYWORDS = 6;

    private final PlagiarismCheckService plagiarismCheckService;

    public FormalCheckService() {
        this.plagiarismCheckService = new PlagiarismCheckService();
    }

    public FormalCheckService(PlagiarismCheckService plagiarismCheckService) {
        this.plagiarismCheckService = plagiarismCheckService;
    }

    /**
     * 自动形式审查（不含查重）
     * @param manuscript 稿件
     * @param bodyText   正文文本（建议传入从 PDF 提取的文本）
     */
    public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText) {
        return performAutomaticChecks(manuscript, bodyText, null, null);
    }

/**
 * 自动形式审查（不含查重）- 兼容旧调用：未提供 PDF 页数时，正文页数检查返回 null（待人工确认）。
 */
public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText, java.util.List<ManuscriptAuthor> authors) {
    return performAutomaticChecks(manuscript, bodyText, authors, null);
}

    /**
     * 自动形式审查（不含查重）
     * @param manuscript 稿件
     * @param bodyText   正文文本（建议传入从 PDF 提取的文本）
     * @param authors    详细作者信息（dbo.ManuscriptAuthors）。若传入，则以 authors 为准校验邮箱。
     */
    public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText, java.util.List<ManuscriptAuthor> authors, Integer pdfPageCount) {
        FormalCheckResult result = new FormalCheckResult();

        result.setManuscriptId(manuscript.getManuscriptId());
        result.setAuthorInfoValid(checkAuthorEmailFormat(manuscript.getAuthorList(), authors));
        result.setAbstractWordCountValid(checkAbstractWordCount(manuscript.getAbstractText()));
        result.setBodyWordCountValid(checkPdfPageCount(pdfPageCount));
        result.setKeywordsValid(checkKeywords(manuscript.getKeywords()));

        // 其它格式项默认不自动判定（保持为 null，页面可人工选择）
        result.setFootnoteNumberingValid(null);
        result.setFigureTableFormatValid(null);
        result.setReferenceFormatValid(null);

        return result;
    }

    /**
     * 自动形式审查（含查重模拟）
     */
    public FormalCheckResult performAutomaticChecksWithPlagiarism(Manuscript manuscript, String bodyText) {
        FormalCheckResult result = performAutomaticChecks(manuscript, bodyText, null, null);

        PlagiarismCheckService.PlagiarismReport report = plagiarismCheckService.checkPlagiarism(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                manuscript.getAbstractText(),
                bodyText
        );

        result.setSimilarityScore(report.getSimilarityScore());
        result.setHighSimilarity(report.isHighSimilarity());
        result.setPlagiarismReportUrl(report.getReportUrl());

        return result;
    }

    /**
     * 单独执行“查重模拟”，用于页面按钮触发
     */
    public PlagiarismCheckService.PlagiarismReport performPlagiarismCheck(Manuscript manuscript, String bodyText) {
        return plagiarismCheckService.checkPlagiarism(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                manuscript.getAbstractText(),
                bodyText
        );
    }

    // =========================
    // 具体检查项
    // =========================

    /**
     * 作者信息仅校验：作者列表字符串里是否至少包含一个邮箱，且所有提取到的邮箱都满足格式。
     * authorList 通常形如：张三(aa@bb.com); 李四(bb@cc.com)
     */
    private boolean checkAuthorEmailFormat(String authorList) {
        return checkAuthorEmailFormat(authorList, null);
    }

    /**
     * 作者信息仅校验：作者邮箱格式。
     * - 若传入 authors（dbo.ManuscriptAuthors），则要求每位作者都必须填写邮箱且格式正确。
     * - 否则回退到从 authorList 字符串中提取邮箱并校验（兼容旧数据）。
     */
    private boolean checkAuthorEmailFormat(String authorList, java.util.List<ManuscriptAuthor> authors) {
        if (authors != null && !authors.isEmpty()) {
            for (ManuscriptAuthor a : authors) {
                if (a == null) return false;
                String email = a.getEmail();
                if (!isValidEmail(email)) return false;
            }
            return true;
        }

        if (authorList == null || authorList.trim().isEmpty()) {
            return false;
        }

        Matcher m = EMAIL_PATTERN.matcher(authorList);
        boolean foundAny = false;

        while (m.find()) {
            foundAny = true;
            String email = m.group(1);
            if (!isValidEmail(email)) {
                return false;
            }
        }

        // 若完全提取不到邮箱，判定不通过
        return foundAny;
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String raw = email.trim();
        if (raw.isEmpty()) return false;

        // 允许输入带有包裹符或附加字符（例如："张三 <a@b.com>"、"a@b.com;"），
        // 只要能提取出合法邮箱即可判定通过。
        Matcher m = EMAIL_PATTERN.matcher(raw);
        if (!m.find()) return false;
        String e = m.group(1);

        // 进一步限制：不允许连续点、首尾点等（轻量校验）
        if (e.startsWith(".") || e.endsWith(".")) return false;
        if (e.contains("..")) return false;
        return true;
    }

    private boolean checkAbstractWordCount(String abstractText) {
        if (abstractText == null || abstractText.trim().isEmpty()) {
            return false;
        }

        int wc = computeAbstractCount(abstractText);
        return wc >= MIN_ABSTRACT_WORDS && wc <= MAX_ABSTRACT_WORDS;
    }

    /**
     * PDF 页数检查：正文 PDF 页数需在 8-10 页之间。
     * - pdfPageCount 为 null：返回 null（表示未检查/无法获取）
     * - pdfPageCount <= 0：返回 false（表示获取失败或文件异常）
     */
    private Boolean checkPdfPageCount(Integer pdfPageCount) {
        if (pdfPageCount == null) return null;
        if (pdfPageCount <= 0) return false;
        return pdfPageCount >= MIN_PDF_PAGES && pdfPageCount <= MAX_PDF_PAGES;
    }

    private boolean checkKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            return false;
        }

        String[] keywordArray = keywords.split("[,;，；]");
        int keywordCount = 0;

        for (String keyword : keywordArray) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                keywordCount++;
            }
        }

        return keywordCount >= MIN_KEYWORDS && keywordCount <= MAX_KEYWORDS;
    }

    // =========================
    // 计数工具
    // =========================

    //（旧的按空白分词统计法会严重低估中文摘要，已改用“字数”（中文字符 + 英文单词））

    /**
     * 统计中文/日文/韩文等 CJK 文字（按“字”计）
     */
    private int countCjkChars(String text) {
        if (text == null || text.isEmpty()) return 0;
        int c = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCjk(ch)) c++;
        }
        return c;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(ch);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || b == Character.UnicodeBlock.HIRAGANA
                || b == Character.UnicodeBlock.KATAKANA
                || b == Character.UnicodeBlock.HANGUL_SYLLABLES
                || b == Character.UnicodeBlock.HANGUL_JAMO;
    }

    /**
     * 英文单词数（剔除 CJK 字符后按空白统计）
     */
    private int countEnglishWordsExcludingCjk(String text) {
        if (text == null || text.trim().isEmpty()) return 0;

        String noCjk = text.replaceAll("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}]", " ");
        noCjk = noCjk.replaceAll("[^A-Za-z0-9]+", " ");
        String[] words = noCjk.trim().split("\\s+");

        int count = 0;
        for (String w : words) {
            if (w != null && !w.trim().isEmpty()) count++;
        }
        return count;
    }

    /**
     * 供外部（Servlet）展示用：计算“字数”（中文字符 + 英文单词）
     */
    public int computeBodyCount(String bodyText) {
        if (bodyText == null || bodyText.trim().isEmpty()) return 0;
        String cleaned = bodyText.replaceAll("<[^>]+>", " ").trim();
        return countCjkChars(cleaned) + countEnglishWordsExcludingCjk(cleaned);
    }

    /**
     * 供外部展示用：计算“摘要字数”（中文字符 + 英文单词）
     */
    public int computeAbstractCount(String abstractText) {
        if (abstractText == null || abstractText.trim().isEmpty()) return 0;
        String cleaned = abstractText.replaceAll("<[^>]+>", " ").trim();
        return countCjkChars(cleaned) + countEnglishWordsExcludingCjk(cleaned);
    }

    /**
     * 生成“形式审查反馈意见”（供 EditorServlet 自动填充）。
     * <p>若某一项为 null，视为“未检查/未选择”，不写入反馈。</p>
     */
    public String generateFeedback(FormalCheckResult result) {
        if (result == null) return "";
        StringBuilder feedback = new StringBuilder();

        if (Boolean.FALSE.equals(result.getAuthorInfoValid())) {
            feedback.append("作者邮箱格式不正确；");
        }
        if (Boolean.FALSE.equals(result.getAbstractWordCountValid())) {
            feedback.append("摘要字数不符合标准（应在").append(MIN_ABSTRACT_WORDS)
                    .append("-").append(MAX_ABSTRACT_WORDS).append("之间）；");
        }
        if (Boolean.FALSE.equals(result.getBodyWordCountValid())) {
            feedback.append("PDF页数不符合标准（应在").append(MIN_PDF_PAGES)
                    .append("-").append(MAX_PDF_PAGES).append("之间）；");
        }
        if (Boolean.FALSE.equals(result.getKeywordsValid())) {
            feedback.append("关键词数量不符合标准（应在").append(MIN_KEYWORDS)
                    .append("-").append(MAX_KEYWORDS).append("个之间）；");
        }
        if (Boolean.FALSE.equals(result.getFootnoteNumberingValid())) {
            feedback.append("注释编号不符合标准；");
        }
        if (Boolean.FALSE.equals(result.getFigureTableFormatValid())) {
            feedback.append("图表格式不符合标准；");
        }
        if (Boolean.FALSE.equals(result.getReferenceFormatValid())) {
            feedback.append("参考文献格式不符合标准；");
        }

        // 查重信息：不管是否超阈值，都可展示
        if (result.getSimilarityScore() != null) {
            feedback.append(String.format("查重率 %.2f%%；", result.getSimilarityScore()));
            if (Boolean.TRUE.equals(result.getHighSimilarity())) {
                feedback.append("查重率过高（需<20%）；");
            }
        }

        return feedback.length() > 0 ? feedback.toString() : "所有检查项均符合标准";
    }

}
