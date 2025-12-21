<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>编辑委员会管理</h2>
<p>维护期刊的主编、副主编、编委等成员信息（dbo.EditorialBoard）。</p>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div style="margin: 12px 0;">
    <div style="margin-bottom: 8px;">
        期刊：<b><c:out value="${not empty primaryJournal ? primaryJournal.name : '（未配置期刊）'}"/></b>
    </div>

    <a href="${ctx}/admin/editorial/add">
        <i class="bi bi-plus-circle"></i> 新增成员
    </a>
    <a style="margin-left: 12px;" href="${ctx}/dashboard">返回工作台</a>
</div>

<c:if test="${empty members}">
    <p>当前期刊暂无编委会成员记录。</p>
</c:if>

<c:if test="${not empty members}">
    <table border="1" cellpadding="6" cellspacing="0" style="width: 100%;">
        <thead>
        <tr>
            <th>ID</th>
            <th>成员</th>
            <th>系统角色</th>
            <th>职务</th>
            <th>栏目/分工</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="m" items="${members}">
            <tr>
                <td>${m.boardMemberId}</td>
                <td>
                    <c:choose>
                        <c:when test="${not empty m.fullName}">${m.fullName}</c:when>
                        <c:otherwise>${m.username}</c:otherwise>
                    </c:choose>
                    <span style="color:#666;">(${m.username})</span>
                </td>
                <td>${m.roleCode}</td>
                <td>${m.position}</td>
                <td>${m.section}</td>
                <td>
                    <a href="${pageContext.request.contextPath}/admin/editorial/edit?id=${m.boardMemberId}">
                        <i class="bi bi-pencil-square"></i> 编辑
                    </a>
                    <form action="${pageContext.request.contextPath}/admin/editorial/delete" method="post" style="display:inline; margin-left: 8px;">
                        <input type="hidden" name="boardMemberId" value="${m.boardMemberId}"/>
                        <button type="submit" onclick="return confirm('确定删除该成员吗？');">
                            <i class="bi bi-trash"></i> 删除
                        </button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
