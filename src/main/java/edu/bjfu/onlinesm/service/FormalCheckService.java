package edu.bjfu.onlinesm.service;

import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.ManuscriptAuthor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FormalCheckService {

    
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    
    private static final int MIN_ABSTRACT_WORDS = 150;
    private static final int MAX_ABSTRACT_WORDS = 700;

    
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

    
    public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText) {
        return performAutomaticChecks(manuscript, bodyText, null, null);
    }


public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText, java.util.List<ManuscriptAuthor> authors) {
    return performAutomaticChecks(manuscript, bodyText, authors, null);
}

    
    public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText, java.util.List<ManuscriptAuthor> authors, Integer pdfPageCount) {
        FormalCheckResult result = new FormalCheckResult();

        result.setManuscriptId(manuscript.getManuscriptId());
        result.setAuthorInfoValid(checkAuthorEmailFormat(manuscript.getAuthorList(), authors));
        result.setAbstractWordCountValid(checkAbstractWordCount(manuscript.getAbstractText()));
        result.setBodyWordCountValid(checkPdfPageCount(pdfPageCount));
        result.setKeywordsValid(checkKeywords(manuscript.getKeywords()));

        
        result.setFootnoteNumberingValid(null);
        result.setFigureTableFormatValid(null);
        result.setReferenceFormatValid(null);

        return result;
    }

    
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

    
    public PlagiarismCheckService.PlagiarismReport performPlagiarismCheck(Manuscript manuscript, String bodyText) {
        return plagiarismCheckService.checkPlagiarism(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                manuscript.getAbstractText(),
                bodyText
        );
    }

    
    
    

    
    private boolean checkAuthorEmailFormat(String authorList) {
        return checkAuthorEmailFormat(authorList, null);
    }

    
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

        
        return foundAny;
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String raw = email.trim();
        if (raw.isEmpty()) return false;

        
        
        Matcher m = EMAIL_PATTERN.matcher(raw);
        if (!m.find()) return false;
        String e = m.group(1);

        
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

    
    public int computeBodyCount(String bodyText) {
        if (bodyText == null || bodyText.trim().isEmpty()) return 0;
        String cleaned = bodyText.replaceAll("<[^>]+>", " ").trim();
        return countCjkChars(cleaned) + countEnglishWordsExcludingCjk(cleaned);
    }

    
    public int computeAbstractCount(String abstractText) {
        if (abstractText == null || abstractText.trim().isEmpty()) return 0;
        String cleaned = abstractText.replaceAll("<[^>]+>", " ").trim();
        return countCjkChars(cleaned) + countEnglishWordsExcludingCjk(cleaned);
    }

    
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

        
        if (result.getSimilarityScore() != null) {
            feedback.append(String.format("查重率 %.2f%%；", result.getSimilarityScore()));
            if (Boolean.TRUE.equals(result.getHighSimilarity())) {
                feedback.append("查重率过高（需<20%）；");
            }
        }

        return feedback.length() > 0 ? feedback.toString() : "所有检查项均符合标准";
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

