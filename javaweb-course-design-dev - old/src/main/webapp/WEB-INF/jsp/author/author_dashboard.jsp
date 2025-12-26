<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">作者工作台</h2>
            <p class="card-subtitle">管理稿件、保存草稿、提交投稿，并跟踪处理进度。</p>
        </div>
    </div>

    <div class="grid grid-2">
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/manuscripts/list">
            <h3>我的稿件</h3>
            <p>按分类查看草稿、处理中、待修改、已决策的稿件。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/manuscripts/submit">
            <h3>新建投稿</h3>
            <p>填写元数据，上传手稿与 Cover Letter，并支持推荐审稿人。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 开始投稿</small>
        </a>
    </div>

    <div class="stack" style="margin-top: var(--space-6);">
        <small>可在后续迭代中补充各状态统计，例如：DRAFT、SUBMITTED、FORMAL_CHECK、UNDER_REVIEW、REVISION、ACCEPTED、REJECTED 等数量。</small>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
