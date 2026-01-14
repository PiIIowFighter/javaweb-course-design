<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card" style="max-width: 560px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">修改密码</h2>
</div></div>
    </div>

    <div class="stack">
        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-success"><c:out value="${message}"/></div>
        </c:if>

        <form action="${ctx}/profile/changePassword" method="post" class="stack">
            <div class="form-row">
                <label for="oldPassword">旧密码</label>
                <input id="oldPassword" type="password" name="oldPassword" required placeholder="请输入旧密码"/>
            </div>

            <div class="form-row">
                <label for="newPassword">新密码</label>
                <input id="newPassword" type="password" name="newPassword" required placeholder="至少 8 位"/>
            </div>

            <div class="form-row">
                <label for="confirmNewPassword">再次输入新密码</label>
                <input id="confirmNewPassword" type="password" name="confirmNewPassword" required placeholder="请再次输入新密码"/>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i>
                    提交修改
                </button>
                <a class="btn-quiet" href="${ctx}/profile" style="text-decoration:none;">返回个人信息</a>
            </div>
        </form>
    </div>
</div>

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
