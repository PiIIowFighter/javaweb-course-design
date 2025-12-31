<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 560px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">密码重置</h2>
            <p class="card-subtitle">此处为课程项目预留页面，可后续接入邮箱验证或管理员重置流程。</p>
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
            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username" required placeholder="请输入用户名"/>
            </div>
            <div class="form-row">
                <label for="email">注册邮箱</label>
                <input id="email" type="email" name="email" placeholder="name@example.com"/>
            </div>
            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-send" aria-hidden="true"></i>
                    提交请求
                </button>
                <a class="btn-quiet" href="${pageContext.request.contextPath}/auth/login" style="text-decoration:none;">返回登录</a>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
