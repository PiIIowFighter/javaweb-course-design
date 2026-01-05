<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${empty article}">
    <div class="alert alert-danger">未找到论文。</div>
</c:if>

<c:if test="${not empty article}">
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title"><c:out value="${article.title}"/></h2>
                <p class="card-subtitle">
                    <c:if test="${article.finalDecisionTime != null}">
                        录用时间：<c:out value="${fn:substring(article.finalDecisionTime, 0, 10)}"/>
                    </c:if>
                </p>
            </div>
        </div>

        <c:if test="${not empty article.authorList}">
            <p><span class="badge">作者</span> <c:out value="${article.authorList}"/></p>
        </c:if>
        <c:if test="${not empty article.keywords}">
            <p><span class="badge">关键词</span> <c:out value="${article.keywords}"/></p>
        </c:if>
        <c:if test="${not empty article.subjectArea}">
            <p><span class="badge">研究主题</span> <c:out value="${article.subjectArea}"/></p>
        </c:if>

        <p>
            <span class="badge">Views</span> <c:out value="${article.viewCount == null ? 0 : article.viewCount}"/>
            <span class="badge">Downloads</span> <c:out value="${article.downloadCount == null ? 0 : article.downloadCount}"/>
            <span class="badge">Citations</span> <c:out value="${article.citationCount == null ? 0 : article.citationCount}"/>
        </p>

        <h3>摘要</h3>
        <c:choose>
            <c:when test="${not empty article.abstractText}">
                                <div class="ql-snow richtext-view">
                    <div class="ql-editor">
                        <c:out value="${article.abstractText}" escapeXml="false"/>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <p>暂无摘要。</p>
            </c:otherwise>
        </c:choose>

        <div class="actions">
            <a class="btn-primary" style="text-decoration:none;" href="${ctx}/articles/download?id=${article.manuscriptId}">
                <i class="bi bi-download" aria-hidden="true"></i>
                下载论文
            </a>
            <a class="btn" style="text-decoration:none;" href="${ctx}/articles?type=latest">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                返回列表
            </a>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>