<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="box" value="${empty requestScope.box ? 'inbox' : requestScope.box}"/>
<c:set var="n" value="${requestScope.notification}"/>

<div class="card">
    <div class="card-header" style="align-items:center;">
        <div style="flex:1;">
            <h2 class="card-title">通知详情</h2>
            <p class="card-subtitle" style="margin-top:10px;">
                <c:choose>
                    <c:when test="${n.read}">
                        <span class="badge">已读</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge badge-warn">未读</span>
                    </c:otherwise>
                </c:choose>
                <span class="muted" style="margin-left:10px;">发送时间：<c:out value="${n.createdAt}"/></span>
            </p>
        </div>

        <div class="actions" style="margin:0; display:flex; gap:10px;">
            <a class="btn" href="${ctx}/notifications?box=${box}">
                <i class="bi bi-arrow-left" aria-hidden="true"></i> 返回列表
            </a>

            <c:if test="${isRecipient and (not n.read)}">
                <form action="${ctx}/notifications/markRead" method="post" style="margin:0;">
                    <input type="hidden" name="id" value="${n.notificationId}"/>
                    <input type="hidden" name="box" value="${box}"/>
                    <input type="hidden" name="redirect" value="view"/>
                    <button class="btn" type="submit">
                        <i class="bi bi-check2" aria-hidden="true"></i> 标记已阅
                    </button>
                </form>
            </c:if>
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
                · <a href="${ctx}/manuscripts/detail?id=${n.relatedManuscriptId}" style="overflow-wrap:anywhere; word-break:break-word;">查看关联稿件</a>
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
