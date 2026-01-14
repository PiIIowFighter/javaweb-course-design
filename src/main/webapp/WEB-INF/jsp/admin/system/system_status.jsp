<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="page-head">
        <h2 class="page-title">系统运行状态</h2>
</div><table border="1" cellpadding="6" cellspacing="0">
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
<style>
    /* page-only: keep recent logs within viewport */
    .status-log-wrap{max-width:100%; overflow-x:auto;}
    .status-log-wrap::-webkit-scrollbar{height:8px;}
    .status-log-table{min-width:980px; width:100%;}
    .status-log-detail{max-width:520px;}
    .status-log-detail .clamp{display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; word-break:break-word; white-space:normal;}
</style>
<c:if test="${empty recentLogs}">
    <p>暂无操作日志。</p>
</c:if>
<c:if test="${not empty recentLogs}">
    <div class="status-log-wrap">
    <table border="1" cellpadding="6" cellspacing="0" class="status-log-table">
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
                <td class="status-log-detail" title="${l.detail}"><div class="clamp">${l.detail}</div></td>
                <td>${l.ip}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</c:if>

<p style="margin-top:12px;">
    <a href="${pageContext.request.contextPath}/admin/logs/list">进入日志查询（支持时间范围/用户/模块筛选）</a>
    <span style="color:#999;"> | </span>
    <a href="${pageContext.request.contextPath}/dashboard">返回工作台</a>
</p>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
