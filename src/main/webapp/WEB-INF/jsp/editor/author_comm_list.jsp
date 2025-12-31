<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>与作者沟通</h2>
<p style="color:#666;">在这里可以按稿件进入“发送消息给作者”，并查看该稿件的沟通历史时间线（支持站内消息/邮件，可抄送主编）。</p>

<c:if test="${empty manuscripts}">
    <p>暂无可沟通的稿件。</p>
</c:if>

<c:if test="${not empty manuscripts}">
    <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; width:100%; max-width: 1200px;">
        <thead>
        <tr>
            <th style="width:90px;">稿件ID</th>
            <th>标题</th>
            <th style="width:160px;">当前状态</th>
            <th style="width:130px;">沟通记录</th>
            <th style="width:160px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${manuscripts}" var="m">
            <tr>
                <td><c:out value="${m.manuscriptId}"/></td>
                <td>
                    <a href="${ctx}/manuscripts/detail?id=${m.manuscriptId}">
                        <c:out value="${m.title}"/>
                    </a>
                </td>
                <td><c:out value="${m.currentStatus}"/></td>
                <td>
                    <c:out value="${commCountMap[m.manuscriptId]}"/> 条
                </td>
                <td>
                    <a href="${ctx}/editor/author/message?manuscriptId=${m.manuscriptId}">发送消息</a>
                    <span style="margin:0 6px; color:#bbb;">|</span>
                    <a href="${ctx}/manuscripts/detail?id=${m.manuscriptId}#authorComm">查看时间线</a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>
