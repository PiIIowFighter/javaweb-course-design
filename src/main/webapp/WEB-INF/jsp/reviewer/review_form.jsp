<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>提交评审意见</h2>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<form method="post" action="${ctx}/reviewer/submit">
    <input type="hidden" name="reviewId" value="${reviewId}"/>

    <p>
        <label>总体评分（可选，0-10）：<br/>
            <input type="number" step="0.1" min="0" max="10" name="score"/>
        </label>
    </p>

    <p>
        <label>评审意见（必填）：<br/>
            <textarea name="content" rows="8" cols="60" required></textarea>
        </label>
    </p>

    <p>
        <label>推荐结论（必选）：<br/>
            <select name="recommendation" required>
                <option value="ACCEPT">接受</option>
                <option value="MINOR_REVISION">小修后接受</option>
                <option value="MAJOR_REVISION">大修后再审</option>
                <option value="REJECT">拒稿</option>
            </select>
        </label>
    </p>

    <p>
        <button type="submit">提交评审</button>
    </p>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
