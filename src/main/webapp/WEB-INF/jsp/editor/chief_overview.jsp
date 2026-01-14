<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header" style="margin-bottom: 12px;">
        <div style="min-width: 0;">
            <div class="page-head">
        <h2 class="page-title">系统全览</h2>
</div></div>
    </div>

    <c:if test="${not empty manuscripts}">
        <div class="alert" style="margin: 0;">
            当前共 <strong><c:out value="${fn:length(manuscripts)}"/></strong> 篇稿件。
        </div>
    </c:if>

<c:if test="${empty manuscripts}">
    <div class="alert">系统内暂时没有任何稿件记录。</div>
</c:if>

<c:if test="${not empty manuscripts}">
    <div class="table-wrap" style="margin-top: 12px;">
        <table class="table-fixed" border="1" cellpadding="4" cellspacing="0">
            <thead>
            <tr>
                <th style="width: 140px;">稿件编号</th>
                <th>标题</th>
                <th style="width: 220px;">当前状态</th>
                <th style="width: 160px;">详情</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${manuscripts}" var="m">
                <tr>
                    <td><c:out value="${m.manuscriptId}"/></td>
                    <td class="cell-wrap"><c:out value="${m.title}"/></td>
                    <td><c:out value="${m.currentStatus}"/></td>
                    <td>
                        <a class="btn-primary" href="${ctx}/manuscripts/detail?id=${m.manuscriptId}">
                            <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i>
                            查看详情
                        </a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
