<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>待评审稿件列表</h2>
<p>以下为当前分配给您的、需要在外审截止日期前完成评审的稿件。</p>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${empty reviews}">
    <p>目前没有待评审的稿件。</p>
</c:if>
<c:if test="${not empty reviews}">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
    <tr>
        <th>审稿记录ID</th>
        <th>稿件编号</th>
        <th>状态</th>
        <th>邀请时间</th>
        <th>截止时间</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${reviews}" var="r">
        <tr>
            <td><c:out value="${r.reviewId}"/></td>
            <td><c:out value="${r.manuscriptId}"/></td>
            <td><c:out value="${r.status}"/></td>
            <td><c:out value="${r.invitedAt}"/></td>
            <td><c:out value="${r.dueAt}"/></td>
            <td>
                <a href="${ctx}/manuscripts/detail?id=${r.manuscriptId}" target="_blank">查看稿件详情/附件</a>
                <span style="margin:0 8px;">|</span>
                <a href="${ctx}/files/preview?manuscriptId=${r.manuscriptId}&type=manuscript" target="_blank">下载手稿</a>
                <br/>
                <c:choose>
                    <c:when test="${r.status == 'INVITED'}">
                        <form method="post" action="${ctx}/reviewer/accept" style="display:inline;">
                            <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                            <button type="submit">接受邀请</button>
                        </form>
                        <form method="post" action="${ctx}/reviewer/decline" style="display:inline;"
                              onsubmit="return confirm('确定要拒绝该审稿邀请吗？');">
                            <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                            <button type="submit">拒绝邀请</button>
                        </form>
                    </c:when>
                    <c:when test="${r.status == 'ACCEPTED'}">
                        <a href="${ctx}/reviewer/reviewForm?id=${r.reviewId}">填写评审意见</a>
                    </c:when>
                    <c:when test="${r.status == 'SUBMITTED'}">
                        已提交
                    </c:when>
                    <c:otherwise>
                        <c:out value="${r.status}"/>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
