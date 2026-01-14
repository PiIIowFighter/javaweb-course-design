<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">终审决策</h2>
</div></div>
    </div>

    <div class="grid grid-2">
        <div class="card mini">
            <div class="kicker">状态说明</div>
            <div class="mt-1" style="display:flex; gap:8px; flex-wrap:wrap;">
                <span class="badge">EDITOR_RECOMMENDATION</span>
                <span class="badge">FINAL_DECISION_PENDING</span>
                <span class="badge">ACCEPTED</span>
                <span class="badge">REJECTED</span>
            </div>
            <ul style="margin-top: 10px;">
                <li>EDITOR_RECOMMENDATION：责任编辑已给出推荐意见；</li>
                <li>FINAL_DECISION_PENDING：等待主编最终决定；</li>
                <li>ACCEPTED / REJECTED：已经给出录用或退稿决定的稿件。</li>
            </ul>
        </div>

        <div class="card mini">
            <div class="kicker">使用提示</div>
            <p class="mt-1 muted">“编辑建议 / 决策”列仅保留关键入口，避免长文本挤占表格空间。</p>
            <ul style="margin-top: 10px;">
                <li>点击 <strong>查看编辑建议</strong> 可查看责任编辑提交给主编的总结与建议。</li>
                <li>终审操作仅对 <strong>主编（EDITOR_IN_CHIEF）</strong> 显示。</li>
            </ul>
        </div>
    </div>

    <c:if test="${empty manuscripts}">
        <div class="alert">当前没有需要终审或已完成终审的稿件。</div>
    </c:if>

    <c:if test="${not empty manuscripts}">
        <div class="table-wrap">
            <table class="table-fixed" border="1" cellpadding="6" cellspacing="0" style="min-width: 1180px;">
                <thead>
                <tr>
                    <th style="width: 110px;">稿件编号</th>
                    <th style="width: 320px;">标题</th>
                    <th style="width: 190px;">当前状态</th>
                    <th style="width: 220px;">编辑建议 / 决策</th>
                    <th style="width: 190px;">终审时间</th>
                    <th style="width: 190px;">提交时间</th>
                    <th style="width: 300px;">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${manuscripts}" var="m">
                    <tr>
                        <td><c:out value="${m.manuscriptId}"/></td>
                        <td class="cell-wrap"><c:out value="${m.title}"/></td>
                        <td>
                            <span class="badge"><c:out value="${m.currentStatus}"/></span>
                        </td>
                        <td>
                            <c:set var="s" value="${suggestionMap[m.manuscriptId]}"/>
                            <c:choose>
                                <c:when test="${not empty s}">
                                    <c:if test="${not empty m.decision}">
                                        <div style="margin-bottom: 8px;">
                                            <span class="badge"><c:out value="${m.decision}"/></span>
                                        </div>
                                    </c:if>
                                    <a class="btn" href="${ctx}/editor/recommend?manuscriptId=${m.manuscriptId}">
                                        <i class="bi bi-eye" aria-hidden="true"></i>
                                        <span>查看编辑建议</span>
                                    </a>
                                </c:when>
                                <c:when test="${not empty m.decision}">
                                    <span class="badge"><c:out value="${m.decision}"/></span>
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
                            <c:choose>
                                <c:when test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                                    <div class="admin-actions">
                                        <!-- 只有主编并且状态在待终审/有编辑推荐时才显示三个决策按钮 -->
                                        <c:if test="${m.currentStatus == 'FINAL_DECISION_PENDING' or m.currentStatus == 'EDITOR_RECOMMENDATION'}">
                                            <form method="post"
                                                  action="${ctx}/editor/finalDecision"
                                                  style="display:inline-flex; gap:8px; flex-wrap:wrap;">
                                                <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>

                                                <button type="submit" class="btn-primary" name="op" value="accept">录用（Accept）</button>

                                                <button type="submit" name="op" value="reject"
                                                        onclick="return confirm('确认退稿？该操作将把稿件状态标记为 REJECTED。');">
                                                    退稿（Reject）
                                                </button>

                                                <button type="submit" name="op" value="revision">要求修回（Revision）</button>
                                            </form>
                                        </c:if>

                                        <!-- 特殊权限：撤销终审决定 / 撤稿 -->
                                        <form method="post" action="${ctx}/editor/finalDecision"
                                              style="display:inline-flex; gap:8px; flex-wrap:wrap;">
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
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <span class="muted">--</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
</div>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
