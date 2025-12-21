<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">征稿通知 (Call for papers)</h2>
            <p class="card-subtitle">Special issue 列表（当前为占位页）</p>
        </div>
    </div>

    <p>
        当前项目的数据库与 src 代码中尚未实现 “征稿通知 / 专刊（Special Issues）” 的完整后端。
        你可以后续新增对应数据表与管理页面后，再将首页与本页面接入真实数据。
    </p>

    <h3>建议的后端建模（可选）</h3>
    <ul>
        <li><span class="badge">dbo.SpecialIssues</span>：IssueId, Title, Scope, Deadline, PublishedAt, IsPublished...</li>
        <li><span class="badge">dbo.CallForPapers</span>：CfpId, Title, Content, Deadline, IsPublished...</li>
        <li>管理员端：新增/编辑/发布/下架；前台端：列表与详情页。</li>
    </ul>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a style="text-decoration:none;" href="${ctx}/">
            <i class="bi bi-house" aria-hidden="true"></i>
            返回首页
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
