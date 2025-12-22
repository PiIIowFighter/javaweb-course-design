<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">新闻列表</h2>
            <p class="card-subtitle">仅展示已发布新闻/公告。</p>
        </div>
        <div>
            <a class="btn" style="text-decoration:none;" href="${ctx}/about/news">
                <i class="bi bi-arrow-left"></i> 返回关于期刊
            </a>
        </div>
    </div>

    <c:if test="${not empty newsLoadError}">
        <div class="notice danger">
            <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
            <div>加载新闻失败：<c:out value="${newsLoadError}"/></div>
        </div>
    </c:if>

    <c:if test="${empty newsList}">
        <p>暂无已发布新闻。</p>
    </c:if>

    <c:if test="${not empty newsList}">
        <ul class="list">
            <c:forEach var="n" items="${newsList}">
                <li class="list-item">
                    <div class="muted"><c:out value="${n.publishedAt}"/></div>
                    <div>
                        <a href="${ctx}/news/detail?id=${n.newsId}"><c:out value="${n.title}"/></a>
                        <c:if test="${not empty n.attachmentPath}">
                            <span class="muted">（含附件）</span>
                        </c:if>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>