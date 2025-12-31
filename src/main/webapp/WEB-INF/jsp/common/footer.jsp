<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not empty sessionScope.currentUser}">
            </section>
        </div>
</c:if>
</div>
</main>

<footer class="site-footer">
    <div class="container footer-inner">
        <button type="button" class="btn-quiet" onclick="history.back();">
            <i class="bi bi-arrow-left" aria-hidden="true"></i>
            返回上一页
        </button>
        <small>&copy; 2025 科研论文在线投稿及管理系统 · JavaWeb 课程设计</small>
    </div>
</footer>

<script>
    function toggleNav() {
        const nav = document.getElementById('primaryNav');
        if (!nav) return;
        nav.classList.toggle('nav-open');
    }

    // Click-away to close on mobile
    document.addEventListener('click', function (e) {
        const nav = document.getElementById('primaryNav');
        const btn = document.querySelector('.nav-toggle');
        if (!nav || !btn) return;
        if (!nav.classList.contains('nav-open')) return;
        const within = nav.contains(e.target) || btn.contains(e.target);
        if (!within) nav.classList.remove('nav-open');
    });

    // Close menu after selecting a link (mobile)
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            const nav = document.getElementById('primaryNav');
            if (nav) nav.classList.remove('nav-open');
        }
    });

    // OTP / email verification cooldown for "发送验证码" buttons
    (function () {
        if (typeof document === 'undefined') return;

        function initOtpButton(btn) {
            if (!btn) return;
            var key = btn.getAttribute('data-otp-key');
            if (!key) return;

            var cooldownSeconds = 60;
            var originalText = btn.getAttribute('data-original-text') || btn.textContent.trim();

            if (!btn.getAttribute('data-original-text')) {
                btn.setAttribute('data-original-text', originalText);
            }

            function applyState() {
                var expiresAt = 0;
                try {
                    if (window.localStorage) {
                        var raw = localStorage.getItem('otp_cooldown_' + key);
                        expiresAt = raw ? parseInt(raw, 10) : 0;
                    }
                } catch (e) {
                    expiresAt = 0;
                }

                var now = Date.now();
                if (!expiresAt || expiresAt <= now) {
                    btn.disabled = false;
                    btn.textContent = btn.getAttribute('data-original-text') || originalText;
                    return;
                }

                var remaining = Math.ceil((expiresAt - now) / 1000);
                if (remaining < 0) remaining = 0;

                btn.disabled = true;
                btn.textContent = remaining + 's';

                window.setTimeout(applyState, 1000);
            }

            // Initialize state on first load / after refresh
            applyState();

            btn.addEventListener('click', function () {
                // 如果当前在冷却中，什么也不做（表单也不会提交，因为按钮被 disabled）
                if (btn.disabled) return;

                var now = Date.now();
                var expiresAt = now + cooldownSeconds * 1000;
                try {
                    if (window.localStorage) {
                        localStorage.setItem('otp_cooldown_' + key, String(expiresAt));
                    }
                } catch (e) {
                    // ignore storage errors
                }
                // 不在这里修改 disabled 或文本，避免影响表单提交逻辑
                // 页面重新加载后，applyState 会根据 localStorage 自动恢复倒计时状态
            });
        }

        document.addEventListener('DOMContentLoaded', function () {
            var buttons = document.querySelectorAll('button.otp-btn[data-otp-key]');
            if (!buttons || buttons.length === 0) return;
            for (var i = 0; i < buttons.length; i++) {
                initOtpButton(buttons[i]);
            }
        });
    })();



    // Sidebar active link highlight (client-side, best-match)
    (function () {
        function normalizePath(p) {
            if (!p) return '';
            p = String(p).split('?')[0].split('#')[0];
            p = p.replace(/\/+/g, '/');
            if (p.length > 1) p = p.replace(/\/+$/, '');
            // treat /list and /index as the same route root
            p = p.replace(/\/(list|index)$/, '');
            if (p.length > 1) p = p.replace(/\/+$/, '');
            return p;
        }

        function pickBestSidebarLink() {
            var sidebar = document.querySelector('.sidebar');
            if (!sidebar) return null;

            var links = sidebar.querySelectorAll('a.side-link[href]');
            if (!links || links.length === 0) return null;

            var cur = normalizePath(window.location.pathname);
            var best = null;
            var bestScore = -1;

            for (var i = 0; i < links.length; i++) {
                var a = links[i];
                var href = a.getAttribute('href');
                if (!href) continue;

                var path = '';
                try {
                    path = normalizePath(new URL(href, window.location.origin).pathname);
                } catch (e) {
                    continue;
                }
                if (!path) continue;

                var score = -1;
                if (cur === path) {
                    score = 10000 + path.length;
                } else if (cur.indexOf(path + '/') === 0) {
                    score = 5000 + path.length;
                } else if (path.indexOf(cur + '/') === 0) {
                    // fallback: current path is shorter than the link (rare)
                    score = 1000 + cur.length;
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = a;
                }
            }

            // only accept a match if we have a meaningful score
            return bestScore > 0 ? best : null;
        }

        document.addEventListener('DOMContentLoaded', function () {
            var best = pickBestSidebarLink();
            if (!best) return;

            var sidebar = document.querySelector('.sidebar');
            if (!sidebar) return;

            // remove "active" from all links to avoid double highlight (e.g. /notifications & /notifications/send)
            var links = sidebar.querySelectorAll('a.side-link');
            for (var i = 0; i < links.length; i++) {
                links[i].classList.remove('active');
                links[i].removeAttribute('aria-current');
            }
            best.classList.add('active');
            best.setAttribute('aria-current', 'page');
        });
    })();

</script>
</body>
</html>