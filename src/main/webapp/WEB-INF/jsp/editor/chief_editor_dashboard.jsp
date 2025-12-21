<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<h2>主编工作台</h2>

<p>
    角色说明：期刊的最高学术负责人，拥有最终录用权。
</p>

<h3>核心功能</h3>
<ul>
    <li>
        <strong>全览权限：</strong>
        <a href="${pageContext.request.contextPath}/editor/overview">查看系统内所有稿件状态 / 历史入口 / 审稿流程</a>
        （通过“查看详情”进入稿件详情页，可查看评审记录、版本与附件）。
    </li>
    <li>
        <strong>初审（Desk Review）：</strong>
        <a href="${pageContext.request.contextPath}/editor/desk">判断稿件是否送审</a>
        ，支持 Desk Accept / Desk Reject。
    </li>
    <li>
        <strong>指派编辑：</strong>
        <a href="${pageContext.request.contextPath}/editor/toAssign">将稿件分配给特定责任编辑处理</a>
        。
    </li>
    <li>
        <strong>终审决策（Final Decision）：</strong>
        <a href="${pageContext.request.contextPath}/editor/finalDecision">基于编辑与审稿意见做最终决定</a>
        ，支持 Accept / Reject / Revision。
    </li>
    <li>
        <strong>管理审稿人库：</strong>
        <a href="${pageContext.request.contextPath}/editor/reviewers">邀请、移除、审核审稿人资格</a>
        。
    </li>
    <li>
        <strong>特殊权限：</strong>
        <a href="${pageContext.request.contextPath}/editor/special">撤稿（Retract）、撤销终审决定（Rescind Decision）</a>
        。
    </li>
</ul>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
