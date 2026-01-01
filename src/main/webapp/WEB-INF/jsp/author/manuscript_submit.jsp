<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <h2 class="card-title">投稿</h2>
            <p class="card-subtitle">你可以先保存草稿（DRAFT），稍后继续编辑；也可以直接最终提交（SUBMITTED）。</p>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger"><c:out value="${error}"/></div>
    </c:if>

    <form id="submitForm" method="post" action="${ctx}/manuscripts/submit" enctype="multipart/form-data" class="stack-lg">
        <c:if test="${not empty manuscript and not empty manuscript.manuscriptId}">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
        </c:if>

        <!-- ========== 1) 元数据 ========== -->
        <fieldset>
            <legend><b>1. 元数据</b></legend>

            <div class="form-row">
                <label>期刊</label>
                <c:set var="primaryJournal" value="${not empty journals ? journals[0] : null}"/>
                <c:set var="resolvedJournalId" value="${(not empty manuscript and not empty manuscript.journalId) ? manuscript.journalId : (not empty primaryJournal ? primaryJournal.journalId : '')}"/>
                <div>
                    <c:out value="${not empty primaryJournal ? primaryJournal.name : '（未配置期刊）'}"/>
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
                <label for="fundingInfo">项目资助情况</label>
                <textarea id="fundingInfo" name="fundingInfo" rows="3"
                          placeholder="如：国家自然科学基金(编号xxxx)、校级大创项目等"><c:out value="${manuscript.fundingInfo}"/></textarea>
            </div>
        </fieldset>

        <!-- ========== 2) 作者列表 ========== -->
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

        <!-- ========== 3) 文件上传 ========== -->
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
                </div>
            </div>
        </fieldset>

        <!-- ========== 4) 推荐审稿人 ========== -->
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

        <!-- ========== 5) 操作按钮 ========== -->
        <div class="actions">
            <button type="submit" name="action" value="saveDraft">
                <i class="bi bi-cloud-arrow-down" aria-hidden="true"></i>
                保存草稿
            </button>
            <button class="btn-primary" type="submit" name="action" value="submit"
                    onclick="return confirm('确认最终提交？提交后将进入处理流程。');">
                <i class="bi bi-send" aria-hidden="true"></i>
                最终提交
            </button>
            <a class="btn-quiet" href="${ctx}/manuscripts/list" style="text-decoration:none;">
                返回列表
            </a>
        </div>
    </form>
</div>

<!-- Quill 富文本编辑器（本地引用） -->
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
<!-- MathJax 用于数学公式渲染 -->
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
    }

    document.getElementById('submitForm').addEventListener('submit', function() {
        // 获取 Quill 编辑器的 HTML 内容并设置到隐藏字段
        var abstractHtml = abstractEditor.root.innerHTML;
        document.getElementById('abstractHidden').value = abstractHtml;

        var coverHtml = coverEditor.root.innerHTML;
        document.getElementById('coverHidden').value = coverHtml;
    });
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
