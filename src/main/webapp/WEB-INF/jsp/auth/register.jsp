<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<div class="card" style="max-width: 720px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">创建账号</h2>
            <p class="card-subtitle">注册成功后账号状态为 ACTIVE，可直接登录。</p>
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

        <form id="registerForm" action="${pageContext.request.contextPath}/auth/register" method="post" class="stack">
            <div class="form-row">
                <label for="username">用户名</label>
                <input id="username" type="text" name="username" required placeholder="例如：kevin"
                       value="${fn:escapeXml(param.username)}"/>
            </div>
            <div class="form-row">
                <label for="password">密码</label>
                <input id="password" type="password" name="password"
                       value="${param.password}" required placeholder="至少 8 位"/>

                <!-- 密码强度条：1种字符=弱(红)；2种=中(黄)；3种=强(绿) -->
                <div style="margin-top:8px;">
                    <div style="display:flex; align-items:center; gap:8px;">
                        <div style="flex:1; height:10px; background:#eee; border-radius:999px; overflow:hidden;">
                            <div id="pwdStrengthBar" style="height:10px; width:0; background:red;"></div>
                        </div>
                        <span id="pwdStrengthText" style="min-width:48px; font-size:12px;">&nbsp;</span>
                    </div>
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
                    <button class="btn-secondary otp-btn" type="button" id="btnSendRegisterCode" data-otp-key="register_email_code">
                        发送验证码
                    </button>
                </div>
                <small>点击“发送验证码”后，请在 5 分钟内查看邮箱并填写下方验证码。</small>
                <div id="sendCodeMsg" style="margin-top:6px; font-size:12px;"></div>
            </div>

            <div class="form-row">
                <label for="emailCode">邮箱验证码</label>
                <input id="emailCode" type="text" name="emailCode" required maxlength="6"
                       value="${fn:escapeXml(param.emailCode)}" placeholder="6 位数字"/>
            </div>
            <div class="form-row">
                <label for="fullName">姓名</label>
                <input id="fullName" type="text" name="fullName" placeholder="可选"
                       value="${fn:escapeXml(param.fullName)}"/>
            </div>
            <div class="form-row">
                <label for="affiliation">单位/机构</label>
                <input id="affiliation" type="text" name="affiliation" placeholder="可选"
                       value="${fn:escapeXml(param.affiliation)}"/>
            </div>
            <div class="form-row">
                <label for="researchArea">研究方向</label>
                <input id="researchArea" type="text" name="researchArea" placeholder="可选"
                       value="${fn:escapeXml(param.researchArea)}"/>
            </div>
            <div class="form-row">
                <label for="registerRole">注册身份</label>
                <select id="registerRole" name="registerRole">
                    <option value="AUTHOR" <c:if test="${empty param.registerRole || param.registerRole == 'AUTHOR'}">selected</c:if>>作者（投稿人）</option>
                    <option value="REVIEWER" <c:if test="${param.registerRole == 'REVIEWER'}">selected</c:if>>审稿人</option>
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

<script>
    // ========== 密码强度条（按字符类型计数） ==========
    (function () {
        var pwd = document.getElementById('password');
        var bar = document.getElementById('pwdStrengthBar');
        var txt = document.getElementById('pwdStrengthText');
        if (!pwd || !bar || !txt) return;

        function countTypes(s) {
            var types = 0;
            if (/[A-Za-z]/.test(s)) types++;
            if (/[0-9]/.test(s)) types++;
            if (/[^A-Za-z0-9]/.test(s)) types++;
            return types;
        }

        function update() {
            var v = pwd.value || '';
            var t = countTypes(v);

            if (!v) {
                bar.style.width = '0';
                bar.style.background = 'red';
                txt.textContent = '';
                return;
            }

            if (t <= 1) {
                bar.style.width = '33%';
                bar.style.background = 'red';
                txt.textContent = '弱';
                txt.style.color = 'red';
            } else if (t === 2) {
                bar.style.width = '66%';
                bar.style.background = 'gold';
                txt.textContent = '中';
                txt.style.color = 'goldenrod';
            } else {
                bar.style.width = '100%';
                bar.style.background = 'green';
                txt.textContent = '强';
                txt.style.color = 'green';
            }
        }

        pwd.addEventListener('input', update);
        update();
    })();

    // ========== 发送验证码：AJAX，不刷新页面，保留已填写内容 ==========
    (function () {
        var btn = document.getElementById('btnSendRegisterCode');
        var emailEl = document.getElementById('email');
        var msgEl = document.getElementById('sendCodeMsg');
        if (!btn || !emailEl || !msgEl) return;

        function setMsg(text, ok) {
            msgEl.textContent = text || '';
            msgEl.style.color = ok ? 'green' : 'red';
        }

        btn.addEventListener('click', function () {
            // footer.jsp 里会做冷却倒计时禁用；这里尊重 disabled 状态
            if (btn.disabled) return;

            var email = (emailEl.value || '').trim();
            if (!email) {
                setMsg('请先填写邮箱', false);
                return;
            }

            setMsg('发送中...', true);

            var form = new URLSearchParams();
            form.append('op', 'sendCode');
            form.append('email', email);

            fetch('${pageContext.request.contextPath}/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: form.toString()
            }).then(function (resp) {
                return resp.text().then(function (t) {
                    return { ok: resp.ok, text: t };
                });
            }).then(function (r) {
                if (r.ok) {
                    setMsg(r.text || '验证码已发送，请查收邮箱（5分钟有效）', true);
                } else {
                    setMsg(r.text || '发送失败，请稍后重试', false);
                }
            }).catch(function () {
                setMsg('发送失败，请检查网络或邮箱配置', false);
            });
        });
    })();
</script>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
