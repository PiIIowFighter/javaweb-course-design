package edu.bjfu.onlinesm.service;

import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.model.Manuscript;

import java.util.regex.Pattern;

public class FormalCheckService {

    private static final Pattern INSTITUTIONAL_EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@(edu|org)\\..*$");
    private static final int MIN_ABSTRACT_WORDS = 150;
    private static final int MAX_ABSTRACT_WORDS = 700;
    private static final int MIN_BODY_WORDS = 3000;
    private static final int MAX_BODY_WORDS = 8000;
    private static final int MIN_KEYWORDS = 3;
    private static final int MAX_KEYWORDS = 7;
    private static final double HIGH_SIMILARITY_THRESHOLD = 20.0;

    private final PlagiarismCheckService plagiarismCheckService;

    public FormalCheckService() {
        this.plagiarismCheckService = new PlagiarismCheckService();
    }

    public FormalCheckService(PlagiarismCheckService plagiarismCheckService) {
        this.plagiarismCheckService = plagiarismCheckService;
    }

    public FormalCheckResult performAutomaticChecks(Manuscript manuscript, String bodyText) {
        FormalCheckResult result = new FormalCheckResult();
        
        result.setManuscriptId(manuscript.getManuscriptId());
        result.setAuthorInfoValid(checkAuthorInfo(manuscript));
        result.setAbstractWordCountValid(checkAbstractWordCount(manuscript.getAbstractText()));
        result.setBodyWordCountValid(checkBodyWordCount(bodyText));
        result.setKeywordsValid(checkKeywords(manuscript.getKeywords()));
        
        return result;
    }

    public FormalCheckResult performAutomaticChecksWithPlagiarism(Manuscript manuscript, String bodyText) {
        FormalCheckResult result = performAutomaticChecks(manuscript, bodyText);
        
        PlagiarismCheckService.PlagiarismReport plagiarismReport = 
            plagiarismCheckService.checkPlagiarism(
                manuscript.getManuscriptId(),
                manuscript.getTitle(),
                manuscript.getAbstractText(),
                bodyText
            );
        
        result.setSimilarityScore(plagiarismReport.getSimilarityScore());
        result.setHighSimilarity(plagiarismReport.isHighSimilarity());
        result.setPlagiarismReportUrl(plagiarismReport.getReportUrl());
        
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

    private boolean checkAuthorInfo(Manuscript manuscript) {
        String authorList = manuscript.getAuthorList();
        if (authorList == null || authorList.trim().isEmpty()) {
            return false;
        }
        
        boolean hasInstitutionalEmail = INSTITUTIONAL_EMAIL_PATTERN.matcher(authorList).find();
        
        return hasInstitutionalEmail;
    }

    private boolean checkAbstractWordCount(String abstractText) {
        if (abstractText == null || abstractText.trim().isEmpty()) {
            return false;
        }
        
        String cleanedText = abstractText.replaceAll("<[^>]+>", "").trim();
        int wordCount = countWords(cleanedText);
        
        return wordCount >= MIN_ABSTRACT_WORDS && wordCount <= MAX_ABSTRACT_WORDS;
    }

    private boolean checkBodyWordCount(String bodyText) {
        if (bodyText == null || bodyText.trim().isEmpty()) {
            return true;
        }
        
        String cleanedText = bodyText.replaceAll("<[^>]+>", "").trim();
        int wordCount = countWords(cleanedText);
        
        return wordCount >= MIN_BODY_WORDS && wordCount <= MAX_BODY_WORDS;
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

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        String[] words = text.split("\\s+");
        int count = 0;
        
        for (String word : words) {
            if (word != null && !word.trim().isEmpty()) {
                count++;
            }
        }
        
        return count;
    }

    public String generateFeedback(FormalCheckResult result) {
        StringBuilder feedback = new StringBuilder();
        
        if (result.getAuthorInfoValid() != null && !result.getAuthorInfoValid()) {
            feedback.append("作者信息不符合标准（缺少机构邮箱）；");
        }
        
        if (result.getAbstractWordCountValid() != null && !result.getAbstractWordCountValid()) {
            feedback.append("摘要字数不符合标准（应在150-700字之间）；");
        }
        
        if (result.getBodyWordCountValid() != null && !result.getBodyWordCountValid()) {
            feedback.append("正文字数不符合标准（应在3000-8000字之间）；");
        }
        
        if (result.getKeywordsValid() != null && !result.getKeywordsValid()) {
            feedback.append("关键词不符合标准（应在3-7个之间）；");
        }
        
        if (result.getFootnoteNumberingValid() != null && !result.getFootnoteNumberingValid()) {
            feedback.append("注释编号不符合标准；");
        }
        
        if (result.getFigureTableFormatValid() != null && !result.getFigureTableFormatValid()) {
            feedback.append("图表格式不符合标准；");
        }
        
        if (result.getReferenceFormatValid() != null && !result.getReferenceFormatValid()) {
            feedback.append("参考文献格式不符合标准；");
        }
        
        if (result.getHighSimilarity() != null && result.getHighSimilarity()) {
            feedback.append(String.format("查重率过高（%.2f%%，超过阈值%.0f%%）；", 
                result.getSimilarityScore(), HIGH_SIMILARITY_THRESHOLD));
        }
        
        return feedback.length() > 0 ? feedback.toString() : "所有检查项均符合标准";
    }
}
