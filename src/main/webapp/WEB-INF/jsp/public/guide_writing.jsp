<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">Writing（写作指南）</h2>
            <p class="card-subtitle">占位页：后续你可以接入数据库或文件下载实现更完整的“作者写作指南”。</p>
        </div>
        <a href="${ctx}/guide" style="white-space:nowrap; text-decoration:none;">
            返回用户指南 <i class="bi bi-arrow-right" aria-hidden="true"></i>
        </a>
    </div>

    <h3>建议结构</h3>
    <ul>
        <li>Title / Abstract / Keywords：简洁明确，突出贡献。</li>
        <li>Introduction：交代背景、现有工作不足、本文贡献点。</li>
        <li>Method：方法描述可复现（公式、伪代码、参数设置）。</li>
        <li>Experiments：数据集、指标、对比方法与消融实验。</li>
        <li>Conclusion：总结贡献与未来工作。</li>
    </ul>

    <h3>常见写作规范</h3>
    <ul>
        <li>避免一稿多投、抄袭、数据造假；如有利益冲突需声明。</li>
        <li>引用他人工作时必须标注参考文献。</li>
        <li>鼓励提供数据/代码链接以提升可复现性。</li>
    </ul>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a style="text-decoration:none;" href="${ctx}/about/policies">
            <i class="bi bi-shield-check" aria-hidden="true"></i>
            Ethics &amp; Policies
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
