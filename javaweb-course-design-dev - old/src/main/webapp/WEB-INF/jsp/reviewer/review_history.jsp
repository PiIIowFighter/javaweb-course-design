<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>历史评审记录</h2>
<p>以下为您已经完成的所有评审记录。</p>

<c:if test="${empty reviews}">
    <p>目前还没有完成的评审记录。</p>
</c:if>
<c:if test="${not empty reviews}">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
    <tr>
        <th>审稿记录ID</th>
        <th>稿件编号</th>
        <th>提交时间</th>
        <th>总体评分</th>
        <th>多维评分</th>
        <th>推荐意见</th>
        <th>稿件下载</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${reviews}" var="r">
        <tr>
            <td><c:out value="${r.reviewId}"/></td>
            <td><c:out value="${r.manuscriptId}"/></td>
            <td><c:out value="${r.submittedAt}"/></td>
            <td><c:out value="${r.score}"/></td>
            <td>
                创新 <c:out value="${r.scoreOriginality}"/>
                / 重要 <c:out value="${r.scoreSignificance}"/>
                / 方法 <c:out value="${r.scoreMethodology}"/>
                / 呈现 <c:out value="${r.scorePresentation}"/>
            </td>
            <td><c:out value="${r.recommendation}"/></td>
            <td>
                <c:set var="ctx" value="${pageContext.request.contextPath}"/>
                <a href="${ctx}/files/preview?manuscriptId=${r.manuscriptId}&type=anonymous" target="_blank">下载脱密稿</a>
                <span style="margin:0 8px;">|</span>
                <a href="${ctx}/files/preview?manuscriptId=${r.manuscriptId}&type=original" target="_blank">下载原稿</a>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
