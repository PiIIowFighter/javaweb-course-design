<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">修改稿件并重新提交</h2>
</div></div>
    </div>

    
    <c:if test="${not empty manuscript and (manuscript.currentStatus == 'RETURNED' or manuscript.currentStatus == 'REVISION') and not empty formalCheckResult and not empty formalCheckResult.feedback}">
        <div class="alert" style="border-color: rgba(245, 158, 11, 0.55); background: rgba(245, 158, 11, 0.08);">
            <b>修改意见（请按此修改后重新提交）</b>
            <div style="margin-top: 6px; white-space: pre-wrap; line-height: 1.6;">
                <c:out value="${formalCheckResult.feedback}"/>
            </div>
        </div>
    </c:if>

    <c:if test="${not empty error}">
        <div class="alert alert-danger"><c:out value="${error}"/></div>
    </c:if>

    <c:if test="${not empty param.msg}">
        <div class="alert alert-success"><c:out value="${param.msg}"/></div>
    </c:if>

    <form id="resubmitEditForm" method="post" action="${ctx}/manuscripts/resubmit" enctype="multipart/form-data" class="stack-lg">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
        
        <input type="hidden" id="resubmitMode" name="mode" value="submit"/>

        
        <fieldset>
            <legend><b>1. 元数据</b></legend>

            <div class="form-row">
                <label>期刊</label>
                <c:set var="primaryJournal" value="${not empty journals ? journals[0] : null}"/>
                <c:set var="resolvedJournalId" value="${(not empty manuscript and not empty manuscript.journalId) ? manuscript.journalId : (not empty primaryJournal ? primaryJournal.journalId : '')}"/>
                <div>
                    <c:out value="${not empty journal ? journal.name : (not empty primaryJournal ? primaryJournal.name : '（未配置期刊）')}"/>
                    <input type="hidden" id="journalId" name="journalId" value="${resolvedJournalId}"/>
                </div>
            </div>


            <div class="form-row">
                <label for="title">标题</label>
                <input id="title" type="text" name="title"
                       value="${manuscript.title}"
                       placeholder="如：基于深度学习的图像识别算法优化"/>
            </div>

            <div class="form-row">
                <label>摘要</label>
                <div>
                    <div id="abstractEditor" style="min-height: 200px;"></div>
                    <input type="hidden" id="abstractHidden" name="abstract" />
                    <c:if test="${not empty manuscript and not empty manuscript.abstractText}">
                        <input type="hidden" id="savedAbstract" value="<c:out value="${manuscript.abstractText}" escapeXml="false"/>" />
                    </c:if>
                    <div class="help">支持富文本：可粘贴/输入格式化内容；支持粗体、斜体、上下标、数学公式等；提交时会保存为 HTML。</div>
                </div>
            </div>

            <div class="form-row">
                <label for="keywords">关键词</label>
                <input id="keywords" type="text" name="keywords"
                       value="${manuscript.keywords}"
                       placeholder="多个关键词用逗号分隔，如：深度学习,图像识别,CNN"/>
            </div>

            <div class="form-row">
                <label for="subjectArea">研究主题</label>
                <input id="subjectArea" type="text" name="subjectArea"
                       value="${manuscript.subjectArea}"
                       placeholder="如：计算机视觉 / 生物信息 / 数据挖掘 等"/>
            </div>

            <div class="form-row">
                <label>项目资助（可多条）</label>
                <div>
                    <div style="overflow-x:auto;">
                        <table id="fundingsTable">
                            <thead>
                            <tr>
                                <th>序号</th>
                                <th>名称 <span style="color: #be123c;">*</span></th>
                                <th>级别</th>
                                <th>资助金额</th>
                                <th>操作</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:choose>
                                <c:when test="${not empty fundings}">
                                    <c:forEach var="f" items="${fundings}" varStatus="st">
                                        <tr>
                                            <td><c:out value="${st.index + 1}"/></td>
                                            <td><input type="text" name="fundingName" value="${f.fundingName}" placeholder="如：国家自然科学基金"/></td>
                                            <td><input type="text" name="fundingLevel" value="${f.fundingLevel}" placeholder="如：国家级/省部级/校级"/></td>
                                            <td><input type="text" name="fundingAmount" value="${f.fundingAmount}" placeholder="如：100000"/></td>
                                            <td style="text-align:center;">
                                                <button type="button" class="btn-quiet" onclick="removeRow(this)">
                                                    <i class="bi bi-trash" aria-hidden="true"></i>
                                                </button>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td>1</td>
                                        <td><input type="text" name="fundingName" placeholder="如：国家自然科学基金"/></td>
                                        <td><input type="text" name="fundingLevel" placeholder="如：国家级/省部级/校级"/></td>
                                        <td><input type="text" name="fundingAmount" placeholder="如：100000"/></td>
                                        <td style="text-align:center;">
                                            <button type="button" class="btn-quiet" onclick="removeRow(this)">
                                                <i class="bi bi-trash" aria-hidden="true"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <div class="actions">
                        <button type="button" onclick="addFundingRow()">
                            <i class="bi bi-plus-lg" aria-hidden="true"></i>
                            添加资助
                        </button>
                    </div>
                    <div class="help">支持添加多条项目资助。若填写某一行任意字段，则“名称”为必填；金额可为空，填写时请使用数字（允许 1,000.00）。</div>
                </div>
            </div>
        </fieldset>

        
        <fieldset>
            <legend><b>2. 作者列表（支持多作者）</b></legend>
            <small>勾选“通讯作者”用于系统记录（默认第一作者）。</small>

            <div style="margin-top: var(--space-4); overflow-x: auto;">

            <table id="authorsTable">
                <thead>
                <tr>
                    <th>顺序</th>
                    <th>通讯作者</th>
                    <th>姓名</th>
                    <th>单位</th>
                    <th>学历</th>
                    <th>职称</th>
                    <th>职位</th>
                    <th>邮箱</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${not empty authors}">
                        <c:forEach var="a" items="${authors}" varStatus="st">
                            <tr>
                                <td><c:out value="${st.index + 1}"/></td>
                                <td style="text-align:center;">
                                    <input type="radio" name="correspondingIndex" value="${st.index}"
                                           <c:if test="${a.corresponding}">checked</c:if> />
                                </td>
                                <td><input type="text" name="authorName" value="${a.fullName}" placeholder="作者姓名"/></td>
                                <td><input type="text" name="authorAffiliation" value="${a.affiliation}" placeholder="单位/学院"/></td>
                                <td><input type="text" name="authorDegree" value="${a.degree}" placeholder="学历"/></td>
                                <td><input type="text" name="authorTitle" value="${a.title}" placeholder="职称"/></td>
                                <td><input type="text" name="authorPosition" value="${a.position}" placeholder="职位"/></td>
                                <td><input type="text" name="authorEmail" value="${a.email}" placeholder="邮箱"/></td>
                                <td style="text-align:center;">
                                    <button type="button" class="btn-quiet" onclick="removeRow(this)">
                                        <i class="bi bi-trash" aria-hidden="true"></i>
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td>1</td>
                            <td style="text-align:center;">
                                <input type="radio" name="correspondingIndex" value="0" checked/>
                            </td>
                            <td><input type="text" name="authorName" placeholder="作者姓名"/></td>
                            <td><input type="text" name="authorAffiliation" placeholder="单位/学院"/></td>
                            <td><input type="text" name="authorDegree" placeholder="学历"/></td>
                            <td><input type="text" name="authorTitle" placeholder="职称"/></td>
                            <td><input type="text" name="authorPosition" placeholder="职位"/></td>
                            <td><input type="text" name="authorEmail" placeholder="邮箱"/></td>
                            <td style="text-align:center;">
                                <button type="button" class="btn-quiet" onclick="removeRow(this)">
                                    <i class="bi bi-trash" aria-hidden="true"></i>
                                </button>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
            </div>

            <div class="actions">
                <button type="button" onclick="addAuthorRow()">
                    <i class="bi bi-plus-lg" aria-hidden="true"></i>
                    添加作者
                </button>
            </div>
        </fieldset>

        
        <fieldset>
            <legend><b>3. 文件上传</b></legend>

            <div class="form-row">
                <label>手稿文件 <span style="color: #be123c;">*</span></label>
                <div>
                    <input type="file" name="manuscriptFile" accept=".pdf"/>
                    <div class="help">请上传包含作者信息的完整稿件（仅支持 PDF 格式）</div>
                    <c:if test="${not empty currentVersion and not empty currentVersion.fileOriginalPath}">
                        <div class="help" style="margin-top: 4px;">
                            当前版本：
                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">
                                <i class="bi bi-file-pdf"></i> 预览/下载
                            </a>
                        </div>
                    </c:if>
                </div>
            </div>

            <div class="form-row">
                <label>匿名手稿 <span style="color: #be123c;">*</span></label>
                <div>
                    <input type="file" name="anonymousFile" accept=".pdf"/>
                    <div class="help">请上传去除作者信息的匿名稿件，用于盲审（仅支持 PDF 格式）</div>
                    <c:if test="${not empty currentVersion and not empty currentVersion.fileAnonymousPath}">
                        <div class="help" style="margin-top: 4px;">
                            当前版本：
                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=anonymous">
                                <i class="bi bi-file-pdf"></i> 预览/下载
                            </a>
                        </div>
                    </c:if>
                </div>
            </div>

            <div class="form-row">
                <label>Cover Letter</label>
                <div>
                    <div id="coverEditor" style="min-height: 200px;"></div>
                    <input type="hidden" id="coverHidden" name="coverLetterHtml" />
                    <div class="help">请输入投稿信内容，系统将自动转换为 PDF 格式保存。支持粗体、斜体、上下标等格式。</div>
                    <c:if test="${not empty currentVersion and not empty currentVersion.coverLetterPath}">
                        <div class="help" style="margin-top: 4px;">
                            当前版本：
                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">
                                <i class="bi bi-file-pdf"></i> 预览/下载
                            </a>
                        </div>
                    </c:if>

                    
                    <div style="margin-top: 10px;">
                        <div class="kicker" style="margin-bottom: 6px;">Cover Letter 附件（可选）</div>
                        <input type="file" name="coverAttachments" multiple/>
                        <div class="help">可上传多个附件，支持任意文件类型（审稿人不可见）。</div>

                        <c:if test="${not empty coverAttachments}">
                            <div class="help" style="margin-top: 6px;">
                                已上传附件：
                                <ul style="margin: 6px 0 0 18px;">
                                    <c:forEach var="f" items="${coverAttachments}">
                                        <li>
                                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=attachment&fileId=${f.fileId}">
                                                <i class="bi bi-paperclip"></i> <c:out value="${f.fileName}"/>
                                            </a>
                                            <c:if test="${not empty f.fileSize}">
                                                <span class="muted">（${f.fileSize} bytes）</span>
                                            </c:if>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </div>
                        </c:if>
                    </div>
                </div>
            </div>
        </fieldset>

        
        <fieldset>
            <legend><b>4. 推荐审稿人（可选）</b></legend>

            <table id="reviewersTable">
                <thead>
                <tr>
                    <th style="width:70px;">#</th>
                    <th style="width:220px;">姓名</th>
                    <th style="width:260px;">邮箱</th>
                    <th>推荐理由</th>
                    <th style="width:110px;">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${not empty recommendedReviewers}">
                        <c:forEach var="r" items="${recommendedReviewers}" varStatus="st">
                            <tr>
                                <td><c:out value="${st.index + 1}"/></td>
                                <td><input type="text" name="recReviewerName" value="${r.fullName}"/></td>
                                <td><input type="text" name="recReviewerEmail" value="${r.email}"/></td>
                                <td><input type="text" name="recReviewerReason" value="${r.reason}"/></td>
                                <td style="text-align:center;">
                                    <button type="button" class="btn-quiet" onclick="removeRow(this)">
                                        <i class="bi bi-trash" aria-hidden="true"></i> 删除
                                    </button>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td>1</td>
                            <td><input type="text" name="recReviewerName" placeholder="姓名"/></td>
                            <td><input type="text" name="recReviewerEmail" placeholder="邮箱"/></td>
                            <td><input type="text" name="recReviewerReason" placeholder="推荐理由"/></td>
                            <td style="text-align:center;">
                                <button type="button" class="btn-quiet" onclick="removeRow(this)">
                                    <i class="bi bi-trash" aria-hidden="true"></i> 删除
                                </button>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>

            <div class="actions">
                <button type="button" onclick="addReviewerRow()">
                    <i class="bi bi-plus-lg" aria-hidden="true"></i>
                    添加推荐审稿人
                </button>
            </div>
        </fieldset>

        
        <div class="actions">
            <button type="submit" class="btn-quiet" onclick="document.getElementById('resubmitMode').value='draft'; return confirm('保存为草稿？\n\n说明：\n- 不会推进流程状态（仍保持待修改）；\n- 可多次保存，稍后再重新提交。');">
                <i class="bi bi-save" aria-hidden="true"></i>
                存为草稿
            </button>

            <button class="btn-primary" type="submit" onclick="document.getElementById('resubmitMode').value='submit'; return confirm('确认重新提交（Resubmit）？提交后将进入后续处理流程。');">
                <i class="bi bi-send" aria-hidden="true"></i>
                重新提交（Resubmit）
            </button>
            <a class="btn-quiet" href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}" style="text-decoration:none;">
                返回详情
            </a>
            <a class="btn-quiet" href="${ctx}/manuscripts/list" style="text-decoration:none;">
                返回列表
            </a>
        </div>
    </form>
