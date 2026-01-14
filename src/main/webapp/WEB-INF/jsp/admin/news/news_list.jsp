<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<div class="page-head">
        <h2 class="page-title">新闻 / 公告管理</h2>
</div>

<style>
    /* 仅本页使用：将起始/结束日期和筛选按钮强制放在同一行，并缩小日期框左右间距 */
    .news-filter-form { margin: 12px 0; }
    .news-filter-row {
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        gap: 10px;
        overflow-x: auto;
        padding-bottom: 2px;
    }
    .news-filter-row label {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        white-space: nowrap;
        margin: 0;
    }
    .news-filter-row input[type="text"] {
        width: 220px;
        max-width: 220px;
    }
    .news-filter-row input[type="date"] {
        width: 150px;
        max-width: 150px;
        padding-left: 8px;
        padding-right: 8px;
    }
    .news-filter-row input[type="submit"],
    .news-filter-row a {
        flex: 0 0 auto;
        white-space: nowrap;
    }
</style>

<form class="news-filter-form" method="get" action="${pageContext.request.contextPath}/admin/news/list">
    <div class="news-filter-row">
        <label>关键词：
            <input type="text" name="keyword" value="${param.keyword}"/>
        </label>
        <label>起始日期：
            <input type="date" name="fromDate" value="${param.fromDate}"/>
        </label>
        <label>结束日期：
            <input type="date" name="toDate" value="${param.toDate}"/>
        </label>
        <input type="submit" value="筛选"/>
        <a href="${pageContext.request.contextPath}/admin/news/list">清除条件</a>
    </div>
</form>

<p>
    <a href="${pageContext.request.contextPath}/admin/news/edit">+ 新增新闻 / 公告</a>
</p>

<c:if test="${empty newsList}">
    <p>当前还没有任何新闻或公告。</p>
</c:if>

<c:if test="${not empty newsList}">
    <table border="1" cellspacing="0" cellpadding="4">
        <tr>
            <th>ID</th>
            <th>标题</th>
            <th>附件</th>
            <th>发布状态</th>
            <th>发布时间</th>
            <th>操作</th>
        </tr>
        <c:forEach var="n" items="${newsList}">
            <tr>
                <td>${n.newsId}</td>
                <td>${n.title}</td>
                <td>
                    <c:if test="${not empty n.attachmentPath}">
                        <a href="${pageContext.request.contextPath}/news/attachment?id=${n.newsId}" target="_blank">查看附件</a>
                    </c:if>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${n.published}">已发布</c:when>
                        <c:otherwise>草稿</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${n.publishedAt != null}">
                            <c:out value="${fn:substring(n.publishedAt, 0, 10)}"/>
                        </c:when>
                        <c:otherwise>未设置</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/admin/news/edit?id=${n.newsId}">编辑</a>
                    |
                    <form action="${pageContext.request.contextPath}/admin/news/delete" method="post"
                          style="display:inline;"
                          onsubmit="return confirm('确定要删除这条新闻/公告吗？删除后无法恢复。');">
                        <input type="hidden" name="id" value="${n.newsId}"/>
                        <input type="submit" value="删除"/>
                    </form>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>

<p style="margin-top: 16px;">
    <button type="button" onclick="history.back()">返回上一页</button>
</p>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />

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
