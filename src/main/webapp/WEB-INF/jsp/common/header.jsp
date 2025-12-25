<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title><c:out value="${pageTitle != null ? pageTitle : '科研论文在线投稿及管理系统'}"/></title>
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <!-- Icons (Bootstrap Icons)
         说明：部分环境可能无法访问外网 CDN。
         本系统大量页面使用 bi 图标类；若你的环境可访问外网，可保留该行。
         若不可访问外网，你可以改为本地引入（/static/ 下放置 bootstrap-icons）。 -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.13.1/font/bootstrap-icons.min.css"/>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/main.css"/>
</head>
<body class="${not empty sessionScope.currentUser ? 'authed' : ''}">
<header class="site-header">
    <div class="container header-inner">
        <a class="brand" href="${pageContext.request.contextPath}/" aria-label="返回首页">
            <span class="brand-mark" aria-hidden="true"></span>
            <span class="brand-name">Online Submission</span>
        </a>

        <div class="header-right" aria-label="顶部功能区">
            <button class="nav-toggle" type="button" aria-label="打开菜单" onclick="toggleNav()">
                <i class="bi bi-list" aria-hidden="true"></i>
            </button>

            <nav id="primaryNav" class="nav" aria-label="主导航">
            <a href="${pageContext.request.contextPath}/">
                <i class="bi bi-house" aria-hidden="true"></i>
                <span>首页</span>
            </a>

            <a href="${pageContext.request.contextPath}/publish">
                <i class="bi bi-journals" aria-hidden="true"></i>
                <span>论文发表</span>
            </a>

            <a href="${pageContext.request.contextPath}/issues?type=latest">
                <i class="bi bi-file-earmark-text" aria-hidden="true"></i>
                <span>文章与专刊</span>
            </a>

            <a href="${pageContext.request.contextPath}/guide">
                <i class="bi bi-book" aria-hidden="true"></i>
                <span>用户指南</span>
            </a>

            <a href="${pageContext.request.contextPath}/about/aims">
                <i class="bi bi-info-circle" aria-hidden="true"></i>
                <span>关于期刊</span>
            </a>

            <a href="${pageContext.request.contextPath}/news/list">
                <i class="bi bi-newspaper" aria-hidden="true"></i>
                <span>新闻</span>
            </a>

            <c:choose>
                <c:when test="${not empty sessionScope.currentUser}">
                    <c:set var="currentRoleCode"
                           value="${empty sessionScope.currentUser.roleCode ? 'AUTHOR' : sessionScope.currentUser.roleCode}"/>

                    <a href="${pageContext.request.contextPath}/dashboard">
                        <i class="bi bi-grid" aria-hidden="true"></i>
                        <span>工作台</span>
                    </a>

                    <c:if test="${currentRoleCode == 'AUTHOR'}">
                        <a href="${pageContext.request.contextPath}/manuscripts/submit">
                            <i class="bi bi-upload" aria-hidden="true"></i>
                            <span>提交论文</span>
                        </a>
                    </c:if>

                    <a href="${pageContext.request.contextPath}/profile">
                        <i class="bi bi-person" aria-hidden="true"></i>
                        <span>个人信息</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/auth/logout">
                        <i class="bi bi-box-arrow-right" aria-hidden="true"></i>
                        <span>退出（${sessionScope.currentUser.username}）</span>
                    </a>

                </c:when>

                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/auth/login">
                        <i class="bi bi-box-arrow-in-right" aria-hidden="true"></i>
                        <span>登录</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/auth/register">
                        <i class="bi bi-person-plus" aria-hidden="true"></i>
                        <span>注册</span>
                    </a>

                    <a href="${pageContext.request.contextPath}/auth/login">
                        <i class="bi bi-upload" aria-hidden="true"></i>
                        <span>提交论文</span>
                    </a>
                </c:otherwise>
            </c:choose>

            </nav>

            <!-- 通知铃铛：放在 nav 外，确保在小屏/菜单折叠时也能看到 -->
            <c:if test="${not empty sessionScope.currentUser}">
                <a class="header-notification" href="${pageContext.request.contextPath}/notifications" title="通知中心">
                    <span class="nav-bell" aria-hidden="true">🔔</span>
                    <c:if test="${not empty requestScope.unreadNotificationCount and requestScope.unreadNotificationCount > 0}">
                        <span class="nav-badge"><c:out value="${requestScope.unreadNotificationCount}"/></span>
                    </c:if>
                </a>
            </c:if>
        </div>
    </div>
</header>

<main class="site-main">
    <div class="container">
        <c:if test="${not empty sessionScope.currentUser}">
            <div class="layout">
                <jsp:include page="/WEB-INF/jsp/common/auth_sidebar.jsp"/>
                <section class="content">
                    <div class="content-inner">
        </c:if>
