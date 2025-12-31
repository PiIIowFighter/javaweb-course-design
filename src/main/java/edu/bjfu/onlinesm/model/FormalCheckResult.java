package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 形式审查结果数据模型
 * 用于记录稿件形式审查的各项检查结果
 */
public class FormalCheckResult implements Serializable {

    private Integer checkId;
    private Integer manuscriptId;
    private Integer reviewerId;

    /** 作者信息是否符合标准（系统自动检查） */
    private Boolean authorInfoValid;

    /** 摘要字数是否符合标准（系统自动检查） */
    private Boolean abstractWordCountValid;

    /** 正文字数是否符合标准（系统自动检查） */
    private Boolean bodyWordCountValid;

    /** 关键词是否符合标准（系统自动检查） */
    private Boolean keywordsValid;

    /** 注释编号是否符合标准（人工判断） */
    private Boolean footnoteNumberingValid;

    /** 图表格式是否符合标准（人工判断） */
    private Boolean figureTableFormatValid;

    /** 参考文献格式是否符合标准（人工判断） */
    private Boolean referenceFormatValid;

    /** 查重率（百分比，0-100） */
    private Double similarityScore;

    /** 是否高相似度（查重率>20%） */
    private Boolean highSimilarity;

    /** 查重报告URL */
    private String plagiarismReportUrl;

    /** 审查结果：PASS（通过）/ FAIL（失败） */
    private String checkResult;

    /** 反馈意见 */
    private String feedback;

    /** 审查时间 */
    private LocalDateTime checkTime;

    public Integer getCheckId() {
        return checkId;
    }

    public void setCheckId(Integer checkId) {
        this.checkId = checkId;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Boolean getAuthorInfoValid() {
        return authorInfoValid;
    }

    public void setAuthorInfoValid(Boolean authorInfoValid) {
        this.authorInfoValid = authorInfoValid;
    }

    public Boolean getAbstractWordCountValid() {
        return abstractWordCountValid;
    }

    public void setAbstractWordCountValid(Boolean abstractWordCountValid) {
        this.abstractWordCountValid = abstractWordCountValid;
    }

    public Boolean getBodyWordCountValid() {
        return bodyWordCountValid;
    }

    public void setBodyWordCountValid(Boolean bodyWordCountValid) {
        this.bodyWordCountValid = bodyWordCountValid;
    }

    public Boolean getKeywordsValid() {
        return keywordsValid;
    }

    public void setKeywordsValid(Boolean keywordsValid) {
        this.keywordsValid = keywordsValid;
    }

    public Boolean getFootnoteNumberingValid() {
        return footnoteNumberingValid;
    }

    public void setFootnoteNumberingValid(Boolean footnoteNumberingValid) {
        this.footnoteNumberingValid = footnoteNumberingValid;
    }

    public Boolean getFigureTableFormatValid() {
        return figureTableFormatValid;
    }

    public void setFigureTableFormatValid(Boolean figureTableFormatValid) {
        this.figureTableFormatValid = figureTableFormatValid;
    }

    public Boolean getReferenceFormatValid() {
        return referenceFormatValid;
    }

    public void setReferenceFormatValid(Boolean referenceFormatValid) {
        this.referenceFormatValid = referenceFormatValid;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Boolean getHighSimilarity() {
        return highSimilarity;
    }

    public void setHighSimilarity(Boolean highSimilarity) {
        this.highSimilarity = highSimilarity;
    }

    public String getPlagiarismReportUrl() {
        return plagiarismReportUrl;
    }

    public void setPlagiarismReportUrl(String plagiarismReportUrl) {
        this.plagiarismReportUrl = plagiarismReportUrl;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    @Override
    public String toString() {
        return "FormalCheckResult{" +
                "checkId=" + checkId +
                ", manuscriptId=" + manuscriptId +
                ", reviewerId=" + reviewerId +
                ", authorInfoValid=" + authorInfoValid +
                ", abstractWordCountValid=" + abstractWordCountValid +
                ", bodyWordCountValid=" + bodyWordCountValid +
                ", keywordsValid=" + keywordsValid +
                ", footnoteNumberingValid=" + footnoteNumberingValid +
                ", figureTableFormatValid=" + figureTableFormatValid +
                ", referenceFormatValid=" + referenceFormatValid +
                ", similarityScore=" + similarityScore +
                ", highSimilarity=" + highSimilarity +
                ", plagiarismReportUrl='" + plagiarismReportUrl + '\'' +
                ", checkResult='" + checkResult + '\'' +
                ", feedback='" + feedback + '\'' +
                ", checkTime=" + checkTime +
                '}';
    }
}
