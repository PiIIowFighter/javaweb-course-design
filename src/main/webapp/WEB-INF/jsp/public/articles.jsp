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
            <h2 class="card-title">文章与专刊 (Articles & Issues)</h2>
            <p class="card-subtitle">Latest published · Top cited · Most downloaded · Most popular</p>
        </div>
    </div>

    <div class="tabs">
        <a class="tab ${type == 'latest' ? 'is-active' : ''}" href="${ctx}/articles?type=latest">
            <i class="bi bi-clock" aria-hidden="true"></i> Latest published
        </a>
        <a class="tab ${type == 'topcited' ? 'is-active' : ''}" href="${ctx}/articles?type=topcited">
            <i class="bi bi-quote" aria-hidden="true"></i> Top cited
        </a>
        <a class="tab ${type == 'downloaded' ? 'is-active' : ''}" href="${ctx}/articles?type=downloaded">
            <i class="bi bi-download" aria-hidden="true"></i> Most downloaded
        </a>
        <a class="tab ${type == 'popular' ? 'is-active' : ''}" href="${ctx}/articles?type=popular">
            <i class="bi bi-fire" aria-hidden="true"></i> Most popular
        </a>
    </div>

    <c:if test="${notImplemented == true}">
        <div class="alert">
            当前栏目尚未实现后端：数据库中缺少“引用次数/下载次数/热度”等统计字段或新表。
            你可以后续补充后端后，再替换本占位逻辑。
        </div>
    </c:if>

    <c:if test="${empty articles}">
        <p>暂无数据。</p>
    </c:if>

    <c:if test="${not empty articles}">
        <ul class="list">
            <c:forEach var="a" items="${articles}">
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
                            <c:if test="${a.finalDecisionTime != null}">录用时间：<c:out value="${fn:substring(a.finalDecisionTime, 0, 10)}"/></c:if>
                        </div>
                    </div>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <small>
        备注：当前实现将 <span class="badge">ACCEPTED</span> 状态稿件近似当作已发表论文。
        如需真正的“发表论文库 + 卷期（Issues）”，建议新增对应数据模型。
    </small>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
