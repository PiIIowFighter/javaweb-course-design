<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<h2>征稿通知管理</h2>

<p>
    期刊：<b><c:out value="${journal.name}"/></b>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="${pageContext.request.contextPath}/admin/journals/list">返回期刊列表</a>
</p>

<p style="margin: 12px 0;">
    <a href="${pageContext.request.contextPath}/admin/journals/calls/edit?journalId=${journal.journalId}">➕ 新增征稿通知</a>
</p>

<c:if test="${empty calls}">
    <p>暂无征稿通知。</p>
</c:if>

<c:if test="${not empty calls}">
    <table border="1" cellpadding="6" cellspacing="0" style="background:#fff; width:100%; max-width: 1200px;">
        <thead>
        <tr>
            <th style="width:80px;">CallId</th>
            <th>标题</th>
            <th style="width:110px;">开始</th>
            <th style="width:110px;">截止</th>
            <th style="width:110px;">结束</th>
            <th style="width:90px;">发布</th>
            <th style="width:160px;">资源</th>
            <th style="width:200px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="c1" items="${calls}">
            <tr>
                <td>${c1.callId}</td>
                <td><c:out value="${c1.title}"/></td>
                <td><c:out value="${c1.startDate}"/></td>
                <td><c:out value="${c1.deadline}"/></td>
                <td><c:out value="${c1.endDate}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${c1.published}">✅</c:when>
                        <c:otherwise>❌</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:if test="${not empty c1.coverImagePath}">
                        <a href="${pageContext.request.contextPath}/journal/asset?type=call_cover&id=${c1.callId}" target="_blank">封面</a>
                    </c:if>
                    <c:if test="${not empty c1.attachmentPath}">
                        <c:if test="${not empty c1.coverImagePath}">&nbsp;|&nbsp;</c:if>
                        <a href="${pageContext.request.contextPath}/journal/asset?type=call_attachment&id=${c1.callId}" target="_blank">附件</a>
                    </c:if>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/admin/journals/calls/edit?journalId=${journal.journalId}&id=${c1.callId}">编辑</a>
                    &nbsp;|&nbsp;
                    <form method="post" action="${pageContext.request.contextPath}/admin/journals/calls/delete"
                          onsubmit="return confirm('确定删除该征稿通知？');" style="display:inline;">
                        <input type="hidden" name="journalId" value="${journal.journalId}"/>
                        <input type="hidden" name="id" value="${c1.callId}"/>
                        <button type="submit">删除</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>
