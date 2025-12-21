package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 与 SQL Server 中 dbo.Reviews 表对应的简单实体类。
 *
 * 仅保留课程设计所需的核心字段：
 *  - ReviewId
 *  - ManuscriptId
 *  - ReviewerId
 *  - Content
 *  - Score
 *  - Recommendation
 *  - Status (INVITED / ACCEPTED / DECLINED / SUBMITTED / EXPIRED)
 *  - InvitedAt / AcceptedAt / SubmittedAt / DueAt
 *  - RemindCount / LastRemindedAt
 */
public class Review implements Serializable {

    private int reviewId;
    private int manuscriptId;
    private int reviewerId;
    private String content;
    private Double score;
    private String recommendation;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime dueAt;
    private int remindCount;
    private LocalDateTime lastRemindedAt;

    // ====== v2: 结构图要求的评审字段（给编辑/作者的意见与打分表） ======
    /** 给编辑的保密意见（Confidential to Editor） */
    private String confidentialToEditor;
    /** 关键评价（Key Evaluation） */
    private String keyEvaluation;
    /** 打分表：原创性（0-10） */
    private Integer scoreOriginality;
    /** 打分表：重要性（0-10） */
    private Integer scoreSignificance;
    /** 打分表：方法/技术质量（0-10） */
    private Integer scoreMethodology;
    /** 打分表：表达/结构（0-10） */
    private Integer scorePresentation;

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(int manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public int getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(int reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public int getRemindCount() {
        return remindCount;
    }

    public void setRemindCount(int remindCount) {
        this.remindCount = remindCount;
    }

    public LocalDateTime getLastRemindedAt() {
        return lastRemindedAt;
    }

    public void setLastRemindedAt(LocalDateTime lastRemindedAt) {
        this.lastRemindedAt = lastRemindedAt;
    }

    public String getConfidentialToEditor() {
        return confidentialToEditor;
    }

    public void setConfidentialToEditor(String confidentialToEditor) {
        this.confidentialToEditor = confidentialToEditor;
    }

    public String getKeyEvaluation() {
        return keyEvaluation;
    }

    public void setKeyEvaluation(String keyEvaluation) {
        this.keyEvaluation = keyEvaluation;
    }

    public Integer getScoreOriginality() {
        return scoreOriginality;
    }

    public void setScoreOriginality(Integer scoreOriginality) {
        this.scoreOriginality = scoreOriginality;
    }

    public Integer getScoreSignificance() {
        return scoreSignificance;
    }

    public void setScoreSignificance(Integer scoreSignificance) {
        this.scoreSignificance = scoreSignificance;
    }

    public Integer getScoreMethodology() {
        return scoreMethodology;
    }

    public void setScoreMethodology(Integer scoreMethodology) {
        this.scoreMethodology = scoreMethodology;
    }

    public Integer getScorePresentation() {
        return scorePresentation;
    }

    public void setScorePresentation(Integer scorePresentation) {
        this.scorePresentation = scorePresentation;
    }


    private String reviewerName;
    private String reviewerEmail;

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerEmail() {
        return reviewerEmail;
    }

    public void setReviewerEmail(String reviewerEmail) {
        this.reviewerEmail = reviewerEmail;
    }

}
