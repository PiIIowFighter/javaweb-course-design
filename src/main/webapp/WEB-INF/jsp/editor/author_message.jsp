<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>发送消息给作者</h2>

<c:if test="${empty manuscript}">
    <p>未找到稿件。</p>
</c:if>

<c:if test="${not empty manuscript}">

    <p>
        <a href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}#authorComm">返回稿件详情</a>
        <span style="margin:0 8px; color:#bbb;">|</span>
        <a href="${ctx}/editor/authorComm">返回沟通列表</a>
    </p>

    <h3>稿件信息</h3>
    <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width: 980px; width:100%;">
        <tr>
            <th style="width:120px;">稿件ID</th>
            <td><c:out value="${manuscript.manuscriptId}"/></td>
        </tr>
        <tr>
            <th>标题</th>
            <td><c:out value="${manuscript.title}"/></td>
        </tr>
        <tr>
            <th>作者</th>
            <td>
                <c:out value="${author.fullName}"/>
                <c:if test="${empty author.fullName}">
                    <c:out value="${author.username}"/>
                </c:if>
                <span style="color:#666; margin-left:8px;">(<c:out value="${author.email}"/>)</span>
            </td>
        </tr>
        <tr>
            <th>当前状态</th>
            <td><c:out value="${manuscript.currentStatus}"/></td>
        </tr>
    </table>

    <c:if test="${not empty flash}">
        <div style="margin-top:10px; padding:8px 10px; background:#f0fff4; border:1px solid #b7eb8f; color:#135200; max-width: 980px;">
            <c:out value="${flash}"/>
        </div>
    </c:if>

    <h3 style="margin-top:16px;">发送新消息</h3>
    <form method="post" action="${ctx}/editor/author/message" style="background:#fff; border:1px solid #e5e7eb; padding:12px; max-width: 980px;">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

        <div style="margin-bottom:8px;">
            <label>标题：</label>
            <input type="text" name="title" style="width:70%;" placeholder="例如：请澄清第 5 节实验设置"/>
        </div>

        <div style="margin-bottom:8px;">
            <label>内容：</label><br/>
            <textarea name="content" rows="6" style="width:100%;" placeholder="输入要发送给作者的内容..." required></textarea>
        </div>

        <div style="margin-bottom:8px;">
            <label>发送方式：</label>
            <label style="margin-left:8px;"><input type="checkbox" name="sendSystem" value="1" checked/> 站内消息</label>
            <label style="margin-left:8px;"><input type="checkbox" name="sendEmail" value="1"/> 邮件</label>
            <label style="margin-left:8px;"><input type="checkbox" name="ccChief" value="1"/> 抄送主编</label>
            <span style="color:#666; margin-left:8px;">（若同时勾选站内消息与邮件，将同时发送）</span>
        </div>

        <button type="submit">发送</button>
    </form>

    <h3 style="margin-top:16px;">沟通历史（时间线）</h3>

    <c:if test="${empty messages}">
        <p>暂无沟通记录。</p>
    </c:if>

    <c:if test="${not empty messages}">
        <div style="border-left:3px solid #ddd; padding-left:12px; margin: 8px 0 16px 0; max-width: 980px;">
            <c:forEach items="${messages}" var="msg">
                <div style="margin: 10px 0;">
                    <div style="color:#666; font-size:12px;">
                        <c:out value="${msg.createdAt}"/>
                        <span style="margin:0 6px;">·</span>
                        <strong>
                            <c:out value="${userMap[msg.createdByUserId].fullName}"/>
                        </strong>
                        <span style="margin:0 6px;">→</span>
                        <strong>
                            <c:out value="${userMap[msg.recipientUserId].fullName}"/>
                        </strong>
                        <span style="margin-left:8px;">[<c:out value="${msg.category}"/>/<c:out value="${msg.type}"/>]</span>
                    </div>
                    <div style="margin-top:4px;">
                        <div><strong><c:out value="${msg.title}"/></strong></div>
                        <div style="white-space:pre-wrap;"><c:out value="${msg.content}"/></div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:if>

</c:if>
