<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<!-- 期刊介绍 (about the journal) -->
<div class="hero stack">
    <h1>
        <c:out value="${journal != null ? journal.name : '期刊首页'}"/>
    </h1>

    <c:choose>
        <c:when test="${journal != null && not empty journal.description}">
            <p><c:out value="${journal.description}"/></p>
        </c:when>
        <c:otherwise>
            <p>欢迎访问本期刊网站。当前期刊简介尚未在数据库中完善（dbo.Journals.Description）。</p>
        </c:otherwise>
    </c:choose>

    <div class="toolbar">
        <c:if test="${journal != null && journal.impactFactor != null}">
            <span class="badge">Impact Factor: <c:out value="${journal.impactFactor}"/></span>
        </c:if>
        <c:if test="${journal != null && not empty journal.issn}">
            <span class="badge">ISSN: <c:out value="${journal.issn}"/></span>
        </c:if>
        <c:if test="${journal != null && not empty journal.timeline}">
            <span class="badge">Timeline: <c:out value="${journal.timeline}"/></span>
        </c:if>
        <span class="grow"></span>
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/about">
            <i class="bi bi-info-circle" aria-hidden="true"></i>
            查看期刊介绍
        </a>
    </div>
</div>

<div class="grid grid-2" style="margin-top: var(--space-6);">

    <!-- 期刊编委会介绍：编委照片/简介 -->
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title">期刊编委会</h2>
                <p class="card-subtitle">编委照片（占位）与简介来自 dbo.EditorialBoard + dbo.Users</p>
            </div>
            <a href="${ctx}/about" style="white-space:nowrap; text-decoration:none;">
                查看全部 <i class="bi bi-arrow-right" aria-hidden="true"></i>
            </a>
        </div>

        <c:if test="${empty boardMembers}">
            <p>暂无编委会成员数据（dbo.EditorialBoard 为空），可以后续补充数据后再展示。</p>
        </c:if>
        <c:if test="${not empty boardMembers}">
            <ul class="list">
                <c:forEach var="m" items="${boardMembers}" begin="0" end="5">
                    <li class="list-item">
                        <span class="avatar" aria-hidden="true"><i class="bi bi-person"></i></span>
                        <div>
                            <div class="list-title">
                                <c:out value="${m.fullName}"/>
                                <c:if test="${not empty m.position}">
                                    <span class="badge" style="margin-left:10px;"><c:out value="${m.position}"/></span>
                                </c:if>
                            </div>
                            <div class="list-meta">
                                <c:out value="${m.affiliation}"/>
                                <c:if test="${not empty m.section}"> · <c:out value="${m.section}"/></c:if>
                            </div>
                        </div>
                    </li>
                </c:forEach>
            </ul>
            <small>说明：当前数据库结构未包含“照片/头像字段”，因此首页使用通用头像图标占位。</small>
        </c:if>
    </div>

    <!-- 论文列表：Latest / Top cited / Most downloaded / Most popular -->
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title">论文列表</h2>
                <p class="card-subtitle">Latest published · Top cited · Most downloaded · Most popular</p>
            </div>
            <a href="${ctx}/articles" style="white-space:nowrap; text-decoration:none;">
                进入列表 <i class="bi bi-arrow-right" aria-hidden="true"></i>
            </a>
        </div>

        <div class="tabs">
            <a class="tab is-active" href="${ctx}/articles?type=latest">
                <i class="bi bi-clock" aria-hidden="true"></i> Latest published
            </a>
            <a class="tab" href="${ctx}/articles?type=topcited">
                <i class="bi bi-quote" aria-hidden="true"></i> Top cited
            </a>
            <a class="tab" href="${ctx}/articles?type=downloaded">
                <i class="bi bi-download" aria-hidden="true"></i> Most downloaded
            </a>
            <a class="tab" href="${ctx}/articles?type=popular">
                <i class="bi bi-fire" aria-hidden="true"></i> Most popular
            </a>
        </div>

        <c:if test="${empty latestPublished}">
            <p>暂无“已发表”论文数据。当前实现将 <span class="badge">ACCEPTED</span> 状态稿件近似当作已发表论文。</p>
        </c:if>
        <c:if test="${not empty latestPublished}">
            <ul class="list">
                <c:forEach var="a" items="${latestPublished}">
                    <li class="list-item">
                        <span class="avatar" aria-hidden="true"><i class="bi bi-journal-text"></i></span>
                        <div>
                            <div class="list-title">
                                <a style="text-decoration:none;" href="${ctx}/articles?view=detail&id=${a.manuscriptId}">
                                    <c:out value="${a.title}"/>
                                </a>
                            </div>
                            <div class="list-meta">
                                <c:if test="${not empty a.authorList}">作者：<c:out value="${a.authorList}"/> · </c:if>
                                <c:if test="${a.finalDecisionTime != null}">
                                    录用时间：<c:out value="${fn:substring(a.finalDecisionTime, 0, 10)}"/>
                                </c:if>
                            </div>
                        </div>
                    </li>
                </c:forEach>
            </ul>
            <small>提示：全文附件下载/引用/下载/热度统计需要新增字段或新表支持，当前仅提供论文基本信息链接。</small>
        </c:if>
    </div>

</div>

<div class="grid grid-2" style="margin-top: var(--space-6);">

    <!-- 新闻列表 -->
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title">新闻列表</h2>
                <p class="card-subtitle">来自 dbo.News（仅展示 IsPublished=1）</p>
            </div>
            <a href="${ctx}/news" style="white-space:nowrap; text-decoration:none;">
                查看更多 <i class="bi bi-arrow-right" aria-hidden="true"></i>
            </a>
        </div>

        <c:if test="${empty newsList}">
            <p>暂无已发布新闻。</p>
        </c:if>
        <c:if test="${not empty newsList}">
            <ul class="list">
                <c:forEach var="n" items="${newsList}" begin="0" end="5">
                    <li class="list-item">
                        <span class="avatar" aria-hidden="true"><i class="bi bi-newspaper"></i></span>
                        <div>
                            <div class="list-title">
                                <a style="text-decoration:none;" href="${ctx}/news?view=detail&id=${n.newsId}">
                                    <c:out value="${n.title}"/>
                                </a>
                            </div>
                            <div class="list-meta">
                                <c:if test="${n.publishedAt != null}">
                                    <c:out value="${fn:substring(n.publishedAt, 0, 10)}"/>
                                </c:if>
                            </div>
                        </div>
                    </li>
                </c:forEach>
            </ul>
        </c:if>
    </div>

    <!-- 征稿通知 Call for papers -->
    <div class="card stack">
        <div class="card-header">
            <div>
                <h2 class="card-title">征稿通知</h2>
                <p class="card-subtitle">Call for papers · Special issues（当前为占位）</p>
            </div>
            <a href="${ctx}/calls" style="white-space:nowrap; text-decoration:none;">
                查看详情 <i class="bi bi-arrow-right" aria-hidden="true"></i>
            </a>
        </div>

        <p>
            当前数据库与后端尚未实现 “Call for papers / Special issue” 管理。
            可以后续新增表（如 dbo.SpecialIssues / dbo.CallForPapers）以及管理页面后再接入。
        </p>

        <div class="actions">
            <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
                <i class="bi bi-upload" aria-hidden="true"></i> Submit your article
            </a>
            <a style="text-decoration:none;" href="${ctx}/guide">
                <i class="bi bi-book" aria-hidden="true"></i> Guide for authors
            </a>
        </div>
    </div>

</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
