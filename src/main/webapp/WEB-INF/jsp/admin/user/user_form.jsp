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
    <c:choose>
        <c:when test="${user.roleCode == 'SUPER_ADMIN'}">
            <div style="padding:8px 12px;border:1px solid #f2c200;background:#fff7d6;margin:10px 0;">
                <strong>提示：</strong>超级管理员（SUPER_ADMIN）为最高权限用户，不能被任何人修改。
            </div>
            <p>
                用户ID：${user.userId}，用户名：${user.username}，邮箱：${user.email}，状态：${user.status}
            </p>
            <p><a href="${pageContext.request.contextPath}/admin/users/list">返回列表</a></p>
        </c:when>
        <c:otherwise>
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
                        <!-- SUPER_ADMIN 永远不可选；SYSTEM_ADMIN 仅 SUPER_ADMIN 可选 -->
                        <c:choose>
                            <c:when test="${r == 'SUPER_ADMIN'}"></c:when>
                            <c:when test="${r == 'SYSTEM_ADMIN' && sessionScope.currentUser.roleCode != 'SUPER_ADMIN'}"></c:when>
                            <c:otherwise>
                                <option value="${r}" <c:if test="${user.roleCode == r}">selected</c:if>>${r}</option>
                            </c:otherwise>
                        </c:choose>
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
        </c:otherwise>
    </c:choose>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
