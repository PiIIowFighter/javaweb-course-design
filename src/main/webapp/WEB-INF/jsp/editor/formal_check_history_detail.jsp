<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>审查记录详情</h2>

<c:if test="${empty result}">
    <p>未找到该审查记录。</p>
</c:if>

<c:if test="${not empty result}">
    <div class="card" style="margin-top: var(--space-5);">
        <div class="stack" style="gap: 8px;">
            <div>
                <strong>记录编号：</strong><c:out value="${result.checkId}"/>
            </div>
            <div>
                <strong>稿件编号：</strong><c:out value="${result.manuscriptId}"/>
            </div>
            <c:if test="${not empty manuscript}">
                <div>
                    <strong>稿件标题：</strong><c:out value="${manuscript.title}"/>
                </div>
            </c:if>
            <div>
                <strong>审查时间：</strong><c:out value="${result.checkTime}"/>
            </div>
            <div>
                <strong>审查结果：</strong><c:out value="${result.checkResult}"/>
            </div>
        </div>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <h3 style="margin:0 0 10px 0;">检查项</h3>
        <table border="1" cellpadding="4" cellspacing="0">
            <tbody>
            <tr><th style="width:220px;">作者信息</th><td><c:out value="${result.authorInfoValid}"/></td></tr>
            <tr><th>摘要字数</th><td><c:out value="${result.abstractWordCountValid}"/></td></tr>
            <tr><th>正文字数</th><td><c:out value="${result.bodyWordCountValid}"/></td></tr>
            <tr><th>关键词</th><td><c:out value="${result.keywordsValid}"/></td></tr>
            <tr><th>注释编号（人工）</th><td><c:out value="${result.footnoteNumberingValid}"/></td></tr>
            <tr><th>图表格式（人工）</th><td><c:out value="${result.figureTableFormatValid}"/></td></tr>
            <tr><th>参考文献格式（人工）</th><td><c:out value="${result.referenceFormatValid}"/></td></tr>
            <tr><th>查重率</th><td><c:out value="${result.similarityScore}"/></td></tr>
            <tr><th>高相似度</th><td><c:out value="${result.highSimilarity}"/></td></tr>
            <tr><th>查重报告</th>
                <td>
                    <c:choose>
                        <c:when test="${not empty result.plagiarismReportUrl}">
                            <a href="<c:out value='${result.plagiarismReportUrl}'/>">打开报告</a>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
            </tr>
            </tbody>
        </table>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <h3 style="margin:0 0 10px 0;">反馈意见</h3>
        <div style="white-space: pre-wrap; line-height: 1.6;">
            <c:out value="${result.feedback}"/>
        </div>
    </div>

    <div class="stack" style="flex-direction: row; gap: 10px; margin-top: var(--space-5);">
        <a class="btn btn-quiet" href="${ctx}/editor/formalCheck/history">返回历史列表</a>
        <c:if test="${not empty manuscript}">
            <a class="btn btn-primary" href="${ctx}/manuscripts/detail?id=${result.manuscriptId}">查看稿件详情</a>
        </c:if>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
