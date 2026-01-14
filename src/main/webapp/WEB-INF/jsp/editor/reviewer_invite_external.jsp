<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
        <h2 class="page-title">邀请外部审稿人（创建账号并邮件邀请）</h2>
</div><div class="stack" style="flex-direction: row; gap: 10px; margin-top: var(--space-4);">
    <c:if test="${not empty backToUrl}">
        <a class="btn btn-quiet" href="${backToUrl}">
            <i class="bi bi-arrow-left" aria-hidden="true"></i>
            返回
        </a>
    </c:if>
    <c:if test="${not empty selectUrl}">
        <a class="btn btn-quiet" href="${selectUrl}">
            <i class="bi bi-people" aria-hidden="true"></i>
            返回审稿人库选择
        </a>
    </c:if>
</div>

<c:if test="${empty manuscript}">
    <p style="margin-top: var(--space-5);">未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">
    <div class="card" style="margin-top: var(--space-5);">
        <div class="stack" style="gap: 8px;">
            <div><strong>稿件编号：</strong><c:out value="${manuscript.manuscriptId}"/></div>
            <div><strong>稿件标题：</strong><c:out value="${manuscript.title}"/></div>
            <div><strong>当前状态：</strong><c:out value="${manuscript.currentStatus}"/></div>
        </div>
    </div>

    <div class="card" style="margin-top: var(--space-5);">
        <form method="post" action="${ctx}/editor/review/inviteExternal">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
            <c:if test="${not empty backToUrl}">
                <input type="hidden" name="backTo" value="${backToUrl}"/>
            </c:if>

            <table cellpadding="10" cellspacing="0" style="width:100%;">
                <tr>
                    <td style="width:180px;"><b>用户名 *</b></td>
                    <td>
                        <input class="input" type="text" name="username" required="required" placeholder="例如 reviewer_zhang" style="max-width: 360px; width: 100%;"/>
                    </td>
                </tr>
                <tr>
                    <td><b>初始密码 *</b></td>
                    <td>
                        <input class="input" type="text" name="password" required="required" placeholder="请设置一个初始密码" style="max-width: 360px; width: 100%;"/>
                    </td>
                </tr>
                <tr>
                    <td><b>姓名</b></td>
                    <td>
                        <input class="input" type="text" name="fullName" placeholder="可选" style="max-width: 360px; width: 100%;"/>
                    </td>
                </tr>
                <tr>
                    <td><b>邮箱 *</b></td>
                    <td>
                        <input class="input" type="email" name="email" required="required" placeholder="example@university.edu" style="max-width: 460px; width: 100%;"/>
                    </td>
                </tr>
                <tr>
                    <td><b>单位/机构</b></td>
                    <td>
                        <input class="input" type="text" name="affiliation" placeholder="可选" style="max-width: 460px; width: 100%;"/>
                    </td>
                </tr>
                <tr>
                    <td><b>研究方向</b></td>
                    <td>
                        <input class="input" type="text" name="researchArea" placeholder="可选" style="max-width: 460px; width: 100%;"/>
                    </td>
                </tr>
                <tr>
                    <td><b>截止日期（可选）</b></td>
                    <td>
                        <input class="input" type="date" name="dueDate" style="max-width: 220px;"/>
                    </td>
                </tr>
            </table>

            <div class="stack" style="flex-direction: row; justify-content:flex-end; gap: 10px; margin-top: var(--space-4);">
                <button class="btn btn-primary" type="submit">
                    <i class="bi bi-send" aria-hidden="true"></i>
                    邀请外部审稿人
                </button>
            </div>
        </form>
    </div>
</c:if>

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
