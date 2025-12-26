<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>终审 / 录用与退稿决策列表</h2>
<p>此页面展示已经完成外审、进入编辑推荐或主编终审阶段的稿件：</p>
<ul>
    <li>EDITOR_RECOMMENDATION：责任编辑已给出推荐意见；</li>
    <li>FINAL_DECISION_PENDING：等待主编最终决定；</li>
    <li>ACCEPTED / REJECTED：已经给出录用或退稿决定的稿件。</li>
</ul>

<c:if test="${empty manuscripts}">
    <p>当前没有需要终审或已完成终审的稿件。</p>
</c:if>

<c:if test="${not empty manuscripts}">
    <table border="1" cellpadding="4" cellspacing="0">
        <thead>
        <tr>
            <th>稿件编号</th>
            <th>标题</th>
            <th>当前状态</th>
            <th>编辑建议 / 决策</th>
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

                    <!-- 只有主编并且状态在待终审/有编辑推荐时才显示三个决策按钮 -->
                    <c:if test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                        <c:if test="${m.currentStatus == 'FINAL_DECISION_PENDING' or m.currentStatus == 'EDITOR_RECOMMENDATION'}">
                            <form method="post"
                                  action="${pageContext.request.contextPath}/editor/finalDecision"
                                  style="display:inline">
                                <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>

                                <button type="submit" name="op" value="accept">
                                    录用（Accept）
                                </button>

                                <button type="submit" name="op" value="reject"
                                        onclick="return confirm('确认退稿？该操作将把稿件状态标记为 REJECTED。');">
                                    退稿（Reject）
                                </button>

                                <button type="submit" name="op" value="revision">
                                    要求修回（Revision）
                                </button>
                            </form>
                        </c:if>

                        <!-- 特殊权限：撤销终审决定 / 撤稿 -->
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
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
