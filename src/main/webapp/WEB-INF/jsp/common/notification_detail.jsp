<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="box" value="${empty requestScope.box ? 'inbox' : requestScope.box}"/>
<c:set var="n" value="${requestScope.notification}"/>

<div class="card">
    <div class="card-header" style="align-items:center;">
        <div style="flex:1;">
            <div class="page-head">
        <h2 class="page-title">通知详情</h2>
</div></div>

        <div class="actions" style="margin:0; display:flex; gap:10px;">
            <a class="btn" href="${ctx}/notifications?box=${box}">
                <i class="bi bi-arrow-left" aria-hidden="true"></i> 返回列表
            </a>
        </div>
    </div>

    <div style="padding: 0 0 6px 0;">
        <div style="font-size:16px; font-weight:700; margin-bottom:10px; line-height:1.4; white-space:normal; overflow-wrap:anywhere; word-break:break-word;">
            <c:out value="${n.title}"/>
        </div>

        <div class="muted" style="font-size:12px; margin-bottom:12px; overflow-wrap:anywhere; word-break:break-word;">
            <c:out value="${n.type}"/>
            <c:if test="${not empty n.category}"> · <c:out value="${n.category}"/></c:if>
            <c:if test="${not empty n.relatedManuscriptId}">
                ·
                <c:choose>
                    <%-- 作者在通知中心点击“查看关联稿件”应跳转到作者可访问的稿件详情页，避免 403 --%>
                    <c:when test="${sessionScope.currentUser.roleCode eq 'AUTHOR'}">
                        <a href="${ctx}/manuscripts/detail?id=${n.relatedManuscriptId}" style="overflow-wrap:anywhere; word-break:break-word;">查看关联稿件</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${ctx}/editor/recommend/detail?manuscriptId=${n.relatedManuscriptId}" style="overflow-wrap:anywhere; word-break:break-word;">查看关联稿件</a>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </div>

        <div style="display:flex; flex-wrap:wrap; gap:18px; margin-bottom:14px; overflow-wrap:anywhere; word-break:break-word;">
            <div style="overflow-wrap:anywhere; word-break:break-word;">
                <span class="muted">发送者：</span>
                <c:choose>
                    <c:when test="${empty n.createdByUserId}">系统</c:when>
                    <c:when test="${not empty senderUser}">
                        <c:choose>
                            <c:when test="${not empty senderUser.fullName}">
                                <c:out value="${senderUser.fullName}"/>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${senderUser.username}"/>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${n.createdByUserId}"/>
                    </c:otherwise>
                </c:choose>
            </div>

            <div style="overflow-wrap:anywhere; word-break:break-word;">
                <span class="muted">接收者：</span>
                <c:choose>
                    <c:when test="${not empty recipientUser}">
                        <c:choose>
                            <c:when test="${not empty recipientUser.fullName}">
                                <c:out value="${recipientUser.fullName}"/>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${recipientUser.username}"/>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:otherwise>
                        <c:out value="${n.recipientUserId}"/>
                    </c:otherwise>
                </c:choose>
            </div>

            <div style="overflow-wrap:anywhere; word-break:break-word;">
                <span class="muted">已读时间：</span>
                <c:choose>
                    <c:when test="${not empty n.readAt}">
                        <c:out value="${n.readAt}"/>
                    </c:when>
                    <c:otherwise>-</c:otherwise>
                </c:choose>
            </div>
        </div>

        <div style="border-top:1px solid var(--border); padding-top:14px;">
            <div style="white-space:pre-wrap; line-height:1.7; overflow-wrap:anywhere; word-break:break-word;">
                <c:out value="${n.content}"/>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
