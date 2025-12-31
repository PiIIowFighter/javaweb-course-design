<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>邮件预览</h2>

<c:if test="${empty draft || empty draft.previews}">
    <p>没有可预览的邮件内容。</p>
</c:if>

<c:if test="${not empty draft && not empty draft.previews}">
    <form method="post" action="${ctx}/mail/send">
        <input type="hidden" name="draftId" value="${draft.draftId}"/>

        <c:forEach items="${draft.previews}" var="p" varStatus="st">
            <div style="border:1px solid #ccc; padding:12px; margin:12px 0; background:#fff;">
                <p><strong>收件人：</strong> <c:out value="${p.to}"/></p>

                <p><strong>主题：</strong></p>
                <input type="text" name="subject" value="${p.subject}" style="width:90%;"/>

                <p style="margin-top:10px;"><strong>正文：</strong></p>
                <textarea name="body" rows="10" style="width:90%"><c:out value="${p.body}"/></textarea>
            </div>
        </c:forEach>

        <button type="submit">确认发送</button>
        <button type="button" onclick="history.back()">返回</button>
    </form>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
