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
            <!-- 默认 op=doRegister，也可以通过按钮覆盖为 sendCode -->
            <input type="hidden" name="op" value="doRegister"/>

            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username"
                       value="${param.username}" required placeholder="例如：kevin"/>
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
                <div style="display:flex; gap:8px; align-items:center;">
                    <input id="email" type="email" name="email"
                           value="${param.email}" placeholder="name@example.com" required/>
                    <button class="btn-secondary" type="submit" name="op" value="sendCode">
                        发送验证码
                    </button>
                </div>
                <small>点击“发送验证码”后，请在 5 分钟内查看邮箱并填写下方验证码。</small>
            </div>

            <div class="form-row">
                <label for="emailCode">邮箱验证码</label>
                <input id="emailCode" type="text" name="emailCode"
                       value="${param.emailCode}" placeholder="6 位数字" maxlength="6"/>
            </div>

            <div class="form-row">
                <label for="fullName">姓名</label>
                <input id="fullName" type="text" name="fullName"
                       value="${param.fullName}" placeholder="可选"/>
            </div>
            <div class="form-row">
                <label for="affiliation">单位/机构</label>
                <input id="affiliation" type="text" name="affiliation"
                       value="${param.affiliation}" placeholder="可选"/>
            </div>
            <div class="form-row">
                <label for="researchArea">研究方向</label>
                <input id="researchArea" type="text" name="researchArea"
                       value="${param.researchArea}" placeholder="可选"/>
            </div>
            <div class="form-row">
                <label for="registerRole">注册身份</label>
                <select id="registerRole" name="registerRole">
                    <option value="AUTHOR" ${param.registerRole == 'AUTHOR' ? 'selected' : ''}>作者（投稿人）</option>
                    <option value="REVIEWER" ${param.registerRole == 'REVIEWER' ? 'selected' : ''}>审稿人</option>
                </select>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-person-plus" aria-hidden="true"></i>
                    注册
                </button>
                <a class="btn-quiet" href="${pageContext.request.contextPath}/auth/login" style="text-decoration:none;">
                    已有账号？去登录
                </a>
            </div>
        </form>

    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
