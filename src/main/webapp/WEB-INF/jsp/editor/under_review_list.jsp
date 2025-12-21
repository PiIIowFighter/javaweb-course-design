<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>送外审稿件列表（UNDER_REVIEW）</h2>
<p>此页面用于展示已经送外审、由审稿人评审中的稿件（状态：UNDER_REVIEW）。</p>

<c:if test="${empty manuscripts}">
    <p>当前没有审稿中的稿件。</p>
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
                <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">查看详情</a>
                <c:if test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                    
                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<h3>根据外审意见提交编辑建议</h3>
<p>编辑在充分参考外审专家意见后，可以在此对稿件给出处理建议（例如：ACCEPT / MINOR_REVISION / MAJOR_REVISION / REJECT），提交后稿件状态将进入 EDITOR_RECOMMENDATION，等待主编最终决策。</p>

<form method="post" action="${pageContext.request.contextPath}/editor/recommend">
    <label>稿件编号：
        <input type="number" name="manuscriptId" required/>
    </label>
    <label>编辑建议：
        <select name="suggestion" required>
            <option value="ACCEPT">接受</option>
            <option value="MINOR_REVISION">小修后接受</option>
            <option value="MAJOR_REVISION">大修后再审</option>
            <option value="REJECT">拒稿</option>
        </select>
    </label>
    <button type="submit">提交编辑建议</button>
</form>


<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
