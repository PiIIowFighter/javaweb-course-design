<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>稿件详情（审稿人匿名视图）</h2>

<c:if test="${empty review || empty manuscript}">
    <p>未找到审稿记录或稿件信息。</p>
</c:if>

<c:if test="${not empty review && not empty manuscript}">
    <div class="alert alert-info">
        <b>匿名审稿提醒：</b>本页面仅展示脱密信息（隐藏作者信息）。审稿过程中仅可查看脱密稿（匿名稿）。
    </div>

    <div style="margin: 8px 0;" class="text-muted">
        <strong>审稿记录ID：</strong><c:out value="${review.reviewId}"/>
        <span style="margin:0 10px;">|</span>
        <strong>审稿状态：</strong><c:out value="${review.status}"/>
        <span style="margin:0 10px;">|</span>
        <strong>截止时间：</strong><c:out value="${review.dueAt}"/>
    </div>

    <div class="card stack-lg" style="max-width: 980px;">
        <div class="card-header">
            <div>
                <h3 class="card-title" style="margin:0;">稿件元数据</h3>
                <p class="card-subtitle">不包含作者姓名、邮箱、单位等身份信息。</p>
            </div>
        </div>

        <div class="card-body">
            <table class="table" style="width:100%;">
                <tbody>
                <tr>
                    <th style="width: 160px;">标题</th>
                    <td><c:out value="${manuscript.title}"/></td>
                </tr>
                <tr>
                    <th>摘要</th>
                    <td>
                        <div class="ql-snow richtext-view">
                            <div class="ql-editor">
                                <c:out value="${manuscript.abstractText}" escapeXml="false"/>
                            </div>
                        </div>
                    </td>
                </tr>
                <tr>
                    <th>关键词</th>
                    <td><c:out value="${manuscript.keywords}"/></td>
                </tr>
                <tr>
                    <th>研究主题</th>
                    <td><c:out value="${manuscript.subjectArea}"/></td>
                </tr>
                <tr>
                    <th>项目资助</th>
                    <td><c:out value="${manuscript.fundingInfo}"/></td>
                </tr>
                </tbody>
            </table>

            <div class="actions" style="margin-top: 12px; gap: 10px;">
                <a class="btn-primary" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript" target="_blank" style="text-decoration:none;">
                    <i class="bi bi-file-earmark-pdf" aria-hidden="true"></i>
                    打开/下载脱密稿（PDF）
                </a>

                <a class="btn-quiet" href="${ctx}/reviewer/reviewForm?id=${review.reviewId}" style="text-decoration:none;">
                    <i class="bi bi-pencil-square" aria-hidden="true"></i>
                    填写评审意见
                </a>

                <a class="btn-quiet" href="${ctx}/reviewer/assigned" style="text-decoration:none;">
                    返回待评审列表
                </a>
            </div>
        </div>
    </div>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
