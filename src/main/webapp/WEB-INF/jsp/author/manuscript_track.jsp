<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="edu.bjfu.onlinesm.model.ManuscriptStatusHistory" %>
<%@ page import="edu.bjfu.onlinesm.model.ManuscriptStageTimestamps" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%-- 定义完整的状态流程 --%>
<%
    // 完整的状态流程定义
    String[][] statusFlow = {
        {"DRAFT", "草稿编辑中", "bi-pencil-square"},
        {"SUBMITTED", "已提交待处理", "bi-send-check"},
        {"FORMAL_CHECK", "形式审查中", "bi-file-earmark-check"},
        {"DESK_REVIEW_INITIAL", "案头初筛", "bi-clipboard-check"},
        {"TO_ASSIGN", "待分配编辑", "bi-person-plus"},
        {"WITH_EDITOR", "编辑处理中", "bi-person-workspace"},
        {"UNDER_REVIEW", "外审进行中", "bi-search"},
        {"EDITOR_RECOMMENDATION", "编辑推荐意见", "bi-chat-square-text"},
        {"FINAL_DECISION_PENDING", "待主编终审", "bi-hourglass-split"}
    };
    
    // 终态
    String[][] finalStatuses = {
        {"ACCEPTED", "已录用", "bi-check-circle-fill"},
        {"REJECTED", "已退稿", "bi-x-circle-fill"},
        {"REVISION", "修回重审", "bi-arrow-repeat"},
        {"RETURNED", "退回待修改", "bi-arrow-counterclockwise"}
    };
    
    request.setAttribute("statusFlow", statusFlow);
    request.setAttribute("finalStatuses", finalStatuses);
    
    // 获取当前状态在流程中的位置
    String currentStatus = ((edu.bjfu.onlinesm.model.Manuscript)request.getAttribute("manuscript")).getCurrentStatus();
    int currentIndex = -1;
    boolean isFinalStatus = false;
    boolean isReturnedStatus = false;
    
    // 检查是否是退回状态（RETURNED需要特殊处理，回退到SUBMITTED阶段）
    if ("RETURNED".equals(currentStatus)) {
        isReturnedStatus = true;
        // RETURNED状态应该回退到SUBMITTED阶段（索引1）
        currentIndex = 1;
    } else {
        // 检查是否在正常流程中
        for (int i = 0; i < statusFlow.length; i++) {
            if (statusFlow[i][0].equals(currentStatus)) {
                currentIndex = i;
                break;
            }
        }
        
        // 检查是否是终态（ACCEPTED, REJECTED, REVISION）
        if ("ACCEPTED".equals(currentStatus) || "REJECTED".equals(currentStatus) || "REVISION".equals(currentStatus)) {
            isFinalStatus = true;
            currentIndex = statusFlow.length; // 表示已完成所有流程步骤
        }
    }
    
    request.setAttribute("currentIndex", currentIndex);
    request.setAttribute("isFinalStatus", isFinalStatus);
    request.setAttribute("isReturnedStatus", isReturnedStatus);
    request.setAttribute("currentStatusCode", currentStatus);
%>

<style>
/* 状态卡片 */
.status-card {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: var(--space-6);
    margin-bottom: var(--space-6);
}

.status-card-header {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    margin-bottom: var(--space-4);
}

.status-icon {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
}

.status-icon.processing {
    background: rgba(47, 111, 109, 0.12);
    color: var(--accent);
}

.status-icon.success {
    background: rgba(34, 197, 94, 0.12);
    color: #16a34a;
}

.status-icon.danger {
    background: rgba(239, 68, 68, 0.12);
    color: #dc2626;
}

.status-icon.warning {
    background: rgba(245, 158, 11, 0.12);
    color: #d97706;
}

.status-info h3 {
    margin: 0 0 4px 0;
    font-size: 14px;
    color: var(--muted);
    font-weight: normal;
}

.status-info .status-value {
    font-size: 20px;
    font-weight: 600;
    color: var(--text);
}

.cycle-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    background: rgba(47, 111, 109, 0.08);
    border: 1px solid rgba(47, 111, 109, 0.2);
    border-radius: 999px;
    font-size: 14px;
    color: var(--accent);
}

/* 完整时间线 */
.full-timeline {
    position: relative;
    padding: var(--space-4) 0;
}

.timeline-step {
    display: flex;
    align-items: flex-start;
    gap: var(--space-4);
    position: relative;
    padding-bottom: var(--space-5);
}

.timeline-step:last-child {
    padding-bottom: 0;
}

