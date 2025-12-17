<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>编辑工作台</title>
</head>
<body style="font-family:Arial;">
  <jsp:include page="../common/header.jsp"/>
  <div style="padding:16px;">
    <h2>编辑工作台（占位页）</h2>
    <p>这里是给对应负责人接入真实模块的入口页。</p>
    <ul>
      <li>你可以保留路由不变，把页面/Controller/Service/Mapper 替换为真实实现。</li>
      <li>统一登录与权限拦截已在骨架中实现。</li>
      <li>统一数据库表结构请用你们导入的 SQL Server 脚本。</li>
    </ul>
  </div>
  <jsp:include page="../common/footer.jsp"/>
</body>
</html>
