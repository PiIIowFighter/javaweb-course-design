<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<!-- 拒绝理由模态框（邀请详情页） -->
<div id="rejectModal" style="display:none; position:fixed; top:50%; left:50%; transform:translate(-50%,-50%);
     background:white; padding:20px; border:2px solid #ccc; border-radius:5px; z-index:1000;
     box-shadow:0 0 20px rgba(0,0,0,0.3); min-width:400px;">
    <h4>拒绝审稿邀请</h4>
    <p>请填写拒绝理由：</p>
    <form method="post" action="${ctx}/reviewer/decline" id="rejectForm">
        <input type="hidden" name="reviewId" id="rejectReviewId" value=""/>
        <textarea name="rejectionReason" id="rejectionReason" rows="4" style="width:100%;"
                  placeholder="例如：时间冲突，无法审稿" required></textarea>
        <br/><br/>
        <div style="text-align:right;">
            <button type="button" onclick="hideRejectModal()" style="margin-right:10px;">取消</button>
            <button type="submit">确认拒绝</button>
        </div>
    </form>
</div>

<!-- 模态框背景遮罩 -->
<div id="modalOverlay" style="display:none; position:fixed; top:0; left:0; width:100%; height:100%;
     background:rgba(0,0,0,0.5); z-index:999;"></div>

<script>
// 显示拒绝理由模态框
function showRejectModal(reviewId) {
    document.getElementById('rejectReviewId').value = reviewId;
    document.getElementById('rejectionReason').value = '';
    document.getElementById('modalOverlay').style.display = 'block';
    document.getElementById('rejectModal').style.display = 'block';
}

// 隐藏拒绝理由模态框
function hideRejectModal() {
    document.getElementById('modalOverlay').style.display = 'none';
    document.getElementById('rejectModal').style.display = 'none';
}

// 点击遮罩层也可以关闭模态框
document.getElementById('modalOverlay').addEventListener('click', function() {
    hideRejectModal();
});

// 防止模态框内的点击事件冒泡到遮罩层
document.getElementById('rejectModal').addEventListener('click', function(e) {
    e.stopPropagation();
});
</script>

<h2>审稿邀请 / 稿件摘要</h2>

<c:if test="${empty review || empty manuscript}">
    <p>未找到审稿记录或稿件信息。</p>
</c:if>

<c:if test="${not empty review && not empty manuscript}">
    <div style="margin: 8px 0;">
        <strong>审稿记录ID：</strong><c:out value="${review.reviewId}"/>
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
        无法看见其他审稿人的意见，无法查看稿件的决策历史，不能直接与作者沟通。
    </p>

    <hr/>

    <h3>操作</h3>

    <c:choose>
        <c:when test="${review.status == 'INVITED'}">
            <form method="post" action="${ctx}/reviewer/accept" style="display:inline;">
                <input type="hidden" name="reviewId" value="${review.reviewId}"/>
                <button type="submit">接受邀请</button>
            </form>
            <button type="button" style="display:inline; margin-left: 8px;" onclick="showRejectModal(${review.reviewId})">拒绝邀请</button>
        </c:when>

        <c:when test="${review.status == 'ACCEPTED'}">
            <p>
                下载与审阅：
                <a href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript" target="_blank">下载匿名稿</a>
                <span style="margin:0 6px;">|</span>
                <a href="${ctx}/reviewer/manuscript?id=${review.reviewId}">查看稿件详情</a>
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
