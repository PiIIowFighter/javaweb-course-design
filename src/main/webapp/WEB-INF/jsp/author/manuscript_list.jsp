<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <h2 class="card-title">我的稿件</h2>
            <p class="card-subtitle">按不同处理状态分类展示投稿记录，便于跟踪稿件在编辑部与审稿流程中的进展。</p>
        </div>
        <div class="actions">
            <a class="btn-primary" href="${ctx}/manuscripts/submit">
                <i class="bi bi-plus-lg" aria-hidden="true"></i>
                新建投稿
            </a>
        </div>
    </div>

    <!-- 顶部分组 Tab：对应任务书中的几类列表 -->
    <div class="tabs" aria-label="稿件分类视图">
        <a class="tab ${group == 'incomplete' ? 'is-active' : ''}" href="${ctx}/manuscripts/list?group=incomplete">
            <i class="bi bi-pencil-square" aria-hidden="true"></i>
            Incomplete（草稿）<span>(<c:out value="${countIncomplete}"/>)</span>
        </a>
        <a class="tab ${group == 'processing' ? 'is-active' : ''}" href="${ctx}/manuscripts/list?group=processing">
            <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
            Processing（处理中）<span>(<c:out value="${countProcessing}"/>)</span>
        </a>
        <a class="tab ${group == 'revision' ? 'is-active' : ''}" href="${ctx}/manuscripts/list?group=revision">
            <i class="bi bi-wrench-adjustable" aria-hidden="true"></i>
            Revision（待修改）<span>(<c:out value="${countRevision}"/>)</span>
        </a>
        <a class="tab ${group == 'decision' ? 'is-active' : ''}" href="${ctx}/manuscripts/list?group=decision">
            <i class="bi bi-check2-circle" aria-hidden="true"></i>
            Decision（已决策）<span>(<c:out value="${countDecision}"/>)</span>
        </a>
    </div>

    <!-- 筛选区域：状态 + 日期范围 -->
    <form method="get" action="${ctx}/manuscripts/list" class="card">
        <input type="hidden" name="group" value="${group}"/>
        <div class="toolbar">
            <div class="grow">
                <label style="display:block; margin-bottom:6px;">状态</label>
                <select name="status">
        <option value="">全部状态</option>
        <option value="DRAFT"              ${statusFilter == 'DRAFT' ? 'selected="selected"' : ''}>DRAFT - 编辑中</option>
        <option value="SUBMITTED"          ${statusFilter == 'SUBMITTED' ? 'selected="selected"' : ''}>SUBMITTED - 已提交待处理</option>
        <option value="FORMAL_CHECK"       ${statusFilter == 'FORMAL_CHECK' ? 'selected="selected"' : ''}>FORMAL_CHECK - 形式审查</option>
        <option value="DESK_REVIEW_INITIAL" ${statusFilter == 'DESK_REVIEW_INITIAL' ? 'selected="selected"' : ''}>DESK_REVIEW_INITIAL - 案头初筛</option>
        <option value="TO_ASSIGN"          ${statusFilter == 'TO_ASSIGN' ? 'selected="selected"' : ''}>TO_ASSIGN - 待分配编辑/审稿人</option>
        <option value="WITH_EDITOR"        ${statusFilter == 'WITH_EDITOR' ? 'selected="selected"' : ''}>WITH_EDITOR - 编辑处理中</option>
        <option value="UNDER_REVIEW"       ${statusFilter == 'UNDER_REVIEW' ? 'selected="selected"' : ''}>UNDER_REVIEW - 审稿中</option>
        <option value="EDITOR_RECOMMENDATION" ${statusFilter == 'EDITOR_RECOMMENDATION' ? 'selected="selected"' : ''}>EDITOR_RECOMMENDATION - 编辑推荐意见</option>
        <option value="FINAL_DECISION_PENDING" ${statusFilter == 'FINAL_DECISION_PENDING' ? 'selected="selected"' : ''}>FINAL_DECISION_PENDING - 待主编终审</option>
        <option value="RETURNED"           ${statusFilter == 'RETURNED' ? 'selected="selected"' : ''}>RETURNED - 编辑部退回待修改</option>
        <option value="REVISION"           ${statusFilter == 'REVISION' ? 'selected="selected"' : ''}>REVISION - 审稿后修改中</option>
        <option value="ACCEPTED"           ${statusFilter == 'ACCEPTED' ? 'selected="selected"' : ''}>ACCEPTED - 已录用</option>
        <option value="REJECTED"           ${statusFilter == 'REJECTED' ? 'selected="selected"' : ''}>REJECTED - 已退稿</option>
                </select>
            </div>

            <div>
                <label style="display:block; margin-bottom:6px;">提交日期</label>
                <div class="toolbar">
                    <input type="date" name="fromDate" value="${fromDate}"/>
                    <span style="color: var(--muted);">至</span>
                    <input type="date" name="toDate" value="${toDate}"/>
                </div>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-funnel" aria-hidden="true"></i>
                    筛选
                </button>
                <a class="btn-quiet" href="${ctx}/manuscripts/list" style="text-decoration:none;">重置</a>
            </div>
        </div>
    </form>

    <div class="toolbar">
        <small class="grow">
            共 <c:out value="${totalCount}"/> 条记录，
            <c:choose>
                <c:when test="${pageCount > 0}">
                    当前第 <c:out value="${page}"/> / <c:out value="${pageCount}"/> 页
                </c:when>
                <c:otherwise>
                    当前第 0 / 0 页
                </c:otherwise>
            </c:choose>
        </small>
        <a href="${ctx}/manuscripts/exportCsv?group=${group}&status=${statusFilter}&fromDate=${fromDate}&toDate=${toDate}">
            <i class="bi bi-download" aria-hidden="true"></i> 导出 CSV
        </a>
    </div>

    <div class="actions">
        <c:if test="${page > 1}">
            <a class="btn-quiet" href="${ctx}/manuscripts/list?group=${group}&status=${statusFilter}&fromDate=${fromDate}&toDate=${toDate}&sort=${sort}&dir=${dir}&page=${page-1}" style="text-decoration:none;">
                <i class="bi bi-arrow-left" aria-hidden="true"></i> 上一页
            </a>
        </c:if>
        <c:if test="${page < pageCount}">
            <a class="btn-quiet" href="${ctx}/manuscripts/list?group=${group}&status=${statusFilter}&fromDate=${fromDate}&toDate=${toDate}&sort=${sort}&dir=${dir}&page=${page+1}" style="text-decoration:none;">
                下一页 <i class="bi bi-arrow-right" aria-hidden="true"></i>
            </a>
        </c:if>
    </div>

