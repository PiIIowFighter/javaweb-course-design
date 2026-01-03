<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="roleCode" value="${empty sessionScope.currentUser.roleCode ? 'AUTHOR' : sessionScope.currentUser.roleCode}"/>
<c:set var="uri" value="${pageContext.request.requestURI}"/>

<aside class="sidebar">
    <div class="card mini sidebar-user">
        <div class="media">
            <div class="avatar">
                <img src="${ctx}/profile/avatar"
                     alt="用户头像"
                     onerror="this.onerror=null;this.src='${ctx}/static/img/default-avatar.svg';"
                     style="width: 100%; height: 100%; border-radius: 999px; object-fit: cover;"/>
            </div>
            <div>
                <div class="sidebar-user-name">
                    <c:out value="${sessionScope.currentUser.username}"/>
                </div>
                <div class="sidebar-user-role">
                    <c:out value="${roleCode}"/>
                </div>
            </div>
        </div>
        <div class="sidebar-user-actions">
            <a class="side-link" href="${ctx}/profile">
                <i class="bi bi-person" aria-hidden="true"></i> 个人信息
            </a>
            <a class="side-link" href="${ctx}/auth/logout">
                <i class="bi bi-box-arrow-right" aria-hidden="true"></i> 退出登录
            </a>
        </div>
    </div>

    <h3 class="side-title">功能菜单</h3>
    <nav class="side-nav" aria-label="登录后功能菜单">


	    <c:choose>
            <c:when test="${roleCode == 'AUTHOR'}">
                <a class="side-link ${fn:contains(uri, '/manuscripts/list') ? 'active' : ''}" href="${ctx}/manuscripts/list">
                    <i class="bi bi-folder2-open" aria-hidden="true"></i> 我的稿件
                </a>
                <a class="side-link ${fn:contains(uri, '/manuscripts/submit') ? 'active' : ''}" href="${ctx}/manuscripts/submit">
                    <i class="bi bi-upload" aria-hidden="true"></i> 提交稿件
                </a>
            </c:when>

            <c:when test="${roleCode == 'REVIEWER'}">
                <a class="side-link ${fn:contains(uri, '/reviewer/assigned') ? 'active' : ''}" href="${ctx}/reviewer/assigned">
                    <i class="bi bi-inbox" aria-hidden="true"></i> 待评审稿件
                </a>
                <a class="side-link ${fn:contains(uri, '/reviewer/history') ? 'active' : ''}" href="${ctx}/reviewer/history">
                    <i class="bi bi-clock-history" aria-hidden="true"></i> 历史评审记录
                </a>
            </c:when>

            <c:when test="${roleCode == 'EDITOR'}">
                <a class="side-link ${fn:contains(uri, '/editor/withEditor') ? 'active' : ''}" href="${ctx}/editor/withEditor">
                    <i class="bi bi-person-workspace" aria-hidden="true"></i> 我的待处理稿件
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/underReview') ? 'active' : ''}" href="${ctx}/editor/underReview">
                    <i class="bi bi-hourglass-split" aria-hidden="true"></i> 审稿中
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/recommend') ? 'active' : ''}" href="${ctx}/editor/recommend">
                    <i class="bi bi-pencil-square" aria-hidden="true"></i> 提出建议
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/review/monitor') ? 'active' : ''}" href="${ctx}/editor/review/monitor">
                    <i class="bi bi-bell" aria-hidden="true"></i> 审稿监控 / 催审
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/authorComm') ? 'active' : ''}" href="${ctx}/editor/authorComm">
                    <i class="bi bi-chat-dots" aria-hidden="true"></i> 与作者沟通
                </a>
</c:when>

            <c:when test="${roleCode == 'EDITOR_IN_CHIEF'}">
                <a class="side-link ${fn:contains(uri, '/editor/overview') ? 'active' : ''}" href="${ctx}/editor/overview">
                    <i class="bi bi-eye" aria-hidden="true"></i> 系统全览
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/desk') ? 'active' : ''}" href="${ctx}/editor/desk">
                    <i class="bi bi-search" aria-hidden="true"></i> 案头审查
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/toAssign') ? 'active' : ''}" href="${ctx}/editor/toAssign">
                    <i class="bi bi-diagram-3" aria-hidden="true"></i> 待分配队列
                </a>
<a class="side-link ${fn:contains(uri, '/editor/reviewers') ? 'active' : ''}" href="${ctx}/editor/reviewers">
                    <i class="bi bi-people" aria-hidden="true"></i> 审稿人库管理
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/special') ? 'active' : ''}" href="${ctx}/editor/special">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i> 特殊权限操作
                </a>
            </c:when>

            <c:when test="${roleCode == 'EO_ADMIN'}">
                <a class="side-link ${fn:contains(uri, '/editor/formalCheck') ? 'active' : ''}" href="${ctx}/editor/formalCheck">
                    <i class="bi bi-clipboard-check" aria-hidden="true"></i> 形式审查 / 格式检查
                </a>
                <a class="side-link ${fn:contains(uri, '/editor/formalCheck/history') ? 'active' : ''}" href="${ctx}/editor/formalCheck/history">
                    <i class="bi bi-clock-history" aria-hidden="true"></i> 审查历史
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/news') ? 'active' : ''}" href="${ctx}/admin/news/list">
                    <i class="bi bi-megaphone" aria-hidden="true"></i> 新闻 / 公告管理
                </a>
            </c:when>

            <c:when test="${roleCode == 'SYSTEM_ADMIN' || roleCode == 'SUPER_ADMIN'}">
                <a class="side-link ${fn:contains(uri, '/admin/users') ? 'active' : ''}" href="${ctx}/admin/users/list">
                    <i class="bi bi-people" aria-hidden="true"></i> 用户管理
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/permissions') ? 'active' : ''}" href="${ctx}/admin/permissions/list">
                    <i class="bi bi-shield-lock" aria-hidden="true"></i> 权限管理
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/system/status') ? 'active' : ''}" href="${ctx}/admin/system/status">
                    <i class="bi bi-activity" aria-hidden="true"></i> 系统状态
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/system/db') ? 'active' : ''}" href="${ctx}/admin/system/db">
                    <i class="bi bi-database-gear" aria-hidden="true"></i> 数据库维护
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/logs') ? 'active' : ''}" href="${ctx}/admin/logs/list">
                    <i class="bi bi-journal-text" aria-hidden="true"></i> 系统日志
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/journals') ? 'active' : ''}" href="${ctx}/admin/journals/list">
                    <i class="bi bi-journals" aria-hidden="true"></i> 期刊管理
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/editorial') ? 'active' : ''}" href="${ctx}/admin/editorial/list">
                    <i class="bi bi-diagram-2" aria-hidden="true"></i> 编委管理
                </a>
                <a class="side-link ${fn:contains(uri, '/admin/news') ? 'active' : ''}" href="${ctx}/admin/news/list">
                    <i class="bi bi-megaphone" aria-hidden="true"></i> 公告 / 新闻
                </a>
            </c:when>

            <c:otherwise>
                <div class="muted" style="padding: 10px 12px;">
                    未识别角色：<c:out value="${roleCode}"/>
                </div>
            </c:otherwise>
        </c:choose>
    </nav>
</aside>
