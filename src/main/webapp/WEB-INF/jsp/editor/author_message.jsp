<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div style="display:flex; align-items:center; justify-content:space-between; max-width: 980px;">
    <h2 style="margin:0;">发送消息给作者</h2>
    <a class="btn" href="${ctx}/editor/authorComm">返回沟通列表</a>
</div>

<c:if test="${empty manuscript}">
    <p>未找到稿件。</p>
</c:if>

<c:if test="${not empty manuscript}">

    <!-- 顶部已提供“返回沟通列表”按钮，按需求移除“返回稿件详情”入口 -->

    <h3>稿件信息</h3>
    <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width: 980px; width:100%;">
        <tr>
            <th>标题</th>
            <td><c:out value="${manuscript.title}"/></td>
        </tr>
        <tr>
            <th>作者</th>
            <td>
                <c:choose>
                    <c:when test="${not empty manuscript.authorList}">
                        <c:out value="${manuscript.authorList}"/>
                    </c:when>
                    <c:when test="${not empty authorUser}">
                        <c:choose>
                            <c:when test="${not empty authorUser.fullName}">
                                <c:out value="${authorUser.fullName}"/>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${authorUser.username}"/>
                            </c:otherwise>
                        </c:choose>
                        <c:if test="${not empty authorUser.email}">
                            <span style="color:#666; margin-left:8px;">(<c:out value="${authorUser.email}"/>)</span>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        —
                    </c:otherwise>
                </c:choose>

                <c:if test="${not empty authorUser}">
                    <div style="margin-top:4px; color:#666; font-size:12px;">
                        投稿人：
                        <c:choose>
                            <c:when test="${not empty authorUser.fullName}"><c:out value="${authorUser.fullName}"/></c:when>
                            <c:otherwise><c:out value="${authorUser.username}"/></c:otherwise>
                        </c:choose>
                        <c:if test="${not empty authorUser.email}">（<c:out value="${authorUser.email}"/>）</c:if>
                    </div>
                </c:if>
            </td>
        </tr>
        <tr>
            <th>当前状态</th>
            <td><c:out value="${manuscript.currentStatus}"/></td>
        </tr>
    </table>

    <c:if test="${not empty authorMessageFlash}">
        <div style="margin-top:10px; padding:8px 10px; background:#f0fff4; border:1px solid #b7eb8f; color:#135200; max-width: 980px;">
            <c:out value="${authorMessageFlash}"/>
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

    <!-- 按需求：移除“沟通历史（时间线）”展示区域 -->

</c:if>
