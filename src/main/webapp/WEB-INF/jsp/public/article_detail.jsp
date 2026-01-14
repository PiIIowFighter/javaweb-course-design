<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${empty article}">
    <div class="alert alert-danger">未找到论文。</div>
</c:if>

<c:if test="${not empty article}">
    <div class="card stack">
        <div class="card-header">
            <div>
                <div class="page-head">
        <h2 class="page-title"><c:out value="${article.title}"/></h2>
</div></div>
        </div>

        <c:if test="${not empty article.authorList}">
            <p><span class="badge">作者</span> <c:out value="${article.authorList}"/></p>
        </c:if>
        <c:if test="${not empty article.keywords}">
            <p><span class="badge">关键词</span> <c:out value="${article.keywords}"/></p>
        </c:if>
        <c:if test="${not empty article.subjectArea}">
            <p><span class="badge">研究方向</span> <c:out value="${article.subjectArea}"/></p>
        </c:if>
        <c:if test="${not empty article.fundingInfo}">
            <p><span class="badge">基金</span> <c:out value="${article.fundingInfo}"/></p>
        </c:if>

        <p>
            <span class="badge">Views</span> <c:out value="${article.viewCount == null ? 0 : article.viewCount}"/>
            <span class="badge">Downloads</span> <c:out value="${article.downloadCount == null ? 0 : article.downloadCount}"/>
            <span class="badge">Citations</span> <c:out value="${article.citationCount == null ? 0 : article.citationCount}"/>
        </p>

<c:if test="${not empty article.journalName}">
    <p>
        <span class="badge">期刊</span>
        <c:out value="${article.journalName}"/>
        <c:if test="${not empty article.journalIssn}">（ISSN：<c:out value="${article.journalIssn}"/>）</c:if>
    </p>
</c:if>

<c:if test="${article.publishedAt != null || article.publishYear != null}">
    <p>
        <span class="badge">发表</span>
        <c:choose>
            <c:when test="${article.publishedAt != null}">
                <c:out value="${fn:substring(article.publishedAt, 0, 10)}"/>
            </c:when>
            <c:otherwise>
                <c:out value="${article.publishYear}"/>
            </c:otherwise>
        </c:choose>
    </p>
</c:if>

<c:if test="${not empty article.volume || not empty article.issue || not empty article.pageRange}">
    <p>
        <span class="badge">卷期页码</span>
        <c:if test="${not empty article.volume}"><c:out value="${article.volume}"/></c:if>
        <c:if test="${not empty article.issue}">（<c:out value="${article.issue}"/>）</c:if>
        <c:if test="${not empty article.pageRange}">：<c:out value="${article.pageRange}"/></c:if>
    </p>
</c:if>

<c:if test="${not empty article.doi}">
    <p>
        <span class="badge">DOI</span>
        <a style="text-decoration:none;" target="_blank" rel="noopener"
           href="https://doi.org/<c:out value='${article.doi}'/>">
            <c:out value="${article.doi}"/>
        </a>
    </p>
</c:if>

<c:if test="${not empty article.articleType}">
    <p><span class="badge">类型</span> <c:out value="${article.articleType}"/></p>
</c:if>

<c:if test="${not empty article.language}">
    <p><span class="badge">语言</span> <c:out value="${article.language}"/></p>
</c:if>

<c:if test="${not empty article.classificationNo}">
    <p><span class="badge">分类号</span> <c:out value="${article.classificationNo}"/></p>
</c:if>

<c:if test="${not empty article.fundingInfo}">
    <p><span class="badge">基金</span> <c:out value="${article.fundingInfo}"/></p>
</c:if>

<c:if test="${not empty article.cnkiUrl}">
    <p>
        <span class="badge">CNKI</span>
        <a style="text-decoration:none;" target="_blank" rel="noopener" href="<c:out value='${article.cnkiUrl}'/>">
            查看来源页
        </a>
    </p>
</c:if>
<h3>摘要</h3>
        <c:choose>
            <c:when test="${not empty article.abstractText}">
                                <div class="ql-snow richtext-view">
                    <div class="ql-editor">
                        <c:out value="${article.abstractText}" escapeXml="false"/>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <p>暂无摘要。</p>
            </c:otherwise>
        </c:choose>

        <div class="actions">
            <a class="btn-primary" style="text-decoration:none;" href="${ctx}/articles/download?id=${article.manuscriptId}">
                <i class="bi bi-download" aria-hidden="true"></i>
                下载论文
            </a>
            <a class="btn" style="text-decoration:none;" href="${ctx}/articles?type=latest">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                返回列表
            </a>
        </div>
    </div>
</c:if>

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
