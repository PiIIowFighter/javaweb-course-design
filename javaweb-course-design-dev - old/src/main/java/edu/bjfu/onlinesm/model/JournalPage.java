package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 期刊页面内容：用于“关于期刊”等可配置页面。
 * 对应 dbo.JournalPages。
 */
public class JournalPage implements Serializable {

    private Integer pageId;
    private Integer journalId;
    private String pageKey;   // aims / policies / ...
    private String title;
    private String content;   // HTML（可包含 <p><ul> 等）

    // 资源（可选）：封面图 / 附件（存储文件名，实际文件在 D:/upload/journal/pages/ 下）
    private String coverImagePath;
    private String attachmentPath;

    private LocalDateTime updatedAt;

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }

    public Integer getJournalId() {
        return journalId;
    }

    public void setJournalId(Integer journalId) {
        this.journalId = journalId;
    }

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
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

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
