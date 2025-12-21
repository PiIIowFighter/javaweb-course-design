package edu.bjfu.onlinesm.model;

import java.io.Serializable;

/**
 * 稿件作者信息：对应 dbo.ManuscriptAuthors。
 */
public class ManuscriptAuthor implements Serializable {

    private Integer authorId;
    private Integer manuscriptId;
    private Integer authorOrder;

    private String fullName;
    private String affiliation;
    private String degree;
    private String title;
    private String position;
    private String email;

    private boolean corresponding;

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public Integer getAuthorOrder() {
        return authorOrder;
    }

    public void setAuthorOrder(Integer authorOrder) {
        this.authorOrder = authorOrder;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isCorresponding() {
        return corresponding;
    }

    public void setCorresponding(boolean corresponding) {
        this.corresponding = corresponding;
    }
}
