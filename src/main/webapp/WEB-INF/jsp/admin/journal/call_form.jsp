<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<link href="${pageContext.request.contextPath}/static/css/quill.snow.css" rel="stylesheet"/>

<h2>
    <c:choose>
        <c:when test="${call != null && call.callId != null}">编辑征稿通知</c:when>
        <c:otherwise>新增征稿通知</c:otherwise>
    </c:choose>
</h2>

<p>
    期刊：<b><c:out value="${journal.name}"/></b>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="${pageContext.request.contextPath}/admin/journals/calls/list?journalId=${journal.journalId}">返回列表</a>
</p>

<form method="post" action="${pageContext.request.contextPath}/admin/journals/calls/save"
      enctype="multipart/form-data" style="max-width: 980px;" onsubmit="return syncCallEditor();">
    <input type="hidden" name="journalId" value="${journal.journalId}"/>
    <input type="hidden" name="callId" value="${call.callId}"/>

    <p>
        <label>标题：
            <input type="text" name="title" value="<c:out value='${call.title}'/>" style="width: 720px;" required/>
        </label>
    </p>

    <p>
        <label>开始日期：
            <input type="date" name="startDate" value="<c:out value='${call.startDate}'/>"/>
        </label>
        &nbsp;&nbsp;
        <label>截止日期：
            <input type="date" name="deadline" value="<c:out value='${call.deadline}'/>"/>
        </label>
        &nbsp;&nbsp;
        <label>结束日期：
            <input type="date" name="endDate" value="<c:out value='${call.endDate}'/>"/>
        </label>
    </p>

    <p>
        <label>
            发布：
            <input type="checkbox" name="published" <c:if test="${call.published}">checked</c:if> />
        </label>
    </p>

    <p>
        <label>
            封面图（可选）：
            <input type="file" name="coverImage" accept="image/*"/>
        </label>
        <c:if test="${not empty call.coverImagePath}">
            <br/>当前封面：
            <a href="${pageContext.request.contextPath}/journal/asset?type=call_cover&id=${call.callId}" target="_blank">查看</a>
        </c:if>
    </p>

    <p>
        <label>
            附件（可选，如 PDF）：
            <input type="file" name="attachment" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip"/>
        </label>
        <c:if test="${not empty call.attachmentPath}">
            <br/>当前附件：
            <a href="${pageContext.request.contextPath}/journal/asset?type=call_attachment&id=${call.callId}" target="_blank">下载</a>
        </c:if>
    </p>

    <p>内容（富文本，提交时保存为 HTML）：</p>
    <div style="max-width: 920px; margin-bottom: 12px;">
        <div id="callContentEditor" style="min-height: 240px;"></div>
        <input type="hidden" id="callContentHidden" name="content"/>
        <c:if test="${call != null && not empty call.content}">
            <textarea id="savedCallContent" style="display:none;"><c:out value="${call.content}" escapeXml="false"/></textarea>
        </c:if>
    </div>

    <p>
        <button type="submit">保存</button>
        &nbsp;&nbsp;
        <a href="${pageContext.request.contextPath}/admin/journals/calls/list?journalId=${journal.journalId}">取消</a>
    </p>
</form>

<script src="${pageContext.request.contextPath}/static/js/quill.min.js"></script>
<script>
    var callQuill = new Quill('#callContentEditor', {
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
        var saved = document.getElementById('savedCallContent');
        if (saved && saved.value) {
            callQuill.root.innerHTML = saved.value;
        }
    })();

    function syncCallEditor() {
        document.getElementById('callContentHidden').value = callQuill.root.innerHTML;
        return true;
    }
</script>

<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />
