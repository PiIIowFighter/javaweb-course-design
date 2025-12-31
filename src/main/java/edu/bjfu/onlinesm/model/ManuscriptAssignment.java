package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 与 SQL Server 中 dbo.ManuscriptAssignments 表对应的实体类。
 */
public class ManuscriptAssignment implements Serializable {

    private Integer assignmentId;
    private Integer manuscriptId;
    private Integer editorId;
    private Integer assignedByChiefId;
    private String chiefComment;
    private LocalDateTime assignedTime;

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public Integer getEditorId() {
        return editorId;
    }

    public void setEditorId(Integer editorId) {
        this.editorId = editorId;
    }

    public Integer getAssignedByChiefId() {
        return assignedByChiefId;
    }

    public void setAssignedByChiefId(Integer assignedByChiefId) {
        this.assignedByChiefId = assignedByChiefId;
    }

    public String getChiefComment() {
        return chiefComment;
    }

    public void setChiefComment(String chiefComment) {
        this.chiefComment = chiefComment;
    }

    public LocalDateTime getAssignedTime() {
        return assignedTime;
    }

    public void setAssignedTime(LocalDateTime assignedTime) {
        this.assignedTime = assignedTime;
    }

    @Override
    public String toString() {
        return "ManuscriptAssignment{" +
                "assignmentId=" + assignmentId +
                ", manuscriptId=" + manuscriptId +
                ", editorId=" + editorId +
                ", assignedByChiefId=" + assignedByChiefId +
                ", chiefComment='" + chiefComment + '\'' +
                ", assignedTime=" + assignedTime +
                '}';
    }
}
