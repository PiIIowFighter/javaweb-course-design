<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title"><c:out value="${news.title}"/></h2>
</div></div>
        <div>
            <a class="btn" style="text-decoration:none;" href="${ctx}/news/list">
                <i class="bi bi-arrow-left"></i> 返回列表
            </a>
        </div>
    </div>

    <c:if test="${empty news}">
        <p>未找到新闻内容。</p>
    </c:if>
    <c:if test="${not empty news}">
        <div class="richtext">
            <div class="ql-snow richtext-view">
                <div class="ql-editor">
                    <c:out value="${news.content}" escapeXml="false"/>
                </div>
            </div>
        </div>
        <c:if test="${not empty news.attachmentPath}">
            <p style="margin-top: 12px;">
                附件：
                <a href="${ctx}/news/attachment?id=${news.newsId}" target="_blank">下载附件</a>
            </p>
        </c:if>
    </c:if>
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
