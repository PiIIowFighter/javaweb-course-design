<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title"><c:choose>
                    <c:when test="${not empty page && not empty page.title}"><c:out value="${page.title}"/></c:when>
                    <c:otherwise>论文发表 (Publish)</c:otherwise>
                </c:choose></h2>
        <div class="chips">
            <span class="chip"><c:choose> <c:when test="${not empty page && not empty page.title}">了解本刊投稿与发表流程、版权与开放获取政策等信息。</c:when> <c:otherwise>期刊投稿与发表流程简介</c:otherwise> </c:choose></span>
        </div>
    </div></div>
    </div>

    <c:if test="${not empty pageLoadError}">
        <p class="card-subtitle" style="color:#b54708;">
            <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
            <c:out value="${pageLoadError}"/>
        </p>
    </c:if>

    <c:choose>
        <c:when test="${not empty page && not empty page.content}">
            <div class="prose" style="line-height: 1.7;">
                <c:out value="${page.content}" escapeXml="false"/>
            </div>
        </c:when>
        <c:otherwise>

            <h3>流程概览</h3>
            <ul>
                <li>作者在线提交稿件（支持草稿与最终提交）。</li>
                <li>编辑部管理员进行形式审查与格式检查。</li>
                <li>主编进行初审、指派编辑、终审决策。</li>
                <li>必要时进入外审流程，并收集审稿意见。</li>
            </ul>
        </c:otherwise>
    </c:choose>

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
