<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">文章与专刊 (Issues)</h2>
            <p class="card-subtitle">
                Latest Issues · Special Issues · All Issues
                <c:if test="${isPlaceholder}">
                    <span class="badge" style="margin-left:8px;">占位</span>
                </c:if>
            </p>
        </div>
    </div>

    <div class="tabs">
        <a class="tab ${type == 'latest' ? 'is-active' : ''}" href="${ctx}/issues?type=latest">
            <i class="bi bi-clock" aria-hidden="true"></i> Latest Issues
        </a>
        <a class="tab ${type == 'special' ? 'is-active' : ''}" href="${ctx}/issues?type=special">
            <i class="bi bi-stars" aria-hidden="true"></i> Special Issues
        </a>
        <a class="tab ${type == 'all' ? 'is-active' : ''}" href="${ctx}/issues?type=all">
            <i class="bi bi-collection" aria-hidden="true"></i> All Issues
        </a>
    </div>

    <c:if test="${isPlaceholder}">
        <p class="card-subtitle">
            当前数据库与后端未实现“卷/期/专刊”建模，因此此页面先展示静态 Issue 列表。
            后续你可以新增 <span class="badge">dbo.Issues</span> / <span class="badge">dbo.SpecialIssues</span>
            / <span class="badge">dbo.IssueArticles</span> 等表，再将这里接入真实数据。
        </p>
    </c:if>

    <c:if test="${empty issues}">
        <p>暂无 Issue。</p>
    </c:if>

    <c:if test="${not empty issues}">
        <ul class="list">
            <c:forEach var="i" items="${issues}">
                <li class="list-item">
                    <span class="avatar" aria-hidden="true"><i class="bi bi-journals"></i></span>
                    <div>
                        <div class="list-title">
                            <a style="text-decoration:none;" href="${ctx}/issues?view=detail&id=${i.issueId}">
                                <c:out value="${i.title}"/>
                            </a>
                        </div>
                        <div class="list-meta">
                            <c:out value="${i.coverNote}"/>
                        </div>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/articles?type=latest">
            <i class="bi bi-journal-text" aria-hidden="true"></i>
            直接查看论文列表
        </a>
        <a style="text-decoration:none;" href="${ctx}/calls">
            <i class="bi bi-megaphone" aria-hidden="true"></i>
            征稿通知
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
