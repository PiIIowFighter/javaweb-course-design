<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 560px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">设置新密码</h2>
            <p class="card-subtitle">邮箱验证码已验证，请为账号设置一个新的登录密码。</p>
        </div>
    </div>

    <div class="stack">
        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-success"><c:out value="${message}"/></div>
        </c:if>

        <form action="${pageContext.request.contextPath}/auth/reset" method="post" class="stack">
            <input type="hidden" name="op" value="doResetPassword"/>

            <div class="form-row">
                <label for="newPassword">新密码</label>
                <input id="newPassword" type="password" name="newPassword" required placeholder="至少 8 位"/>
            </div>
            <div class="form-row">
                <label for="confirmNewPassword">确认新密码</label>
                <input id="confirmNewPassword" type="password" name="confirmNewPassword" required placeholder="再次输入新密码"/>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-check-circle" aria-hidden="true"></i>
                    确认重置
                </button>
                <a class="btn-quiet" href="${pageContext.request.contextPath}/auth/login" style="text-decoration:none;">
                    取消并返回登录
                </a>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
