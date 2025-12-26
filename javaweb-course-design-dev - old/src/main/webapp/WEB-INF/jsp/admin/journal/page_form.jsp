<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<link href="https://cdn.jsdelivr.net/npm/quill@1.3.7/dist/quill.snow.css" rel="stylesheet"/>

<h2>
    <c:choose>
        <c:when test="${page != null && page.pageId != null}">编辑页面</c:when>
        <c:otherwise>新增页面</c:otherwise>
    </c:choose>
</h2>

<p>
    期刊：<b><c:out value="${journal.name}"/></b>
    &nbsp;&nbsp;|&nbsp;&nbsp;
    <a href="${pageContext.request.contextPath}/admin/journals/pages/list?journalId=${journal.journalId}">返回页面列表</a>
</p>

<form method="post" action="${pageContext.request.contextPath}/admin/journals/pages/save"
      enctype="multipart/form-data" style="max-width: 980px;" onsubmit="return syncPageEditor();">
    <input type="hidden" name="journalId" value="${journal.journalId}"/>
    <input type="hidden" name="pageId" value="${page.pageId}"/>

    <p>
        <label>页面 Key：
            <select name="pageKey" style="width: 220px;">
                <option value="aims" <c:if test="${page.pageKey=='aims'}">selected</c:if>>aims（办刊宗旨）</option>
                <option value="policies" <c:if test="${page.pageKey=='policies'}">selected</c:if>>policies（政策与流程）</option>
                <option value="about" <c:if test="${page.pageKey=='about'}">selected</c:if>>about（关于）</option>
                <option value="contact" <c:if test="${page.pageKey=='contact'}">selected</c:if>>contact（联系方式）</option>
                <option value="faq" <c:if test="${page.pageKey=='faq'}">selected</c:if>>faq</option>
                <option value="other" <c:if test="${page.pageKey=='other'}">selected</c:if>>other</option>
            </select>
        </label>
        <span style="color:#666; font-size:0.9em;">（前台目前主要使用 aims / policies）</span>
    </p>

    <p>
        <label>标题：
            <input type="text" name="title" value="<c:out value='${page.title}'/>" style="width: 520px;"/>
        </label>
    </p>

    <p>
        <label>
            封面图（可选）：
            <input type="file" name="coverImage" accept="image/*"/>
        </label>
        <c:if test="${not empty page.coverImagePath}">
            <br/>当前封面：
            <a href="${pageContext.request.contextPath}/journal/asset?type=page_cover&id=${page.pageId}" target="_blank">查看</a>
        </c:if>
    </p>

    <p>
        <label>
            附件（可选，如 PDF）：
            <input type="file" name="attachment" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip"/>
        </label>
        <c:if test="${not empty page.attachmentPath}">
            <br/>当前附件：
            <a href="${pageContext.request.contextPath}/journal/asset?type=page_attachment&id=${page.pageId}" target="_blank">下载</a>
        </c:if>
    </p>

    <p>内容（富文本，提交时保存为 HTML）：</p>
    <div style="max-width: 920px; margin-bottom: 12px;">
        <div id="pageContentEditor" style="min-height: 240px;"></div>
        <input type="hidden" id="pageContentHidden" name="content"/>
        <c:if test="${page != null && not empty page.content}">
            <textarea id="savedPageContent" style="display:none;"><c:out value="${page.content}" escapeXml="false"/></textarea>
        </c:if>
    </div>

    <p>
        <button type="submit">保存</button>
        &nbsp;&nbsp;
        <a href="${pageContext.request.contextPath}/admin/journals/pages/list?journalId=${journal.journalId}">取消</a>
    </p>
</form>

<script src="https://cdn.jsdelivr.net/npm/quill@1.3.7/dist/quill.min.js"></script>
<script>
    var pageQuill = new Quill('#pageContentEditor', {
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
        var saved = document.getElementById('savedPageContent');
        if (saved && saved.value) {
            pageQuill.root.innerHTML = saved.value;
        }
    })();

    function syncPageEditor() {
        document.getElementById('pageContentHidden').value = pageQuill.root.innerHTML;
        return true;
    }
</script>
