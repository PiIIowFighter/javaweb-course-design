package edu.bjfu.onlinesm.model;

import java.io.Serializable;

/**
 * 推荐审稿人：对应 dbo.ManuscriptRecommendedReviewers。
 */
public class ManuscriptRecommendedReviewer implements Serializable {

    private Integer id;
    private Integer manuscriptId;
    private String fullName;
    private String email;
    private String reason;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
