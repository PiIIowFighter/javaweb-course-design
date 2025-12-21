<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>审稿人库管理</h2>
<p>此页面由主编负责，用于维护期刊的审稿人库，包括新增审稿人账号以及封禁 / 解封现有审稿人。</p>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h3>现有审稿人列表</h3>
<c:if test="${empty reviewers}">
    <p>当前还没有任何审稿人账号。</p>
</c:if>
<c:if test="${not empty reviewers}">
    <table border="1" cellpadding="4" cellspacing="0">
        <thead>
        <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>单位 / 机构</th>
            <th>研究方向</th>
            <th>状态</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${reviewers}" var="r">
            <tr>
                <td><c:out value="${r.userId}"/></td>
                <td><c:out value="${r.username}"/></td>
                <td><c:out value="${r.fullName}"/></td>
                <td><c:out value="${r.email}"/></td>
                <td><c:out value="${r.affiliation}"/></td>
                <td><c:out value="${r.researchArea}"/></td>
                <td><c:out value="${r.status}"/></td>
                <td>
                    <form method="post" action="${ctx}/editor/reviewers" style="display:inline">
                        <input type="hidden" name="userId" value="${r.userId}"/>
                        <c:choose>
                            <c:when test="${r.status == 'ACTIVE'}">
                                <button type="submit" name="op" value="disable"
                                        onclick="return confirm('确认封禁该审稿人账号？');">
                                    封禁
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button type="submit" name="op" value="enable">
                                    解封
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<h3>新增审稿人账号</h3>
<form method="post" action="${ctx}/editor/reviewers">
    <input type="hidden" name="op" value="create"/>
    <table>
        <tr>
            <td>用户名 *</td>
            <td><input type="text" name="username" required/></td>
        </tr>
        <tr>
            <td>初始密码 *</td>
            <td><input type="password" name="password" required/></td>
        </tr>
        <tr>
            <td>姓名</td>
            <td><input type="text" name="fullName"/></td>
        </tr>
        <tr>
            <td>邮箱</td>
            <td><input type="email" name="email"/></td>
        </tr>
        <tr>
            <td>单位 / 机构</td>
            <td><input type="text" name="affiliation"/></td>
        </tr>
        <tr>
            <td>研究方向</td>
            <td><input type="text" name="researchArea"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">新增审稿人</button>
            </td>
        </tr>
    </table>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
