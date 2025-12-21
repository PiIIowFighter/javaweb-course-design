<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>编辑用户</h2>

<c:if test="${empty user}">
    <p>未找到该用户。</p>
    <p><a href="${pageContext.request.contextPath}/admin/users/list">返回列表</a></p>
</c:if>

<c:if test="${not empty user}">
    <form action="${pageContext.request.contextPath}/admin/users/update" method="post">
        <input type="hidden" name="userId" value="${user.userId}"/>

        <div>
            <label>用户ID：
                <input type="text" value="${user.userId}" readonly/>
            </label>
        </div>
        <div>
            <label>用户名：
                <input type="text" value="${user.username}" readonly/>
            </label>
        </div>
        <div>
            <label>姓名：
                <input type="text" name="fullName" value="${user.fullName}"/>
            </label>
        </div>
        <div>
            <label>邮箱：
                <input type="email" name="email" value="${user.email}"/>
            </label>
        </div>
        <div>
            <label>单位/机构：
                <input type="text" name="affiliation" value="${user.affiliation}"/>
            </label>
        </div>
        <div>
            <label>研究方向：
                <input type="text" name="researchArea" value="${user.researchArea}"/>
            </label>
        </div>
        <div>
            <label>角色：
                <select name="roleCode" required>
                    <c:forEach var="r" items="${roles}">
                        <c:if test="${r != 'SUPER_ADMIN'}">
                            <option value="${r}" <c:if test="${user.roleCode == r}">selected</c:if>>${r}</option>
                        </c:if>
                    </c:forEach>
                </select>
            </label>
        </div>
        <div>
            <label>状态：
                <select name="status">
                    <option value="ACTIVE" <c:if test="${user.status == 'ACTIVE'}">selected</c:if>>ACTIVE（正常）</option>
                    <option value="DISABLED" <c:if test="${user.status == 'DISABLED'}">selected</c:if>>DISABLED（禁用）</option>
                    <option value="LOCKED" <c:if test="${user.status == 'LOCKED'}">selected</c:if>>LOCKED（锁定）</option>
                </select>
            </label>
        </div>

        <div style="margin-top:12px;">
            <button type="submit">保存修改</button>
            <a href="${pageContext.request.contextPath}/admin/users/list">返回列表</a>
        </div>
    </form>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
