<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 720px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">创建账号</h2>
</div></div>
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
                <input id="password" type="password" name="password"
                       value="${param.password}" required placeholder="至少 8 位"/>
                
                <div class="pwd-strength" aria-live="polite">
                    <div class="pwd-strength-track">
                        <div id="pwdStrengthBar" class="pwd-strength-bar" style="width:0%; background:#e53935;"></div>
                    </div>
                    <small id="pwdStrengthText" class="pwd-strength-text">强度：-</small>
                </div>
            </div>
            <div class="form-row">
                <label for="confirmPassword">确认密码</label>
                <input id="confirmPassword" type="password" name="confirmPassword"
                       value="${param.confirmPassword}" required placeholder="再次输入密码"/>
            </div>
            <div class="form-row">
                <label for="email">邮箱</label>
                <div style="display:flex; gap:8px; align-items:center;">
                    <input id="email" type="email" name="email"
                           value="${param.email}" placeholder="name@example.com" required/>
                    <button class="btn-secondary otp-btn" type="submit" name="op" value="sendCode" data-otp-key="register_email_code" formnovalidate>
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

<style>
    /* 注册页：密码强度条（仅本页使用，避免影响全站样式） */
    .pwd-strength { margin-top: 8px; display: grid; gap: 6px; }
    .pwd-strength-track {
        width: 100%;
        height: 8px;
        border-radius: 999px;
        background: rgba(0,0,0,0.08);
        overflow: hidden;
    }
    .pwd-strength-bar {
        height: 100%;
        border-radius: 999px;
        transition: width 120ms ease, background 120ms ease;
    }
    .pwd-strength-text { color: rgba(0,0,0,0.65); }
</style>

<script>
    (function () {
        function calcTypes(pwd) {
            var hasLetter = /[a-zA-Z]/.test(pwd);
            var hasDigit = /[0-9]/.test(pwd);
            var hasOther = /[^a-zA-Z0-9]/.test(pwd);
            return (hasLetter ? 1 : 0) + (hasDigit ? 1 : 0) + (hasOther ? 1 : 0);
        }

        function updateStrength(pwd) {
            var bar = document.getElementById('pwdStrengthBar');
            var text = document.getElementById('pwdStrengthText');
            if (!bar || !text) return;

            if (!pwd) {
                bar.style.width = '0%';
                bar.style.background = '#e53935';
                text.textContent = '强度：-';
                return;
            }

            var types = calcTypes(pwd);
            if (types <= 1) {
                bar.style.width = '33%';
                bar.style.background = '#e53935';
                text.textContent = '强度：弱';
            } else if (types === 2) {
                bar.style.width = '66%';
                bar.style.background = '#f6a100';
                text.textContent = '强度：中';
            } else {
                bar.style.width = '100%';
                bar.style.background = '#2e7d32';
                text.textContent = '强度：强';
            }
        }

        document.addEventListener('DOMContentLoaded', function () {
            var pwdInput = document.getElementById('password');
            if (!pwdInput) return;
            updateStrength(pwdInput.value || '');
            pwdInput.addEventListener('input', function () {
                updateStrength(pwdInput.value || '');
            });
        });
    })();
</script>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
