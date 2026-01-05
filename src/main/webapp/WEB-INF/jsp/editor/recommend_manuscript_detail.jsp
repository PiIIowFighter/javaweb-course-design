<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>稿件详情</h2>

<c:if test="${empty manuscript}">
    <p style="color:#d00;">未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">

    <div class="card" style="max-width: 1100px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">稿件详情</h3>
                <p class="card-subtitle">
                    ManuscriptId：<strong><c:out value="${manuscript.manuscriptId}"/></strong>
                    &nbsp;&nbsp;|&nbsp;&nbsp;
                    当前状态：<strong><c:out value="${manuscript.currentStatus}"/></strong>
                </p>
            </div>
        </div>
        <div class="card-body">
            <table border="1" cellpadding="6" cellspacing="0" style="width:100%;">
                <tr><th style="width:160px;">标题</th><td><c:out value="${manuscript.title}"/></td></tr>
                <tr><th>研究主题</th><td><c:out value="${manuscript.subjectArea}"/></td></tr>
                <tr><th>关键词</th><td><c:out value="${manuscript.keywords}"/></td></tr>
                <tr><th>资助信息</th><td><c:out value="${manuscript.fundingInfo}"/></td></tr>
                <tr><th>投稿时间</th><td><c:out value="${manuscript.submitTime}"/></td></tr>
                <tr><th>决策</th><td><c:out value="${manuscript.decision}"/></td></tr>
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
            </table>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">作者列表</h3>
            </div>
        </div>
        <div class="card-body">
            <c:if test="${empty authors}">
                <p>暂无作者信息。</p>
            </c:if>
            <c:if test="${not empty authors}">
                <table border="1" cellpadding="6" cellspacing="0" style="width:100%;">
                    <thead>
                    <tr>
                        <th style="width:64px;">序号</th>
                        <th>姓名</th>
                        <th>单位</th>
                        <th>Email</th>
                        <th style="width:90px;">通讯作者</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${authors}" var="a" varStatus="st">
                        <tr>
                            <td><c:out value="${st.index + 1}"/></td>
                            <td><c:out value="${a.fullName}"/></td>
                            <td><c:out value="${a.affiliation}"/></td>
                            <td><c:out value="${a.email}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${a.corresponding}">是</c:when>
                                    <c:otherwise>否</c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">作者推荐审稿人</h3>
            </div>
        </div>
        <div class="card-body">
            <c:if test="${empty recommendedReviewers}">
                <p>作者未推荐审稿人。</p>
            </c:if>
            <c:if test="${not empty recommendedReviewers}">
                <table border="1" cellpadding="6" cellspacing="0" style="width:100%;">
                    <thead>
                    <tr>
                        <th style="width:64px;">序号</th>
                        <th>姓名</th>
                        <th>Email</th>
                        <th>推荐理由</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${recommendedReviewers}" var="rr" varStatus="st">
                        <tr>
                            <td><c:out value="${st.index + 1}"/></td>
                            <td><c:out value="${rr.fullName}"/></td>
                            <td><c:out value="${rr.email}"/></td>
                            <td><c:out value="${rr.reason}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">附件下载</h3>
            </div>
        </div>
        <div class="card-body">
            <c:if test="${empty currentVersion}">
                <p>暂无版本文件。</p>
            </c:if>
            <c:if test="${not empty currentVersion}">
                <div style="line-height: 1.9;">

                    <c:if test="${not empty currentVersion.fileOriginalPath or not empty currentVersion.fileAnonymousPath}">
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">Manuscript 预览/下载</a>
                    </c:if>

                    <c:if test="${not empty currentVersion.fileAnonymousPath}">
                        <span style="margin-left:12px;"></span>
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=anonymous">匿名稿 预览/下载</a>
                    </c:if>

                    <c:if test="${not empty currentVersion.coverLetterPath}">
                        <span style="margin-left:12px;"></span>
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">Cover Letter 预览/下载</a>
                    </c:if>

                    <c:if test="${not empty currentVersion.responseLetterPath}">
                        <span style="margin-left:12px;"></span>
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=response">Response Letter 预览/下载</a>
                    </c:if>

                    <c:if test="${empty currentVersion.fileOriginalPath and empty currentVersion.fileAnonymousPath and empty currentVersion.coverLetterPath and empty currentVersion.responseLetterPath}">
                        暂无附件
                    </c:if>
                </div>
            </c:if>
        </div>
    </div>

    <div style="max-width: 1100px; margin-top: 14px; display:flex; justify-content:flex-end;">
        <button type="button" class="btn btn-quiet" onclick="if (history.length > 1) { history.back(); } else { window.location.href='${ctx}/editor/recommend'; }">返回</button>
    </div>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
