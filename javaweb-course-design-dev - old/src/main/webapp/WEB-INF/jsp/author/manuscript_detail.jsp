<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<h2>稿件详情</h2>
<c:choose>
    <c:when test="${empty manuscript}">
        <p>未找到指定的稿件记录。</p>
    </c:when>
    <c:otherwise>
        <p><strong>稿件编号：</strong><c:out value="${manuscript.manuscriptId}"/></p>
        <p><strong>标题：</strong><c:out value="${manuscript.title}"/></p>
        <p><strong>期刊ID：</strong><c:out value="${manuscript.journalId}"/></p>
        <p><strong>当前状态：</strong><c:out value="${manuscript.currentStatus}"/></p>
        <p><strong>提交时间：</strong><c:out value="${manuscript.submitTime}"/></p>
        <p><strong>摘要：</strong></p>
        <p><c:out value="${manuscript.abstractText}"/></p>
        <p><strong>关键词：</strong><c:out value="${manuscript.keywords}"/></p>
        <p>版本信息与状态流转历史可在后续根据 machine.pdf 继续扩展实现。</p>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>