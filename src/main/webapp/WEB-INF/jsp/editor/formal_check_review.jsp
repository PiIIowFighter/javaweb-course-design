<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
        <h2 class="page-title">形式审查</h2>
    </div><c:if test="${empty manuscript}">
    <p>未找到稿件信息。</p>
</c:if>

<c:if test="${not empty manuscript}">
    <div class="card" style="margin-bottom: var(--space-5);">
        <div class="card-header">
            <div>
                <h3 class="card-title">稿件信息</h3>
                <p class="card-subtitle">当前状态：<c:out value="${manuscript.currentStatus}"/></p>
            </div>
            <div class="toolbar" style="gap:10px;">
                <a class="btn btn-quiet" href="${ctx}/editor/formalCheck">返回列表</a>
                <a class="btn btn-quiet" href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}">查看稿件详情</a>
            </div>
        </div>
        <div class="card-content">
            <p style="margin:0;"><strong>标题：</strong><c:out value="${manuscript.title}"/></p>
            <c:if test="${not empty manuscript.submitTime}">
                <p style="margin:6px 0 0 0;"><strong>提交时间：</strong><c:out value="${manuscript.submitTime}"/></p>
            </c:if>
        </div>
    </div>

    <div id="msgBox" class="card" style="display:none; padding:12px; margin-bottom: var(--space-4);"></div>

    <div class="card" style="margin-bottom: var(--space-5);">
        <div class="card-header">
            <div>
                <h3 class="card-title">自动检查与查重</h3>
                <p class="card-subtitle">可先进行系统自动检查/查重，再补充人工项并提交结果。</p>
            </div>
            <div class="toolbar" style="gap:10px;">
                <button class="btn btn-primary" type="button" id="btnAutoCheck">执行自动检查</button>
                <button class="btn btn-primary" type="button" id="btnPlagiarism">执行查重</button>
            </div>
        </div>
    </div>

    <form id="formalCheckForm" class="card" style="padding: var(--space-4);">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

        <h3 style="margin-top:0;">检查项</h3>
        <table border="1" cellpadding="6" cellspacing="0" style="width:100%;">
            <thead>
            <tr>
                <th style="width: 260px;">检查项</th>
                <th style="width: 180px;">结果</th>
                <th>说明</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>作者信息是否规范（自动）</td>
                <td>
                    <select name="authorInfoValid" id="authorInfoValid">
                        <option value="" ${empty formalCheckResult.authorInfoValid ? 'selected' : ''}>未检查</option>
                        <option value="true" ${formalCheckResult.authorInfoValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.authorInfoValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
                <td class="muted">作者姓名、单位、邮箱等信息是否齐全</td>
            </tr>
            <tr>
                <td>摘要字数是否符合（自动）</td>
                <td>
                    <select name="abstractWordCountValid" id="abstractWordCountValid">
                        <option value="" ${empty formalCheckResult.abstractWordCountValid ? 'selected' : ''}>未检查</option>
                        <option value="true" ${formalCheckResult.abstractWordCountValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.abstractWordCountValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
	                <td class="muted">
	                    摘要字数：
	                    <b><span id="abstractCountText">${empty abstractCount ? 0 : abstractCount}</span></b>
	                    <span class="muted">（要求 100 - 300）</span>
	                </td>
            </tr>
            <tr>
                <td>PDF页数是否符合（自动）</td>
                <td>
                    <select name="bodyWordCountValid" id="bodyWordCountValid">
                        <option value="" ${empty formalCheckResult.bodyWordCountValid ? 'selected' : ''}>未检查</option>
                        <option value="true" ${formalCheckResult.bodyWordCountValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.bodyWordCountValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
                <td class="muted">
                    PDF页数（从作者上传 PDF 统计）：
                    <b><span id="bodyCountText">${empty pdfPageCount ? (empty bodyCount ? 0 : bodyCount) : pdfPageCount}</span></b>
                    <span class="muted">（要求 8 - 10 页）</span>
                </td>
            </tr>
            <tr>
                <td>关键词是否规范（自动）</td>
                <td>
                    <select name="keywordsValid" id="keywordsValid">
                        <option value="" ${empty formalCheckResult.keywordsValid ? 'selected' : ''}>未检查</option>
                        <option value="true" ${formalCheckResult.keywordsValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.keywordsValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
                <td class="muted">关键词数量与分隔符是否符合要求</td>
            </tr>
            <tr>
                <td>注释编号是否规范（人工）</td>
                <td>
                    <select name="footnoteNumberingValid" id="footnoteNumberingValid">
                        <option value="" ${empty formalCheckResult.footnoteNumberingValid ? 'selected' : ''}>未填写</option>
                        <option value="true" ${formalCheckResult.footnoteNumberingValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.footnoteNumberingValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
                <td class="muted">脚注/尾注编号连续、格式统一</td>
            </tr>
            <tr>
                <td>图表格式是否规范（人工）</td>
                <td>
                    <select name="figureTableFormatValid" id="figureTableFormatValid">
                        <option value="" ${empty formalCheckResult.figureTableFormatValid ? 'selected' : ''}>未填写</option>
                        <option value="true" ${formalCheckResult.figureTableFormatValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.figureTableFormatValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
                <td class="muted">图题、表题、编号、引用是否符合规范</td>
            </tr>
            <tr>
                <td>参考文献格式是否规范（人工）</td>
                <td>
                    <select name="referenceFormatValid" id="referenceFormatValid">
                        <option value="" ${empty formalCheckResult.referenceFormatValid ? 'selected' : ''}>未填写</option>
                        <option value="true" ${formalCheckResult.referenceFormatValid == true ? 'selected' : ''}>通过</option>
                        <option value="false" ${formalCheckResult.referenceFormatValid == false ? 'selected' : ''}>不通过</option>
                    </select>
                </td>
                <td class="muted">引用与著录格式（如 GB/T、APA 等）</td>
            </tr>
            </tbody>
        </table>

        <h3 style="margin-top: var(--space-5);">查重信息</h3>
        <div class="grid grid-2" style="gap: 14px;">
            <div>
                <label>查重率（0-100）</label>
                <input type="number" step="0.01" min="0" max="100" name="similarityScore" id="similarityScore"
                       value="${empty formalCheckResult.similarityScore ? '' : formalCheckResult.similarityScore}"/>
            </div>
            <div>
                <label>是否高相似度（>20%）</label>
                <select name="highSimilarity" id="highSimilarity">
                    <option value="" ${empty formalCheckResult.highSimilarity ? 'selected' : ''}>未填写</option>
                    <option value="true" ${formalCheckResult.highSimilarity == true ? 'selected' : ''}>是</option>
                    <option value="false" ${formalCheckResult.highSimilarity == false ? 'selected' : ''}>否</option>
                </select>
            </div>
        </div>
        <div style="margin-top: 12px;">
            <label>查重报告链接（可选）</label>
            <input type="text" name="plagiarismReportUrl" id="plagiarismReportUrl"
                   value="${empty formalCheckResult.plagiarismReportUrl ? '' : formalCheckResult.plagiarismReportUrl}"/>
        </div>

        <h3 style="margin-top: var(--space-5);">反馈意见</h3>
        <textarea name="feedback" id="feedback" rows="6" placeholder="若不通过，请写清楚需要作者修改的格式问题；若留空，系统会根据勾选项自动生成。">${empty formalCheckResult.feedback ? '' : formalCheckResult.feedback}</textarea>

        <h3 style="margin-top: var(--space-5);">审查结果</h3>
<div class="form-row" style="max-width: 520px;">
    <label for="checkResult"><strong>审查结果：</strong></label>
    <select name="checkResult" id="checkResult" required>
        <option value="">请选择</option>
        <option value="PASS" ${formalCheckResult.checkResult == 'PASS' ? 'selected' : ''}>通过（送主编案头）</option>
        <option value="FAIL" ${formalCheckResult.checkResult == 'FAIL' ? 'selected' : ''}>不通过（退回作者修改）</option>
    </select>
    <small class="muted">最终提交以此处选择为准（不会再被检查项自动覆盖）。</small>
</div>

<div class="toolbar" style="margin-top: var(--space-4); gap: 10px;">
    <button class="btn btn-primary" type="button" id="btnSubmit">提交</button>
</div>
    </form>
</c:if>

<script>
(function(){
    const ctx = "${ctx}";
    const manuscriptId = "${empty manuscript ? '' : manuscript.manuscriptId}";

    const msgBox = document.getElementById('msgBox');
    function showMsg(text, ok){
        if(!msgBox) return;
        msgBox.style.display = 'block';
        msgBox.className = 'card ' + (ok ? '' : '');
        msgBox.innerHTML = '<strong>' + (ok ? '成功：' : '提示：') + '</strong> ' + (text || '');
    }

    async function postForm(url, dataObj){
        // ⚠️ 不能用 FormData 直接 POST（multipart/form-data），否则普通 Servlet 的 getParameter 取不到 op/manuscriptId
        // 这里改为 application/x-www-form-urlencoded
        const params = new URLSearchParams();
        Object.keys(dataObj || {}).forEach(k => {
            const v = dataObj[k];
            if (v !== undefined && v !== null) params.append(k, String(v));
        });
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        });
        const txt = await res.text();
        try { return JSON.parse(txt); }
        catch(e) { return { success: false, message: txt || ('HTTP ' + res.status) }; }
    }

    function setSelect(id, valueStr){
        const el = document.getElementById(id);
        if(!el) return;
        for(const opt of el.options){
            if(String(opt.value) === String(valueStr)) { opt.selected = true; return; }
        }
    }

    document.getElementById('btnAutoCheck')?.addEventListener('click', async function(){
        if(!manuscriptId) return;
        showMsg('正在执行自动检查...', true);
        const json = await postForm(ctx + '/editor/formalCheck/autoCheck', {
            manuscriptId, op: 'autoCheck'
        });
        if(json.success){
            setSelect('authorInfoValid', json.authorInfoValid);
            setSelect('abstractWordCountValid', json.abstractWordCountValid);
            setSelect('bodyWordCountValid', json.bodyWordCountValid);
            setSelect('keywordsValid', json.keywordsValid);
            const bcEl = document.getElementById('bodyCountText');
            if (bcEl && json.bodyCount !== undefined && json.bodyCount !== null) {
                bcEl.textContent = String(json.bodyCount);
            }
	            const acEl = document.getElementById('abstractCountText');
	            if (acEl && json.abstractCount !== undefined && json.abstractCount !== null) {
	                acEl.textContent = String(json.abstractCount);
	            }
            showMsg(json.message || '自动检查完成。', true);
        }else{
            showMsg(json.message || '自动检查失败。', false);
        }
    });

    document.getElementById('btnPlagiarism')?.addEventListener('click', async function(){
        if(!manuscriptId) return;
        showMsg('正在执行查重...', true);
        const json = await postForm(ctx + '/editor/formalCheck/autoCheck', {
            manuscriptId, op: 'plagiarismCheck'
        });
        if(json.success){
            const sim = document.getElementById('similarityScore');
            const high = document.getElementById('highSimilarity');
            const url = document.getElementById('plagiarismReportUrl');
            if(sim && json.similarityScore !== undefined) sim.value = json.similarityScore;
            if(high) setSelect('highSimilarity', String(json.highSimilarity));
            if(url && json.reportUrl) url.value = json.reportUrl;
            showMsg(json.message || '查重完成。', true);
        }else{
            showMsg(json.message || '查重失败。', false);
        }
    });

    async function submitResult(){
        const form = document.getElementById('formalCheckForm');
        if(!form) return;

        const params = new URLSearchParams();
        // 用 FormData 读取表单字段，但最终以 x-www-form-urlencoded 发送，保证后端 getParameter 可用
        new FormData(form).forEach((v, k) => {
            if (v !== undefined && v !== null) params.append(k, String(v));
        });

        params.append('op', 'submit');
        // checkResult 来自表单下拉选择（name=checkResult）
        // 让后端返回 JSON（避免后端对普通 form 走 302 跳转导致这里解析失败）
        params.append('ajax', '1');

        const res = await fetch(ctx + '/editor/formalCheck', {
            method:'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
        });

        const txt = await res.text();
        let json;
        try { json = JSON.parse(txt); } catch(e) { json = { success:false, message: txt || ('HTTP ' + res.status) }; }

        if(json.success){
            showMsg(json.message || '提交成功。', true);
            // 提交后回到列表，历史可在“审查历史”查看
            window.setTimeout(()=>{ window.location.href = ctx + '/editor/formalCheck'; }, 600);
        }else{
            showMsg(json.message || '提交失败。', false);
        }
    }

    document.getElementById('btnSubmit')?.addEventListener('click', ()=>submitResult());
})();
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
