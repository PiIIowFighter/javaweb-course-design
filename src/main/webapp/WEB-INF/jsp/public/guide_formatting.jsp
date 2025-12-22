<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack">
    <div class="card-header">
        <div>
            <h2 class="card-title">Formatting（格式要求）</h2>
            <p class="card-subtitle">占位页：后续可接入模板下载、格式检查后端（如上传并自动校验）。</p>
        </div>
        <a href="${ctx}/guide" style="white-space:nowrap; text-decoration:none;">
            返回用户指南 <i class="bi bi-arrow-right" aria-hidden="true"></i>
        </a>
    </div>

    <h3>基本要求（示例）</h3>
    <ul>
        <li>稿件文件：建议 PDF + 源文件（Word/LaTeX）。</li>
        <li>图表：清晰、编号完整、带标题与单位；引用数据需注明来源。</li>
        <li>参考文献：按期刊格式统一（后续可提供 BibTeX/EndNote 样式文件）。</li>
        <li>补充材料：数据、代码、附录可作为 Supplementary files 上传。</li>
    </ul>

    <h3>与系统的对应关系</h3>
    <ul>
        <li>编辑部管理员可进行“形式审查 / 格式检查”（后台功能）。</li>
        <li>作者在投稿页面上传 Manuscript 与 Cover Letter。</li>
    </ul>

    <div class="actions">
        <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
            <i class="bi bi-upload" aria-hidden="true"></i>
            Submit your article
        </a>
        <a style="text-decoration:none;" href="${ctx}/publish">
            <i class="bi bi-diagram-3" aria-hidden="true"></i>
            发表流程
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
