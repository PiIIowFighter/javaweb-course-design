<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>用户列表</h2>
<p>此页面展示系统用户信息，并支持按角色筛选、编辑、删除、禁用等操作。</p>

<p>
    <a href="${pageContext.request.contextPath}/admin/users/add">+ 新增用户</a>
</p>

<form action="${pageContext.request.contextPath}/admin/users/list" method="get" style="margin: 12px 0;">
    <label>按角色筛选：
        <select name="roleCode">
            <option value="ALL" <c:if test="${selectedRole == 'ALL'}">selected</c:if>>全部</option>
            <c:forEach var="r" items="${roles}">
                <option value="${r}" <c:if test="${selectedRole == r}">selected</c:if>>${r}</option>
            </c:forEach>
        </select>
    </label>
    <button type="submit">筛选</button>
</form>

<c:if test="${empty users}">
    <p>当前暂无用户数据。</p>
</c:if>

<c:if test="${not empty users}">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
        <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
        </tr>
    </thead>
    <tbody>
    <c:forEach var="u" items="${users}">
        <tr>
            <td>${u.userId}</td>
            <td>${u.username}</td>
            <td>${u.fullName}</td>
            <td>${u.email}</td>
            <td>${u.roleCode}</td>
            <td>${u.status}</td>
            <td>
                <a href="${pageContext.request.contextPath}/admin/users/edit?userId=${u.userId}">编辑</a>
                <span style="color:#999"> | </span>

                <!-- 封禁 / 解封 -->
                <c:choose>
                    <c:when test="${u.status == 'ACTIVE'}">
                        <form action="${pageContext.request.contextPath}/admin/users/status" method="post" style="display:inline;">
                            <input type="hidden" name="userId" value="${u.userId}"/>
                            <input type="hidden" name="targetStatus" value="DISABLED"/>
                            <button type="submit">封禁</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <form action="${pageContext.request.contextPath}/admin/users/status" method="post" style="display:inline;">
                            <input type="hidden" name="userId" value="${u.userId}"/>
                            <input type="hidden" name="targetStatus" value="ACTIVE"/>
                            <button type="submit">解封</button>
                        </form>
                    </c:otherwise>
                </c:choose>

                <!-- 重置密码 -->
                <form action="${pageContext.request.contextPath}/admin/users/resetPassword" method="post" style="display:inline;margin-left:4px;">
                    <input type="hidden" name="userId" value="${u.userId}"/>
                    <button type="submit">重置密码为 123456</button>
                </form>

                <span style="color:#999"> | </span>
                <!-- 删除（不建议频繁使用，课程设计阶段提供） -->
                <form action="${pageContext.request.contextPath}/admin/users/delete" method="post" style="display:inline;" onsubmit="return confirm('确定删除该用户吗？此操作不可恢复');">
                    <input type="hidden" name="userId" value="${u.userId}"/>
                    <button type="submit">删除</button>
                </form>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
