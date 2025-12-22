<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>待分配审稿人稿件列表（TO_ASSIGN）</h2>
<p>此页面展示已经通过案头处理、等待主编/编辑分配外审专家的稿件（状态：TO_ASSIGN）。</p>

<c:if test="${empty manuscripts}">
    <p>当前没有等待分配审稿人的稿件。</p>
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
                    <form method="post" action="${pageContext.request.contextPath}/editor/toAssign" style="display:inline">
                        <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                        <select name="editorId">
                            <c:forEach items="${editorList}" var="e">
                                <option value="${e.userId}">
                                    <c:out value="${e.fullName}"/>（<c:out value="${e.username}"/>）
                                </option>
                            </c:forEach>
                        </select>
                        <button type="submit" name="op" value="assign">
                            指派责任编辑
                        </button>
                    </form>
                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
