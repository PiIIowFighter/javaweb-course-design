<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>为稿件选择审稿人</h2>

<c:set var="backToUrl" value="${pageContext.request.requestURI}"/>
<c:if test="${not empty pageContext.request.queryString}">
    <c:set var="backToUrl" value="${backToUrl}?${pageContext.request.queryString}"/>
</c:if>

<c:if test="${not empty param.cancelMsg}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #b7eb8f; background:#f6ffed; color:#135200; border-radius:6px;">
        <c:out value="${param.cancelMsg}"/>
    </div>
</c:if>

<c:if test="${not empty param.inviteMsg}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #b7eb8f; background:#f6ffed; color:#135200; border-radius:6px;">
        <c:out value="${param.inviteMsg}"/>
    </div>
</c:if>
<c:if test="${not empty param.inviteErr}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #ffa39e; background:#fff1f0; color:#a8071a; border-radius:6px;">
        <c:out value="${param.inviteErr}"/>
    </div>
</c:if>

<c:if test="${empty manuscript}">
    <p>未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">
    <p>
        <a href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}">
            返回稿件详情
        </a>
    </p>

    <h3>稿件基本信息</h3>
    <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width:960px;">
        <tr>
            <th>稿件编号</th>
            <td><c:out value="${manuscript.manuscriptId}"/></td>
        </tr>
        <tr>
            <th>标题</th>
            <td><c:out value="${manuscript.title}"/></td>
        </tr>
        <tr>
            <th>研究主题</th>
            <td><c:out value="${manuscript.subjectArea}"/></td>
        </tr>
        <tr>
            <th>关键词</th>
            <td><c:out value="${manuscript.keywords}"/></td>
        </tr>
        <tr>
            <th>当前状态</th>
            <td><c:out value="${manuscript.currentStatus}"/></td>
        </tr>
    </table>

    <h3 style="margin-top:16px;">作者推荐审稿人</h3>
    <c:if test="${empty recommendedReviewers}">
        <p>（未推荐审稿人）</p>
    </c:if>
    <c:if test="${not empty recommendedReviewers}">
        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width:960px;">
            <thead>
            <tr>
                <th>姓名</th>
                <th>邮箱</th>
                <th>推荐理由</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${recommendedReviewers}" var="rr">
                <tr>
                    <td><c:out value="${rr.fullName}"/></td>
                    <td><c:out value="${rr.email}"/></td>
                    <td><c:out value="${rr.reason}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    

    <c:if test="${not empty currentReviews}">
        <h3 style="margin-top:16px;">已邀请/已接受/已提交的审稿人</h3>
        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width:960px;">
            <thead>
            <tr>
                <th>审稿人</th>
                <th>状态</th>
                <th>邀请时间</th>
                <th>截止时间</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${currentReviews}" var="r">
                <tr>
                    <td>
                        <c:choose>
                            <c:when test="${not empty reviewerMap[r.reviewerId]}">
                                <c:out value="${reviewerMap[r.reviewerId].fullName}"/>
                                (<c:out value="${reviewerMap[r.reviewerId].email}"/>)
                            </c:when>
                            <c:otherwise>审稿人ID: <c:out value="${r.reviewerId}"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td><c:out value="${r.status}"/></td>
                    <td><c:out value="${r.invitedAt}"/></td>
                    <td><c:out value="${r.dueAt}"/></td>
                    <td>
    <c:choose>
        <c:when test="${r.status == 'INVITED' || r.status == 'ACCEPTED'}">
            <form method="post" action="${ctx}/editor/review/remind" style="display:inline">
                <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                <button type="submit">催审</button>
            </form>
            <form method="post" action="${ctx}/editor/review/cancel" style="display:inline; margin-left:6px;" onsubmit="return confirm('确认解除该审稿人？')">
                <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                <input type="hidden" name="backTo" value="${backToUrl}"/>
                <button type="submit">撤回分配</button>
            </form>
        </c:when>
        <c:otherwise>
            <c:choose>
                <c:when test="${r.status == 'SUBMITTED'}">
                    <a href="${ctx}/editor/review/detail?reviewId=${r.reviewId}">查看详细评价</a>
                </c:when>
                <c:otherwise>-</c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

