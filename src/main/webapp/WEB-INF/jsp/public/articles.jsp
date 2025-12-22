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
            <h2 class="card-title">文章与专刊 (Articles & Issues)</h2>
            <p class="card-subtitle">Latest published · Top cited · Most downloaded · Most popular</p>
        </div>
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
            如需真实“已发表论文库/卷期”，请使用 <span class="badge">dbo.Issues</span> + <span class="badge">dbo.IssueManuscripts</span>。
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
                            <c:if test="${a.finalDecisionTime != null}">录用：<c:out value="${fn:substring(a.finalDecisionTime, 0, 10)}"/></c:if>
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
