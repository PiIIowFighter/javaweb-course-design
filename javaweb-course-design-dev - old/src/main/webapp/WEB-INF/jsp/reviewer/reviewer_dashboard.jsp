<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">审稿人工作台</h2>
            <p class="card-subtitle">查看分配给你的稿件，提交评审意见，并回顾历史记录。</p>
        </div>
    </div>

    <div class="grid grid-2">
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/reviewer/assigned">
            <h3><i class="bi bi-inbox" aria-hidden="true"></i> 待评审</h3>
            <p>查看当前分配给你的审稿任务并提交评审表。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/reviewer/history">
            <h3><i class="bi bi-clock-history" aria-hidden="true"></i> 历史记录</h3>
            <p>回顾以往提交过的评审意见与决策。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 查看历史</small>
        </a>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
