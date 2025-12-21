<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<h2>主编工作台</h2>
<ul>
    <li>
        <a href="${pageContext.request.contextPath}/editor/desk">
            待初审稿件（DESK_REVIEW_INITIAL）
        </a>
    </li>
    <li>
        <a href="${pageContext.request.contextPath}/editor/toAssign">
            待分配编辑/审稿人（TO_ASSIGN）
        </a>
    </li>
    <li>
        <a href="${pageContext.request.contextPath}/editor/finalDecision">
            待最终决定稿件（FINAL_DECISION_PENDING）
        </a>
    </li>
    <li>
        <a href="${pageContext.request.contextPath}/editor/reviewers">
            审稿人库管理
        </a>
    </li>
</ul>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
