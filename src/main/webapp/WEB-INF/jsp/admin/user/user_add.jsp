<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>新增用户</h2>
<p>通过此表单可以为系统创建新用户。默认初始密码为 <strong>123456</strong>，请用户登录后尽快修改密码。</p>

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
                    <c:if test="${r != 'SUPER_ADMIN'}">
                        <option value="${r}">${r}</option>
                    </c:if>
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
