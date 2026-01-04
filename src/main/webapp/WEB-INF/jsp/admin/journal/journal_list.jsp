<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <h2 class="card-title">期刊管理（超级管理员）</h2>
            <p class="card-subtitle">按“板块”分别维护：期刊基本信息 / 关于期刊页面 / 卷期与专刊 / 征稿通知。</p>
        </div>
        <div class="admin-actions">
            <a href="${pageContext.request.contextPath}/admin/journals/basic/edit">➕ 新增期刊</a>
        </div>
    </div>

<c:if test="${empty journals}">
    <p>当前数据库没有期刊记录。你可以先点击“新增期刊”。</p>
</c:if>

<c:if test="${not empty journals}">
    <div class="table-wrap">
    <table class="table-fixed" border="1" cellpadding="6" cellspacing="0" style="min-width: 1240px;">
        <thead>
        <tr>
            <th style="width:70px;">ID</th>
            <th>期刊名称</th>
            <th style="width:150px;">ISSN</th>
            <th style="width:110px;">ImpactFactor</th>
            <th style="width:360px;">板块入口</th>
            <th style="width:200px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="j" items="${journals}">
            <tr>
                <td>${j.journalId}</td>
                <td><div class="cell-wrap"><c:out value="${j.name}"/></div></td>
                <td><div class="cell-wrap"><c:out value="${j.issn}"/></div></td>
                <td>
                    <c:choose>
                        <c:when test="${j.impactFactor != null}">${j.impactFactor}</c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <div class="link-pills">
                        <a href="${pageContext.request.contextPath}/admin/journals/basic/edit?journalId=${j.journalId}">基本信息</a>
                        <a href="${pageContext.request.contextPath}/admin/journals/pages/list?journalId=${j.journalId}">关于期刊页面</a>
                        <a href="${pageContext.request.contextPath}/admin/journals/issues/list?journalId=${j.journalId}">卷期 / 专刊</a>
                        <a href="${pageContext.request.contextPath}/admin/journals/calls/list?journalId=${j.journalId}">征稿通知</a>
                    </div>
                </td>
                <td>
                    <div class="admin-actions">
                        <a class="btn-primary" href="${pageContext.request.contextPath}/admin/journals/basic/edit?journalId=${j.journalId}">编辑</a>
                        <form method="post" action="${pageContext.request.contextPath}/admin/journals/basic/delete"
                              onsubmit="return confirm('确定删除该期刊？删除后相关页面/期次/征稿可能因外键约束失败。');"
                              style="display:inline;">
                            <input type="hidden" name="journalId" value="${j.journalId}"/>
                            <button type="submit">删除</button>
                        </form>
                    </div>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    </div>
</c:if>

</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