/* 连接线 */
.timeline-step::before {
    content: '';
    position: absolute;
    left: 19px;
    top: 40px;
    bottom: 0;
    width: 2px;
    background: var(--border);
}

.timeline-step:last-child::before {
    display: none;
}

.timeline-step.completed::before {
    background: #16a34a;
}

/* 节点圆点 */
.timeline-node {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 16px;
    border: 2px solid var(--border);
    background: var(--surface);
    color: var(--muted);
    z-index: 1;
}

.timeline-step.completed .timeline-node {
    background: var(--accent);
    border-color: var(--accent);
    color: #fff;
}

.timeline-step.current .timeline-node {
    background: var(--accent);
    border-color: var(--accent);
    color: #fff;
    animation: pulse-ring 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse-ring {
    0% {
        box-shadow: 0 0 0 0 rgba(0, 90, 156, 0.2), 
                    0 0 0 0 rgba(0, 90, 156, 0.15);
    }
    50% {
        box-shadow: 0 0 0 8px rgba(0, 90, 156, 0.1), 
                    0 0 0 16px rgba(0, 90, 156, 0.05);
    }
    100% {
        box-shadow: 0 0 0 12px rgba(0, 90, 156, 0), 
                    0 0 0 24px rgba(0, 90, 156, 0);
    }
}

.timeline-step.pending .timeline-node {
    background: #f3f4f6;
    border-color: #e5e7eb;
    color: #9ca3af;
}

/* 内容区 */
.timeline-content {
    flex: 1;
    padding-top: 8px;
}

.timeline-title {
    font-weight: 600;
    color: var(--text);
    margin-bottom: 2px;
}

.timeline-step.pending .timeline-title {
    color: var(--muted);
}

.timeline-status-code {
    font-size: 12px;
    color: var(--muted);
    font-family: ui-monospace, monospace;
}

.timeline-time {
    font-size: 13px;
    color: var(--muted);
    margin-top: 4px;
}

.timeline-time i {
    margin-right: 4px;
}

/* 终态分支 */
.final-status-section {
    margin-top: var(--space-6);
    padding-top: var(--space-6);
    border-top: 1px dashed var(--border);
}

.final-status-title {
    font-size: 14px;
    color: var(--muted);
    margin-bottom: var(--space-4);
}

.final-status-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: var(--space-3);
}

.final-status-item {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-sm);
    border: 1px solid var(--border);
    background: #f9fafb;
}

.final-status-item.active {
    border-color: #16a34a;
    background: rgba(34, 197, 94, 0.08);
}

.final-status-item.active.rejected {
    border-color: #dc2626;
    background: rgba(239, 68, 68, 0.08);
}

.final-status-item.active.revision {
    border-color: #d97706;
    background: rgba(245, 158, 11, 0.08);
}

.final-status-item i {
    font-size: 18px;
    color: var(--muted);
}

.final-status-item.active i {
    color: #16a34a;
}

.final-status-item.active.rejected i {
    color: #dc2626;
}

.final-status-item.active.revision i {
    color: #d97706;
}

.final-status-item span {
    font-size: 14px;
    color: var(--muted);
}

.final-status-item.active span {
    color: var(--text);
    font-weight: 500;
}

/* 标签页 */
.tab-buttons {
    display: inline-flex;
    gap: 0;
    margin-bottom: var(--space-6);
    border: 1px solid var(--border);
    border-bottom: none;
    border-radius: 0;
    background: var(--surface);
    overflow: visible;
    position: relative;
}

.tab-buttons::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 1px;
    background: var(--border);
    z-index: 1;
}

.tab-btn {
    padding: 12px 24px;
    border: none;
    border-right: 1px solid var(--border);
    background: transparent;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    color: var(--muted);
    transition: all 0.2s ease;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    position: relative;
    border-radius: 0;
}

.tab-btn:last-child {
    border-right: none;
}

.tab-btn:hover {
    color: var(--accent);
    background: rgba(0, 90, 156, 0.06);
}

.tab-btn.active {
    color: var(--accent);
    background: rgba(0, 90, 156, 0.1);
    font-weight: 600;
}

.tab-content {
    display: none;
}

.tab-content.active {
    display: block;
}

/* 历史记录表格 */
.history-table {
    width: 100%;
    border-collapse: collapse;
}

.history-table th,
.history-table td {
    padding: 12px;
    text-align: left;
    border-bottom: 1px solid var(--border);
}

.history-table th {
    background: #f9fafb;
    font-weight: 600;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--muted);
}

.history-table tr:hover {
    background: #f9fafb;
}

