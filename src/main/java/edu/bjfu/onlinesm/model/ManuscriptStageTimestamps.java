package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 稿件阶段时间戳实体类
 * 对应数据库表 dbo.ManuscriptStageTimestamps
 * 用于记录每份稿件在各审稿阶段的完成时间
 */
public class ManuscriptStageTimestamps implements Serializable {

    private Integer manuscriptId;
    
    /** 草稿编辑完成时间 - DRAFT */
    private LocalDateTime draftCompletedAt;
    
    /** 已提交待处理完成时间 - SUBMITTED */
    private LocalDateTime submittedAt;
    
    /** 形式审查完成时间 - FORMAL_CHECK */
    private LocalDateTime formalCheckCompletedAt;
    
    /** 案头初筛完成时间 - DESK_REVIEW_INITIAL */
    private LocalDateTime deskReviewInitialCompletedAt;
    
    /** 待分配编辑完成时间 - TO_ASSIGN */
    private LocalDateTime toAssignCompletedAt;
    
    /** 编辑处理完成时间 - WITH_EDITOR */
    private LocalDateTime withEditorCompletedAt;
    
    /** 外审完成时间 - UNDER_REVIEW */
    private LocalDateTime underReviewCompletedAt;
    
    /** 编辑推荐意见完成时间 - EDITOR_RECOMMENDATION */
    private LocalDateTime editorRecommendationCompletedAt;
    
    /** 待主编终审完成时间 - FINAL_DECISION_PENDING */
    private LocalDateTime finalDecisionPendingCompletedAt;

    /**
     * 根据状态码获取对应的完成时间
     * @param statusCode 状态码
     * @return 对应的完成时间，如果状态码无效则返回null
     */
    public LocalDateTime getCompletedAtByStatus(String statusCode) {
        if (statusCode == null) return null;
        switch (statusCode) {
            case "DRAFT":
                return draftCompletedAt;
            case "SUBMITTED":
                return submittedAt;
            case "FORMAL_CHECK":
                return formalCheckCompletedAt;
            case "DESK_REVIEW_INITIAL":
                return deskReviewInitialCompletedAt;
            case "TO_ASSIGN":
                return toAssignCompletedAt;
            case "WITH_EDITOR":
                return withEditorCompletedAt;
            case "UNDER_REVIEW":
                return underReviewCompletedAt;
            case "EDITOR_RECOMMENDATION":
                return editorRecommendationCompletedAt;
            case "FINAL_DECISION_PENDING":
                return finalDecisionPendingCompletedAt;
            default:
                return null;
        }
    }

    // ========== Getters and Setters ==========

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public LocalDateTime getDraftCompletedAt() {
        return draftCompletedAt;
    }

    public void setDraftCompletedAt(LocalDateTime draftCompletedAt) {
        this.draftCompletedAt = draftCompletedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getFormalCheckCompletedAt() {
        return formalCheckCompletedAt;
    }

    public void setFormalCheckCompletedAt(LocalDateTime formalCheckCompletedAt) {
        this.formalCheckCompletedAt = formalCheckCompletedAt;
    }

    public LocalDateTime getDeskReviewInitialCompletedAt() {
        return deskReviewInitialCompletedAt;
    }

    public void setDeskReviewInitialCompletedAt(LocalDateTime deskReviewInitialCompletedAt) {
        this.deskReviewInitialCompletedAt = deskReviewInitialCompletedAt;
    }

    public LocalDateTime getToAssignCompletedAt() {
        return toAssignCompletedAt;
    }

    public void setToAssignCompletedAt(LocalDateTime toAssignCompletedAt) {
        this.toAssignCompletedAt = toAssignCompletedAt;
    }

    public LocalDateTime getWithEditorCompletedAt() {
        return withEditorCompletedAt;
    }

    public void setWithEditorCompletedAt(LocalDateTime withEditorCompletedAt) {
        this.withEditorCompletedAt = withEditorCompletedAt;
    }

    public LocalDateTime getUnderReviewCompletedAt() {
        return underReviewCompletedAt;
    }

    public void setUnderReviewCompletedAt(LocalDateTime underReviewCompletedAt) {
        this.underReviewCompletedAt = underReviewCompletedAt;
    }

    public LocalDateTime getEditorRecommendationCompletedAt() {
        return editorRecommendationCompletedAt;
    }

    public void setEditorRecommendationCompletedAt(LocalDateTime editorRecommendationCompletedAt) {
        this.editorRecommendationCompletedAt = editorRecommendationCompletedAt;
    }

    public LocalDateTime getFinalDecisionPendingCompletedAt() {
        return finalDecisionPendingCompletedAt;
    }

    public void setFinalDecisionPendingCompletedAt(LocalDateTime finalDecisionPendingCompletedAt) {
        this.finalDecisionPendingCompletedAt = finalDecisionPendingCompletedAt;
    }

    @Override
    public String toString() {
        return "ManuscriptStageTimestamps{" +
                "manuscriptId=" + manuscriptId +
                ", draftCompletedAt=" + draftCompletedAt +
                ", submittedAt=" + submittedAt +
                ", formalCheckCompletedAt=" + formalCheckCompletedAt +
                ", deskReviewInitialCompletedAt=" + deskReviewInitialCompletedAt +
                ", toAssignCompletedAt=" + toAssignCompletedAt +
                ", withEditorCompletedAt=" + withEditorCompletedAt +
                ", underReviewCompletedAt=" + underReviewCompletedAt +
                ", editorRecommendationCompletedAt=" + editorRecommendationCompletedAt +
                ", finalDecisionPendingCompletedAt=" + finalDecisionPendingCompletedAt +
                '}';
    }
}
