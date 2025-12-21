package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 稿件版本：对应 dbo.ManuscriptVersions。
 */
public class ManuscriptVersion implements Serializable {

    private Integer versionId;
    private Integer manuscriptId;
    private Integer versionNumber;
    private boolean current;

    private String fileAnonymousPath;
    private String fileOriginalPath;
    private String coverLetterPath;
    private String responseLetterPath;

    private LocalDateTime createdAt;
    private Integer createdBy;
    private String remark;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getFileAnonymousPath() {
        return fileAnonymousPath;
    }

    public void setFileAnonymousPath(String fileAnonymousPath) {
        this.fileAnonymousPath = fileAnonymousPath;
    }

    public String getFileOriginalPath() {
        return fileOriginalPath;
    }

    public void setFileOriginalPath(String fileOriginalPath) {
        this.fileOriginalPath = fileOriginalPath;
    }

    public String getCoverLetterPath() {
        return coverLetterPath;
    }

    public void setCoverLetterPath(String coverLetterPath) {
        this.coverLetterPath = coverLetterPath;
    }

    public String getResponseLetterPath() {
        return responseLetterPath;
    }

    public void setResponseLetterPath(String responseLetterPath) {
        this.responseLetterPath = responseLetterPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
