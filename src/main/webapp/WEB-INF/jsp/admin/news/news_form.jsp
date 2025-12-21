<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<h2>
    <c:choose>
        <c:when test="${news != null && news.newsId != null}">
            编辑新闻 / 公告
        </c:when>
        <c:otherwise>
            新增新闻 / 公告
        </c:otherwise>
    </c:choose>
</h2>

<p>通过此表单维护期刊公告、投稿须知等内容。</p>

<form action="${pageContext.request.contextPath}/admin/news/save" method="post">
    <input type="hidden" name="id" value="${news.newsId}"/>

    <p>
        <label>标题：
            <input type="text" name="title" value="${news.title}" required="required"
                   style="width: 400px;"/>
        </label>
    </p>

    <p>内容：</p>
    <p>
        <textarea name="content" rows="12" cols="80">${news.content}</textarea>
    </p>

    <p>
        <label>
            <input type="checkbox" name="published" value="true"
                <c:if test="${news.published}">checked="checked"</c:if> />
            已发布
        </label>
    </p>

    <p>
        <input type="submit" value="保存"/>
        <a href="${pageContext.request.contextPath}/admin/news/list">返回列表</a>
    </p>
</form>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
