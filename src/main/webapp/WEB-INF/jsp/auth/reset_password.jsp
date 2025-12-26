<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 560px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">密码重置</h2>
            <p class="card-subtitle">请输入注册邮箱并完成邮箱验证码验证，验证通过后即可设置新密码。</p>
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
            <input type="hidden" name="op" value="sendResetCode"/>

            <div class="form-row">
                <label for="email">注册邮箱</label>
                <input id="email" type="email" name="email"
                       value="${resetEmail != null ? resetEmail : param.email}"
                       placeholder="name@example.com" required/>
            </div>

            <div class="form-row">
                <label for="resetCode">验证码</label>
                <div style="display:flex; gap:8px; align-items:center;">
                    <input id="resetCode" type="text" name="resetCode"
                           value="${param.resetCode}" placeholder="6 位数字" maxlength="6"/>
                    <button class="btn-secondary otp-btn" type="submit" name="op" value="sendResetCode" data-otp-key="reset_email_code">
                        发送验证码
                    </button>
                    <button class="btn-primary" type="submit" name="op" value="verifyResetCode">
                        验证并下一步
                    </button>
                </div>
                <small>首先点击“发送验证码”，收到邮件后在此输入验证码，再点击“验证并下一步”。</small>
            </div>

            <div class="actions">
                <a class="btn-quiet" href="${pageContext.request.contextPath}/auth/login" style="text-decoration:none;">
                    返回登录
                </a>
            </div>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
