<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>首页 - Online_SMsystem4SP</title>
</head>
<body style="font-family:Arial;">
  <jsp:include page="common/header.jsp"/>
  <div style="padding:16px;">
    <h2>欢迎进入系统</h2>
    <p>当前用户：<b>${sessionScope.LOGIN_USER.username}</b></p>
    <p>角色集合：<b>${roles}</b></p>

    <div style="background:#f7f7f7;border:1px solid #ddd;padding:12px;border-radius:6px;margin-top:10px;">
      <b>以下模块已完整可用：</b>
      <ul>
        <li>用户管理：/admin/users（增删/审核/禁用）</li>
        <li>登录日志：/admin/logs/login</li>
        <li>操作日志：/admin/logs/op（带 @OpLog 自动写入）</li>
        <li>登录拦截：未登录直访会跳回 /login</li>
        <li>权限拦截：@RequiresPerm 缺权限返回 403</li>
      </ul>
    </div>

    <div style="margin-top:16px;">
      <b>其他模块入口（当前是占位页）：</b>
      <ul>
        <li><a href="${pageContext.request.contextPath}/stub/author">作者工作台</a></li>
        <li><a href="${pageContext.request.contextPath}/stub/editor">编辑工作台</a></li>
        <li><a href="${pageContext.request.contextPath}/stub/reviewer">审稿人中心</a></li>
        <li><a href="${pageContext.request.contextPath}/stub/eic">主编决策台</a></li>
        <li><a href="${pageContext.request.contextPath}/stub/cms">期刊CMS</a></li>
      </ul>
    </div>
  </div>
  <jsp:include page="common/footer.jsp"/>
</body>
</html>
