<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 720px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">创建账号</h2>
            <p class="card-subtitle">注册成功后账号状态为待审核（PENDING），需系统管理员审核通过后方可登录。</p>
        </div>
    </div>

    <div class="stack">
        <small>密码建议至少 8 位，包含字母、数字与符号（例如：Password@123）。</small>

        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-success"><c:out value="${message}"/></div>
        </c:if>

        <form action="${pageContext.request.contextPath}/auth/register" method="post" class="stack">
            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username" required placeholder="例如：kevin"/>
            </div>
            <div class="form-row">
                <label for="password">密码</label>
                <input id="password" type="password" name="password" required placeholder="至少 8 位"/>
            </div>
            <div class="form-row">
                <label for="confirmPassword">确认密码</label>
                <input id="confirmPassword" type="password" name="confirmPassword" required placeholder="再次输入密码"/>
            </div>
            <div class="form-row">
                <label for="email">邮箱</label>
                <input id="email" type="email" name="email" placeholder="name@example.com"/>
            </div>
            <div class="form-row">
                <label for="fullName">姓名</label>
                <input id="fullName" type="text" name="fullName" placeholder="可选"/>
            </div>
            <div class="form-row">
                <label for="affiliation">单位/机构</label>
                <input id="affiliation" type="text" name="affiliation" placeholder="可选"/>
            </div>
            <div class="form-row">
                <label for="researchArea">研究方向</label>
                <input id="researchArea" type="text" name="researchArea" placeholder="可选"/>
            </div>
            <div class="form-row">
                <label for="registerRole">注册身份</label>
                <select id="registerRole" name="registerRole">
                    <option value="AUTHOR">作者（投稿人）</option>
                    <option value="REVIEWER">审稿人</option>
                </select>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-person-plus" aria-hidden="true"></i>
                    注册
                </button>
                <a class="btn-quiet" href="${pageContext.request.contextPath}/auth/login" style="text-decoration:none;">已有账号？去登录</a>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
