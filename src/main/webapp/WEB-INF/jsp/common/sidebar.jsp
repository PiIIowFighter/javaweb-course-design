<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="uri" value="${pageContext.request.requestURI}"/>

<aside class="sidebar">
    <!-- 顶部用户卡片 -->
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
                    <c:out value="${sessionScope.currentUser.roleCode}"/>
                </div>
            </div>
        </div>
        <div class="sidebar-user-actions">
            <a class="side-link ${fn:contains(uri, '/dashboard') ? 'active' : ''}" href="${ctx}/dashboard">
                <i class="bi bi-speedometer2" aria-hidden="true"></i> 工作台
            </a>
        </div>
    </div>

    <h3 class="side-title">功能菜单</h3>
    <nav class="side-nav" aria-label="登录后功能菜单">

        <!-- 投稿 / 作者 -->
        <c:if test="${sessionScope.menuPermMap['MENU_AUTHOR_MY_MANUSCRIPTS'] or sessionScope.menuPermMap['MENU_AUTHOR_SUBMIT']}">
            <div class="muted" style="padding: 10px 12px;">投稿 / 作者</div>
            <c:if test="${sessionScope.menuPermMap['MENU_AUTHOR_MY_MANUSCRIPTS']}">
                <a class="side-link ${fn:contains(uri, '/manuscripts/list') ? 'active' : ''}" href="${ctx}/manuscripts/list">
                    <i class="bi bi-folder2-open" aria-hidden="true"></i> 我的稿件
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_AUTHOR_SUBMIT']}">
                <a class="side-link ${fn:contains(uri, '/manuscripts/submit') ? 'active' : ''}" href="${ctx}/manuscripts/submit">
                    <i class="bi bi-upload" aria-hidden="true"></i> 提交稿件
                </a>
            </c:if>
        </c:if>

        <!-- 审稿人 -->
        <c:if test="${sessionScope.menuPermMap['MENU_REVIEWER_ASSIGNED'] or sessionScope.menuPermMap['MENU_REVIEWER_HISTORY']}">
            <div class="muted" style="padding: 10px 12px;">外审 / 审稿人</div>
            <c:if test="${sessionScope.menuPermMap['MENU_REVIEWER_ASSIGNED']}">
                <a class="side-link ${fn:contains(uri, '/reviewer/assigned') ? 'active' : ''}" href="${ctx}/reviewer/assigned">
                    <i class="bi bi-inbox" aria-hidden="true"></i> 待评审稿件
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_REVIEWER_HISTORY']}">
                <a class="side-link ${fn:contains(uri, '/reviewer/history') ? 'active' : ''}" href="${ctx}/reviewer/history">
                    <i class="bi bi-clock-history" aria-hidden="true"></i> 历史评审
                </a>
            </c:if>
        </c:if>

        <!-- 编辑 -->
        <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_TODO'] or sessionScope.menuPermMap['MENU_EDITOR_UNDER_REVIEW']
            or sessionScope.menuPermMap['MENU_EDITOR_RECOMMEND'] or sessionScope.menuPermMap['MENU_EDITOR_REVIEW_MONITOR']
            or sessionScope.menuPermMap['MENU_EDITOR_AUTHOR_COMM']}">
            <div class="muted" style="padding: 10px 12px;">编辑工作台</div>
            <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_TODO']}">
                <a class="side-link ${fn:contains(uri, '/editor/withEditor') ? 'active' : ''}" href="${ctx}/editor/withEditor">
                    <i class="bi bi-person-workspace" aria-hidden="true"></i> 编辑待办
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_UNDER_REVIEW']}">
                <a class="side-link ${fn:contains(uri, '/editor/underReview') ? 'active' : ''}" href="${ctx}/editor/underReview">
                    <i class="bi bi-hourglass-split" aria-hidden="true"></i> 审稿中
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_RECOMMEND']}">
                <a class="side-link ${fn:contains(uri, '/editor/recommend') ? 'active' : ''}" href="${ctx}/editor/recommend">
                    <i class="bi bi-pencil-square" aria-hidden="true"></i> 编辑推荐
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_REVIEW_MONITOR']}">
                <a class="side-link ${fn:contains(uri, '/editor/review/monitor') ? 'active' : ''}" href="${ctx}/editor/review/monitor">
                    <i class="bi bi-bell" aria-hidden="true"></i> 审稿监控 / 催审
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_AUTHOR_COMM']}">
                <a class="side-link ${fn:contains(uri, '/editor/authorComm') ? 'active' : ''}" href="${ctx}/editor/authorComm">
                    <i class="bi bi-chat-dots" aria-hidden="true"></i> 与作者沟通
                </a>
            </c:if>
        </c:if>

        <!-- 主编 -->
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_OVERVIEW'] or sessionScope.menuPermMap['MENU_EIC_DESK']
            or sessionScope.menuPermMap['MENU_EIC_TO_ASSIGN'] or sessionScope.menuPermMap['MENU_EIC_REVIEWERS']
            or sessionScope.menuPermMap['MENU_EIC_FINAL_DECISION'] or sessionScope.menuPermMap['MENU_EIC_SPECIAL']}">
            <div class="muted" style="padding: 10px 12px;">主编工作台</div>
            <c:if test="${sessionScope.menuPermMap['MENU_EIC_OVERVIEW']}">
                <a class="side-link ${fn:contains(uri, '/editor/overview') ? 'active' : ''}" href="${ctx}/editor/overview">
                    <i class="bi bi-eye" aria-hidden="true"></i> 系统全览
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EIC_DESK']}">
                <a class="side-link ${fn:contains(uri, '/editor/desk') ? 'active' : ''}" href="${ctx}/editor/desk">
                    <i class="bi bi-search" aria-hidden="true"></i> 案头审查
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EIC_TO_ASSIGN']}">
                <a class="side-link ${fn:contains(uri, '/editor/toAssign') ? 'active' : ''}" href="${ctx}/editor/toAssign">
                    <i class="bi bi-diagram-3" aria-hidden="true"></i> 待分配队列
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EIC_REVIEWERS']}">
                <a class="side-link ${fn:contains(uri, '/editor/reviewers') ? 'active' : ''}" href="${ctx}/editor/reviewers">
                    <i class="bi bi-people" aria-hidden="true"></i> 审稿人库
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EIC_FINAL_DECISION']}">
                <a class="side-link ${fn:contains(uri, '/editor/finalDecision') ? 'active' : ''}" href="${ctx}/editor/finalDecision">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i> 终审决策
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EIC_SPECIAL']}">
                <a class="side-link ${fn:contains(uri, '/editor/special') ? 'active' : ''}" href="${ctx}/editor/special">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i> 专项工作
                </a>
            </c:if>
        </c:if>

        <!-- 编辑部管理员 -->
        <c:if test="${sessionScope.menuPermMap['MENU_EO_FORMAL_CHECK'] or sessionScope.menuPermMap['MENU_EO_FORMAL_HISTORY']}">
            <div class="muted" style="padding: 10px 12px;">编辑部管理</div>
            <c:if test="${sessionScope.menuPermMap['MENU_EO_FORMAL_CHECK']}">
                <a class="side-link ${fn:contains(uri, '/editor/formalCheck') and not fn:contains(uri, '/history') ? 'active' : ''}" href="${ctx}/editor/formalCheck">
                    <i class="bi bi-clipboard-check" aria-hidden="true"></i> 形式审查
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['MENU_EO_FORMAL_HISTORY']}">
                <a class="side-link ${fn:contains(uri, '/editor/formalCheck/history') ? 'active' : ''}" href="${ctx}/editor/formalCheck/history">
                    <i class="bi bi-clock-history" aria-hidden="true"></i> 审查历史
                </a>
            </c:if>
        </c:if>

        <!-- 后台管理 -->
        <c:if test="${sessionScope.menuPermMap['ADMIN_USERS'] or sessionScope.menuPermMap['ADMIN_PERMISSIONS'] or sessionScope.menuPermMap['ADMIN_LOGS']
            or sessionScope.menuPermMap['ADMIN_SYSTEM'] or sessionScope.menuPermMap['ADMIN_DB_MAINTENANCE'] or sessionScope.menuPermMap['ADMIN_JOURNALS']
            or sessionScope.menuPermMap['ADMIN_EDITORIAL'] or sessionScope.menuPermMap['ADMIN_NEWS']}">
            <div class="muted" style="padding: 10px 12px;">后台管理</div>
            <c:if test="${sessionScope.menuPermMap['ADMIN_USERS']}">
                <a class="side-link ${fn:contains(uri, '/admin/users') ? 'active' : ''}" href="${ctx}/admin/users/list">
                    <i class="bi bi-people" aria-hidden="true"></i> 用户管理
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_PERMISSIONS']}">
                <a class="side-link ${fn:contains(uri, '/admin/permissions') ? 'active' : ''}" href="${ctx}/admin/permissions/list">
                    <i class="bi bi-shield-lock" aria-hidden="true"></i> 权限管理
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_SYSTEM']}">
                <a class="side-link ${fn:contains(uri, '/admin/system/status') ? 'active' : ''}" href="${ctx}/admin/system/status">
                    <i class="bi bi-activity" aria-hidden="true"></i> 系统状态
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_DB_MAINTENANCE']}">
                <a class="side-link ${fn:contains(uri, '/admin/system/db') ? 'active' : ''}" href="${ctx}/admin/system/db">
                    <i class="bi bi-database-gear" aria-hidden="true"></i> 数据库维护
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_LOGS']}">
                <a class="side-link ${fn:contains(uri, '/admin/logs') ? 'active' : ''}" href="${ctx}/admin/logs/list">
                    <i class="bi bi-journal-text" aria-hidden="true"></i> 系统日志
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_JOURNALS']}">
                <a class="side-link ${fn:contains(uri, '/admin/journals') ? 'active' : ''}" href="${ctx}/admin/journals/list">
                    <i class="bi bi-journals" aria-hidden="true"></i> 期刊管理
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_EDITORIAL']}">
                <a class="side-link ${fn:contains(uri, '/admin/editorial') ? 'active' : ''}" href="${ctx}/admin/editorial/list">
                    <i class="bi bi-diagram-2" aria-hidden="true"></i> 编委管理
                </a>
            </c:if>
            <c:if test="${sessionScope.menuPermMap['ADMIN_NEWS']}">
                <a class="side-link ${fn:contains(uri, '/admin/news') ? 'active' : ''}" href="${ctx}/admin/news/list">
                    <i class="bi bi-megaphone" aria-hidden="true"></i> 公告 / 新闻
                </a>
            </c:if>
        </c:if>

    </nav>
</aside>
