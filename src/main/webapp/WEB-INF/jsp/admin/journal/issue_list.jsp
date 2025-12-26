<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<h2>卷期 / 专刊管理</h2>

<p>
    期刊：<b><c:out value="${journal.name}"/></b>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="${pageContext.request.contextPath}/admin/journals/list">返回期刊列表</a>
</p>

<p style="margin: 12px 0;">
    <a href="${pageContext.request.contextPath}/admin/journals/issues/edit?journalId=${journal.journalId}">➕ 新增期次/专刊</a>
</p>

<c:if test="${empty issues}">
    <p>暂无期次/专刊记录。</p>
</c:if>

<c:if test="${not empty issues}">
    <table border="1" cellpadding="6" cellspacing="0" style="background:#fff; width:100%; max-width: 1200px;">
        <thead>
        <tr>
            <th style="width:80px;">IssueId</th>
            <th style="width:90px;">类型</th>
            <th>标题</th>
            <th style="width:80px;">卷</th>
            <th style="width:80px;">期</th>
            <th style="width:80px;">年</th>
            <th style="width:90px;">发布</th>
            <th style="width:120px;">发布日期</th>
            <th style="width:160px;">资源</th>
            <th style="width:200px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="i" items="${issues}">
            <tr>
                <td>${i.issueId}</td>
                <td><c:out value="${i.issueType}"/></td>
                <td><c:out value="${i.title}"/></td>
                <td><c:out value="${i.volume}"/></td>
                <td><c:out value="${i.number}"/></td>
                <td><c:out value="${i.year}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${i.published}">✅</c:when>
                        <c:otherwise>❌</c:otherwise>
                    </c:choose>
                </td>
                <td><c:out value="${i.publishDate}"/></td>
                <td>
                    <c:if test="${not empty i.coverImagePath}">
                        <a href="${pageContext.request.contextPath}/journal/asset?type=issue_cover&id=${i.issueId}" target="_blank">封面</a>
                    </c:if>
                    <c:if test="${not empty i.attachmentPath}">
                        <c:if test="${not empty i.coverImagePath}">&nbsp;|&nbsp;</c:if>
                        <a href="${pageContext.request.contextPath}/journal/asset?type=issue_attachment&id=${i.issueId}" target="_blank">附件</a>
                    </c:if>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/admin/journals/issues/edit?journalId=${journal.journalId}&id=${i.issueId}">编辑</a>
                    &nbsp;|&nbsp;
                    <form method="post" action="${pageContext.request.contextPath}/admin/journals/issues/delete"
                          onsubmit="return confirm('确定删除该期次/专刊？');" style="display:inline;">
                        <input type="hidden" name="journalId" value="${journal.journalId}"/>
                        <input type="hidden" name="id" value="${i.issueId}"/>
                        <button type="submit">删除</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>
