<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>提出建议</h2>

<c:if test="${empty manuscript}">
    <p style="color:#d00;">未找到稿件。</p>
</c:if>

<c:if test="${not empty manuscript}">
    <div class="card" style="max-width: 1100px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">稿件信息</h3>
                <p class="card-subtitle">
                    稿件编号：<strong><c:out value="${manuscript.manuscriptId}"/></strong>
                    &nbsp;&nbsp;|&nbsp;&nbsp;
                    状态：<strong><c:out value="${manuscript.currentStatus}"/></strong>
                </p>
            </div>
        </div>
        <div class="card-body">
            <p><strong>标题：</strong><c:out value="${manuscript.title}"/></p>
            <p style="margin-top: 10px;">
                <a href="${ctx}/editor/recommend/detail?manuscriptId=${manuscript.manuscriptId}">查看稿件详情</a>
            </p>
        </div>
    </div>

    <h3>审稿意见汇总</h3>
    <c:if test="${empty submittedReviews}">
        <p style="color:#d00;">当前未找到已提交（SUBMITTED）的审稿意见，无法形成建议。</p>
    </c:if>

    <c:if test="${not empty submittedReviews}">
        <p>
            <strong>Recommendation 统计：</strong>
            <c:forEach items="${recommendStats}" var="e">
                <span style="margin-right: 12px;">
                    <c:out value="${e.key}"/> × <c:out value="${e.value}"/>
                </span>
            </c:forEach>
        </p>

        <table border="1" cellpadding="6" cellspacing="0" style="width: 100%; max-width: 1100px;">
            <thead>
            <tr>
                <th>审稿人</th>
                <th>推荐</th>
                <th>评分</th>
                <th>关键评价</th>
                <th>给编辑的保密意见</th>
                <th>提交时间</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${submittedReviews}" var="r">
                <tr>
                    <td><c:out value="${r.reviewerName}"/></td>
                    <td><c:out value="${r.recommendation}"/></td>
                    <td><c:out value="${r.score}"/></td>
                    <td style="max-width: 340px;"><c:out value="${r.keyEvaluation}"/></td>
                    <td style="max-width: 340px;"><c:out value="${r.confidentialToEditor}"/></td>
                    <td><c:out value="${r.submittedAt}"/></td>
                    <td>
                        <a href="${ctx}/editor/review/detail?reviewId=${r.reviewId}">查看详细评价</a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    <h3 style="margin-top: 18px;">填写总结与建议</h3>

    <c:if test="${not empty editorSuggestion}">
        <div class="alert" style="max-width: 1100px; padding: 12px; border: 1px solid #ddd; border-radius: 10px; margin-bottom: 12px;">
            <strong>提示：</strong>该稿件已存在编辑建议记录，你可以修改后重新提交（将覆盖旧建议）。
        </div>
    </c:if>

    <form method="post" action="${ctx}/editor/recommend" style="max-width: 1100px;">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

        <div style="margin-bottom: 10px;">
            <label><strong>总结报告：</strong></label><br/>
            <textarea name="summary" rows="4" style="width: 100%;" placeholder="例如：三位审稿人均建议小修，整体质量高。"><c:out value="${editorSuggestion.summary}"/></textarea>
        </div>

        <div style="margin-bottom: 12px;">
            <label><strong>建议：</strong></label><br/>
            <select name="suggestion" required style="min-width: 320px;">
                <option value="ACCEPT" <c:if test="${editorSuggestion.suggestion == 'ACCEPT'}">selected</c:if>>Suggest Acceptance</option>
                <option value="MINOR_REVISION" <c:if test="${editorSuggestion.suggestion == 'MINOR_REVISION'}">selected</c:if>>Suggest Acceptance after Minor Revision</option>
                <option value="MAJOR_REVISION" <c:if test="${editorSuggestion.suggestion == 'MAJOR_REVISION'}">selected</c:if>>Suggest Major Revision</option>
                <option value="REJECT" <c:if test="${editorSuggestion.suggestion == 'REJECT'}">selected</c:if>>Suggest Reject</option>
            </select>
        </div>

        <button type="submit">提交给主编</button>
        &nbsp;&nbsp;
        <a href="${ctx}/editor/recommend">返回列表</a>
    </form>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
