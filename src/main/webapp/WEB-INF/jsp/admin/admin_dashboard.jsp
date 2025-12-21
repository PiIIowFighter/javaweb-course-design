<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<h2>管理员工作台</h2>
<ul>
    <li><a href="${pageContext.request.contextPath}/admin/users/list">用户管理</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/permissions/list">权限管理</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/system/status">系统维护</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/logs/list">系统日志查询</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/journals/list">期刊管理</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/editorial/list">编辑委员会管理</a></li>
    <li><a href="${pageContext.request.contextPath}/admin/news/list">公告/新闻管理</a></li>
</ul>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
