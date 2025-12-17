<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>登录 - Online_SMsystem4SP</title>
</head>
<body style="font-family:Arial;">
  <div style="width:420px;margin:80px auto;border:1px solid #ddd;padding:20px;border-radius:6px;">
    <h2 style="margin-top:0;">系统登录</h2>

    <c:if test="${not empty requestScope.error}">
      <div style="background:#ffecec;border:1px solid #f5aca6;padding:8px;margin:10px 0;">
        ${requestScope.error}
      </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/auth/login">
      <div style="margin:10px 0;">
        <label>用户名：</label><br/>
        <input name="username" style="width:100%;height:32px;" placeholder="admin / eic1 / editor1 / reviewer1 / author1 / cms1"/>
      </div>
      <div style="margin:10px 0;">
        <label>密码：</label><br/>
        <input type="password" name="password" style="width:100%;height:32px;" placeholder="统一密码：123456"/>
      </div>
      <div style="margin-top:16px;">
        <button type="submit" style="width:100%;height:36px;">登录</button>
      </div>
      <div style="margin-top:10px;color:#666;font-size:12px;">
        说明：账号来自journal_system，默认密码为 123456。
      </div>
    </form>
  </div>
</body>
</html>
