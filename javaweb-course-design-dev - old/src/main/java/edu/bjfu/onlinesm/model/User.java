package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 与 SQL Server 中 dbo.Users 表大致对应的实体类。
 * 这里只保留课程设计当前阶段需要用到的字段，
 * 方便后续替换为真正的 DAO / Service 实现。
 */
public class User implements Serializable {

    private Integer userId;
    /** 登录名，对应 dbo.Users.Username */
    private String username;
    /** 明文/散列密码，当前阶段直接使用明文，后续可替换为加密存储 */
    private String passwordHash;
    private String email;
    private String fullName;
    /** 单位/机构 */
    private String affiliation;
    /** 研究方向 */
    private String researchArea;
    /** 角色代码，例如：SUPER_ADMIN / SYSTEM_ADMIN / AUTHOR / REVIEWER / EDITOR / EDITOR_IN_CHIEF */
    private String roleCode;
    /** 账号状态：ACTIVE / DISABLED / LOCKED 等 */
    private String status;
    /** 注册时间（可为空，后续与数据库同步时再使用） */
    private LocalDateTime registerTime;

    public User() {
    }

    public User(Integer userId, String username, String passwordHash, String email,
                String fullName, String affiliation, String researchArea,
                String roleCode, String status, LocalDateTime registerTime) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.affiliation = affiliation;
        this.researchArea = researchArea;
        this.roleCode = roleCode;
        this.status = status;
        this.registerTime = registerTime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getResearchArea() {
        return researchArea;
    }

    public void setResearchArea(String researchArea) {
        this.researchArea = researchArea;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roleCode='" + roleCode + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
