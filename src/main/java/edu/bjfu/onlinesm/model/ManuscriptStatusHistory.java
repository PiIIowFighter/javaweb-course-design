package edu.bjfu.onlinesm.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 稿件状态变更历史记录实体类
 * 对应数据库表 dbo.ManuscriptStatusHistory
 */
public class ManuscriptStatusHistory implements Serializable {

    private Long historyId;
    private Integer manuscriptId;
    private String fromStatus;
    private String toStatus;
    private String event;
    private Integer changedBy;
    private LocalDateTime changeTime;
    private String remark;
    
    /** 操作者用户名（关联查询） */
    private String changedByUsername;
    /** 操作者全名（关联查询） */
    private String changedByFullName;

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
    }

    public String getChangedByFullName() {
        return changedByFullName;
    }

    public void setChangedByFullName(String changedByFullName) {
        this.changedByFullName = changedByFullName;
    }

    /**
     * 获取状态的中文描述
     */
    public static String getStatusDescription(String status) {
        if (status == null) return "未知状态";
        switch (status) {
            case "DRAFT": return "草稿编辑中";
            case "SUBMITTED": return "已提交待处理";
            case "FORMAL_CHECK": return "形式审查中";
            case "FORMAT_CHECK": return "格式审查中";
            case "RETURNED": return "退回待修改";
            case "DESK_REVIEW_INITIAL": return "案头初筛";
            case "TO_ASSIGN": return "待分配编辑";
            case "WITH_EDITOR": return "编辑处理中";
            case "REVIEWER_ASSIGNED": return "审稿人已分配";
            case "UNDER_REVIEW": return "外审进行中";
            case "EDITOR_RECOMMENDATION": return "编辑推荐意见";
            case "FINAL_DECISION_PENDING": return "待主编终审";
            case "REVISION": return "修回重审";
            case "ACCEPTED": return "已录用";
            case "REJECTED": return "已退稿";
            case "ARCHIVED": return "已存档";
            default: return status;
        }
    }

    /**
     * 获取事件的中文描述
     */
    public static String getEventDescription(String event) {
        if (event == null) return "状态变更";
        switch (event) {
            case "SUBMIT": return "投稿提交";
            case "FORMAL_CHECK_PASS": return "形式审查通过";
            case "FORMAL_CHECK_FAIL": return "形式审查退回";
            case "DESK_ACCEPT": return "案头初筛通过";
            case "DESK_REJECT": return "案头初筛退稿";
            case "ASSIGN_EDITOR": return "分配责任编辑";
            case "INVITE_REVIEWER": return "邀请审稿人";
            case "REVIEWER_ACCEPT": return "审稿人接受邀请";
            case "REVIEWER_DECLINE": return "审稿人拒绝邀请";
            case "REVIEW_SUBMITTED": return "审稿意见提交";
            case "EDITOR_RECOMMEND": return "编辑提交推荐意见";
            case "FINAL_ACCEPT": return "终审录用";
            case "FINAL_REJECT": return "终审退稿";
            case "FINAL_REVISION": return "终审修回";
            case "RESUBMIT": return "作者重新提交";
            case "CHANGE_DESK_DECISION": return "更改初审决定";
            case "CHANGE_FINAL_DECISION": return "更改终审决定";
            case "RETRACT_PUBLISHED": return "撤稿";
            case "ARCHIVE": return "归档处理";
            default: return event;
        }
    }

    @Override
    public String toString() {
        return "ManuscriptStatusHistory{" +
                "historyId=" + historyId +
                ", manuscriptId=" + manuscriptId +
                ", fromStatus='" + fromStatus + '\'' +
                ", toStatus='" + toStatus + '\'' +
                ", event='" + event + '\'' +
                ", changeTime=" + changeTime +
                '}';
    }
}
