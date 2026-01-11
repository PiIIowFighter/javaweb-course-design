<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">文章与专刊 (Articles & Issues)</h2>
        <div class="chips">
            <span class="chip">Latest published · Top cited · Most downloaded · Most popular</span>
        </div>
    </div></div>
    </div>

    <div class="tabs">
        <a class="tab ${type == 'latest' ? 'is-active' : ''}" href="${ctx}/articles?type=latest">
            <i class="bi bi-clock" aria-hidden="true"></i> Latest published
        </a>
        <a class="tab ${type == 'topcited' ? 'is-active' : ''}" href="${ctx}/articles?type=topcited">
            <i class="bi bi-quote" aria-hidden="true"></i> Top cited
        </a>
        <a class="tab ${type == 'downloaded' ? 'is-active' : ''}" href="${ctx}/articles?type=downloaded">
            <i class="bi bi-download" aria-hidden="true"></i> Most downloaded
        </a>
        <a class="tab ${type == 'popular' ? 'is-active' : ''}" href="${ctx}/articles?type=popular">
            <i class="bi bi-fire" aria-hidden="true"></i> Most popular
        </a>
    </div>

    <c:if test="${empty articles}">
        <p>暂无数据。</p>
        <small class="muted">
            说明：当前实现将 <span class="badge">ACCEPTED</span> 状态稿件近似当作已发表论文。
            如需按卷期/专刊浏览，请前往 Issues 页面。
        </small>
    </c:if>

    <c:if test="${not empty articles}">
        <ul class="list">
            <c:forEach var="a" items="${articles}">
                <li class="list-item">
                    <span class="avatar" aria-hidden="true"><i class="bi bi-journal-text"></i></span>
                    <div>
                        <div class="list-title">
                            <a style="text-decoration:none;" href="${ctx}/articles?view=detail&id=${a.manuscriptId}">
                                <c:out value="${a.title}"/>
                            </a>
                        </div>
                        <div class="list-meta">
    <c:if test="${not empty a.authorList}">作者：<c:out value="${a.authorList}"/> · </c:if>

    <c:if test="${not empty a.journalName}">
        <c:out value="${a.journalName}"/>
        <c:if test="${not empty a.journalIssn}">（ISSN：<c:out value="${a.journalIssn}"/>）</c:if>
        ·
    </c:if>

    <c:choose>
        <c:when test="${a.publishYear != null}">
            <c:out value="${a.publishYear}"/>年
        </c:when>
        <c:when test="${not empty a.publishedAt}">
            <c:out value="${fn:substring(a.publishedAt, 0, 4)}"/>年
        </c:when>
        <c:otherwise>
            <c:if test="${a.finalDecisionTime != null}">
                <c:out value="${fn:substring(a.finalDecisionTime, 0, 4)}"/>年
            </c:if>
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty a.volume}">，<c:out value="${a.volume}"/></c:if>
    <c:if test="${not empty a.issue}">（<c:out value="${a.issue}"/>）</c:if>
    <c:if test="${not empty a.pageRange}">：<c:out value="${a.pageRange}"/></c:if>

    <c:choose>
        <c:when test="${not empty a.publishedAt}">
            · 发表：<c:out value="${fn:substring(a.publishedAt, 0, 10)}"/>
        </c:when>
        <c:when test="${a.finalDecisionTime != null}">
            · 录用：<c:out value="${fn:substring(a.finalDecisionTime, 0, 10)}"/>
        </c:when>
    </c:choose>
</div>
<div class="list-meta">
    <span class="badge">Views</span> <c:out value="${a.viewCount == null ? 0 : a.viewCount}"/>
    <span class="badge">Downloads</span> <c:out value="${a.downloadCount == null ? 0 : a.downloadCount}"/>
    <span class="badge">Citations</span> <c:out value="${a.citationCount == null ? 0 : a.citationCount}"/>
</div>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <div class="actions" style="margin-top:16px;">
        <a class="btn" style="text-decoration:none;" href="${ctx}/issues?type=latest">
            <i class="bi bi-journal" aria-hidden="true"></i> Issues
        </a>
        <a class="btn" style="text-decoration:none;" href="${ctx}/calls">
            <i class="bi bi-megaphone" aria-hidden="true"></i> Call for papers
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
