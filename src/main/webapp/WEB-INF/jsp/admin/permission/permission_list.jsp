<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>权限管理</h2>

<p>为不同角色分配后台模块访问权限（URL 级别）。</p>

<c:if test="${readOnly}">
    <div style="padding:8px 12px;border:1px solid #f2c200;background:#fff7d6;margin:10px 0;">
        <strong>提示：</strong>SUPER_ADMIN 为最高权限角色，默认拥有全部权限，且不能被任何人修改。
    </div>
</c:if>

<c:if test="${msg == 'SUPER_ADMIN_READONLY'}">
    <div style="padding:8px 12px;border:1px solid #f2c200;background:#fff7d6;margin:10px 0;">
        已阻止对 SUPER_ADMIN 权限的修改。
    </div>
</c:if>

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
                    <input type="checkbox" name="perm" value="${p.key}"
                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                           <c:if test="${readOnly}">disabled</c:if>/>
                </td>
                <td>${p.name} <span style="color:#666">(${p.key})</span></td>
                <td>${p.description}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <div style="margin-top:12px;">
        <c:if test="${not readOnly}">
            <button type="submit">保存权限配置</button>
        </c:if>
        <a href="${pageContext.request.contextPath}/dashboard">返回工作台</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
