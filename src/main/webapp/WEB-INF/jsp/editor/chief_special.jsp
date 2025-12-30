<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>主编特殊权限</h2>
<p>
    说明：终审列表页已取消“撤销/撤稿”入口；涉及改判与撤稿的操作<strong>统一在本页面完成</strong>。
    <br/>
    <strong>更改案头初审决定</strong>：针对案头初审阶段的结果（通常表现为状态为 TO_ASSIGN 或“初审退稿”），允许改为“送外审（TO_ASSIGN）”或“初审退稿（REJECTED）”。
    <br/>
    <strong>更改终审决定</strong>：仅对已做出终审决定的稿件（ACCEPTED/REJECTED/REVISION 且 FinalDecisionTime 非空）有效，可重新改判。
    <br/>
    <strong>撤稿</strong>：仅允许对<strong>已发表</strong>（Issues.IsPublished=1 且 IssueManuscripts 关联到该稿件）的论文执行撤稿；撤稿后稿件将被归档并在前台/各角色列表中隐藏。
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
                    <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">查看详情</a>

                    <!-- 1) 更改案头初审决定：仅对“尚未形成终审决定”的稿件开放（FinalDecisionTime 为空）。
                         注意：即使稿件已进入 WITH_EDITOR/UNDER_REVIEW/FINAL_DECISION_PENDING 等阶段，主编仍可在此把初审结论改为“送外审/初审退稿”，
                         系统会同步清空 CurrentEditorId 并将未完成审稿任务置为 EXPIRED，避免编辑/审稿人继续操作。 -->
                    <c:if test="${m.finalDecisionTime == null}">
                        <form method="post" action="${pageContext.request.contextPath}/editor/special" style="display:inline">
                            <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                            <input type="hidden" name="op" value="changeDesk"/>

                            <select name="deskOp">
                                <option value="deskAccept">改为：送外审（TO_ASSIGN）</option>
                                <option value="deskReject">改为：初审退稿（REJECTED）</option>
                            </select>
                            <input type="text" name="reason" required size="18" placeholder="必填：原因"/>
                            <button type="submit" onclick="return confirm('确认更改案头初审决定？更改后编辑/审稿人侧将按新状态同步限制操作。');">
                                更改初审
                            </button>
                        </form>
                    </c:if>

                    <!-- 2) 更改终审决定：必须是已做出终审决定的稿件 -->
                    <c:if test="${m.finalDecisionTime != null and (m.currentStatus == 'ACCEPTED' or m.currentStatus == 'REJECTED' or m.currentStatus == 'REVISION')}">
                        <form method="post" action="${pageContext.request.contextPath}/editor/special" style="display:inline">
                            <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                            <input type="hidden" name="op" value="changeFinal"/>

                            <select name="finalOp">
                                <option value="accept">改为：录用（ACCEPTED）</option>
                                <option value="reject">改为：退稿（REJECTED）</option>
                                <option value="revision">改为：修回（REVISION）</option>
                            </select>
                            <input type="text" name="reason" required size="18" placeholder="必填：原因"/>
                            <button type="submit" onclick="return confirm('确认更改终审决定？该操作会同步影响编辑/审稿人侧可操作性，并写入状态历史。');">
                                更改终审
                            </button>
                        </form>
                    </c:if>

                    <!-- 3) 撤稿：课程口径为 ACCEPTED 即视为已发表（后端仍会二次校验） -->
                    <c:if test="${m.currentStatus == 'ACCEPTED'}">
                        <form method="post" action="${pageContext.request.contextPath}/editor/special" style="display:inline">
                            <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                            <input type="hidden" name="op" value="retract"/>
                            <input type="text" name="reason" required size="16" placeholder="撤稿原因"/>
                            <button type="submit" onclick="return confirm('确认撤稿？本系统口径：ACCEPTED 视为已发表；撤稿后将归档并隐藏。');">
                                撤稿(ACCEPTED)
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
