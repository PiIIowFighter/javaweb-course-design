<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>系统运行状态</h2>

<p style="color:#666;">
    系统管理员/超级管理员可在此查看运行环境与数据库连通性，并通过“日志查询”对所有操作进行审计和排查故障。
</p>

<table border="1" cellpadding="6" cellspacing="0">
    <tr><th>当前时间</th><td>${now}</td></tr>
    <tr><th>Java 版本</th><td>${javaVersion}</td></tr>
    <tr><th>操作系统</th><td>${osName}</td></tr>
    <tr><th>时区</th><td>${userTimeZone}</td></tr>
    <tr><th>CPU 核心数</th><td>${processors}</td></tr>
    <tr><th>JVM 最大内存</th><td>${maxMemory}</td></tr>
    <tr><th>JVM 已分配内存</th><td>${totalMemory}</td></tr>
    <tr><th>JVM 可用内存</th><td>${freeMemory}</td></tr>
    <tr>
        <th>数据库连通性</th>
        <td>
            <c:choose>
                <c:when test="${dbOk}"><span style="color:green;">OK</span></c:when>
                <c:otherwise><span style="color:red;">FAIL</span> ${dbError}</c:otherwise>
            </c:choose>
        </td>
    </tr>
</table>

<h3 style="margin-top:20px;">最近操作日志</h3>
<c:if test="${empty recentLogs}">
    <p>暂无操作日志。</p>
</c:if>
<c:if test="${not empty recentLogs}">
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
        <c:forEach var="l" items="${recentLogs}">
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

<p style="margin-top:12px;">
    <a href="${pageContext.request.contextPath}/admin/logs/list">进入日志查询（支持时间范围/用户/模块筛选）</a>
    <span style="color:#999;"> | </span>
    <a href="${pageContext.request.contextPath}/dashboard">返回工作台</a>
</p>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
