<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<style>
    /* Auth split layout: left illustration + right form
       - left image fills its panel
       - right form panel matches the left panel size for a symmetric layout
       - keep everything visible in one screen on common desktop sizes
    */
    .auth-split {
        display: flex;
        gap: 28px;
        /* Center the whole block vertically within the page content area */
        align-items: center;
        justify-content: center;
        padding: 18px 0;
        margin: 0 auto;
        max-width: 1280px;

        /*
           Keep the entire auth block vertically centered on screen.
           We can't know the exact header/footer height here, so keep a safe buffer.
        */
        min-height: calc(100vh - 220px);
        --auth-panel-h: clamp(340px, 58vh, 520px);
    }

    .auth-illus {
        flex: 1;
        height: var(--auth-panel-h);
        border-radius: 18px;
        overflow: hidden;
        position: relative;
        background: linear-gradient(135deg, rgba(16, 120, 255, 0.10), rgba(0, 0, 0, 0.00));
        border: 1px solid rgba(0, 0, 0, 0.06);
        display: block;
    }

    .auth-illus img {
        /* Keep panel size fixed, show the lower part of the image (bottom aligned) */
        position: absolute;
        left: 0;
        top: auto;
        bottom: 0;
        width: 100%;
        height: 150%;
        object-fit: cover;
        object-position: center bottom;
        padding: 0;
        display: block;
        filter: saturate(1.05);
    }

    .auth-form {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        height: var(--auth-panel-h);
    }

    .auth-form .card {
        width: 100%;
        max-width: none;
        margin: 0;
        height: 100%;
        display: flex;
        flex-direction: column;
        /* Title at top-left; content fills the card */
        justify-content: flex-start;
        padding: 28px 56px 32px;
    }

    .auth-form .card .card-header {
        margin-bottom: 22px;
        width: 100%;
    }

    /* Make "登录" larger and align it to the top-left corner of the card */
    .auth-form .card .page-head {
        display: flex;
        align-items: center;
        justify-content: flex-start;
        gap: 12px;
    }

    .auth-form .card .page-title {
        font-size: 30px;
        font-weight: 800;
        letter-spacing: 0.5px;
        margin: 0;
    }

    /* Keep the actual input area comfortable even when the right panel is wide */
    .auth-form .card .stack {
        width: 100%;
        max-width: 520px;
        margin: 0 auto;
        height: 100%;
        display: flex;
        flex-direction: column;
    }

    /* Enlarge labels and inputs */
    .auth-form .card .form-row label {
        font-size: 16px;
        font-weight: 600;
    }

    .auth-form .card .form-row input {
        font-size: 16px;
        padding: 12px 14px;
        height: 46px;
        border-radius: 12px;
    }

    /* Make the form occupy the card and place buttons at the mid-lower area */
    .auth-form .card form.stack {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 14px;
        margin-top: 12px;
    }

    /* Center username/password rows within the available space, keep label/input aligned */
    .auth-form .card form.stack .form-fields {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: center;
        gap: 18px;
        width: 100%;
        max-width: 640px;
        margin: 0 auto;
    }

    .auth-form .card .form-row {
        display: grid;
        grid-template-columns: 120px 1fr;
        align-items: center;
        column-gap: 18px;
        width: 100%;
    }

    .auth-form .card .actions {
        margin-top: auto;
        display: flex;
        gap: 16px;
        padding-top: 12px;
    }

    .auth-form .card .actions > * {
        flex: 1;
        height: 46px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
    }

    @media (max-width: 992px) {
        .auth-split {
            flex-direction: column;
            align-items: stretch;
            padding: 10px 0;
            gap: 16px;
            min-height: 0;
            --auth-panel-h: auto;
        }

        .auth-illus {
            height: 260px;
        }

        .auth-form {
            align-items: stretch;
            height: auto;
        }

        .auth-form .card {
            padding: 22px 24px 26px;
        }

        .auth-form .card .stack {
            max-width: 520px;
        }
    }

    @media (max-width: 520px) {
        .auth-illus {
            display: none;
        }
    }
</style>

<div class="auth-split">
    <div class="auth-illus" aria-hidden="true">
        <img src="${ctx}/static/img/illustrations/auth-ai-left.png" alt="AI illustration"/>
    </div>

    <div class="auth-form">
        <div class="card">
            <div class="card-header">
                <div>
                    <div class="page-head">
                        <h2 class="page-title">登录</h2>
                        <div class="chips">
                            <span class="chip">欢迎回来。请输入账号信息进入工作台。</span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="stack">
                <c:if test="${not empty error}">
                    <div class="alert alert-danger"><c:out value="${error}"/></div>
                </c:if>
                <c:if test="${not empty message}">
                    <div class="alert alert-success"><c:out value="${message}"/></div>
                </c:if>

                <form action="${ctx}/auth/login" method="post" class="stack">
                    <div class="form-fields">
                        <div class="form-row">
                            <label for="username">用户名</label>
                            <input id="username" type="text" name="username" required placeholder="请输入用户名"/>
                        </div>
                        <div class="form-row">
                            <label for="password">密码</label>
                            <input id="password" type="password" name="password" required placeholder="请输入密码"/>
                        </div>
                    </div>
                    <div class="actions">
                        <button class="btn-primary" type="submit">
                            <i class="bi bi-box-arrow-in-right" aria-hidden="true"></i>
                            登录
                        </button>
                        <a class="btn-quiet" href="${ctx}/auth/reset" style="text-decoration:none;">
                            忘记密码
                        </a>
                    </div>
                </form>

                <small>没有账号？<a href="${ctx}/auth/register">创建一个账号</a></small>
            </div>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