</div>


<link href="${ctx}/static/css/quill.snow.css" rel="stylesheet">
<script src="${ctx}/static/js/quill.min.js"></script>
<style>
    /* 自定义 Quill 编辑器样式 */
    #abstractEditor, #coverEditor {
        border: 1px solid var(--border);
        border-radius: var(--radius-sm);
        background: rgba(255, 255, 255, 0.85);
    }
    #abstractEditor .ql-container, #coverEditor .ql-container {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
        font-size: 14px;
        min-height: 200px;
    }
    #abstractEditor .ql-editor, #coverEditor .ql-editor {
        min-height: 200px;
    }
    /* 确保公式显示正确 */
    #abstractEditor .ql-formula, #coverEditor .ql-formula {
        display: inline-block;
        vertical-align: middle;
    }
    
    /* 作者列表表格优化 */
    #authorsTable {
        table-layout: fixed;
        width: 100%;
    }
    #authorsTable th:nth-child(1) { width: 50px; }   /* 顺序 */
    #authorsTable th:nth-child(2) { width: 70px; }   /* 通讯作者 */
    #authorsTable th:nth-child(3) { width: 150px; }  /* 姓名 */
    #authorsTable th:nth-child(4) { width: 250px; }  /* 单位 */
    #authorsTable th:nth-child(5) { width: 90px; }   /* 学历 */
    #authorsTable th:nth-child(6) { width: 90px; }   /* 职称 */
    #authorsTable th:nth-child(7) { width: 100px; }  /* 职位 */
    #authorsTable th:nth-child(8) { width: 180px; }  /* 邮箱 */
    #authorsTable th:nth-child(9) { width: 70px; }   /* 操作 */
    
    #authorsTable td input[type="text"] {
        width: 100%;
        min-width: 0;
        box-sizing: border-box;
    }
    
    /* 允许文本换行 */
    #authorsTable td {
        word-wrap: break-word;
        white-space: normal;
        vertical-align: middle;
    }
    
    /* 响应式：小屏幕时允许表格横向滚动 */
    @media (max-width: 1200px) {
        #authorsTable {
            table-layout: auto;
            min-width: 1000px;
        }
    }