<c:if test="${empty manuscripts}">
    <p>当前分类和筛选条件下没有稿件记录。</p>
</c:if>

<c:if test="${not empty manuscripts}">
<table>
    <thead>
        <tr>
            <th>
                <a href="${ctx}/manuscripts/list?group=${group}&status=${statusFilter}&fromDate=${fromDate}&toDate=${toDate}&sort=id&dir=asc">
                    稿件编号
                </a>
            </th>
            <th>标题</th>
            <th>期刊ID</th>
            <th>当前状态</th>
            <th>
                <a href="${ctx}/manuscripts/list?group=${group}&status=${statusFilter}&fromDate=${fromDate}&toDate=${toDate}&sort=submitTime&dir=desc">
                    提交时间（点击按时间降序）
                </a>
            </th>
            <th>操作</th>
        </tr>
    </thead>
    <tbody>
        <c:forEach var="m" items="${manuscripts}">
            <tr>
                <td><c:out value="${m.manuscriptId}"/></td>
                <td><c:out value="${m.title}"/></td>
                <td><c:out value="${m.journalId}"/></td>
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
                    <a href="${ctx}/manuscripts/detail?id=${m.manuscriptId}"><i class="bi bi-eye" aria-hidden="true"></i> 查看详情</a>
                    <c:if test="${m.currentStatus == 'DRAFT'}">
                        <a style="margin-left:6px;" href="${ctx}/manuscripts/edit?id=${m.manuscriptId}"><i class="bi bi-pencil" aria-hidden="true"></i> 继续编辑</a>
                    </c:if>
                    <c:if test="${m.currentStatus == 'RETURNED' or m.currentStatus == 'REVISION'}">
                        <!-- “待修改”按钮：引导作者进入详情页编辑并重新提交 -->
                        <form method="get"
                              action="${ctx}/manuscripts/detail"
                              style="display:inline;">
                            <input type="hidden" name="id" value="${m.manuscriptId}"/>
                            <button type="submit" class="btn-primary"><i class="bi bi-wrench-adjustable" aria-hidden="true"></i> 待修改</button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
    </tbody>
</table>
</c:if>

</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
