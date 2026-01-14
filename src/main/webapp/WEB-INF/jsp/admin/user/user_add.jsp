<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="page-head">
        <h2 class="page-title">新增用户</h2>
</div><form action="${pageContext.request.contextPath}/admin/users/add" method="post">
    <div>
        <label>用户名：
            <input type="text" name="username" required/>
        </label>
    </div>
    <div>
        <label>姓名：
            <input type="text" name="fullName"/>
        </label>
    </div>
    <div>
        <label>邮箱：
            <input type="email" name="email"/>
        </label>
    </div>
    <div>
        <label>初始密码：
            <input type="text" name="password" value="123456"/>
        </label>
    </div>
    <div>
        <label>角色：
            <select name="roleCode" required>
                <option value="">-- 请选择角色 --</option>
                <c:forEach var="r" items="${roles}">
                    
                    <c:choose>
                        <c:when test="${r == 'SUPER_ADMIN'}"></c:when>
                        <c:when test="${r == 'AUTHOR'}"></c:when>
                        <c:when test="${r == 'REVIEWER'}"></c:when>
                        <c:when test="${r == 'SYSTEM_ADMIN' && sessionScope.currentUser.roleCode != 'SUPER_ADMIN'}"></c:when>
                        <c:otherwise>
                            <option value="${r}">${r}</option>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </select>
        </label>
    </div>
    <div>
        <label>状态：
            <select name="status">
                <option value="ACTIVE">ACTIVE（正常）</option>
                <option value="DISABLED">DISABLED（禁用）</option>
                <option value="LOCKED">LOCKED（锁定）</option>
            </select>
        </label>
    </div>

    <div style="margin-top:12px;">
        <button type="submit">保存</button>
        <a href="${pageContext.request.contextPath}/admin/users/list">返回列表</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
