package edu.bjfu.onlinesm.model;

import java.io.Serializable;

/**
 * 期刊实体：对应 dbo.Journals。
 */
public class Journal implements Serializable {

    private Integer journalId;
    private String name;
    private String description;
    private Double impactFactor;
    private String timeline;
    private String issn;

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getImpactFactor() {
        return impactFactor;
    }

    public void setImpactFactor(Double impactFactor) {
        this.impactFactor = impactFactor;
    }

    public String getTimeline() {
        return timeline;
    }

    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    @Override
    public String toString() {
        return "Journal{" +
                "journalId=" + journalId +
                ", name='" + name + '\'' +
                ", description='" + (description != null ? (description.length() > 40 ? description.substring(0, 40) + "..." : description) : null) + '\'' +
                ", impactFactor=" + impactFactor +
                ", timeline='" + timeline + '\'' +
                ", issn='" + issn + '\'' +
                '}';
    }
}
