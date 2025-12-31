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
                    <a href="${ctx}/manuscripts/detail?id=${m.manuscriptId}">查看详情</a>
                    <!-- 仅编辑部管理员显示实际操作按钮 -->
                    <c:if test="${sessionScope.currentUser.roleCode == 'EO_ADMIN'}">
                        <form method="post" action="${ctx}/editor/formalCheck" style="display:inline">
                            <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                            <c:choose>
                                <c:when test="${m.currentStatus == 'SUBMITTED'}">
                                    <button type="submit" name="op" value="start">
                                        开始形式审查
                                    </button>
                                    <button type="submit" name="op" value="return"
                                            onclick="return confirm('确认将该稿件退回作者修改格式？');">
                                        直接退回作者
                                    </button>
                                </c:when>
                                <c:when test="${m.currentStatus == 'FORMAL_CHECK'}">
                                    <button type="submit" name="op" value="approve">
                                        格式合格，送主编案头
                                    </button>
                                    <button type="submit" name="op" value="return"
                                            onclick="return confirm('确认将该稿件退回作者修改格式？');">
                                        退回作者修改格式
                                    </button>
                                </c:when>
                                <c:otherwise>
                                    <!-- 其他状态不展示可点击操作 -->
                                </c:otherwise>
                            </c:choose>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
