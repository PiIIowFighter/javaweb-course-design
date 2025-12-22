package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 期刊卷期 / 专刊（dbo.Issues）。
 */
public class Issue implements Serializable {

    private Integer issueId;
    private Integer journalId;
    private String issueType; // LATEST / SPECIAL

    private String title;
    private Integer volume;
    private Integer number;
    private Integer year;

    private String guestEditors;
    private String description;

    private Boolean published;
    private LocalDate publishDate;

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
    }

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGuestEditors() {
        return guestEditors;
    }

    public void setGuestEditors(String guestEditors) {
        this.guestEditors = guestEditors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }
}
