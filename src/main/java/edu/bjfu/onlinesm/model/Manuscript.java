package edu.bjfu.onlinesm.model; // 必须补充包名

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

    /** 作者列表简要字符串（用于列表显示/冗余展示），详细作者信息见 dbo.ManuscriptAuthors。 */
    private String authorList;

    private String currentStatus;
    private LocalDateTime submitTime;

    /** 编辑建议 / 最终决策（ACCEPT / REJECT / REVISION 等），对应 dbo.Manuscripts.Decision */
    private String decision;

    /** 最终决策时间：主编给出最终决策的时间点 */
    private LocalDateTime finalDecisionTime;

    /* =========================
       前台展示用统计字段（dbo.ArticleMetrics）
       说明：不影响投稿流程，仅用于 Articles 页面排序/展示。
       ========================= */
    private Integer viewCount;
    private Integer downloadCount;
    private Integer citationCount;
    private Double popularityScore;

    // ========== 新增兼容方法 ==========
    /**
     * 兼容方法：获取用户ID
     * 返回 submitterId，因为现有代码可能期望这个方法
     */
    public Integer getUserId() {
        return submitterId;
    }
    
    /**
     * 兼容方法：设置用户ID
     */
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
    // ========== 所有字段的getter/setter ==========
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
                '}';
    }
}