<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">关于期刊 (About the journal)</h2>
</div></div>
    </div>

    <h3>期刊介绍</h3>
    <c:if test="${journal == null}">
        <p>未找到期刊信息。</p>
    </c:if>
    <c:if test="${journal != null}">
        <p><strong><c:out value="${journal.name}"/></strong></p>
        <c:choose>
            <c:when test="${not empty journal.description}">
                <p><c:out value="${journal.description}"/></p>
            </c:when>
            <c:otherwise>
                <p>期刊简介尚未完善。</p>
            </c:otherwise>
        </c:choose>

        <div class="toolbar" style="margin-top: var(--space-3);">
            <c:if test="${journal.impactFactor != null}">
                <span class="badge">Impact Factor: <c:out value="${journal.impactFactor}"/></span>
            </c:if>
            <c:if test="${not empty journal.issn}">
                <span class="badge">ISSN: <c:out value="${journal.issn}"/></span>
            </c:if>
            <c:if test="${not empty journal.timeline}">
                <span class="badge">Timeline: <c:out value="${journal.timeline}"/></span>
            </c:if>
        </div>
    </c:if>

	<h3 style="margin-top: var(--space-6);">期刊编委会介绍</h3>

    <c:if test="${empty boardMembers}">
        <p>编委会信息将陆续更新，敬请关注。</p>
    </c:if>
    <c:if test="${not empty boardMembers}">
        <div class="grid grid-2">
            <c:forEach var="m" items="${boardMembers}">
                <div class="card" style="padding: var(--space-5);">
                    <div style="display:flex; gap: var(--space-4); align-items:flex-start;">
						<span class="avatar" aria-hidden="true" style="overflow:hidden;">
							<img src="${ctx}/public/avatar?userId=${m.userId}"
							     alt="avatar"
							     style="width:100%;height:100%;object-fit:cover;display:block;"/>
						</span>
                        <div>
                            <div style="font-weight: 700;">
                                <c:out value="${m.fullName}"/>
                            </div>
                            <div class="list-meta">
                                <c:out value="${m.affiliation}"/>
                            </div>
                            <div style="margin-top: 8px; display:flex; gap: 8px; flex-wrap: wrap;">
                                <c:if test="${not empty m.position}"><span class="badge"><c:out value="${m.position}"/></span></c:if>
                                <c:if test="${not empty m.section}"><span class="badge"><c:out value="${m.section}"/></span></c:if>
                            </div>
                            <c:if test="${not empty m.bio}">
                                <p style="margin-top: 10px;"><c:out value="${m.bio}"/></p>
                            </c:if>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:if>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a style="text-decoration:none;" href="${ctx}/news">
            <i class="bi bi-newspaper" aria-hidden="true"></i>
            查看新闻
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
