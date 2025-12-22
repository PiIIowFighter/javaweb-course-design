<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div style="display:flex; justify-content:space-between; align-items:center; gap:12px;">
            <div>
                <h2 class="card-title">文章与专刊 (Issues)</h2>
                <p class="card-subtitle">Latest Issues · Special Issues · All Issues</p>
            </div>
            <a style="text-decoration:none;" href="${ctx}/guide">用户指南 →</a>
        </div>
    </div>

    <c:if test="${not empty requestScope['javax.servlet.error.message']}">
        <div class="alert alert-danger">
            <c:out value="${requestScope['javax.servlet.error.message']}"/>
        </div>
    </c:if>

    <div class="tabs">
        <a class="tab ${type == 'latest' ? 'is-active' : ''}" href="${ctx}/issues?type=latest">
            <i class="bi bi-clock" aria-hidden="true"></i> Latest Issues
        </a>
        <a class="tab ${type == 'special' ? 'is-active' : ''}" href="${ctx}/issues?type=special">
            <i class="bi bi-stars" aria-hidden="true"></i> Special Issues
        </a>
        <a class="tab ${type == 'all' ? 'is-active' : ''}" href="${ctx}/issues?type=all">
            <i class="bi bi-grid" aria-hidden="true"></i> All Issues
        </a>
    </div>

    <c:if test="${empty issues}">
        <p>暂无已发布 Issue。</p>
        <small class="muted">提示：请先在数据库 <span class="badge">dbo.Issues</span> 中插入 <span class="badge">IsPublished=1</span> 的记录。</small>
    </c:if>

    <c:if test="${not empty issues}">
        <ul class="list">
            <c:forEach var="it" items="${issues}">
                <li class="list-item">
                    <span class="avatar" aria-hidden="true"><i class="bi bi-journal"></i></span>
                    <div>
                        <div class="list-title">
                            <a style="text-decoration:none;" href="${ctx}/issues?view=detail&id=${it.issueId}">
                                <c:out value="${it.title}"/>
                            </a>
                        </div>
                        <div class="list-meta">
                            <c:if test="${it.year != null}">Year：<c:out value="${it.year}"/> · </c:if>
                            <c:if test="${it.volume != null}">Vol：<c:out value="${it.volume}"/> · </c:if>
                            <c:if test="${it.number != null}">No：<c:out value="${it.number}"/> · </c:if>
                            <c:if test="${it.publishDate != null}">Published：<c:out value="${it.publishDate}"/></c:if>
                        </div>
                        <c:if test="${not empty it.guestEditors}">
                            <div class="list-meta">Guest Editors：<c:out value="${it.guestEditors}"/></div>
                        </c:if>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <div class="actions" style="margin-top:16px;">
        <a class="btn" style="text-decoration:none;" href="${ctx}/calls">
            <i class="bi bi-megaphone" aria-hidden="true"></i> Call for papers
        </a>
        <a class="btn" style="text-decoration:none;" href="${ctx}/articles?type=latest">
            <i class="bi bi-journal-text" aria-hidden="true"></i> Latest articles
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
