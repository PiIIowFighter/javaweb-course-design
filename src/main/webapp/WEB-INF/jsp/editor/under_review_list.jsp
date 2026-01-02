<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>外审阶段稿件列表</h2>

<p>
    此页面仅展示 <strong>UNDER_REVIEW</strong>（外审进行中）的稿件。
    已完成外审并进入 <strong>EDITOR_RECOMMENDATION</strong> 的稿件请到“提出建议”模块查看。
</p>

<h3>外审进行中（UNDER_REVIEW）</h3>

<p style="margin:8px 0;">
    <a class="btn btn-quiet" href="${pageContext.request.contextPath}/editor/review/monitor">进入全局催审/逾期监控</a>
    <span style="margin-left:8px; color:#666;">（用于查看所有稿件的逾期审稿与批量催审；单篇稿件的邀请/撤回/催审请点击下方“查看详细信息”。）</span>
</p>

<c:if test="${empty underReviewList}">
    <p>当前没有外审进行中的稿件。</p>
</c:if>

<c:if test="${not empty underReviewList}">
    <table border="1" cellpadding="4" cellspacing="0">
        <thead>
        <tr>
            <th>稿件编号</th>
            <th>标题</th>
            <th>当前状态</th>
            <th>提交时间</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${underReviewList}" var="m">
            <tr>
                <td><c:out value="${m.manuscriptId}"/></td>
                <td><c:out value="${m.title}"/></td>
                <td><c:out value="${m.currentStatus}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${m.submitTime != null}">
                            <c:out value="${m.submitTime}"/>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a class="btn btn-quiet" href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}#inviteReviewers">查看详细信息</a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>