<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<h2>关于期刊页面管理</h2>

<p>
    期刊：<b><c:out value="${journal.name}"/></b>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="${pageContext.request.contextPath}/admin/journals/list">返回期刊列表</a>
</p>

<p style="margin: 12px 0;">
    <a href="${pageContext.request.contextPath}/admin/journals/pages/edit?journalId=${journal.journalId}">➕ 新增页面</a>
</p>

<c:if test="${empty pages}">
    <p>该期刊暂未配置页面内容。你可以点击“新增页面”。</p>
</c:if>

<c:if test="${not empty pages}">
    <table border="1" cellpadding="6" cellspacing="0" style="background:#fff; width:100%; max-width: 1100px;">
        <thead>
        <tr>
            <th style="width:80px;">PageId</th>
            <th style="width:160px;">Key</th>
            <th>标题</th>
            <th style="width:160px;">更新时间</th>
            <th style="width:160px;">资源</th>
            <th style="width:200px;">操作</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${pages}">
            <tr>
                <td>${p.pageId}</td>
                <td><c:out value="${p.pageKey}"/></td>
                <td><c:out value="${p.title}"/></td>
                <td><c:out value="${p.updatedAt}"/></td>
                <td>
                    <c:if test="${not empty p.coverImagePath}">
                        <a href="${pageContext.request.contextPath}/journal/asset?type=page_cover&id=${p.pageId}" target="_blank">封面</a>
                    </c:if>
                    <c:if test="${not empty p.attachmentPath}">
                        <c:if test="${not empty p.coverImagePath}">&nbsp;|&nbsp;</c:if>
                        <a href="${pageContext.request.contextPath}/journal/asset?type=page_attachment&id=${p.pageId}" target="_blank">附件</a>
                    </c:if>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/admin/journals/pages/edit?journalId=${journal.journalId}&id=${p.pageId}">编辑</a>
                    &nbsp;|&nbsp;
                    <form method="post" action="${pageContext.request.contextPath}/admin/journals/pages/delete"
                          onsubmit="return confirm('确定删除该页面？');" style="display:inline;">
                        <input type="hidden" name="journalId" value="${journal.journalId}"/>
                        <input type="hidden" name="id" value="${p.pageId}"/>
                        <button type="submit">删除</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
