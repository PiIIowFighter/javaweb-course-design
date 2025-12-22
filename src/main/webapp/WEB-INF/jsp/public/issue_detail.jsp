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
            <h2 class="card-title">Issue 详情</h2>
            <p class="card-subtitle">
                <c:out value="${issue.title}"/>
                <c:if test="${isPlaceholder}">
                    <span class="badge" style="margin-left:8px;">占位</span>
                </c:if>
            </p>
        </div>
        <a href="${ctx}/issues?type=all" style="white-space:nowrap; text-decoration:none;">
            返回 Issue 列表 <i class="bi bi-arrow-right" aria-hidden="true"></i>
        </a>
    </div>

    <p><c:out value="${issue.coverNote}"/></p>

    <c:if test="${isPlaceholder}">
        <p class="card-subtitle">
            说明：当前尚未实现 Issue 与文章的关联关系。
            因此这里先用“最近录用（ACCEPTED）稿件”作为文章列表示例。
            后续你可以新增 Issue-Article 关联表后替换此逻辑。
        </p>
    </c:if>

    <c:if test="${not empty articleLoadError}">
        <div class="alert">加载文章失败：<c:out value="${articleLoadError}"/></div>
    </c:if>

    <h3>本 Issue 收录文章（示例）</h3>
    <c:if test="${empty articles}">
        <p>暂无文章。</p>
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
                            <c:if test="${a.finalDecisionTime != null}">
                                录用时间：<c:out value="${fn:substring(a.finalDecisionTime, 0, 10)}"/>
                            </c:if>
                        </div>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a style="text-decoration:none;" href="${ctx}/guide">
            <i class="bi bi-book" aria-hidden="true"></i>
            Guide for authors
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
