<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 560px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">登录</h2>
            <p class="card-subtitle">欢迎回来。请输入账号信息进入工作台。</p>
        </div>
    </div>

    <div class="stack">
        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-success"><c:out value="${message}"/></div>
        </c:if>

        <form action="${pageContext.request.contextPath}/auth/login" method="post" class="stack">
            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username" required placeholder="请输入用户名"/>
            </div>
            <div class="form-row">
                <label for="password">密码</label>
                <input id="password" type="password" name="password" required placeholder="请输入密码"/>
            </div>
            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-box-arrow-in-right" aria-hidden="true"></i>
                    登录
                </button>
                <a class="btn-quiet" href="${pageContext.request.contextPath}/auth/reset" style="text-decoration:none;">
                    忘记密码
                </a>
            </div>
        </form>

        <small>没有账号？<a href="${pageContext.request.contextPath}/auth/register">创建一个账号</a></small>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
