<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">征稿通知 (Call for papers)</h2>
            <p class="card-subtitle">Special issue / Call for papers 列表</p>
        </div>
    </div>

    <c:if test="${empty calls}">
        <p>暂无已发布征稿通知。</p>
        <small class="muted">提示：请在数据库 <span class="badge">dbo.CallForPapers</span> 中插入 <span class="badge">IsPublished=1</span> 的记录。</small>
    </c:if>

    <c:if test="${not empty calls}">
        <ul class="list">
            <c:forEach var="c" items="${calls}">
                <li class="list-item">
                    <span class="avatar" aria-hidden="true"><i class="bi bi-megaphone"></i></span>
                    <div>
                        <div class="list-title">
                            <a style="text-decoration:none;" href="${ctx}/calls?view=detail&id=${c.callId}">
                                <c:out value="${c.title}"/>
                            </a>
                        </div>
                        <div class="list-meta">
                            <c:if test="${c.deadline != null}">Deadline：<c:out value="${c.deadline}"/> · </c:if>
                            <c:if test="${c.startDate != null}">Start：<c:out value="${c.startDate}"/> · </c:if>
                            <c:if test="${c.endDate != null}">End：<c:out value="${c.endDate}"/></c:if>
                        </div>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a class="btn" style="text-decoration:none;" href="${ctx}/issues?type=latest">
            <i class="bi bi-journal" aria-hidden="true"></i>
            Issues
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
