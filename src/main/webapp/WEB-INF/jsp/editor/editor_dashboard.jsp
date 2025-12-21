<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">编辑部工作台</h2>
            <p class="card-subtitle">在一个视图中完成形式审查、案头审查、外审推进与终审决策。</p>
        </div>
    </div>

    <div class="grid grid-2">
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/formalCheck">
            <h3><i class="bi bi-clipboard-check" aria-hidden="true"></i> 形式审查</h3>
            <p>处理 SUBMITTED / FORMAL_CHECK：检查格式与基础合规性。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开工作台</small>
        </a>
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/desk">
            <h3><i class="bi bi-search" aria-hidden="true"></i> 案头审查</h3>
            <p>处理 DESK_REVIEW_INITIAL：初筛、建议退稿或进入分配环节。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>

        <c:choose>
            <c:when test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/toAssign">
                    <h3><i class="bi bi-diagram-3" aria-hidden="true"></i> 待分配队列</h3>
                    <p>处理 TO_ASSIGN：指派责任编辑与审稿人，推进外审流程。</p>
                    <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
                </a>
            </c:when>
            <c:otherwise>
                <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/withEditor">
                    <h3><i class="bi bi-person-workspace" aria-hidden="true"></i> 我的待处理稿件</h3>
                    <p>处理 WITH_EDITOR：为稿件选择审稿人并发起外审。</p>
                    <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
                </a>
            </c:otherwise>
        </c:choose>

        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/underReview">
            <h3><i class="bi bi-hourglass-split" aria-hidden="true"></i> 审稿中</h3>
            <p>处理 UNDER_REVIEW：跟踪审稿进度与回收评审意见。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/finalDecision">
            <h3><i class="bi bi-check2-circle" aria-hidden="true"></i> 终审决策</h3>
            <p>编辑推荐意见与主编终审：录用 / 退稿 / 退修。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/reviewers">
            <h3><i class="bi bi-people" aria-hidden="true"></i> 审稿人库</h3>
            <p>维护审稿人信息（仅主编可操作）。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 管理库</small>
        </a>
    </div>

    <div class="stack" style="margin-top: var(--space-6);">
        <small>后续可在列表页中接入更丰富的筛选与统计，按状态机（DRAFT、SUBMITTED、FORMAL_CHECK、UNDER_REVIEW、REVISION、ACCEPTED、REJECTED 等）展示不同环节。</small>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
