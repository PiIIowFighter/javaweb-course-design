<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>待评审稿件列表</h2>
<p>以下为当前分配给您的、需要在外审截止日期前完成评审的稿件。</p>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:if test="${empty reviews}">
    <p>目前没有待评审的稿件。</p>
</c:if>

<!-- 全局拒绝理由模态框 -->
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

<c:if test="${not empty reviews}">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
    <tr>
        <th>审稿记录ID</th>
        <th>稿件编号</th>
        <th>状态</th>
        <th>邀请时间</th>
        <th>截止时间</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${reviews}" var="r">
        <tr>
            <td><c:out value="${r.reviewId}"/></td>
            <td><c:out value="${r.manuscriptId}"/></td>
            <td><c:out value="${r.status}"/></td>
            <td><c:out value="${r.invitedAt}"/></td>
            <td><c:out value="${r.dueAt}"/></td>
            <td>
                <a href="${ctx}/reviewer/invitation?id=${r.reviewId}">查看稿件摘要</a>
                <br/>
                <c:if test="${r.status == 'ACCEPTED' || r.status == 'SUBMITTED'}">
                    下载与审阅：
                    <a href="${ctx}/files/preview?manuscriptId=${r.manuscriptId}&type=manuscript" target="_blank">匿名稿</a>
                    <span style="margin:0 6px;">|</span>
                    <a href="${ctx}/files/preview?manuscriptId=${r.manuscriptId}&type=original" target="_blank">原稿</a>
                    <br/>
                </c:if>
                <c:choose>
                    <c:when test="${r.status == 'INVITED'}">
                        <form method="post" action="${ctx}/reviewer/accept" style="display:inline;">
                            <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                            <button type="submit">接受邀请</button>
                        </form>
                        
                        <!-- 修改拒绝按钮，触发全局模态框 -->
                        <button type="button" onclick="showRejectModal(${r.reviewId})">拒绝邀请</button>
                    </c:when>
                    <c:when test="${r.status == 'ACCEPTED'}">
                        <a href="${ctx}/reviewer/reviewForm?id=${r.reviewId}">填写评审意见</a>
                    </c:when>
                    <c:when test="${r.status == 'SUBMITTED'}">
                        已提交
                    </c:when>
                    <c:otherwise>
                        <c:out value="${r.status}"/>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>