/* 状态标签 */
.status-tag {
    display: inline-block;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 500;
}

.status-tag.processing { background: rgba(47, 111, 109, 0.12); color: var(--accent); }
.status-tag.success { background: rgba(34, 197, 94, 0.12); color: #16a34a; }
.status-tag.warning { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.status-tag.danger { background: rgba(239, 68, 68, 0.12); color: #dc2626; }
.status-tag.neutral { background: #f3f4f6; color: #6b7280; }

/* 空状态 */
.empty-state {
    text-align: center;
    padding: var(--space-10);
    color: var(--muted);
}

.empty-state i {
    font-size: 48px;
    opacity: 0.4;
    margin-bottom: var(--space-4);
}
</style>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <h2 class="card-title">
                <i class="bi bi-clock-history" aria-hidden="true"></i>
                稿件状态追踪
            </h2>
            <p class="card-subtitle">
                稿件编号：<strong><c:out value="${manuscript.manuscriptId}"/></strong> |
                标题：<c:out value="${manuscript.title}"/>
            </p>
        </div>
        <div class="actions">
            <a class="btn-quiet" href="${ctx}/manuscripts/list">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                返回列表
            </a>
            <a class="btn-quiet" href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}">
                <i class="bi bi-eye" aria-hidden="true"></i>
                查看详情
            </a>
        </div>
    </div>

    <!-- 当前状态卡片 -->
    <div class="status-card">
        <div class="status-card-header">
            <div class="status-icon 
                <c:choose>
                    <c:when test="${manuscript.currentStatus == 'ACCEPTED'}">success</c:when>
                    <c:when test="${manuscript.currentStatus == 'REJECTED'}">danger</c:when>
                    <c:when test="${manuscript.currentStatus == 'REVISION' || manuscript.currentStatus == 'RETURNED'}">warning</c:when>
                    <c:otherwise>processing</c:otherwise>
                </c:choose>
            ">
                <i class="bi 
                    <c:choose>
                        <c:when test="${manuscript.currentStatus == 'ACCEPTED'}">bi-check-circle-fill</c:when>
                        <c:when test="${manuscript.currentStatus == 'REJECTED'}">bi-x-circle-fill</c:when>
                        <c:when test="${manuscript.currentStatus == 'REVISION'}">bi-arrow-repeat</c:when>
                        <c:when test="${manuscript.currentStatus == 'RETURNED'}">bi-arrow-counterclockwise</c:when>
                        <c:otherwise>bi-hourglass-split</c:otherwise>
                    </c:choose>
                "></i>
            </div>
            <div class="status-info">
                <h3>当前状态</h3>
                <div class="status-value">
                    <c:out value="${currentStatusDesc}"/>
                </div>
            </div>
        </div>
        <div class="cycle-badge">
            <i class="bi bi-clock"></i>
            预计周期：<c:out value="${estimatedCycle}"/>
        </div>
    </div>

    <!-- 标签页切换 -->
    <div class="tab-buttons">
        <button class="tab-btn active" onclick="showTab('timeline')">
            <i class="bi bi-diagram-3" aria-hidden="true"></i>
            流程进度
        </button>
        <button class="tab-btn" onclick="showTab('history')">
            <i class="bi bi-list-ul" aria-hidden="true"></i>
            变更历史
        </button>
    </div>

    <!-- 流程进度视图 -->
    <div id="timeline-tab" class="tab-content active">
        <div class="full-timeline">
            <c:forEach var="step" items="${statusFlow}" varStatus="idx">
                <div class="timeline-step 
                    <c:choose>
                        <c:when test="${isReturnedStatus && idx.index >= currentIndex}">pending</c:when>
                        <c:when test="${idx.index < currentIndex}">completed</c:when>
                        <c:when test="${idx.index == currentIndex && !isFinalStatus && !isReturnedStatus}">current</c:when>
                        <c:when test="${isFinalStatus}">completed</c:when>
                        <c:otherwise>pending</c:otherwise>
                    </c:choose>
                ">
                    <div class="timeline-node">
                        <c:choose>
                            <c:when test="${idx.index < currentIndex || isFinalStatus}">
                                <i class="bi bi-check"></i>
                            </c:when>
                            <c:when test="${idx.index == currentIndex && !isFinalStatus}">
                                <i class="bi ${step[2]}"></i>
                            </c:when>
                            <c:otherwise>
                                <i class="bi ${step[2]}"></i>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="timeline-content">
                        <div class="timeline-title">${step[1]}</div>
                        <div class="timeline-status-code">${step[0]}</div>
                        <%-- 显示该阶段的完成时间（从 stageTimestamps 获取） --%>
                        <%
                            String statusCode = (String)((String[])pageContext.getAttribute("step"))[0];
                            ManuscriptStageTimestamps timestamps = (ManuscriptStageTimestamps)request.getAttribute("stageTimestamps");
                            LocalDateTime completedAt = null;
                            if (timestamps != null) {
                                completedAt = timestamps.getCompletedAtByStatus(statusCode);
                            }
                            if (completedAt != null) {
                                String formattedTime = completedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        %>
                        <div class="timeline-time">
                            <i class="bi bi-check-circle"></i>
                            完成于 <%= formattedTime %>
                        </div>
                        <%
                            }
                        %>
                    </div>
                </div>
            </c:forEach>
        </div>

        <!-- 终态分支 -->
        <div class="final-status-section">
            <div class="final-status-title">最终决策</div>
            <div class="final-status-grid">
                <c:forEach var="fs" items="${finalStatuses}">
                    <div class="final-status-item 
                        <c:if test="${currentStatusCode == fs[0]}">active</c:if>
                        <c:if test="${fs[0] == 'REJECTED'}">rejected</c:if>
                        <c:if test="${fs[0] == 'REVISION' || fs[0] == 'RETURNED'}">revision</c:if>
                    ">
                        <i class="bi ${fs[2]}"></i>
                        <span>${fs[1]}</span>
                    </div>
                </c:forEach>
            </div>
        </div>
    </div>

    <!-- 变更历史视图 -->
    <div id="history-tab" class="tab-content">
        <c:choose>
            <c:when test="${empty historyList}">
                <div class="empty-state">
                    <i class="bi bi-inbox"></i>
                    <p>暂无状态变更记录</p>
                </div>
            </c:when>
            <c:otherwise>
                <table class="history-table">
                    <thead>
                        <tr>
                            <th>时间</th>
                            <th>事件</th>
                            <th>原状态</th>
                            <th>新状态</th>
                            <th>操作者</th>
                            <th>备注</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="h" items="${historyList}">
                            <tr>
                                <td>
                                    <c:if test="${h.changeTime != null}">
                                        <fmt:parseDate value="${h.changeTime}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedTime" type="both"/>
                                        <fmt:formatDate value="${parsedTime}" pattern="yyyy-MM-dd HH:mm:ss"/>
                                    </c:if>
                                </td>
                                <td>
                                    <%= ManuscriptStatusHistory.getEventDescription(((edu.bjfu.onlinesm.model.ManuscriptStatusHistory)pageContext.getAttribute("h")).getEvent()) %>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty h.fromStatus}">
                                            <span class="status-tag neutral">
                                                <%= ManuscriptStatusHistory.getStatusDescription(((edu.bjfu.onlinesm.model.ManuscriptStatusHistory)pageContext.getAttribute("h")).getFromStatus()) %>
                                            </span>
                                        </c:when>
                                        <c:otherwise>--</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <span class="status-tag 
                                        <c:choose>
                                            <c:when test="${h.toStatus == 'ACCEPTED'}">success</c:when>
                                            <c:when test="${h.toStatus == 'REJECTED'}">danger</c:when>
                                            <c:when test="${h.toStatus == 'REVISION' || h.toStatus == 'RETURNED'}">warning</c:when>
                                            <c:otherwise>processing</c:otherwise>
                                        </c:choose>
                                    ">
                                        <%= ManuscriptStatusHistory.getStatusDescription(((edu.bjfu.onlinesm.model.ManuscriptStatusHistory)pageContext.getAttribute("h")).getToStatus()) %>
                                    </span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty h.changedByFullName}">
                                            <c:out value="${h.changedByFullName}"/>
                                        </c:when>
                                        <c:when test="${not empty h.changedByUsername}">
                                            <c:out value="${h.changedByUsername}"/>
                                        </c:when>
                                        <c:otherwise>系统</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty h.remark}">
                                            <c:out value="${h.remark}"/>
                                        </c:when>
                                        <c:otherwise>--</c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<script>
function showTab(tabName) {
    // 隐藏所有标签内容
    document.querySelectorAll('.tab-content').forEach(function(tab) {
        tab.classList.remove('active');
    });
    // 移除所有按钮的激活状态
    document.querySelectorAll('.tab-btn').forEach(function(btn) {
        btn.classList.remove('active');
    });
    // 显示选中的标签内容
    document.getElementById(tabName + '-tab').classList.add('active');
    // 激活对应的按钮
    event.target.closest('.tab-btn').classList.add('active');
}
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
