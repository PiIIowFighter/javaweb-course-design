<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <h2 class="card-title">稿件详情</h2>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty manuscript}">
            <p>未找到指定的稿件记录。</p>
        </c:when>
        <c:otherwise>
            <!-- 基本信息 -->
            <div class="stack">
                <p><strong>稿件编号：</strong><c:out value="${manuscript.manuscriptId}"/></p>
                <p><strong>标题：</strong><c:out value="${manuscript.title}"/></p>
                <p><strong>期刊ID：</strong><c:out value="${manuscript.journalId}"/></p>
                <p><strong>所属专刊：</strong>
                    <c:choose>
                        <c:when test="${not empty linkedIssue}">
                            <a href="${ctx}/issues?view=detail&id=${linkedIssue.issueId}"><c:out value="${linkedIssue.title}"/></a>
                        </c:when>
                        <c:when test="${not empty manuscript.issueTitle}">
                            <c:out value="${manuscript.issueTitle}"/>
                        </c:when>
                        <c:otherwise>（默认 / 未关联）</c:otherwise>
                    </c:choose>
                </p>
                <p><strong>当前状态：</strong><c:out value="${manuscript.currentStatus}"/></p>
                <p><strong>提交时间：</strong><c:out value="${manuscript.submitTime}"/></p>
                <p><strong>关键词：</strong><c:out value="${manuscript.keywords}"/></p>
            </div>

            <!-- 摘要 -->
            <div class="stack">
                <p><strong>摘要：</strong></p>
                                <div class="ql-snow richtext-view">
                    <div class="ql-editor">
                        <c:out value="${manuscript.abstractText}" escapeXml="false"/>
                    </div>
                </div>
            </div>

            <!-- 稿件文件 -->
            <c:if test="${not empty currentVersion}">
                <div class="stack">
                    <h3>稿件文件</h3>
                    <div style="display: flex; flex-wrap: wrap; gap: 16px;">
                        <c:if test="${not empty currentVersion.fileOriginalPath}">
                            <div style="padding: 12px 16px; background: rgba(255,255,255,0.65); border: 1px solid var(--border); border-radius: var(--radius-sm);">
                                <strong>手稿文件：</strong>
                                <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">
                                    <i class="bi bi-file-pdf"></i> 预览/下载
                                </a>
                            </div>
                        </c:if>
                        
                        <c:if test="${not empty currentVersion.fileAnonymousPath}">
                            <div style="padding: 12px 16px; background: rgba(255,255,255,0.65); border: 1px solid var(--border); border-radius: var(--radius-sm);">
                                <strong>匿名手稿：</strong>
                                <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=anonymous">
                                    <i class="bi bi-file-pdf"></i> 预览/下载
                                </a>
                            </div>
                        </c:if>
                        
                        <c:if test="${not empty currentVersion.coverLetterPath}">
                            <div style="padding: 12px 16px; background: rgba(255,255,255,0.65); border: 1px solid var(--border); border-radius: var(--radius-sm);">
                                <strong>Cover Letter：</strong>
                                <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">
                                    <i class="bi bi-file-pdf"></i> 预览/下载
                                </a>
                            </div>
                        </c:if>
                    </div>
                </div>
            </c:if>

            <!-- 操作按钮 -->
            <div class="actions">
                <a class="btn-quiet" href="${ctx}/manuscripts/list" style="text-decoration:none;">
                    <i class="bi bi-arrow-left"></i> 返回列表
                </a>
                <a class="btn-quiet" href="${ctx}/manuscripts/track?id=${manuscript.manuscriptId}" style="text-decoration:none;">
                    <i class="bi bi-clock-history"></i> 状态追踪
                </a>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>