<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ include file="/WEB-INF/jsp/common/header.jsp"%>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>审稿人库管理</h2>
<p>主编在此维护期刊审稿人库：<b>邀请</b>、<b>审核资格</b>、<b>移除/禁用</b>、<b>重新启用</b>。</p>

<c:if test="${not empty msg}">
    <div style="padding:10px;border:1px solid #b6e3b6;background:#eef9ee;margin:10px 0;">
        <c:out value="${msg}"/>
    </div>
</c:if>
<c:if test="${not empty error}">
    <div style="padding:10px;border:1px solid #f1b3b3;background:#fff0f0;margin:10px 0;">
        <c:out value="${error}"/>
    </div>
</c:if>

<hr/>

<h3>邀请审稿人（创建待审核账号）</h3>
<form method="post" action="${ctx}/editor/reviewers">
    <input type="hidden" name="op" value="invite"/>

    <table cellpadding="6" cellspacing="0">
        <tr>
            <td style="width:140px;">用户名（登录名） *</td>
            <td><input type="text" name="username" required style="width:260px;"/></td>
        </tr>
        <tr>
            <td>初始密码 *</td>
            <td><input type="password" name="password" required style="width:260px;"/></td>
        </tr>
        <tr>
            <td>姓名</td>
            <td><input type="text" name="fullName" style="width:260px;"/></td>
        </tr>
        <tr>
            <td>邮箱</td>
            <td><input type="email" name="email" style="width:260px;"/></td>
        </tr>
        <tr>
            <td>单位 / 机构</td>
            <td><input type="text" name="affiliation" style="width:260px;"/></td>
        </tr>
        <tr>
            <td>研究方向</td>
            <td><input type="text" name="researchArea" style="width:260px;"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">发送邀请（生成待审核审稿人）</button>
            </td>
        </tr>
    </table>
</form>

<hr/>

<h3>审稿人列表</h3>
<c:if test="${empty reviewers}">
    <p>当前还没有任何审稿人账号。</p>
</c:if>

<c:if test="${not empty reviewers}">
    <table border="1" cellpadding="6" cellspacing="0" style="width:100%;border-collapse:collapse;">
        <thead>
        <tr style="background:#f6f6f6;">
            <th style="width:70px;">ID</th>
            <th style="width:140px;">用户名</th>
            <th style="width:140px;">姓名</th>
            <th style="width:220px;">邮箱</th>
            <th>单位 / 机构</th>
            <th style="width:180px;">研究方向</th>
            <th style="width:120px;">状态</th>
            <th style="width:260px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${reviewers}" var="r">
            <tr>
                <td><c:out value="${r.userId}"/></td>
                <td><c:out value="${r.username}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${empty r.fullName}">-</c:when>
                        <c:otherwise><c:out value="${r.fullName}"/></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${empty r.email}">-</c:when>
                        <c:otherwise><c:out value="${r.email}"/></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${empty r.affiliation}">-</c:when>
                        <c:otherwise><c:out value="${r.affiliation}"/></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${empty r.researchArea}">-</c:when>
                        <c:otherwise><c:out value="${r.researchArea}"/></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${r.status eq 'PENDING'}">待审核</c:when>
                        <c:when test="${r.status eq 'ACTIVE'}">已启用</c:when>
                        <c:when test="${r.status eq 'DISABLED'}">已禁用</c:when>
                        <c:otherwise><c:out value="${r.status}"/></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:if test="${r.status eq 'PENDING'}">
                        <form method="post" action="${ctx}/editor/reviewers" style="display:inline;">
                            <input type="hidden" name="userId" value="${r.userId}"/>
                            <button type="submit" name="op" value="approve">审核通过</button>
                        </form>
                    </c:if>

                    <c:if test="${r.status ne 'DISABLED'}">
                        <form method="post" action="${ctx}/editor/reviewers" style="display:inline;margin-left:6px;">
                            <input type="hidden" name="userId" value="${r.userId}"/>
                            <button type="submit" name="op" value="disable"
                                    onclick="return confirm('确认禁用该审稿人账号？');">移除/禁用</button>
                        </form>
                    </c:if>

                    <c:if test="${r.status eq 'DISABLED'}">
                        <form method="post" action="${ctx}/editor/reviewers" style="display:inline;">
                            <input type="hidden" name="userId" value="${r.userId}"/>
                            <button type="submit" name="op" value="enable">重新启用</button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<hr/>
<p><a href="${ctx}/editor/index">返回主编工作台</a></p>

<%@ include file="/WEB-INF/jsp/common/footer.jsp"%>
