<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<link href="https://cdn.jsdelivr.net/npm/quill@1.3.7/dist/quill.snow.css" rel="stylesheet"/>

<h2>
    <c:choose>
        <c:when test="${issue != null && issue.issueId != null}">编辑期次/专刊</c:when>
        <c:otherwise>新增期次/专刊</c:otherwise>
    </c:choose>
</h2>

<p>
    期刊：<b><c:out value="${journal.name}"/></b>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="${pageContext.request.contextPath}/admin/journals/issues/list?journalId=${journal.journalId}">返回列表</a>
</p>

<form method="post" action="${pageContext.request.contextPath}/admin/journals/issues/save"
      enctype="multipart/form-data" style="max-width: 980px;" onsubmit="return syncIssueEditor();">
    <input type="hidden" name="journalId" value="${journal.journalId}"/>
    <input type="hidden" name="issueId" value="${issue.issueId}"/>

    <p>
        <label>类型：
            <select name="issueType" style="width: 160px;">
                <option value="LATEST" <c:if test="${issue.issueType=='LATEST'}">selected</c:if>>LATEST（最新）</option>
                <option value="SPECIAL" <c:if test="${issue.issueType=='SPECIAL'}">selected</c:if>>SPECIAL（专刊）</option>
            </select>
        </label>
    </p>

    <p>
        <label>标题：
            <input type="text" name="title" value="<c:out value='${issue.title}'/>" style="width: 620px;" required/>
        </label>
    </p>

    <p>
        <label>卷：
            <input type="number" name="volume" value="<c:out value='${issue.volume}'/>" style="width: 120px;"/>
        </label>
        &nbsp;&nbsp;
        <label>期：
            <input type="number" name="number" value="<c:out value='${issue.number}'/>" style="width: 120px;"/>
        </label>
        &nbsp;&nbsp;
        <label>年：
            <input type="number" name="year" value="<c:out value='${issue.year}'/>" style="width: 120px;"/>
        </label>
    </p>

    <p>
        <label>Guest Editors（可选）：
            <input type="text" name="guestEditors" value="<c:out value='${issue.guestEditors}'/>" style="width: 620px;"/>
        </label>
    </p>

    <p>
        <label>
            发布：
            <input type="checkbox" name="published" <c:if test="${issue.published}">checked</c:if> />
        </label>
        &nbsp;&nbsp;
        <label>发布日期：
            <input type="date" name="publishDate" value="<c:out value='${issue.publishDate}'/>"/>
        </label>
        <span style="color:#666; font-size:0.9em;">（前台展示可仅显示年月日）</span>
    </p>

    <p>
        <label>
            封面图（可选）：
            <input type="file" name="coverImage" accept="image/*"/>
        </label>
        <c:if test="${not empty issue.coverImagePath}">
            <br/>当前封面：
            <a href="${pageContext.request.contextPath}/journal/asset?type=issue_cover&id=${issue.issueId}" target="_blank">查看</a>
        </c:if>
    </p>

    <p>
        <label>
            附件（可选，如 PDF）：
            <input type="file" name="attachment" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip"/>
        </label>
        <c:if test="${not empty issue.attachmentPath}">
            <br/>当前附件：
            <a href="${pageContext.request.contextPath}/journal/asset?type=issue_attachment&id=${issue.issueId}" target="_blank">下载</a>
        </c:if>
    </p>

    <p>简介 / 说明（富文本，提交时保存为 HTML）：</p>
    <div style="max-width: 920px; margin-bottom: 12px;">
        <div id="issueDescEditor" style="min-height: 240px;"></div>
        <input type="hidden" id="issueDescHidden" name="description"/>
        <c:if test="${issue != null && not empty issue.description}">
            <textarea id="savedIssueDesc" style="display:none;"><c:out value="${issue.description}" escapeXml="false"/></textarea>
        </c:if>
    </div>

    <p>
        <button type="submit">保存</button>
        &nbsp;&nbsp;
        <a href="${pageContext.request.contextPath}/admin/journals/issues/list?journalId=${journal.journalId}">取消</a>
    </p>
</form>

<script src="https://cdn.jsdelivr.net/npm/quill@1.3.7/dist/quill.min.js"></script>
<script>
    var issueQuill = new Quill('#issueDescEditor', {
        theme: 'snow',
        modules: {
            toolbar: [
                ['bold', 'italic', 'underline', 'strike'],
                [{'header': 1}, {'header': 2}],
                [{'list': 'ordered'}, {'list': 'bullet'}],
                [{'script': 'sub'}, {'script': 'super'}],
                [{'indent': '-1'}, {'indent': '+1'}],
                [{'direction': 'rtl'}],
                [{'size': ['small', false, 'large', 'huge']}],
                [{'color': []}, {'background': []}],
                [{'font': []}],
                [{'align': []}],
                ['link', 'image', 'video'],
                ['clean']
            ]
        }
    });

    (function initSaved() {
        var saved = document.getElementById('savedIssueDesc');
        if (saved && saved.value) {
            issueQuill.root.innerHTML = saved.value;
        }
    })();

    function syncIssueEditor() {
        document.getElementById('issueDescHidden').value = issueQuill.root.innerHTML;
        return true;
    }
</script>