</style>

<script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
<script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
<script>
    // 配置 MathJax
    window.MathJax = {
        tex: {
            inlineMath: [['$', '$'], ['\\(', '\\)']],
            displayMath: [['$$', '$$'], ['\\[', '\\]']]
        }
    };

    // 初始化摘要编辑器
    var abstractEditor = new Quill('#abstractEditor', {
        theme: 'snow',
        modules: {
            toolbar: [
                [{ 'header': [1, 2, 3, false] }],
                ['bold', 'italic', 'underline', 'strike'],
                [{ 'script': 'sub'}, { 'script': 'super'}],
                [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                [{ 'align': [] }],
                ['link', 'formula'],
                ['clean']
            ]
        },
        placeholder: '请输入摘要内容...'
    });

    // 初始化 Cover Letter 编辑器
    var coverEditor = new Quill('#coverEditor', {
        theme: 'snow',
        modules: {
            toolbar: [
                [{ 'header': [1, 2, 3, false] }],
                ['bold', 'italic', 'underline', 'strike'],
                [{ 'script': 'sub'}, { 'script': 'super'}],
                [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                [{ 'align': [] }],
                ['link', 'formula'],
                ['clean']
            ]
        },
        placeholder: '请输入 Cover Letter 内容...'
    });

    // 如果有已保存的内容，设置到编辑器中
    var savedAbstractEl = document.getElementById('savedAbstract');
    if (savedAbstractEl && savedAbstractEl.value) {
        abstractEditor.root.innerHTML = savedAbstractEl.value;
    }

    function removeRow(btn) {
        var tr = btn.closest('tr');
        if (!tr) return;
        var tbody = tr.parentNode;
        tbody.removeChild(tr);
        renumberTables();
    }

    function addAuthorRow() {
        var tbody = document.querySelector('#authorsTable tbody');
        var index = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML = '' +
            '<td>' + (index + 1) + '</td>' +
            '<td style="text-align:center;"><input type="radio" name="correspondingIndex" value="' + index + '"></td>' +
            '<td><input type="text" name="authorName" placeholder="作者姓名"></td>' +
            '<td><input type="text" name="authorAffiliation" placeholder="单位/学院"></td>' +
            '<td><input type="text" name="authorDegree" placeholder="学历"></td>' +
            '<td><input type="text" name="authorTitle" placeholder="职称"></td>' +
            '<td><input type="text" name="authorPosition" placeholder="职位"></td>' +
            '<td><input type="text" name="authorEmail" placeholder="邮箱"></td>' +
            '<td style="text-align:center;">' +
                '<button type="button" class="btn-quiet" onclick="removeRow(this)">' +
                    '<i class="bi bi-trash" aria-hidden="true"></i>' +
                '</button>' +
            '</td>';
        tbody.appendChild(tr);
    }

    
    function addFundingRow() {
        var tbody = document.querySelector('#fundingsTable tbody');
        var index = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML = '' +
            '<td>' + (index + 1) + '</td>' +
            '<td><input type="text" name="fundingName" placeholder="如：国家自然科学基金"></td>' +
            '<td><input type="text" name="fundingLevel" placeholder="如：国家级/省部级/校级"></td>' +
            '<td><input type="text" name="fundingAmount" placeholder="如：100000"></td>' +
            '<td style="text-align:center;">' +
                '<button type="button" class="btn-quiet" onclick="removeRow(this)">' +
                    '<i class="bi bi-trash" aria-hidden="true"></i>' +
                '</button>' +
            '</td>';
        tbody.appendChild(tr);
        renumberTables();
    }

function addReviewerRow() {
        var tbody = document.querySelector('#reviewersTable tbody');
        var index = tbody.querySelectorAll('tr').length;
        var tr = document.createElement('tr');
        tr.innerHTML = '' +
            '<td>' + (index + 1) + '</td>' +
            '<td><input type="text" name="recReviewerName" placeholder="姓名"></td>' +
            '<td><input type="text" name="recReviewerEmail" placeholder="邮箱"></td>' +
            '<td><input type="text" name="recReviewerReason" placeholder="推荐理由"></td>' +
            '<td style="text-align:center;">' +
                '<button type="button" class="btn-quiet" onclick="removeRow(this)">' +
                    '<i class="bi bi-trash" aria-hidden="true"></i> 删除' +
                '</button>' +
            '</td>';
        tbody.appendChild(tr);
    }

    function renumberTables() {
        var authorRows = document.querySelectorAll('#authorsTable tbody tr');
        authorRows.forEach(function(tr, idx) {
            tr.children[0].innerText = (idx + 1);
            var radio = tr.querySelector('input[type=radio][name=correspondingIndex]');
            if (radio) radio.value = idx;
        });

        var reviewerRows = document.querySelectorAll('#reviewersTable tbody tr');
        reviewerRows.forEach(function(tr, idx) {
            tr.children[0].innerText = (idx + 1);
        });

        var fundingRows = document.querySelectorAll('#fundingsTable tbody tr');
        fundingRows.forEach(function(tr, idx) {
            tr.children[0].innerText = (idx + 1);
        });

    }

    document.getElementById('resubmitEditForm').addEventListener('submit', function() {
        // 获取 Quill 编辑器的 HTML 内容并设置到隐藏字段
        var abstractHtml = abstractEditor.root.innerHTML;
        document.getElementById('abstractHidden').value = abstractHtml;

        var coverHtml = coverEditor.root.innerHTML;
        document.getElementById('coverHidden').value = coverHtml;
    });
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
