<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>主编全览（系统内所有稿件）</h2>
<p>
    该页面用于主编查看系统内全部稿件的当前状态与关键时间点。
    如需查看稿件的<strong>审稿流程 / 评审记录 / 版本与附件</strong>，请点击“查看详情”进入稿件详情页。
</p>

<c:if test="${empty manuscripts}">
    <p>系统内暂时没有任何稿件记录。</p>
</c:if>

<c:if test="${not empty manuscripts}">
    <table border="1" cellpadding="4" cellspacing="0">
        <thead>
        <tr>
            <th>稿件编号</th>
            <th>标题</th>
            <th>当前状态</th>
            <th>决策</th>
            <th>终审时间</th>
            <th>提交时间</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${manuscripts}" var="m">
            <tr>
                <td><c:out value="${m.manuscriptId}"/></td>
                <td><c:out value="${m.title}"/></td>
                <td><c:out value="${m.currentStatus}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty m.decision}">
                            <c:out value="${m.decision}"/>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${m.finalDecisionTime != null}">
                            <c:out value="${m.finalDecisionTime}"/>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${m.submitTime != null}">
                            <c:out value="${m.submitTime}"/>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">
                        查看详情
                    </a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
