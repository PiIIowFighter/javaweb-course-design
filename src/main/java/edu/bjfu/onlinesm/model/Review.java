package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;


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

    
    
    private String confidentialToEditor;
    
    private String keyEvaluation;
    
    private Integer scoreOriginality;
    
    private Integer scoreSignificance;
    
    private Integer scoreMethodology;
    
    private Integer scorePresentation;
    
    private Integer scoreExperimentation;
    
    private Integer scoreLiteratureReview;
    
    private Integer scoreConclusions;
    
    private Integer scoreAcademicIntegrity;
    
    private Integer scorePracticality;

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

    public Integer getScoreExperimentation() {
        return scoreExperimentation;
    }

    public void setScoreExperimentation(Integer scoreExperimentation) {
        this.scoreExperimentation = scoreExperimentation;
    }

    public Integer getScoreLiteratureReview() {
        return scoreLiteratureReview;
    }

    public void setScoreLiteratureReview(Integer scoreLiteratureReview) {
        this.scoreLiteratureReview = scoreLiteratureReview;
    }

    public Integer getScoreConclusions() {
        return scoreConclusions;
    }

    public void setScoreConclusions(Integer scoreConclusions) {
        this.scoreConclusions = scoreConclusions;
    }

    public Integer getScoreAcademicIntegrity() {
        return scoreAcademicIntegrity;
    }

    public void setScoreAcademicIntegrity(Integer scoreAcademicIntegrity) {
        this.scoreAcademicIntegrity = scoreAcademicIntegrity;
    }

    public Integer getScorePracticality() {
        return scorePracticality;
    }

    public void setScorePracticality(Integer scorePracticality) {
        this.scorePracticality = scorePracticality;
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
 
    private String manuscriptTitle;
    
    public String getManuscriptTitle() {
        return manuscriptTitle;
    }
    
    public void setManuscriptTitle(String manuscriptTitle) {
        this.manuscriptTitle = manuscriptTitle;
    }
 
    
    private String rejectionReason;

    
    private LocalDateTime declinedAt;

    

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getDeclinedAt() {
        return declinedAt;
    }

    public void setDeclinedAt(LocalDateTime declinedAt) {
        this.declinedAt = declinedAt;
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

