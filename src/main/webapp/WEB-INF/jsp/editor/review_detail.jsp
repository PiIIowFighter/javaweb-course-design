<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>审稿意见详情</h2>

<c:if test="${empty review}">
    <p style="color:#d00;">未找到审稿记录。</p>
</c:if>

<c:if test="${not empty review}">
    <div class="card" style="max-width: 1100px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">基本信息</h3>
                <p class="card-subtitle">
                    ReviewId：<strong><c:out value="${review.reviewId}"/></strong>
                    &nbsp;&nbsp;|&nbsp;&nbsp;
                    ManuscriptId：<strong><c:out value="${review.manuscriptId}"/></strong>
                    &nbsp;&nbsp;|&nbsp;&nbsp;
                    状态：<strong><c:out value="${review.status}"/></strong>
                </p>
            </div>
        </div>
        <div class="card-body">
            <p>
                <strong>审稿人：</strong>
                <c:out value="${review.reviewerName}"/>
                <c:if test="${not empty review.reviewerEmail}">
                    &lt;<c:out value="${review.reviewerEmail}"/>&gt;
                </c:if>
            </p>
            <p><strong>提交时间：</strong><c:out value="${review.submittedAt}"/></p>

            <c:if test="${not empty manuscript}">
                <p><strong>稿件标题：</strong><c:out value="${manuscript.title}"/></p>
            </c:if>

            <p style="margin-top: 10px;">
                <a href="${ctx}/manuscripts/detail?id=${review.manuscriptId}">查看稿件详情</a>
                &nbsp;|&nbsp;
                <a href="${ctx}/editor/recommend?manuscriptId=${review.manuscriptId}">返回提出建议</a>
            </p>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">审稿结论</h3>
            </div>
        </div>
        <div class="card-body">
            <p><strong>Recommendation：</strong><c:out value="${review.recommendation}"/></p>
            <p><strong>综合评分：</strong><c:out value="${review.score}"/></p>
            <p>
                <strong>分项评分：</strong>
                Originality=<c:out value="${review.scoreOriginality}"/>
                &nbsp;|&nbsp;
                Significance=<c:out value="${review.scoreSignificance}"/>
                &nbsp;|&nbsp;
                Methodology=<c:out value="${review.scoreMethodology}"/>
                &nbsp;|&nbsp;
                Presentation=<c:out value="${review.scorePresentation}"/>
            </p>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">关键评价</h3>
            </div>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty review.keyEvaluation}">
                    <pre style="white-space: pre-wrap;">${fn:escapeXml(review.keyEvaluation)}</pre>
                </c:when>
                <c:otherwise><p>--</p></c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">给编辑的保密意见</h3>
            </div>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty review.confidentialToEditor}">
                    <pre style="white-space: pre-wrap;">${fn:escapeXml(review.confidentialToEditor)}</pre>
                </c:when>
                <c:otherwise><p>--</p></c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">给作者的意见（Content）</h3>
            </div>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty review.content}">
                    <pre style="white-space: pre-wrap;">${fn:escapeXml(review.content)}</pre>
                </c:when>
                <c:otherwise><p>--</p></c:otherwise>
            </c:choose>
        </div>
    </div>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
