package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;


public class FormalCheckResult implements Serializable {

    private Integer checkId;
    private Integer manuscriptId;
    private Integer reviewerId;

    
    private Boolean authorInfoValid;

    
    private Boolean abstractWordCountValid;

    
    private Boolean bodyWordCountValid;

    
    private Boolean keywordsValid;

    
    private Boolean footnoteNumberingValid;

    
    private Boolean figureTableFormatValid;

    
    private Boolean referenceFormatValid;

    
    private Double similarityScore;

    
    private Boolean highSimilarity;

    
    private String plagiarismReportUrl;

    
    private String checkResult;

    
    private String feedback;

    
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

