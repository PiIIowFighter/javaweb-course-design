<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>审稿邀请 / 稿件摘要</h2>

<c:if test="${empty review || empty manuscript}">
    <p>未找到审稿记录或稿件信息。</p>
</c:if>

<c:if test="${not empty review && not empty manuscript}">
    <div style="margin: 8px 0;">
        <strong>审稿记录ID：</strong><c:out value="${review.reviewId}"/>
        <span style="margin:0 10px;">|</span>
        <strong>稿件编号：</strong><c:out value="${manuscript.manuscriptId}"/>
        <span style="margin:0 10px;">|</span>
        <strong>当前状态：</strong><c:out value="${review.status}"/>
        <span style="margin:0 10px;">|</span>
        <strong>截止时间：</strong><c:out value="${review.dueAt}"/>
    </div>

    <hr/>

    <h3>稿件元数据（摘要视图）</h3>
    <table border="1" cellpadding="6" cellspacing="0" style="width:100%; max-width: 980px;">
        <tr>
            <th style="width:160px;">标题</th>
            <td><c:out value="${manuscript.title}"/></td>
        </tr>
        <tr>
            <th>摘要</th>
            <td>
                <div class="ql-snow richtext-view">
                    <div class="ql-editor">
                        <c:out value="${manuscript.abstractText}" escapeXml="false"/>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <th>关键词</th>
            <td><c:out value="${manuscript.keywords}"/></td>
        </tr>
        <tr>
            <th>研究主题</th>
            <td><c:out value="${manuscript.subjectArea}"/></td>
        </tr>
        <tr>
            <th>项目资助</th>
            <td><c:out value="${manuscript.fundingInfo}"/></td>
        </tr>
    </table>

    <p style="margin-top: 10px; color: #666;">
        限制：通常看不到其他审稿人的意见，无法查看稿件的决策历史，不能直接与作者沟通（必须通过编辑）。
    </p>

    <hr/>

    <h3>操作</h3>

    <c:choose>
        <c:when test="${review.status == 'INVITED'}">
            <form method="post" action="${ctx}/reviewer/accept" style="display:inline;">
                <input type="hidden" name="reviewId" value="${review.reviewId}"/>
                <button type="submit">接受邀请</button>
            </form>
            <form method="post" action="${ctx}/reviewer/decline" style="display:inline; margin-left: 8px;"
                  onsubmit="return confirm('确定要拒绝该审稿邀请吗？');">
                <input type="hidden" name="reviewId" value="${review.reviewId}"/>
                <button type="submit">拒绝邀请</button>
            </form>
        </c:when>

        <c:when test="${review.status == 'ACCEPTED'}">
            <p>
                下载与审阅：
                <a href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript" target="_blank">下载匿名稿</a>
                <span style="margin:0 6px;">|</span>
                <a href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=original" target="_blank">下载原稿</a>
            </p>
            <p>
                <a href="${ctx}/reviewer/reviewForm?id=${review.reviewId}">提交评审意见</a>
            </p>
        </c:when>

        <c:when test="${review.status == 'SUBMITTED'}">
            <p>您已提交本稿件的评审意见。</p>
        </c:when>

        <c:otherwise>
            <p>当前状态：<c:out value="${review.status}"/></p>
        </c:otherwise>
    </c:choose>

    <p style="margin-top: 14px;">
        <a href="${ctx}/reviewer/assigned">返回待评审列表</a>
    </p>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
