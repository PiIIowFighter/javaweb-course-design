<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>操作日志</title>
</head>
<body style="font-family:Arial;">
<jsp:include page="../common/header.jsp"/>
<div style="padding:16px;">
  <h2>操作日志（最近 200 条）</h2>

  <form method="get" action="${pageContext.request.contextPath}/admin/logs/op" style="margin:10px 0;">
    username：<input name="username" value="${username}" />
    module：<input name="module" value="${module}" placeholder="SYS 等"/>
    <button type="submit">查询</button>
  </form>

  <table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
    <tr style="background:#f1f1f1;">
      <th>time</th><th>username</th><th>module</th><th>action</th><th>path</th><th>success</th><th>cost(ms)</th><th>error</th>
    </tr>
    <c:forEach items="${logs}" var="l">
      <tr>
        <td>${l.createdAt}</td>
        <td>${l.username}</td>
        <td>${l.module}</td>
        <td>${l.action}</td>
        <td>${l.path}</td>
        <td>${l.success}</td>
        <td>${l.costMs}</td>
        <td style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${l.errorMsg}</td>
      </tr>
    </c:forEach>
  </table>

  <div style="margin-top:10px;color:#666;font-size:12px;">
    说明：用户管理里的创建/删除/审核/禁用动作都带 @OpLog，会自动写入 sys_op_log。
  </div>
</div>
<jsp:include page="../common/footer.jsp"/>
</body>
</html>
