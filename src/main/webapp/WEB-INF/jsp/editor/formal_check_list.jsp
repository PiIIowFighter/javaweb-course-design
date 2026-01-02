<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>形式审查 / 格式检查工作台</h2>
<p>此页面用于编辑部管理员对作者新提交的稿件进行形式审查和格式检查：</p>
<ul>
    <li>SUBMITTED：作者刚提交的稿件，尚未开始形式审查；</li>
    <li>FORMAL_CHECK：正在形式审查 / 格式检查阶段的稿件。</li>
</ul>

<c:if test="${empty manuscripts}">
    <p>当前没有需要形式审查或格式检查的稿件。</p>
</c:if>
<c:if test="${not empty manuscripts}">
    <c:set var="ctx" value="${pageContext.request.contextPath}"/>
    <table border="1" cellpadding="4" cellspacing="0">
        <thead>
        <tr>
            <th>ID</th>
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
                <td><c:out value="${m.submitTime}"/></td>
                <td>
                    <c:if test="${sessionScope.currentUser.roleCode == 'EO_ADMIN'}">
                        <c:choose>
                            <c:when test="${m.currentStatus == 'SUBMITTED'}">
                                <!-- 一开始仅保留“点击开始审查”按钮；点击后由后端更新状态并跳转到稿件详情页 -->
                                <form method="post" action="${ctx}/editor/formalCheck" style="display:inline">
                                    <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                                    <button class="btn btn-primary" type="submit" name="op" value="start">点击开始审查</button>
                                </form>
                            </c:when>
                            <c:when test="${m.currentStatus == 'FORMAL_CHECK'}">
                                <!-- 审查进行中：直接进入稿件详情页继续审查 -->
                                <a class="btn btn-primary" href="${ctx}/manuscripts/detail?id=${m.manuscriptId}">进入审查</a>
                            </c:when>
                            <c:otherwise>
                                --
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
