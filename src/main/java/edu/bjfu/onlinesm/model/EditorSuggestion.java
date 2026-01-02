package edu.bjfu.onlinesm.model;

import java.time.LocalDateTime;

/**
 * 编辑对稿件的处理建议（无最终决策权，需主编批准）。
 */
public class EditorSuggestion {

    private int manuscriptId;
    private int editorId;
    private String suggestion; // ACCEPT / MINOR_REVISION / MAJOR_REVISION / REJECT
    private String summary;    // 总结报告
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    // 展示用（可选）
    private String editorName;

    public int getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(int manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public int getEditorId() {
        return editorId;
    }

    public void setEditorId(int editorId) {
        this.editorId = editorId;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEditorName() {
        return editorName;
    }

    public void setEditorName(String editorName) {
        this.editorName = editorName;
    }
}
