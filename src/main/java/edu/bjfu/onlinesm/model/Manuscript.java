package edu.bjfu.onlinesm.model; 

import java.io.Serializable;
import java.time.LocalDateTime;

public class Manuscript implements Serializable {

    private Integer manuscriptId;
    private Integer journalId;
    private Integer submitterId;

    private String title;
    private String abstractText;
    private String keywords;
    private String subjectArea;
    private String fundingInfo;

    
    private String authorList;

    private String currentStatus;
    private LocalDateTime submitTime;

    
    private String decision;

    
    private LocalDateTime finalDecisionTime;

    
    private Integer viewCount;
    private Integer downloadCount;
    private Integer citationCount;
    private Double popularityScore;

    
    private String journalName;
    private String journalIssn;

    private String doi;
    private Integer publishYear;
    private String volume;
    private String issue;
    private String pageRange;
    private String language;
    private String articleType;
    private String classificationNo;
    private String cnkiUrl;
    private LocalDateTime publishedAt;

    
    
    public Integer getUserId() {
        return submitterId;
    }
    
    
    public void setUserId(Integer userId) {
        this.submitterId = userId;
    }
    private Integer editorId;

    public Integer getEditorId() {
        return editorId;
    }

    public void setEditorId(Integer editorId) {
        this.editorId = editorId;
    }
    
    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
    }
    public Integer getSubmitterId() {
        return submitterId;
    }

    public void setSubmitterId(Integer submitterId) {
        this.submitterId = submitterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSubjectArea() {
        return subjectArea;
    }

    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
    }

    public String getFundingInfo() {
        return fundingInfo;
    }

    public void setFundingInfo(String fundingInfo) {
        this.fundingInfo = fundingInfo;
    }

    public String getAuthorList() {
        return authorList;
    }

    public void setAuthorList(String authorList) {
        this.authorList = authorList;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public LocalDateTime getFinalDecisionTime() {
        return finalDecisionTime;
    }

    public void setFinalDecisionTime(LocalDateTime finalDecisionTime) {
        this.finalDecisionTime = finalDecisionTime;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(Integer citationCount) {
        this.citationCount = citationCount;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    public String getJournalName() {
        return journalName;
    }

    public void setJournalName(String journalName) {
        this.journalName = journalName;
    }

    public String getJournalIssn() {
        return journalIssn;
    }

    public void setJournalIssn(String journalIssn) {
        this.journalIssn = journalIssn;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getPageRange() {
        return pageRange;
    }

    public void setPageRange(String pageRange) {
        this.pageRange = pageRange;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getArticleType() {
        return articleType;
    }

    public void setArticleType(String articleType) {
        this.articleType = articleType;
    }

    public String getClassificationNo() {
        return classificationNo;
    }

    public void setClassificationNo(String classificationNo) {
        this.classificationNo = classificationNo;
    }

    public String getCnkiUrl() {
        return cnkiUrl;
    }

    public void setCnkiUrl(String cnkiUrl) {
        this.cnkiUrl = cnkiUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Override
    public String toString() {
        return "Manuscript{" +
                "manuscriptId=" + manuscriptId +
                ", journalId=" + journalId +
                ", submitterId=" + submitterId +
                ", title='" + title + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", decision='" + decision + '\'' +
                ", finalDecisionTime=" + finalDecisionTime +
                ", viewCount=" + viewCount +
                ", downloadCount=" + downloadCount +
                ", citationCount=" + citationCount +
                ", popularityScore=" + popularityScore +
                ", journalName='" + journalName + '\'' +
                ", doi='" + doi + '\'' +
                ", publishYear=" + publishYear +
                ", volume='" + volume + '\'' +
                ", issue='" + issue + '\'' +
                ", pageRange='" + pageRange + '\'' +
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

