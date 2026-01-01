<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>主编特殊权限</h2>
<p>
    本页面提供主编的两项特殊操作：
    <strong>撤销终审决定（Rescind Decision）</strong> 与 <strong>撤稿（Retract）</strong>。
    <br/>
    撤销终审决定：仅对已做出最终决定的稿件（ACCEPTED/REJECTED/REVISION）有效，将状态回退到 FINAL_DECISION_PENDING。
    <br/>
    撤稿：将稿件标记为撤稿并归档（Status=ARCHIVED）。
    <br/>
    注：如需查看审稿流程与附件，请点击“进入工作台”。
</p>

<c:if test="${empty manuscripts}">
    <p>当前没有可用于“特殊权限”操作的稿件。</p>
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
                    <a class="btn btn-quiet" href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">进入工作台</a>

                    <form method="post" action="${pageContext.request.contextPath}/editor/finalDecision" style="display:inline">
                        <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>

                        <c:if test="${m.currentStatus == 'ACCEPTED' or m.currentStatus == 'REJECTED' or m.currentStatus == 'REVISION'}">
                            <button type="submit" name="op" value="rescind"
                                    onclick="return confirm('确认撤销该稿件的终审决定？将回退到 FINAL_DECISION_PENDING。');">
                                撤销决策
                            </button>
                        </c:if>

                        <button type="submit" name="op" value="retract"
                                onclick="return confirm('确认撤稿并归档该稿件？该操作通常不可逆。');">
                            撤稿
                        </button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
