<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">论文发表 (Publish)</h2>
            <p class="card-subtitle">期刊投稿与发表流程简介（占位页，可后续接入更完整后端）</p>
        </div>
    </div>

    <h3>流程概览</h3>
    <ul>
        <li>作者在线提交稿件（支持草稿与最终提交）。</li>
        <li>编辑部管理员进行形式审查与格式检查。</li>
        <li>主编进行初审、指派编辑、终审决策。</li>
        <li>必要时进入外审流程，并收集审稿意见。</li>
    </ul>

    <p class="card-subtitle">
        说明：当前系统中“已发表论文库 / 期刊卷期 / 引用与下载统计”尚未建模，
        如需实现与期刊网站一致的 Publish 页面，建议后续新增相关数据表与管理界面。
    </p>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a style="text-decoration:none;" href="${ctx}/guide">
            <i class="bi bi-book" aria-hidden="true"></i>
            Guide for authors
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
