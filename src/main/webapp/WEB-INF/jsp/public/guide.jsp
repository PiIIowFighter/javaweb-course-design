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
        <h2 class="page-title"><c:choose>
                    <c:when test="${not empty page && not empty page.title}">
                        <c:out value="${page.title}"/>
                    </c:when>
                    <c:otherwise>用户指南 (Guide for authors)</c:otherwise>
                </c:choose></h2>
        <div class="chips">
            <span class="chip">为用户提供期刊介绍、投稿指南与写作/格式要求入口</span>
        </div>
    </div></div>
    </div>

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

    <c:if test="${not empty pageLoadError}">
        <div class="notice danger" style="margin-top: 14px;">
            <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
            <div>
                未找到页面配置数据（JournalPages 中没有对应记录）：pageKey=guide
            </div>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${not empty page && not empty page.content}">
            <div class="richtext" style="margin-top: 14px;">
                <c:out value="${page.content}" escapeXml="false"/>
            </div>
        </c:when>
        <c:otherwise>
            <div style="margin-top: 14px;">
                <h3>投稿准备</h3>
                <ul>
                    <li>确认研究主题符合期刊范围（Aims &amp; Scope）。</li>
                    <li>准备作者信息、单位、基金与通讯作者邮箱。</li>
                    <li>整理正文、图表、补充材料与数据/代码链接（如有）。</li>
                </ul>
                <h3>写作与格式</h3>
                <ul>
                    <li>摘要包含背景/方法/结果/结论四要素；关键词 3–6 个。</li>
                    <li>图表清晰，图题与注释完整；参考文献格式统一。</li>
                </ul>
                <p class="muted">提示：后台“期刊管理 → 关于期刊页面”可配置本页内容。</p>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
