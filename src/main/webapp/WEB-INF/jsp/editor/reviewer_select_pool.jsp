<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
        <h2 class="page-title">从审稿人库中选择</h2>
    </div><c:if test="${not empty backToUrl}">
    <p style="margin-top: var(--space-3);">
        <a class="btn btn-quiet" href="${backToUrl}">
            <i class="bi bi-arrow-left" aria-hidden="true"></i>
            返回稿件详情
        </a>
    </p>
</c:if>

<c:if test="${not empty param.cancelMsg}">
    <div class="alert success" style="margin-top: var(--space-4);">
        <c:out value="${param.cancelMsg}"/>
    </div>
</c:if>
<c:if test="${not empty param.inviteMsg}">
    <div class="alert success" style="margin-top: var(--space-4);">
        <c:out value="${param.inviteMsg}"/>
    </div>
</c:if>
<c:if test="${not empty param.inviteErr}">
    <div class="alert danger" style="margin-top: var(--space-4);">
        <c:out value="${param.inviteErr}"/>
    </div>
</c:if>

<c:if test="${empty manuscript}">
    <p>未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">

    <div class="card" style="margin-top: var(--space-5);">
        <div class="stack" style="gap: 8px;">
            <div><strong>稿件编号：</strong><c:out value="${manuscript.manuscriptId}"/></div>
            <div><strong>稿件标题：</strong><c:out value="${manuscript.title}"/></div>
            <div><strong>当前状态：</strong><c:out value="${manuscript.currentStatus}"/></div>
        </div>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <h3 style="margin:0 0 10px 0;">检索审稿人</h3>
        <form method="get" action="${ctx}/editor/review/select" class="stack" style="gap: 10px;">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
            <c:if test="${not empty backToUrl}">
                <input type="hidden" name="backTo" value="${backToUrl}"/>
            </c:if>

            <div class="stack" style="flex-direction: row; flex-wrap: wrap; gap: 10px; align-items: center;">
                <label style="display:flex; flex-direction:column; gap:6px;">
                    <span style="color: var(--muted);">关键词（姓名/研究方向/单位/用户名）</span>
                    <input type="text" name="reviewerKeyword" value="${fn:escapeXml(param.reviewerKeyword)}" placeholder="例如：深度学习" style="min-width: 260px;"/>
                </label>

                <label style="display:flex; flex-direction:column; gap:6px;">
                    <span style="color: var(--muted);">最低完成审稿数</span>
                    <input type="number" name="minCompleted" min="0" value="${fn:escapeXml(param.minCompleted)}" style="width: 160px;"/>
                </label>

                <label style="display:flex; flex-direction:column; gap:6px;">
                    <span style="color: var(--muted);">最低平均分（0-10）</span>
                    <input type="number" name="minAvgScore" min="0" max="10" step="1" value="${fn:escapeXml(param.minAvgScore)}" style="width: 160px;"/>
                </label>

                <div class="stack" style="flex-direction: row; gap: 10px; align-items:flex-end;">
                    <button class="btn btn-primary" type="submit">
                        <i class="bi bi-search" aria-hidden="true"></i>
                        搜索
                    </button>
                    <c:url var="resetUrl" value="/editor/review/select">
                        <c:param name="manuscriptId" value="${manuscript.manuscriptId}"/>
                        <c:if test="${not empty backToUrl}">
                            <c:param name="backTo" value="${backToUrl}"/>
                        </c:if>
                    </c:url>
                    <a class="btn btn-quiet" href="${resetUrl}">重置</a>
                </div>
            </div>
        </form>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <h3 style="margin:0;">审稿人列表</h3>

        <c:url var="externalUrl" value="/editor/review/externalInvite">
            <c:param name="manuscriptId" value="${manuscript.manuscriptId}"/>
            <c:if test="${not empty backToUrl}">
                <c:param name="backTo" value="${backToUrl}"/>
            </c:if>
        </c:url>

        <c:if test="${empty reviewers}">
            <p style="margin-top: 12px; color: var(--muted);">当前审稿人库为空或未命中搜索条件。你也可以点击右上角“邀请外部审稿人”。</p>
        </c:if>

        <c:if test="${not empty reviewers}">
            <!-- 多选邀请：后端已支持 reviewerIds[]，这里统一用 reviewerIds 传递多个值 -->
            <form method="post" action="${ctx}/editor/review/invite" onsubmit="return window.__checkInviteSelected && window.__checkInviteSelected();">
                <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                <c:if test="${not empty backToUrl}">
                    <input type="hidden" name="backTo" value="${backToUrl}"/>
                </c:if>

                <table border="1" cellpadding="4" cellspacing="0" style="margin-top: 12px;">
                    <thead>
                    <tr>
                        <th>姓名</th>
                        <th>用户名</th>
                        <th>单位/机构</th>
                        <th>研究方向</th>
                        <th style="width:120px;">完成审稿数</th>
                        <th style="width:120px;">平均分</th>
                        <th style="width:80px;">选择</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${reviewers}" var="u">
                        <tr>
                            <td><c:out value="${u.fullName}"/></td>
                            <td><c:out value="${u.username}"/></td>
                            <td><c:out value="${u.affiliation}"/></td>
                            <td><c:out value="${u.researchArea}"/></td>
                            <td style="text-align:center;">
                                <c:out value="${empty u.completedReviewCount ? 0 : u.completedReviewCount}"/>
                            </td>
                            <td style="text-align:center;">
                                <c:choose>
                                    <c:when test="${empty u.avgReviewScore}">-</c:when>
                                    <c:otherwise><c:out value="${u.avgReviewScore}"/></c:otherwise>
                                </c:choose>
                            </td>
                            <td style="text-align:center;">
                                <input type="checkbox" name="reviewerIds" value="${u.userId}"
                                       <c:if test="${assignedReviewerIds.contains(u.userId) || declinedReviewerIds.contains(u.userId)}">disabled="disabled"</c:if> />

                                <c:if test="${declinedReviewerIds.contains(u.userId)}">
                                    <div style="color: var(--muted); font-size: 12px; margin-top: 2px;">已拒绝</div>
                                </c:if>

                                <c:if test="${!declinedReviewerIds.contains(u.userId) && assignedReviewerIds.contains(u.userId)}">
                                    <div style="color: var(--muted); font-size: 12px; margin-top: 2px;">已分配</div>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

                <div class="stack" style="flex-direction: row; justify-content: space-between; align-items:center; gap: 10px; margin-top: var(--space-4); flex-wrap: wrap;">
                    <label style="display:flex; align-items:center; gap:8px;">
                        <span style="color: var(--muted);">统一截止日期（可选）</span>
                        <input type="date" name="dueDate"/>
                    </label>
                    <button class="btn btn-primary" type="submit">
                        <i class="bi bi-send" aria-hidden="true"></i>
                        邀请审稿人
                    </button>
                </div>
            </form>

            <script>
                // 至少选择 1 位审稿人
                window.__checkInviteSelected = function () {
                    try {
                        var boxes = document.querySelectorAll('input[name="reviewerIds"]');
                        for (var i = 0; i < boxes.length; i++) {
                            if (boxes[i].checked) return true;
                        }
                    } catch (e) {
                        // ignore
                    }
                    alert('请至少勾选 1 位审稿人。');
                    return false;
                };
            </script>
        </c:if>

        <div class="stack" style="flex-direction: row; justify-content:flex-end; gap: 10px; margin-top: var(--space-5);">
            <c:if test="${not empty backToUrl}">
                <a class="btn btn-quiet" href="${backToUrl}">返回详情</a>
            </c:if>
            <!-- 注意：c:url 会自动拼接 contextPath，避免再手动拼 ${ctx} 导致 /ctx/ctx/... -->
            <a class="btn btn-quiet" href="${externalUrl}">邀请外部审稿人</a>
        </div>
    </div>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
