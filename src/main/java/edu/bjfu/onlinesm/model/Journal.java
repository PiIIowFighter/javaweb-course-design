package edu.bjfu.onlinesm.model;

import java.io.Serializable;

/**
 * 期刊实体：对应 dbo.Journals。
 */
public class Journal implements Serializable {

    private Integer journalId;
    private String name;

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

    @Override
    public String toString() {
        return "Journal{" +
                "journalId=" + journalId +
                ", name='" + name + '\'' +
                '}';
    }
}
