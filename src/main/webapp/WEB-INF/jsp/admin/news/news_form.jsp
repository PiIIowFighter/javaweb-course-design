<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<link href="https://cdn.jsdelivr.net/npm/quill@1.3.7/dist/quill.snow.css" rel="stylesheet"/>

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

<form id="newsForm" action="${pageContext.request.contextPath}/admin/news/save" method="post" enctype="multipart/form-data">
    <input type="hidden" name="id" value="${news.newsId}"/>

    <p>
        <label>标题：
            <input type="text" name="title" value="${news.title}" required="required"
                   style="width: 400px;"/>
        </label>
    </p>

    <p>内容：</p>
    <div style="max-width: 840px; margin-bottom: 12px;">
        <div id="newsContentEditor" style="min-height: 200px;"></div>
        <input type="hidden" id="newsContentHidden" name="content" />
        <c:if test="${news != null && not empty news.content}">
            <textarea id="savedNewsContent" style="display:none;"><c:out value="${news.content}" escapeXml="false"/></textarea>
        </c:if>
        <div class="help" style="color:#666; font-size:0.9em; margin-top:4px;">
            支持富文本：可粘贴/输入格式化内容；支持粗体、斜体、上下标、数学公式等；支持插入图片和视频（使用工具栏中的图片 / 视频按钮）；提交时会保存为 HTML。
        </div>
    </div>
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
            是否可见
        </label>
    </p>

    <p>
        <input type="submit" value="保存"/>
        <a href="${pageContext.request.contextPath}/admin/news/list">返回列表</a>
    </p>
</form>


<!-- Quill 富文本编辑器：用于新闻 / 公告内容 -->

<style>
    #newsContentEditor {
        border: 1px solid var(--border);
        border-radius: var(--radius-sm);
        background: rgba(255, 255, 255, 0.85);
    }
    #newsContentEditor .ql-container {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        font-size: 14px;
        min-height: 200px;
    }
    #newsContentEditor .ql-editor {
        min-height: 200px;
    }
</style>

<script src="https://cdn.jsdelivr.net/npm/quill@1.3.7/dist/quill.min.js"></script>
<script>
    (function() {
        var editorEl = document.getElementById('newsContentEditor');
        if (!editorEl) {
            return;
        }

        var newsEditor = new Quill('#newsContentEditor', {
            theme: 'snow',
            modules: {
                toolbar: [
                    [{ 'header': [1, 2, 3, false] }],
                    ['bold', 'italic', 'underline', 'strike'],
                    [{ 'script': 'sub'}, { 'script': 'super'}],
                    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                    [{ 'align': [] }],
                    ['link', 'image', 'video', 'formula'],
                    ['clean']
                ]
            },
            placeholder: '请输入新闻 / 公告内容...'
        });

        // 如果有已保存的内容，恢复到编辑器
        var savedEl = document.getElementById('savedNewsContent');
        if (savedEl && savedEl.value && savedEl.value.trim().length > 0) {
            newsEditor.root.innerHTML = savedEl.value;
        }

        // 提交前，将富文本 HTML 写入隐藏字段
        var form = document.getElementById('newsForm');
        if (form) {
            form.addEventListener('submit', function() {
                var hidden = document.getElementById('newsContentHidden');
                if (hidden) {
                    hidden.value = newsEditor.root.innerHTML;
                }
            });
        }
    })();
</script>


<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />