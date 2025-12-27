<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">审稿监控 / 催审面板</h2>
            <p class="card-subtitle">
                查看逾期审稿任务，并手动或自动发送催审提醒邮件。
            </p>
        </div>
    </div>

    <div class="stack">
        <form method="get" action="${pageContext.request.contextPath}/editor/review/monitor" class="stack stack-horizontal">
            <label>逾期天数：
                <input type="number" name="overdueDays" min="0" style="width:80px;"
                       value="${monitorOverdueDays}"/>
            </label>
            <label>催审冷却期（天）：
                <input type="number" name="cooldownDays" min="0" style="width:80px;"
                       value="${monitorCooldownDays}"/>
            </label>
            <label>最大记录数：
                <input type="number" name="limit" min="1" style="width:80px;"
                       value="${monitorLimit}"/>
            </label>
            <button type="submit">刷新列表</button>
        </form>

        <form method="post" action="${pageContext.request.contextPath}/editor/review/autoRemindNow">
            <input type="hidden" name="overdueDays" value="${monitorOverdueDays}"/>
            <input type="hidden" name="cooldownDays" value="${monitorCooldownDays}"/>
            <input type="hidden" name="limit" value="${monitorLimit}"/>
            <button type="submit" onclick="return confirm('确认根据当前规则执行一次自动催审？');">
                立即执行自动催审
            </button>
        </form>

        <c:if test="${not empty monitorMessage}">
            <div class="alert alert-info" style="margin-top:8px;">
                <c:out value="${monitorMessage}"/>
            </div>
        </c:if>
    </div>

    <c:if test="${empty overdueReviews}">
        <p style="margin-top:12px;">当前没有符合条件的逾期审稿任务。</p>
    </c:if>

    <c:if test="${not empty overdueReviews}">
        <table border="1" cellpadding="4" cellspacing="0" style="margin-top:12px; background:#fff; width:100%;">
            <thead>
            <tr>
                <th>审稿ID</th>
                <th>稿件ID</th>
                <th>稿件标题</th>
                <th>审稿人ID</th>
                <th>状态</th>
                <th>邀请时间</th>
                <th>截止时间</th>
                <th>最后催审时间</th>
                <th>催审次数</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${overdueReviews}" var="r">
                <tr>
                    <td><c:out value="${r.reviewId}"/></td>
                    <td><c:out value="${r.manuscriptId}"/></td>
                    <td>
                        <c:out value="${monitorTitles[r.reviewId]}"/>
                    </td>
                    <td><c:out value="${r.reviewerId}"/></td>
                    <td><c:out value="${r.status}"/></td>
                    <td><c:out value="${r.invitedAt}"/></td>
                    <td><c:out value="${r.dueAt}"/></td>
                    <td><c:out value="${r.lastRemindedAt}"/></td>
                    <td><c:out value="${r.remindCount}"/></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/editor/review/remindForm?reviewId=${r.reviewId}&back=monitor">
                            手动催审
                        </a>
                        |
                        <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${r.manuscriptId}" target="_blank">
                            查看稿件
                        </a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
