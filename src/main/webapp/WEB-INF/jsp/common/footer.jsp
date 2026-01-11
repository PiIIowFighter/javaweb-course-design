<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not empty sessionScope.currentUser}">
                    </div>
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



    // Sidebar: keep <details> open state across navigation + correct highlight (especially for manuscripts?group=...)
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

        function scoreLink(curPath, linkPath) {
            if (!curPath || !linkPath) return -1;
            if (curPath === linkPath) return 10000 + linkPath.length;
            if (curPath.indexOf(linkPath + '/') === 0) return 5000 + linkPath.length;
            if (linkPath.indexOf(curPath + '/') === 0) return 1000 + curPath.length;
            return -1;
        }

        function getMsGroupFromHref(href) {
            if (!href) return null;
            try {
                var u = new URL(href, window.location.origin);
                var g = u.searchParams.get('group');
                return g ? String(g).toLowerCase() : null;
            } catch (e) {
                return null;
            }
        }

        function restoreDetailsOpenState(sidebar) {
            if (!sidebar) return;
            var detailsList = sidebar.querySelectorAll('details.side-group');
            if (!detailsList || detailsList.length === 0) return;

            for (var i = 0; i < detailsList.length; i++) {
                (function (idx) {
                    var d = detailsList[idx];
                    var key = d.getAttribute('data-side-key') || ('side-group-' + idx);

                    // restore
                    try {
                        var stored = window.sessionStorage ? sessionStorage.getItem('sidebar_open_' + key) : null;
                        if (stored === '1') d.open = true;
                        if (stored === '0') d.open = false;
                    } catch (e) {
                        // ignore
                    }

                    // persist
                    d.addEventListener('toggle', function () {
                        try {
                            if (!window.sessionStorage) return;
                            sessionStorage.setItem('sidebar_open_' + key, d.open ? '1' : '0');
                        } catch (e2) {
                            // ignore
                        }
                    });
                })(i);
            }
        }

        function markActive(a) {
            if (!a) return;
            a.classList.add('active');
            a.setAttribute('aria-current', 'page');

            // if inside a <details>, force it open (so sidebar stays expanded)
            var parent = a.parentElement;
            while (parent) {
                if (parent.tagName && parent.tagName.toLowerCase() === 'details') {
                    parent.open = true;
                    break;
                }
                parent = parent.parentElement;
            }
        }

        function clearActive(links) {
            for (var i = 0; i < links.length; i++) {
                links[i].classList.remove('active');
                links[i].removeAttribute('aria-current');
            }
        }

        document.addEventListener('DOMContentLoaded', function () {
            var sidebar = document.querySelector('.sidebar');
            if (!sidebar) return;

            restoreDetailsOpenState(sidebar);

            var allLinks = sidebar.querySelectorAll('a.side-link[href]');
            if (!allLinks || allLinks.length === 0) return;

            var curPath = normalizePath(window.location.pathname);
            var params = new URLSearchParams(window.location.search || '');

            // --- 1) Special: manuscripts list should highlight by ?group=... ---
            var rawPath = window.location.pathname || '';
            var isMsPath = rawPath.indexOf('/manuscripts/list') !== -1
                || rawPath.indexOf('/manuscripts/detail') !== -1
                || rawPath.indexOf('/manuscripts/track') !== -1
                || rawPath.indexOf('/manuscripts/edit') !== -1
                || rawPath.indexOf('/manuscripts/resubmit') !== -1;
            var msGroup = (params.get('group') || '').toLowerCase();

            // remember last group on click
            var msLinks = sidebar.querySelectorAll('a.side-sublink[data-ms-group]');
            for (var k = 0; k < msLinks.length; k++) {
                msLinks[k].addEventListener('click', function () {
                    try {
                        if (!window.sessionStorage) return;
                        var g = (this.getAttribute('data-ms-group') || '').toLowerCase();
                        if (g) sessionStorage.setItem('sidebar_ms_group', g);
                    } catch (e) {
                        // ignore
                    }
                });
            }

            if (isMsPath) {
                if (!msGroup) {
                    try {
                        if (window.sessionStorage) msGroup = (sessionStorage.getItem('sidebar_ms_group') || '').toLowerCase();
                    } catch (e0) {
                        // ignore
                    }
                }

                if (msGroup) {
                    // Only clear active on manuscript sublinks, don't touch other menu items.
                    clearActive(msLinks);

                    var picked = null;
                    for (var m = 0; m < msLinks.length; m++) {
                        var g2 = (msLinks[m].getAttribute('data-ms-group') || '').toLowerCase();
                        if (g2 === msGroup) {
                            picked = msLinks[m];
                            break;
                        }
                    }

                    if (!picked) {
                        // fallback: match via href query
                        for (var n = 0; n < msLinks.length; n++) {
                            if (getMsGroupFromHref(msLinks[n].getAttribute('href')) === msGroup) {
                                picked = msLinks[n];
                                break;
                            }
                        }
                    }

                    if (picked) {
                        markActive(picked);
                        return; // manuscripts handled
                    }
                }
            }

            // --- 2) If server already highlighted links, keep only the best one (avoid double highlight) ---
            var preActives = sidebar.querySelectorAll('a.side-link.active[href]');
            if (preActives && preActives.length > 0) {
                var keep = preActives[0];
                var bestScore = -1;
                for (var i = 0; i < preActives.length; i++) {
                    var p;
                    try {
                        p = normalizePath(new URL(preActives[i].getAttribute('href'), window.location.origin).pathname);
                    } catch (e1) {
                        continue;
                    }
                    var s = scoreLink(curPath, p);
                    if (s > bestScore) {
                        bestScore = s;
                        keep = preActives[i];
                    }
                }
                // clear others
                for (var j = 0; j < preActives.length; j++) {
                    if (preActives[j] !== keep) {
                        preActives[j].classList.remove('active');
                        preActives[j].removeAttribute('aria-current');
                    }
                }
                markActive(keep);
                return;
            }

            // --- 3) Otherwise: best-match by path (fallback) ---
            var best = null;
            var bestS = -1;
            for (var t = 0; t < allLinks.length; t++) {
                var href = allLinks[t].getAttribute('href');
                var path = '';
                try {
                    path = normalizePath(new URL(href, window.location.origin).pathname);
                } catch (e2) {
                    continue;
                }
                var sc = scoreLink(curPath, path);
                if (sc > bestS) {
                    bestS = sc;
                    best = allLinks[t];
                }
            }

            if (bestS > 0 && best) {
                clearActive(allLinks);
                markActive(best);
            }
        });
    })();

</script>
</body>
</html>