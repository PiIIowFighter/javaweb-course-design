<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">科研论文在线投稿及管理系统</h2>
            <p class="card-subtitle">简洁、专业、可追踪的投稿与审稿流程：作者投稿 · 编辑处理 · 审稿评审 · 终审决策</p>
        </div>
    </div>

    <div class="grid grid-2">
        <div class="stack">
            <h3>你可以做什么</h3>
            <ul>
                <li>创建稿件并保存草稿，完善元数据与附件</li>
                <li>跟踪稿件状态流转（形式审查、案头审查、外审、终审等）</li>
                <li>在不同角色工作台中执行对应操作</li>
            </ul>
        </div>

        <div class="stack">
            <h3>开始使用</h3>
            <p>如果你还没有账号，请先注册；已有账号则直接登录进入工作台。</p>
            <div class="actions">
                <a class="btn-primary" style="text-decoration:none;" href="${pageContext.request.contextPath}/auth/register">
                    <i class="bi bi-person-plus" aria-hidden="true"></i> 注册
                </a>
                <a style="text-decoration:none;" href="${pageContext.request.contextPath}/auth/login">
                    <i class="bi bi-box-arrow-in-right" aria-hidden="true"></i> 登录
                </a>
            </div>
            <small>提示：登录后会根据角色进入对应工作台。</small>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
