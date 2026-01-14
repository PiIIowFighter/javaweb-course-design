<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">期刊管理</h2>
</div></div>
    </div>

    <c:if test="${empty journal || journal.journalId == null}">
        <p class="card-subtitle">未找到期刊记录，请先创建期刊。</p>
    </c:if>

    <c:if test="${not empty journal && journal.journalId != null}">

        <h3 style="margin-top: 6px;">基本信息</h3>

        <form method="post" action="${ctx}/admin/journals/basic/save" style="max-width: 980px;">
            <input type="hidden" name="journalId" value="${journal.journalId}"/>

            <p>
                <label>期刊名称：
                    <input type="text" name="name" value="<c:out value='${journal.name}'/>" style="width: 520px;" required/>
                </label>
            </p>

            <p>
                <label>ISSN：
                    <input type="text" name="issn" value="<c:out value='${journal.issn}'/>" style="width: 220px;"/>
                </label>
                &nbsp;&nbsp;
                <label>影响因子：
                    <input type="text" name="impactFactor" value="<c:out value='${journal.impactFactor}'/>" style="width: 140px;"/>
                </label>
            </p>

            <p>
                <label>时间线 / 发展历程（可选）：
                    <input type="text" name="timeline" value="<c:out value='${journal.timeline}'/>" style="width: 620px;"/>
                </label>
            </p>

            <p>简介（支持普通文本或 HTML；如需更完整内容请到“关于期刊页面”板块维护）：</p>
            <p>
                <textarea name="description" rows="6" style="width: 100%; max-width: 980px;"><c:out value="${journal.description}"/></textarea>
            </p>

            <div class="actions" style="margin: 10px 0 6px;">
                <button class="btn-primary" type="submit" style="text-decoration:none;">
                    <i class="bi bi-save" aria-hidden="true"></i>
                    保存基本信息
                </button>
            </div>
        </form>

        <hr style="margin: 18px 0;"/>

        <h3>板块入口</h3>

        <div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px;">
            <a class="card" style="text-decoration:none;" href="${ctx}/admin/journals/pages/list?journalId=${journal.journalId}">
                <h3 style="margin:0;"><i class="bi bi-file-earmark-text" aria-hidden="true"></i> 关于期刊页面</h3>
                <p class="card-subtitle" style="margin:8px 0 0;">维护 Publish / Guide / Aims / Policies 四个页面的内容。</p>
            </a>

            <a class="card" style="text-decoration:none;" href="${ctx}/admin/journals/issues/list?journalId=${journal.journalId}">
                <h3 style="margin:0;"><i class="bi bi-collection" aria-hidden="true"></i> 卷期 / 专刊</h3>
                <p class="card-subtitle" style="margin:8px 0 0;">维护 Latest Issues / Special Issues 等卷期信息。</p>
            </a>

            <a class="card" style="text-decoration:none;" href="${ctx}/admin/journals/calls/list?journalId=${journal.journalId}">
                <h3 style="margin:0;"><i class="bi bi-megaphone" aria-hidden="true"></i> 征稿通知</h3>
                <p class="card-subtitle" style="margin:8px 0 0;">维护征稿通知列表与详情内容。</p>
            </a>
        </div>
    </c:if>

</div>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
