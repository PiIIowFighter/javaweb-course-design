<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>登录日志</title>
</head>
<body style="font-family:Arial;">
<jsp:include page="../common/header.jsp"/>
<div style="padding:16px;">
  <h2>登录日志（最近 200 条）</h2>

  <form method="get" action="${pageContext.request.contextPath}/admin/logs/login" style="margin:10px 0;">
    用户名：<input name="username" value="${username}" />
    <button type="submit">查询</button>
  </form>

  <table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
    <tr style="background:#f1f1f1;">
      <th>time</th><th>username</th><th>success</th><th>ip</th><th>msg</th><th>ua</th>
    </tr>
    <c:forEach items="${logs}" var="l">
      <tr>
        <td>${l.loginTime}</td>
        <td>${l.username}</td>
        <td>${l.success}</td>
        <td>${l.ip}</td>
        <td>${l.msg}</td>
        <td style="max-width:380px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${l.ua}</td>
      </tr>
    </c:forEach>
  </table>
</div>
<jsp:include page="../common/footer.jsp"/>
</body>
</html>
