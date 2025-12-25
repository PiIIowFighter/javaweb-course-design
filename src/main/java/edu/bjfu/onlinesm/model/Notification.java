package edu.bjfu.onlinesm.model;

import java.time.LocalDateTime;

/**
 * 站内通知（单向）。
 *
 * 不做对话串/回复，仅用于“通知中心”。
 */
public class Notification {

    private Integer notificationId;
    private Integer recipientUserId;
    private Integer createdByUserId;

    /** SYSTEM / MANUAL */
    private String type;
    /** SUBMISSION / ASSIGN / REVIEW / DECISION / ADMIN ... */
    private String category;

    private String title;
    private String content;
    private Integer relatedManuscriptId;

    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getRelatedManuscriptId() {
        return relatedManuscriptId;
    }

    public void setRelatedManuscriptId(Integer relatedManuscriptId) {
        this.relatedManuscriptId = relatedManuscriptId;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
