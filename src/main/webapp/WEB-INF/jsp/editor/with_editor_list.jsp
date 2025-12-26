<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>编辑处理中的稿件列表（WITH_EDITOR）</h2>

<c:if test="${empty manuscripts}">
    <p>当前没有由编辑处理中的稿件。</p>
</c:if>

<c:if test="${not empty manuscripts}">
<table border="1" cellpadding="6" cellspacing="0" width="100%">
    <thead>
    <tr>
        <th>稿件编号</th>
        <th>标题</th>
        <th>当前状态</th>
        <th>分配时间</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${manuscripts}" var="m">
        <tr>
            <td>${m.manuscriptId}</td>
            <td>${m.title}</td>
            <td>${m.status}</td>
            <td>
                <c:choose>
                    <c:when test="${m.assignedTime != null}">
                        ${m.assignedTime}
                    </c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td>
                <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">
                    查看详情
                </a>
                &nbsp;|&nbsp;
                <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}#inviteReviewers">
                    邀请审稿人
                </a>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>



