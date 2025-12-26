package edu.bjfu.onlinesm.model;

import java.io.Serializable;

/**
 * 编辑委员会成员（前台展示用）。
 * 数据来自 dbo.EditorialBoard + dbo.Users。
 */
public class EditorialBoardMember implements Serializable {

    private Integer boardMemberId;
    private Integer userId;
    private Integer journalId;
    private String position;
    private String section;
    private String bio;

    private String fullName;
    private String affiliation;
    private String email;

    /** 关联的系统账号信息（用于后台编委会管理列表展示） */
    private String username;
    private String roleCode;

    public Integer getBoardMemberId() {
        return boardMemberId;
    }

    public void setBoardMemberId(Integer boardMemberId) {
        this.boardMemberId = boardMemberId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
