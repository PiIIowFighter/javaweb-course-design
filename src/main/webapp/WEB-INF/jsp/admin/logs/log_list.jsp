<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<h2>系统日志查询</h2>
<p>展示系统的操作日志（dbo.OperationLogs）。</p>

<form action="${pageContext.request.contextPath}/admin/logs/list" method="get" style="margin:12px 0;">
    <label>关键字：
        <input type="text" name="keyword" value="${keyword}" placeholder="用户名/模块/动作/详情"/>
    </label>
    <button type="submit">搜索</button>
</form>

<c:if test="${empty logs}">
    <p>暂无日志。</p>
</c:if>

<c:if test="${not empty logs}">
    <table border="1" cellpadding="6" cellspacing="0">
        <thead>
        <tr>
            <th>时间</th>
            <th>用户</th>
            <th>模块</th>
            <th>动作</th>
            <th>详情</th>
            <th>IP</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="l" items="${logs}">
            <tr>
                <td>${l.createdAt}</td>
                <td>${l.actorUsername}</td>
                <td>${l.module}</td>
                <td>${l.action}</td>
                <td>${l.detail}</td>
                <td>${l.ip}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
