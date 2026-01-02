<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<style>
    /* Page-specific tweaks to keep the history table readable */
    .history-table th { white-space: nowrap; }
    .history-table td { word-break: break-word; }

    /* Fixed widths for compact columns */
    .history-table th.col-time, .history-table td.col-time { width: 170px; }
    .history-table th.col-id, .history-table td.col-id { width: 90px; }
    .history-table th.col-result, .history-table td.col-result { width: 96px; }
    .history-table th.col-sim, .history-table td.col-sim { width: 110px; }
    .history-table th.col-actions, .history-table td.col-actions { width: 240px; }

    /* Let long text wrap instead of squeezing other columns */
    .history-table td.col-title { max-width: 420px; }
    .history-table td.col-summary { max-width: 520px; }

    .summary-clamp {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .action-group {
        display: flex;
        flex-wrap: wrap;
        gap: var(--space-2);
        align-items: center;
        white-space: nowrap;
    }
    .action-group .btn { white-space: nowrap; }

    .badge-pass {
        border-color: rgba(0, 90, 156, 0.22);
        background: rgba(0, 90, 156, 0.08);
        color: rgba(0, 90, 156, 0.95);
    }
    .badge-fail {
        border-color: rgba(190, 18, 60, 0.30);
        background: rgba(190, 18, 60, 0.06);
        color: rgba(190, 18, 60, 0.95);
    }
</style>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">审查历史</h2>
            <p class="card-subtitle">展示你历史提交的形式审查记录（最新在前）。</p>
        </div>
    </div>

    <c:if test="${empty history}">
        <div class="alert">暂无审查历史记录。</div>
    </c:if>

    <c:if test="${not empty history}">
        <div style="overflow:auto;">
            <table class="history-table">
                <thead>
                <tr>
                    <th class="col-time">审查时间</th>
                    <th class="col-id">稿件编号</th>
                    <th>稿件标题</th>
                    <th class="col-result">结果</th>
                    <th class="col-sim">相似度</th>
                    <th>反馈摘要</th>
                    <th class="col-actions">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${history}" var="r">
                    <c:set var="m" value="${manuscriptMap[r.manuscriptId]}"/>
                    <tr>
                        <td class="col-time"><c:out value="${r.checkTime}"/></td>
                        <td class="col-id"><c:out value="${r.manuscriptId}"/></td>
                        <td class="col-title">
                            <c:choose>
                                <c:when test="${not empty m}">
                                    <c:out value="${m.title}"/>
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col-result">
                            <c:choose>
                                <c:when test="${r.checkResult == 'PASS'}">
                                    <span class="badge badge-pass">通过</span>
                                </c:when>
                                <c:when test="${r.checkResult == 'FAIL'}">
                                    <span class="badge badge-fail">退回</span>
                                </c:when>
                                <c:otherwise><span class="badge"><c:out value="${r.checkResult}"/></span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col-sim">
                            <c:choose>
                                <c:when test="${not empty r.similarityScore}">
                                    <c:out value="${r.similarityScore}"/>%
                                </c:when>
                                <c:otherwise>--</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="col-summary">
                            <span class="summary-clamp"><c:out value="${r.feedback}"/></span>
                        </td>
                        <td class="col-actions">
                            <div class="action-group">
                                <a class="btn btn-quiet"
                                   href="${ctx}/editor/formalCheck/history/detail?checkId=${r.checkId}">查看详情</a>
                                <a class="btn btn-quiet"
                                   href="${ctx}/manuscripts/detail?id=${r.manuscriptId}">查看稿件详情</a>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
