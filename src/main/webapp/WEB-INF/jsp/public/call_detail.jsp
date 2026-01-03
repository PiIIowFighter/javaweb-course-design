<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${empty call}">
    <div class="alert alert-danger">未找到征稿通知。</div>
</c:if>

<c:if test="${not empty call}">
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title"><c:out value="${call.title}"/></h2>
                <p class="card-subtitle">
                    <c:if test="${call.deadline != null}">Deadline：<c:out value="${call.deadline}"/> · </c:if>
                    <c:if test="${call.startDate != null}">Start：<c:out value="${call.startDate}"/> · </c:if>
                    <c:if test="${call.endDate != null}">End：<c:out value="${call.endDate}"/></c:if>
                </p>
            </div>
        </div>

	        <c:if test="${not empty call.coverImagePath}">
	            <div style="border-radius: 16px; overflow:hidden; border: 1px solid rgba(0,0,0,.08);">
	                <img src="${ctx}/journal/asset?type=call_cover&id=${call.callId}"
	                     alt="cover"
	                     style="width:100%; max-height: 320px; object-fit: cover; display:block;"/>
	            </div>
	        </c:if>

	        <c:if test="${not empty call.attachmentPath}">
	            <div class="notice">
	                <i class="bi bi-paperclip" aria-hidden="true"></i>
	                <div>
	                    附件：
	                    <a style="text-decoration:none;" href="${ctx}/journal/asset?type=call_attachment&id=${call.callId}">
	                        点击下载
	                    </a>
	                </div>
	            </div>
	        </c:if>

	        <h3>内容</h3>
        <c:choose>
            <c:when test="${not empty call.content}">
                <!-- 允许存 HTML：这里用 out 防注入；如确需渲染 HTML，可改为 c:out escapeXml=false 并自行保证安全 -->
                                <div class="ql-snow richtext-view">
                    <div class="ql-editor">
                        <c:out value="${call.content}" escapeXml="false"/>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <p>暂无内容。</p>
            </c:otherwise>
        </c:choose>

        <div class="actions">
            <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
                <i class="bi bi-upload" aria-hidden="true"></i>
                Submit your article
            </a>
            <a class="btn" style="text-decoration:none;" href="${ctx}/calls">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                返回列表
            </a>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