<h3 style="margin-top:16px;">检索审稿人</h3>
    <form method="get" action="${ctx}/editor/review/select" style="margin-bottom:8px;">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
        <span>按研究方向或姓名搜索：</span>
        <input type="text" name="reviewerKeyword" size="20"
               value="${fn:escapeXml(param.reviewerKeyword)}"/>
        <span style="margin-left:6px;">最低完成评审次数：</span>
        <input type="number" name="minCompleted" min="0" style="width:70px;"
               value="${fn:escapeXml(param.minCompleted)}"/>
        <span style="margin-left:6px;">最低平均评分：</span>
        <input type="number" name="minAvgScore" min="0" max="10" step="1" style="width:70px;"
               value="${fn:escapeXml(param.minAvgScore)}"/>
        <button type="submit" style="margin-left:8px;">搜索</button>
        <a href="${ctx}/editor/review/select?manuscriptId=${manuscript.manuscriptId}"
           style="margin-left:4px;">重置</a>
    </form>

    <h3 style="margin-top:16px;">从审稿人库中选择</h3>

    <c:if test="${empty reviewers}">
        <p>当前审稿人库为空，或未命中搜索条件。请调整搜索条件，或由主编在“审稿人库管理”中添加审稿人。</p>
    </c:if>

    <c:if test="${not empty reviewers}">
        <form method="post" action="${ctx}/editor/review/invite">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

            <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width:960px;">
                <thead>
                <tr>
                    <th style="width:60px;">选择</th>
                    <th>姓名</th>
                    <th>用户名</th>
                    <th>邮箱</th>
                    <th>研究方向</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${reviewers}" var="u">
                    <tr>
                        <td style="text-align:center;">
                            <input type="checkbox" name="reviewerIds" value="${u.userId}"
                                   <c:if test="${assignedReviewerIds.contains(u.userId)}">disabled="disabled"</c:if> />
                            <c:if test="${assignedReviewerIds.contains(u.userId)}">
                                <span style="color:#999; font-size:12px;">已分配</span>
                            </c:if>
                        </td>
                        <td><c:out value="${u.fullName}"/></td>
                        <td><c:out value="${u.username}"/></td>
                        <td><c:out value="${u.email}"/></td>
                        <td><c:out value="${u.researchArea}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <p style="margin-top:10px;">
                <label>截止日期（可选）：
                    <input type="date" name="dueDate"/>
                </label>
            </p>

            <p style="margin-top:10px;">
                <button type="submit">向选中审稿人发出邀请</button>
                <a href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}" style="margin-left:8px;">
                    取消并返回
                </a>
            </p>
        </form>
    </c:if>

    <hr style="margin:22px 0; max-width:960px;"/>

    <h3 style="margin-top:16px;">邀请外部审稿人（创建账号并邮件邀请）</h3>
    <p style="color:#666; max-width:960px;">当审稿人不在现有审稿人库中时，可在此创建审稿人账号，并向其发送账户信息及本稿件的审稿邀请邮件。</p>

    <form method="post" action="${ctx}/editor/review/inviteExternal" style="max-width:960px; background:#fff; border:1px solid #e5e7eb; padding:12px 14px; border-radius:8px;">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

        <table cellpadding="6" cellspacing="0" style="width:100%;">
            <tr>
                <td style="width:140px;"><b>用户名 *</b></td>
                <td><input type="text" name="username" required="required" style="width:280px;" placeholder="例如 reviewer_zhang"/></td>
            </tr>
            <tr>
                <td><b>初始密码 *</b></td>
                <td><input type="text" name="password" required="required" style="width:280px;" placeholder="请设置一个初始密码"/></td>
            </tr>
            <tr>
                <td><b>姓名</b></td>
                <td><input type="text" name="fullName" style="width:280px;" placeholder="可选"/></td>
            </tr>
            <tr>
                <td><b>邮箱 *</b></td>
                <td><input type="email" name="email" required="required" style="width:360px;" placeholder="example@university.edu"/></td>
            </tr>
            <tr>
                <td><b>单位/机构</b></td>
                <td><input type="text" name="affiliation" style="width:360px;" placeholder="可选"/></td>
            </tr>
            <tr>
                <td><b>研究方向</b></td>
                <td><input type="text" name="researchArea" style="width:360px;" placeholder="可选"/></td>
            </tr>
            <tr>
                <td><b>截止日期（可选）</b></td>
                <td><input type="date" name="dueDate"/></td>
            </tr>
        </table>

        <div style="margin-top:10px;">
            <button type="submit">创建并邀请外部审稿人</button>
        </div>
    </form>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
