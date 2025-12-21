<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>权限管理</h2>

<p>为不同角色分配后台模块访问权限（URL 级别）。</p>

<form action="${pageContext.request.contextPath}/admin/permissions/list" method="get" style="margin: 12px 0;">
    <label>选择角色：
        <select name="roleCode" onchange="this.form.submit()">
            <c:forEach var="r" items="${roles}">
                <option value="${r}" <c:if test="${roleCode == r}">selected</c:if>>${r}</option>
            </c:forEach>
        </select>
    </label>
</form>

<form action="${pageContext.request.contextPath}/admin/permissions/save" method="post">
    <input type="hidden" name="roleCode" value="${roleCode}"/>
    <table border="1" cellpadding="6" cellspacing="0">
        <thead>
        <tr>
            <th>启用</th>
            <th>权限点</th>
            <th>说明</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${allPermissions}">
            <tr>
                <td>
                    <input type="checkbox" name="perm" value="${p.key}" <c:if test="${assigned contains p.key}">checked</c:if>/>
                </td>
                <td>${p.name} <span style="color:#666">(${p.key})</span></td>
                <td>${p.description}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <div style="margin-top:12px;">
        <button type="submit">保存权限配置</button>
        <a href="${pageContext.request.contextPath}/dashboard">返回工作台</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
