package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;


public class ManuscriptStageTimestamps implements Serializable {

    private Integer manuscriptId;
    
    
    private LocalDateTime draftCompletedAt;
    
    
    private LocalDateTime submittedAt;
    
    
    private LocalDateTime formalCheckCompletedAt;
    
    
    private LocalDateTime deskReviewInitialCompletedAt;
    
    
    private LocalDateTime toAssignCompletedAt;
    
    
    private LocalDateTime withEditorCompletedAt;
    
    
    private LocalDateTime underReviewCompletedAt;
    
    
    private LocalDateTime editorRecommendationCompletedAt;
    
    
    private LocalDateTime finalDecisionPendingCompletedAt;

    
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

