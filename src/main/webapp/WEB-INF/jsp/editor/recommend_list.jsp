<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
        <h2 class="page-title">编辑推荐</h2>
</div><c:if test="${param.ok == '1'}">
    <div class="alert" style="max-width: 1100px; padding: 12px; border: 1px solid #d0f0d0; background: #f3fff3; border-radius: 10px; margin: 10px 0;">
        已成功提交编辑建议，等待主编终审。
    </div>
</c:if>

<c:if test="${empty readyList}">
    <p style="color:#d00;">当前没有可提交建议的稿件。</p>
</c:if>

<c:if test="${not empty readyList}">
    <table border="1" cellpadding="6" cellspacing="0" style="width: 100%; max-width: 1100px;">
        <thead>
        <tr>
            <th>稿件编号</th>
            <th>标题</th>
            <th>当前状态</th>
            <th>提交时间</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${readyList}" var="m">
            <tr>
                <td><c:out value="${m.manuscriptId}"/></td>
                <td><c:out value="${m.title}"/></td>
                <td><c:out value="${m.currentStatus}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${m.submitTime != null}">
                            <c:out value="${m.submitTime}"/>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${ctx}/editor/recommend?manuscriptId=${m.manuscriptId}">进入提出建议</a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
