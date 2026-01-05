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

    /**
     * 兼容旧版 JSP/表单字段：reviewerName / reviewerEmail。
     * 一些页面使用 rr.reviewerName / rr.reviewerEmail（EL 会去找 getReviewerName()/getReviewerEmail()）。
     * 但当前模型字段命名为 fullName / email。
     * 因此这里提供别名 getter/setter，避免 JSP 报 PropertyNotFoundException。
     */
    public String getReviewerName() {
        return getFullName();
    }

    public void setReviewerName(String reviewerName) {
        setFullName(reviewerName);
    }

    public String getReviewerEmail() {
        return getEmail();
    }

    public void setReviewerEmail(String reviewerEmail) {
        setEmail(reviewerEmail);
    }

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
