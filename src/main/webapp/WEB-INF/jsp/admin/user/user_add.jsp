<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>新增用户</h2>
<p>
    通过此表单可以为系统创建<strong>后台内部账号</strong>（如系统管理员、主编、编辑、编辑部管理员等）。
    默认初始密码为 <strong>123456</strong>，请用户登录后尽快修改密码。
</p>
<p style="color:#666;">
    说明：作者/审稿人属于外部用户，一般应通过“注册”功能自行创建账号（后台不提供创建入口）。
    同时，只有<strong>超级管理员</strong>可以创建“系统管理员（SYSTEM_ADMIN）”账号。
</p>

<form action="${pageContext.request.contextPath}/admin/users/add" method="post">
    <div>
        <label>用户名：
            <input type="text" name="username" required/>
        </label>
    </div>
    <div>
        <label>姓名：
            <input type="text" name="fullName"/>
        </label>
    </div>
    <div>
        <label>邮箱：
            <input type="email" name="email"/>
        </label>
    </div>
    <div>
        <label>初始密码：
            <input type="text" name="password" value="123456"/>
        </label>
    </div>
    <div>
        <label>角色：
            <select name="roleCode" required>
                <option value="">-- 请选择角色 --</option>
                <c:forEach var="r" items="${roles}">
                    <!-- 不允许创建 SUPER_ADMIN；作者/审稿人通过注册产生；SYSTEM_ADMIN 仅 SUPER_ADMIN 可创建 -->
                    <c:choose>
                        <c:when test="${r == 'SUPER_ADMIN'}"></c:when>
                        <c:when test="${r == 'AUTHOR'}"></c:when>
                        <c:when test="${r == 'REVIEWER'}"></c:when>
                        <c:when test="${r == 'SYSTEM_ADMIN' && sessionScope.currentUser.roleCode != 'SUPER_ADMIN'}"></c:when>
                        <c:otherwise>
                            <option value="${r}">${r}</option>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </select>
        </label>
    </div>
    <div>
        <label>状态：
            <select name="status">
                <option value="ACTIVE">ACTIVE（正常）</option>
                <option value="DISABLED">DISABLED（禁用）</option>
                <option value="LOCKED">LOCKED（锁定）</option>
            </select>
        </label>
    </div>

    <div style="margin-top:12px;">
        <button type="submit">保存</button>
        <a href="${pageContext.request.contextPath}/admin/users/list">返回列表</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
