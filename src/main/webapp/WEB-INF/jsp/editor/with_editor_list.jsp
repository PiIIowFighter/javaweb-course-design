<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>编辑处理中的稿件列表（WITH_EDITOR）</h2>
<p>此页面用于展示当前责任编辑正在处理的稿件（状态：WITH_EDITOR）。</p>

<c:if test="${empty manuscripts}">
    <p>当前没有由编辑处理中的稿件。</p>
</c:if>
<c:if test="${not empty manuscripts}">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
    <tr>
        <th>稿件编号</th>
        <th>标题</th>
        <th>当前状态</th>
        <th>提交时间</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${manuscripts}" var="m">
        <tr>
            <td><c:out value="${m.manuscriptId}"/></td>
            <td><c:out value="${m.title}"/></td>
            <td><c:out value="${m.currentStatus}"/></td>
            <td>
                <c:choose>
                    <c:when test="${m.submitTime != null}">
                        <c:out value="${m.submitTime}"/>
                    </c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td>
                <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">查看详情</a>
                <c:if test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                    
                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
