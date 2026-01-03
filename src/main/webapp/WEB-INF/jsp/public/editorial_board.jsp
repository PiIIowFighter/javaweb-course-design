<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="hero stack">
    <h1>期刊编委会</h1>
    <c:if test="${journal != null}">
        <p><c:out value="${journal.name}"/> · 编委成员信息来自 dbo.EditorialBoard + dbo.Users</p>
    </c:if>
</div>

<div class="card stack" style="margin-top: var(--space-6);">
    <div class="card-header">
        <div>
            <h2 class="card-title">编委会成员</h2>
            <p class="card-subtitle">展示完整名单与简介</p>
        </div>
        <a href="${ctx}/" style="white-space:nowrap; text-decoration:none;">
            返回首页 <i class="bi bi-arrow-right" aria-hidden="true"></i>
        </a>
    </div>

    <c:if test="${empty boardMembers}">
        <p>暂无编委会成员数据（dbo.EditorialBoard 为空）。</p>
    </c:if>

    <c:if test="${not empty boardMembers}">
        <ul class="list">
            <c:forEach var="m" items="${boardMembers}">
                <li class="list-item">
                    <span class="avatar" aria-hidden="true" style="overflow:hidden;">
                        <img src="${ctx}/public/avatar?userId=${m.userId}"
                             alt="avatar"
                             style="width:100%;height:100%;object-fit:cover;display:block;"/>
                    </span>
                    <div style="flex:1;">
                        <div class="list-title">
                            <c:out value="${m.fullName}"/>
                            <c:if test="${not empty m.position}">
                                <span class="badge" style="margin-left:10px;"><c:out value="${m.position}"/></span>
                            </c:if>
                        </div>
                        <div class="list-meta">
                            <c:out value="${m.affiliation}"/>
                            <c:if test="${not empty m.section}"> · <c:out value="${m.section}"/></c:if>
                            <c:if test="${not empty m.email}"> · <c:out value="${m.email}"/></c:if>
                        </div>
                        <c:if test="${not empty m.bio}">
                            <div class="list-meta" style="margin-top:6px;">
                                <c:out value="${m.bio}"/>
                            </div>
                        </c:if>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>
</div>
