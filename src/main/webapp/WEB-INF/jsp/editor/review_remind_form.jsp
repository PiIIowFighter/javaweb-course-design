<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card" style="max-width: 720px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">手动催审</h2>
            <p class="card-subtitle">
                向审稿人发送一封自定义内容的催审邮件。
            </p>
        </div>
    </div>

    <div class="stack">
        <p>
            <b>稿件编号：</b><c:out value="${review.manuscriptId}"/><br/>
            <b>稿件标题：</b><c:out value="${reviewManuscript.title}"/><br/>
            <b>审稿人：</b>
            <c:out value="${reviewReviewer.fullName}"/>
            （ID：<c:out value="${review.reviewerId}"/>）
        </p>

        <form method="post" action="${pageContext.request.contextPath}/editor/review/remindCustom">
            <input type="hidden" name="reviewId" value="${review.reviewId}"/>
            <input type="hidden" name="manuscriptId" value="${review.manuscriptId}"/>
            <input type="hidden" name="back" value="${back}"/>

            <div class="stack">
                <label>邮件正文（只需填写核心提醒语句，系统会自动附加稿件信息与链接）：</label>
                <textarea name="message" rows="6" style="width:100%;"><c:out value="${defaultRemindText}"/></textarea>

                <button type="submit" onclick="return confirm('确认发送催审邮件？');">
                    发送催审邮件
                </button>

                <c:choose>
                    <c:when test="${back == 'monitor'}">
                        <a href="${pageContext.request.contextPath}/editor/review/monitor">返回审稿监控</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${review.manuscriptId}">
                            返回稿件详情
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
