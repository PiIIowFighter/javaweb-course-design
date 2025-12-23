<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<h2>新闻 / 公告管理</h2>
<p>展示 dbo.News 表中的公告列表，并支持新增、编辑和删除操作。</p>


<form method="get" action="${pageContext.request.contextPath}/admin/news/list" style="margin: 12px 0;">
    <label>关键词：
        <input type="text" name="keyword" value="${param.keyword}" style="width: 220px;"/>
    </label>
    &nbsp;&nbsp;
    <label>起始日期：
        <input type="date" name="fromDate" value="${param.fromDate}"/>
    </label>
    &nbsp;&nbsp;
    <label>结束日期：
        <input type="date" name="toDate" value="${param.toDate}"/>
    </label>
    &nbsp;&nbsp;
    <input type="submit" value="筛选"/>
    <a href="${pageContext.request.contextPath}/admin/news/list">清除条件</a>
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
                        <c:when test="${n.published}">可见</c:when>
                        <c:otherwise>草稿</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${n.publishedAt != null}">
                            ${n.publishedAt}
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

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />