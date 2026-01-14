<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="eb-page-wrap">

    <div class="hero stack">
        <h1>期刊编委会</h1>
        <c:if test="${journal != null}">
            <p><c:out value="${journal.name}"/> · 编委会成员信息</p>
        </c:if>
    </div>

    <div class="card stack" style="margin-top: var(--space-6);">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">编委会成员</h2>
</div></div>
        <a href="${ctx}/" style="white-space:nowrap; text-decoration:none;">
            返回首页 <i class="bi bi-arrow-right" aria-hidden="true"></i>
        </a>
    </div>

    <c:if test="${empty boardMembers}">
        <p>编委会信息将陆续更新，敬请关注。</p>
    </c:if>

    <c:if test="${not empty boardMembers}">
        <div class="eb-grid">
            <c:forEach var="m" items="${boardMembers}">
                <div class="eb-card-wrap">
                    <div class="eb-card">
                    <div style="display:flex; gap: var(--space-3); align-items:center;">
                        <span class="avatar" aria-hidden="true" style="overflow:hidden;">
                            <img src="${ctx}/public/avatar?userId=${m.userId}"
                                 alt="avatar"
                                 style="width:100%;height:100%;object-fit:cover;display:block;"/>
                        </span>
                        <div style="min-width:0;">
                            <div class="eb-name"><c:out value="${m.fullName}"/></div>
                            <c:if test="${not empty m.position}">
                                <span class="badge"><c:out value="${m.position}"/></span>
                            </c:if>
                        </div>
                    </div>

                    <div class="eb-meta">
                        <c:out value="${m.affiliation}"/>
                        <c:if test="${not empty m.section}"> · <c:out value="${m.section}"/></c:if>
                    </div>

                    <c:if test="${not empty m.bio}">
                        <div class="eb-bio"><c:out value="${m.bio}"/></div>
                    </c:if>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:if>
</div>

</div>

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
