<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">用户指南 (Guide for authors)</h2>
            <p class="card-subtitle">为用户提供期刊介绍、投稿指南与写作/格式要求入口</p>
        </div>
    </div>

    <h3>推荐阅读</h3>
    <ul class="list">
        <li class="list-item">
            <span class="avatar" aria-hidden="true"><i class="bi bi-info-circle"></i></span>
            <div>
                <div class="list-title"><a style="text-decoration:none;" href="${ctx}/about/aims">About the journal</a></div>
                <div class="list-meta">期刊主旨与投稿范围（Aims and scope）</div>
            </div>
        </li>
        <li class="list-item">
            <span class="avatar" aria-hidden="true"><i class="bi bi-shield-check"></i></span>
            <div>
                <div class="list-title"><a style="text-decoration:none;" href="${ctx}/about/policies">Ethics and policies</a></div>
                <div class="list-meta">出版伦理、同行评审政策、版权与许可等</div>
            </div>
        </li>
        <li class="list-item">
            <span class="avatar" aria-hidden="true"><i class="bi bi-pencil"></i></span>
            <div>
                <div class="list-title"><a style="text-decoration:none;" href="${ctx}/guide/writing">Writing</a></div>
                <div class="list-meta">写作结构建议、引用规范与可复现性建议（占位页）</div>
            </div>
        </li>
        <li class="list-item">
            <span class="avatar" aria-hidden="true"><i class="bi bi-layout-text-window"></i></span>
            <div>
                <div class="list-title"><a style="text-decoration:none;" href="${ctx}/guide/formatting">Writing and formatting</a></div>
                <div class="list-meta">模板与格式要求、图表/参考文献规范（占位页）</div>
            </div>
        </li>
    </ul>

    <h3>投稿前准备</h3>
    <ul>
        <li>准备稿件文件（Manuscript）与 Cover Letter。</li>
        <li>确保标题、摘要、关键词、研究主题、作者列表等元数据齐全。</li>
        <li>可选：填写项目资助信息与推荐审稿人。</li>
    </ul>

    <h3>系统内操作</h3>
    <ol>
        <li>注册并登录系统。</li>
        <li>进入“投稿（Submit）”模块，创建新稿件。</li>
        <li>支持先保存为草稿，再最终提交。</li>
        <li>在“我的稿件”中查看状态流转与编辑/审稿反馈。</li>
    </ol>

    <p class="card-subtitle">
        后端拓展建议：如果你希望“Writing/Formatting”从数据库动态读取并支持模板下载，
        可以新增如 <span class="badge">dbo.AuthorGuidelines</span> / <span class="badge">dbo.FileResources</span> 等表，
        并在管理员端提供富文本编辑与附件上传。
    </p>

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
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
