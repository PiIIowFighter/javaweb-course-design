<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>案头稿件列表（FORMAL_CHECK 之后、待进一步处理）</h2>
<p>此页面展示已经通过形式审查、进入编辑部案头阶段（状态：DESK_REVIEW_INITIAL）的稿件。</p>

<c:if test="${empty manuscripts}">
    <p>当前没有需要处理的案头稿件。</p>
</c:if>
<c:if test="${not empty manuscripts}">
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
    <c:forEach items="${manuscripts}" var="m">
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
                <c:if test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                    <form method="post" action="${pageContext.request.contextPath}/editor/desk" style="display:inline">
                        <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                        <button type="submit" name="op" value="deskAccept">
                            送外审 / 指派编辑（Desk Accept）
                        </button>
                        <button type="submit" name="op" value="deskReject"
                                onclick="return confirm('确认直接退稿？该操作将把稿件状态标记为 REJECTED。');">
                            退稿（Desk Reject）
                        </button>
                    </form>

                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
