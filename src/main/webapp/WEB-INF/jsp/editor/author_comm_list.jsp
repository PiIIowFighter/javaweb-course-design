<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
        <h2 class="page-title">与作者沟通</h2>
</div><c:if test="${empty manuscripts}">
    <p>暂无可沟通的稿件。</p>
</c:if>

<c:if test="${not empty manuscripts}">
    <table border="1" cellpadding="6" cellspacing="0" style="background:#fff; width:100%; max-width: 1200px;">
        <thead>
        <tr>
            <th style="width:90px;">稿件ID</th>
            <th>标题</th>
            <th style="width:160px;">当前状态</th>
            <th style="width:130px;">沟通记录</th>
            <th style="width:260px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${manuscripts}" var="m">
            <tr>
                <td><c:out value="${m.manuscriptId}"/></td>
                <td><c:out value="${m.title}"/></td>
                <td><c:out value="${m.currentStatus}"/></td>
                <td>
                    <c:out value="${commCountMap[m.manuscriptId]}"/> 条
                </td>
                <td>
                    <a class="btn-primary" href="${ctx}/editor/author/message?manuscriptId=${m.manuscriptId}">
                        <i class="bi bi-chat-dots" aria-hidden="true"></i>
                        发送消息
                    </a>
                    <a class="btn" style="margin-left:10px;" href="${ctx}/editor/recommend/detail?manuscriptId=${m.manuscriptId}">
                        <i class="bi bi-file-earmark-text" aria-hidden="true"></i>
                        查看稿件详情
                    </a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
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
