<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<h2>
    <c:choose>
        <c:when test="${journal != null && journal.journalId != null}">
            编辑期刊基本信息
        </c:when>
        <c:otherwise>
            新增期刊
        </c:otherwise>
    </c:choose>
</h2>

<form method="post" action="${pageContext.request.contextPath}/admin/journals/basic/save" style="max-width: 900px;">
    <input type="hidden" name="journalId" value="${journal.journalId}"/>

    <p>
        <label>期刊名称：
            <input type="text" name="name" value="<c:out value='${journal.name}'/>" style="width: 420px;" required/>
        </label>
    </p>

    <p>
        <label>ISSN：
            <input type="text" name="issn" value="<c:out value='${journal.issn}'/>" style="width: 220px;"/>
        </label>
        &nbsp;&nbsp;
        <label>影响因子：
            <input type="text" name="impactFactor" value="<c:out value='${journal.impactFactor}'/>" style="width: 120px;"/>
        </label>
    </p>

    <p>
        <label>时间线 / 发展历程（可选）：
            <input type="text" name="timeline" value="<c:out value='${journal.timeline}'/>" style="width: 520px;"/>
        </label>
    </p>

    <p>简介（可粘贴 HTML 或普通文本；建议配合“关于期刊页面”板块写更完整内容）：</p>
    <p>
        <textarea name="description" rows="6" style="width: 100%; max-width: 860px;"><c:out value="${journal.description}"/></textarea>
    </p>

    <p>
        <button type="submit">保存</button>
        &nbsp;&nbsp;
        <a href="${pageContext.request.contextPath}/admin/journals/list">返回列表</a>
    </p>
</form>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
