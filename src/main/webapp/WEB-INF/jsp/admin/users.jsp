<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>用户管理</title>
</head>
<body style="font-family:Arial;">
<jsp:include page="../common/header.jsp"/>
<div style="padding:16px;">
  <h2>用户管理</h2>

  <form method="get" action="${pageContext.request.contextPath}/admin/users" style="margin:10px 0;">
    关键词：<input name="keyword" value="${keyword}" />
    状态：
    <select name="status">
      <option value="">全部</option>
      <option value="0" <c:if test="${status==0}">selected</c:if>>待审核</option>
      <option value="1" <c:if test="${status==1}">selected</c:if>>启用</option>
      <option value="2" <c:if test="${status==2}">selected</c:if>>禁用</option>
    </select>
    <button type="submit">查询</button>
  </form>

  <h3>新增用户</h3>
  <form method="post" action="${pageContext.request.contextPath}/admin/users/create" style="border:1px solid #ddd;padding:10px;border-radius:6px;">
    用户名：<input name="username" required />
    密码：<input name="password" required />
    角色：
    <select name="roleCode">
      <option value="ADMIN">ADMIN</option>
      <option value="EIC">EIC</option>
      <option value="EDITOR">EDITOR</option>
      <option value="REVIEWER">REVIEWER</option>
      <option value="AUTHOR">AUTHOR</option>
      <option value="CMS">CMS</option>
    </select>
    <button type="submit">创建</button>
    <div style="color:#666;font-size:12px;margin-top:6px;">提示：创建的密码会用 BCrypt 加密存入 sys_user.password_hash。</div>
  </form>

  <h3 style="margin-top:16px;">用户列表</h3>
  <table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
    <tr style="background:#f1f1f1;">
      <th>ID</th>
      <th>username</th>
      <th>real_name</th>
      <th>email</th>
      <th>status</th>
      <th>created</th>
      <th>操作</th>
    </tr>
    <c:forEach items="${users}" var="u">
      <tr>
        <td>${u.id}</td>
        <td>${u.username}</td>
        <td>${u.realName}</td>
        <td>${u.email}</td>
        <td>${u.status}</td>
        <td>${u.createdAt}</td>
        <td>
          <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/approve" style="display:inline;">
            <button type="submit">审核通过</button>
          </form>
          <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/disable" style="display:inline;">
            <button type="submit">禁用</button>
          </form>
          <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/delete" style="display:inline;" onclick="return confirm('确定删除？');">
            <button type="submit">删除</button>
          </form>
        </td>
      </tr>
    </c:forEach>
  </table>
</div>
<jsp:include page="../common/footer.jsp"/>
</body>
</html>
