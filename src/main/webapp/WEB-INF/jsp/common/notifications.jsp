<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">通知中心</h2>
            <p class="card-subtitle">未读：<c:out value="${unreadCount}"/> 条</p>
        </div>
        <div class="actions" style="margin:0">
            <form action="${ctx}/notifications/markAllRead" method="post" style="margin:0">
                <button class="btn" type="submit"><i class="bi bi-check2-all" aria-hidden="true"></i> 全部标记已读</button>
            </form>
            <c:if test="${canSend}">
                <a class="btn" href="${ctx}/notifications/send"><i class="bi bi-send" aria-hidden="true"></i> 发送通知</a>
            </c:if>
        </div>
    </div>

    <c:if test="${not empty param.msg}">
        <div class="alert alert-success" style="margin-bottom:16px;">
            <c:out value="${param.msg}"/>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty notifications}">
            <div class="muted">暂无通知。</div>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th style="width:110px;">状态</th>
                    <th>标题</th>
                    <th>内容</th>
                    <th style="width:160px;">时间</th>
                    <th style="width:140px;">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="n" items="${notifications}">
                    <tr>
                        <td>
                            <c:choose>
                                <c:when test="${n.read}"><span class="badge">已读</span></c:when>
                                <c:otherwise><span class="badge badge-warn">未读</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <div style="font-weight:600;">
                                <c:out value="${n.title}"/>
                            </div>
                            <div class="muted" style="font-size:12px; margin-top:6px;">
                                <c:out value="${n.type}"/>
                                <c:if test="${not empty n.category}"> · <c:out value="${n.category}"/></c:if>
                                <c:if test="${not empty n.relatedManuscriptId}">
                                    · <a href="${ctx}/manuscripts/detail?id=${n.relatedManuscriptId}">查看稿件</a>
                                </c:if>
                            </div>
                        </td>
                        <td style="white-space:pre-wrap;"><c:out value="${n.content}"/></td>
                        <td>
                            <c:out value="${n.createdAt}"/>
                        </td>
                        <td>
                            <c:if test="${not n.read}">
                                <form action="${ctx}/notifications/markRead" method="post" style="margin:0">
                                    <input type="hidden" name="id" value="${n.notificationId}"/>
                                    <button class="btn" type="submit"><i class="bi bi-check2" aria-hidden="true"></i> 标记已读</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
