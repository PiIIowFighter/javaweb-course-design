<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
        <h2 class="page-title">稿件详情</h2>
    </div><c:if test="${not empty param.cancelMsg}">
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
        <div class="stack" style="gap:10px;">
            <div class="stack" style="flex-direction: row; justify-content: space-between; align-items:center; gap:10px;">
                <div>
                    <div style="font-weight:700; font-size: 18px; line-height:1.3;">
                        <c:out value="${manuscript.title}"/>
                    </div>
                    <div style="color: var(--muted); margin-top: 2px;">
                        稿件编号：<c:out value="${manuscript.manuscriptId}"/> · 状态：<c:out value="${manuscript.currentStatus}"/>
                    </div>
                </div>
                <div class="stack" style="flex-direction: row; gap:8px;">
                    <button class="btn btn-quiet" type="button" onclick="history.back()">返回上一页</button>
                </div>
            </div>

            <table border="1" cellpadding="4" cellspacing="0">
                <tbody>
<tr>
                    <th>关键词</th>
                    <td><c:out value="${manuscript.subjectArea}"/></td>
                </tr>
                <tr>
                    <th>提交时间</th>
                    <td>
                        <c:choose>
                            <c:when test="${not empty manuscript.submitTime}"><c:out value="${manuscript.submitTime}"/></c:when>
                            <c:otherwise>--</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <h3 style="margin:0 0 10px 0;">作者列表</h3>
        <c:if test="${empty authors}">
            <p style="color: var(--muted);">（未提供作者信息）</p>
        </c:if>
        <c:if test="${not empty authors}">
            <table border="1" cellpadding="4" cellspacing="0">
                <thead>
                <tr>
                    <th style="width:70px;">序号</th>
                    <th>姓名</th>
                    <th>邮箱</th>
                    <th>单位/机构</th>
                    <th style="width:90px;">通讯作者</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${authors}" var="a">
                    <tr>
                        <td style="text-align:center;"><c:out value="${a.authorOrder}"/></td>
                        <td><c:out value="${a.fullName}"/></td>
                        <td><c:out value="${a.email}"/></td>
                        <td><c:out value="${a.affiliation}"/></td>
                        <td style="text-align:center;">
                            <c:choose>
                                <c:when test="${a.corresponding}">是</c:when>
                                <c:otherwise>否</c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <h3 style="margin:0 0 10px 0;">推荐审稿人</h3>
        <c:if test="${empty recommendedReviewers}">
            <p style="color: var(--muted);">（作者未推荐审稿人）</p>
        </c:if>
        <c:if test="${not empty recommendedReviewers}">
            <table border="1" cellpadding="4" cellspacing="0">
                <thead>
                <tr>
                    <th style="width:160px;">姓名</th>
                    <th style="width:220px;">邮箱</th>
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
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <div class="stack" style="flex-direction: row; justify-content: space-between; align-items:center; gap:10px;">
            <h3 style="margin:0;">当前审稿记录</h3>
            
            <c:url var="selectUrl" value="/editor/review/select">
                <c:param name="manuscriptId" value="${manuscript.manuscriptId}"/>
                <c:param name="backTo" value="${backToUrl}"/>
            </c:url>
        </div>

        <c:if test="${empty reviews}">
            <p style="margin-top: 10px; color: var(--muted);">（暂无审稿记录）</p>
        </c:if>

        <c:if test="${not empty reviews}">
            <table border="1" cellpadding="4" cellspacing="0" style="margin-top: 12px;">
                <thead>
                <tr>
                    <th>审稿人</th>
                    <th style="width:120px;">状态</th>
                    <th style="width:170px;">邀请时间</th>
                    <th style="width:170px;">截止时间</th>
                    <th style="width:180px;">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${reviews}" var="r">
                    <tr>
                        <td>
                            <c:choose>
                                <c:when test="${not empty reviewerMap[r.reviewerId]}">
                                    <c:out value="${reviewerMap[r.reviewerId].fullName}"/>
                                    <c:if test="${not empty reviewerMap[r.reviewerId].email}">
                                        <span style="color: var(--muted);">（<c:out value="${reviewerMap[r.reviewerId].email}"/>）</span>
                                    </c:if>
                                </c:when>
                                <c:otherwise>审稿人ID：<c:out value="${r.reviewerId}"/></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${r.status}"/></td>
                        <td><c:out value="${r.invitedAt}"/></td>
                        <td><c:out value="${r.dueAt}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${r.status == 'INVITED' || r.status == 'ACCEPTED'}">
                                    
                                    <form method="post" action="${ctx}/editor/review/cancel" style="display:inline;" onsubmit="return confirm('确认解除该审稿人？')">
                                        <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                                        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                                        <input type="hidden" name="backTo" value="${backToUrl}"/>
                                        <button class="btn btn-danger" type="submit">撤回分配</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <c:choose>
                                        <c:when test="${r.status == 'SUBMITTED'}">
                                            <a class="btn btn-quiet" href="${ctx}/editor/review/detail?reviewId=${r.reviewId}">查看详细评价</a>
                                        </c:when>

                                        
                                        <c:when test="${r.status == 'DECLINED' || (r.status == 'EXPIRED' && (not empty r.rejectionReason || not empty r.declinedAt))}">
                                            <button class="btn btn-quiet" type="button" disabled="disabled">已拒绝</button>
                                        </c:when>

                                        <c:otherwise>--</c:otherwise>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>

        <p style="margin-top: 12px; color: var(--muted);">
            提示：若需从审稿人库筛选或创建外部审稿人账号，请使用页面底部的“添加邀请审稿人”。
        </p>
    </div>

    
    <div class="stack" style="flex-direction: row; gap: 10px; margin-top: var(--space-5); justify-content:flex-end;">
        <a class="btn btn-primary" href="${selectUrl}">
            <i class="bi bi-person-plus" aria-hidden="true"></i>
            <span>添加邀请审稿人</span>
        </a>
    </div>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
