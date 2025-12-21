<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>编辑部管理员工作台</h2>

<ul>
    <li><a href="${pageContext.request.contextPath}/editor/formalCheck">
        形式审查 / 格式检查工作台
    </a></li>
    <li><a href="${pageContext.request.contextPath}/editor/dashboard">
        稿件与审稿工作台（编辑部视角）
    </a></li>
    <li><a href="${pageContext.request.contextPath}/admin/news/list">
        新闻 / 公告管理
    </a></li>
    <!-- 后续还可以加：形式审查列表、退修管理等链接 -->
</ul>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
