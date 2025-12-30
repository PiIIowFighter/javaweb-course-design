<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">发送站内通知</h2>
            <p class="card-subtitle">主编/编辑部管理员/系统管理员可以向任意用户发送自定义通知（单向）。</p>
        </div>
        <div class="actions" style="margin:0">
            <a class="btn" href="${ctx}/notifications"><i class="bi bi-bell" aria-hidden="true"></i> 返回通知中心</a>
        </div>
    </div>

    <c:if test="${not empty param.msg}">
        <div class="alert alert-danger" style="margin-bottom:16px;">
            <c:out value="${param.msg}"/>
        </div>
    </c:if>

    <form action="${ctx}/notifications/send" method="post" class="form">
        <div class="form-row">
            <label>接收用户</label>
            <select name="recipientId" required>
                <option value="">-- 请选择用户 --</option>
                <c:forEach var="u" items="${users}">
                    <option value="${u.userId}">
                        <c:out value="${u.username}"/>
                        <c:if test="${not empty u.fullName}"> - <c:out value="${u.fullName}"/></c:if>
                        <c:if test="${not empty u.roleCode}"> (<c:out value="${u.roleCode}"/>)</c:if>
                    </option>
                </c:forEach>
            </select>
            <div class="help">支持给作者、审稿人、编辑等任意角色发送。</div>
        </div>

        <div class="form-row">
            <label>标题</label>
            <input type="text" name="title" maxlength="200" required placeholder="例如：请尽快补全个人信息" />
            <div class="help">必填，最多 200 字。</div>
        </div>

        <div class="form-row">
            <label>内容</label>
            <textarea name="content" rows="6" placeholder="可填写更详细说明（可留空）"></textarea>
            <div class="help">可选，支持换行。</div>
        </div>

        <div class="actions">
            <button class="btn" type="submit"><i class="bi bi-send" aria-hidden="true"></i> 发送</button>
            <a class="btn btn-quiet" href="${ctx}/notifications">取消</a>
        </div>
    </form>
</div>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
