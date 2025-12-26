<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${empty issue}">
    <div class="alert alert-danger">未找到 Issue。</div>
</c:if>

<c:if test="${not empty issue}">
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title"><c:out value="${issue.title}"/></h2>
                <p class="card-subtitle">
                    <c:if test="${issue.year != null}">Year：<c:out value="${issue.year}"/> · </c:if>
                    <c:if test="${issue.volume != null}">Vol：<c:out value="${issue.volume}"/> · </c:if>
                    <c:if test="${issue.number != null}">No：<c:out value="${issue.number}"/> · </c:if>
                    <c:if test="${issue.publishDate != null}">Published：<c:out value="${issue.publishDate}"/></c:if>
                </p>
            </div>
        </div>

        <c:if test="${not empty issue.guestEditors}">
            <p><span class="badge">Guest Editors</span> <c:out value="${issue.guestEditors}"/></p>
        </c:if>

        <c:if test="${not empty issue.description}">
            <h3>简介</h3>
            <p style="white-space:pre-wrap;"><c:out value="${issue.description}"/></p>
        </c:if>

        <h3>包含文章</h3>
        <c:if test="${not empty articleLoadError}">
            <div class="alert">加载 Issue 文章列表失败：<c:out value="${articleLoadError}"/></div>
        </c:if>

        <c:if test="${empty articles}">
            <p>暂无文章。</p>
            <small class="muted">提示：请在数据库 <span class="badge">dbo.IssueManuscripts</span> 中把稿件关联到该 Issue。</small>
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
                                <c:if test="${a.finalDecisionTime != null}">录用时间：<c:out value="${fn:substring(a.finalDecisionTime, 0, 10)}"/></c:if>
                            </div>
                        </div>
                    </li>
                </c:forEach>
            </ul>
        </c:if>

        <div class="actions">
            <a class="btn" style="text-decoration:none;" href="${ctx}/issues?type=latest">
                <i class="bi bi-arrow-left" aria-hidden="true"></i> 返回 Issues
            </a>
            <a class="btn" style="text-decoration:none;" href="${ctx}/calls">
                <i class="bi bi-megaphone" aria-hidden="true"></i> Call for papers
            </a>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
