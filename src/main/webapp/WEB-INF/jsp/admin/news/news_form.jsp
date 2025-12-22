<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

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

<form action="${pageContext.request.contextPath}/admin/news/save" method="post" enctype="multipart/form-data">
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
            发布日期：
            <input type="date" name="publishDate"
                   value="${news.publishedAt != null ? fn:substring(news.publishedAt, 0, 10) : ''}"/>
            <span style="color: #666; font-size: 0.9em;">不填写则在勾选“已发布”时默认使用当前时间。</span>
        </label>
    </p>

    <p>
        <label>
            附件（如 PDF 征稿指南）：
            <input type="file" name="attachment" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip"/>
        </label>
        <c:if test="${not empty news.attachmentPath}">
            <br/>当前附件：
            <a href="${pageContext.request.contextPath}/news/attachment?id=${news.newsId}" target="_blank">下载附件</a>
        </c:if>
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