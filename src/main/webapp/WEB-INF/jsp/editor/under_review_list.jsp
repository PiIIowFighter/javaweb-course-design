<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>外审阶段稿件列表</h2>

<p>
    此页面展示外审相关阶段的稿件：
    <strong>UNDER_REVIEW</strong>（外审进行中） 与
    <strong>EDITOR_RECOMMENDATION</strong>（可提交编辑建议）。
</p>

<h3>外审进行中（UNDER_REVIEW）</h3>

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
                    <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">查看详情</a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>


<h3>提交编辑建议（仅限状态为 EDITOR_RECOMMENDATION）</h3>

<p>
    当某稿件的所有有效审稿邀请均已提交（SUBMITTED）后，系统会自动将稿件状态从
    <strong>UNDER_REVIEW</strong> 推进为 <strong>EDITOR_RECOMMENDATION</strong>。
    编辑可在此提交建议，提交后稿件状态将推进至 <strong>FINAL_DECISION_PENDING</strong>，等待主编终审。
</p>

<c:if test="${empty readyList}">
    <p style="color:#d00;">当前列表中没有状态为 EDITOR_RECOMMENDATION 的稿件，因此无法提交编辑建议。</p>
</c:if>

<c:if test="${not empty readyList}">
    <form method="post" action="${pageContext.request.contextPath}/editor/recommend">

        <label>选择稿件：</label>
        <select name="manuscriptId" required style="min-width: 320px;">
            <option value="">-- 请选择 --</option>
            <c:forEach items="${readyList}" var="m">
                <option value="${m.manuscriptId}">#${m.manuscriptId} - <c:out value="${m.title}"/></option>
            </c:forEach>
        </select>

        <br/><br/>

        <label>编辑建议：</label>
        <select name="suggestion" required style="min-width: 220px;">
            <option value="ACCEPT">接受</option>
            <option value="MINOR_REVISION">小修后接受</option>
            <option value="MAJOR_REVISION">大修后再审</option>
            <option value="REJECT">拒稿</option>
        </select>

        <br/><br/>

        <button type="submit">提交编辑建议</button>
    </form>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
