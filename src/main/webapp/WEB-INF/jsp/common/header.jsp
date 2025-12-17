<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div style="padding:10px;border-bottom:1px solid #ddd;">
  <div style="display:flex;justify-content:space-between;align-items:center;">
    <div>
      <b>Online_SMsystem4SP</b>（科研论文在线投稿及管理系统）
    </div>
    <div>
      <c:if test="${not empty sessionScope.LOGIN_USER}">
        当前用户：<b>${sessionScope.LOGIN_USER.username}</b>
        <a style="margin-left:10px;" href="${pageContext.request.contextPath}/logout">退出</a>
      </c:if>
    </div>
  </div>
  <div style="margin-top:8px;">
    <a href="${pageContext.request.contextPath}/app/home">首页</a>
    <span style="margin:0 8px;color:#999;">|</span>

    <a href="${pageContext.request.contextPath}/admin/users">用户管理</a>
    <a style="margin-left:10px;" href="${pageContext.request.contextPath}/admin/logs/login">登录日志</a>
    <a style="margin-left:10px;" href="${pageContext.request.contextPath}/admin/logs/op">操作日志</a>

    <span style="margin:0 8px;color:#999;">|</span>
    <a href="${pageContext.request.contextPath}/stub/author">作者工作台</a>
    <a style="margin-left:10px;" href="${pageContext.request.contextPath}/stub/editor">编辑工作台</a>
    <a style="margin-left:10px;" href="${pageContext.request.contextPath}/stub/reviewer">审稿人中心</a>
    <a style="margin-left:10px;" href="${pageContext.request.contextPath}/stub/eic">主编决策台</a>
    <a style="margin-left:10px;" href="${pageContext.request.contextPath}/stub/cms">期刊CMS</a>
  </div>
</div>
