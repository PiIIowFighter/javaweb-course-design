<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>数据库维护</h2>

<p style="color:#666;">
    本页面用于<strong>系统管理员/超级管理员</strong>手动检查并修复课程设计中用到的关键表结构，
    例如角色权限表 <code>RolePermissions</code>、编委会表 <code>EditorialBoard</code>、
    操作日志表 <code>OperationLogs</code> 等。所有操作都会记录到“操作日志”中。
</p>

<table border="1" cellpadding="6" cellspacing="0">
    <tr>
        <th>数据库连通性</th>
        <td>
            <c:choose>
                <c:when test="${dbOk}">
                    <span style="color:green;">OK</span>
                </c:when>
                <c:otherwise>
                    <span style="color:red;">FAIL</span>
                    <c:if test="${not empty dbError}">
                        ${dbError}
                    </c:if>
                </c:otherwise>
            </c:choose>
        </td>
    </tr>
</table>

<c:if test="${not empty message}">
    <div style="margin-top:12px;padding:8px 12px;border:1px solid #cce5ff;background:#e8f3ff;color:#004085;">
        ${message}
    </div>
</c:if>

<c:if test="${not empty error}">
    <div style="margin-top:12px;padding:8px 12px;border:1px solid #f5c6cb;background:#f8d7da;color:#721c24;">
        ${error}
    </div>
</c:if>

<h3 style="margin-top:20px;">数据浏览 / 修改</h3>

<p style="color:#666;">
    下方首先列出当前数据库中的所有用户表名称。点击某个表名即可浏览该表中的数据，
    并在需要时直接修改某一行后点击“保存本行”提交更新。
</p>

<c:if test="${empty tableNames}">
    <p>未能读取到任何数据表，请检查数据库连接或数据库用户权限。</p>
</c:if>

<c:if test="${not empty tableNames}">
    <ul>
        <c:forEach items="${tableNames}" var="tb">
            <li>
                <a href="${pageContext.request.contextPath}/admin/system/db?table=${tb}">
                    <c:out value="${tb}"/>
                </a>
                <c:if test="${tb == selectedTable}">
                    <span style="color:#999;">（当前）</span>
                </c:if>
            </li>
        </c:forEach>
    </ul>
</c:if>

<c:if test="${not empty selectedTable}">
    <h3 style="margin-top:20px;">表 <code>${selectedTable}</code> 的数据</h3>

    <c:if test="${empty columnNames}">
        <p>未能获取该表的列信息，或者该表目前没有任何数据。</p>
    </c:if>

    <c:if test="${not empty columnNames}">
        <p style="color:#666;">
            提示：每一行都是一个独立的表单，修改需要的字段后点击“保存本行”即可更新数据库。
        </p>

        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff;">
            <thead>
            <tr>
                <th>操作</th>
                <c:forEach items="${columnNames}" var="col">
                    <th><c:out value="${col}"/></th>
                </c:forEach>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${rows}" var="row">
                <form action="${pageContext.request.contextPath}/admin/system/db" method="post">
                    <tr>
                        <td>
                            <input type="hidden" name="action" value="updateRow"/>
                            <input type="hidden" name="table" value="${selectedTable}"/>
                            <c:forEach items="${pkColumns}" var="pk">
                                <input type="hidden" name="pk_${pk}" value="${row[pk]}"/>
                            </c:forEach>
                            <button type="submit">保存本行</button>
                        </td>
                        <c:forEach items="${columnNames}" var="col">
                            <td>
                                <input type="text"
                                       name="col_${col}"
                                       value="${row[col]}"
                                       style="width:120px;"/>
                            </td>
                        </c:forEach>
                    </tr>
                </form>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
</c:if>

<p style="margin-top:16px;">
    <a href="${pageContext.request.contextPath}/admin/system/status">返回系统状态</a>
    <span style="color:#999;"> | </span>
    <a href="${pageContext.request.contextPath}/dashboard">返回工作台</a>
</p>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
