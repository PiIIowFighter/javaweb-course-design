<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%-- Quill 富文本编辑器（本地引用） --%>
<link href="${ctx}/static/css/quill.snow.css" rel="stylesheet">
<script src="${ctx}/static/js/quill.min.js"></script>

<style>
    /* Quill 编辑器：与系统表单风格对齐 */
    .quill-editor {
        background: #fff;
        border: 1px solid var(--border);
        border-radius: var(--radius-sm);
        min-height: 220px;
    }
    .quill-editor .ql-toolbar {
        border: none;
        border-bottom: 1px solid var(--border);
        border-top-left-radius: var(--radius-sm);
        border-top-right-radius: var(--radius-sm);
    }
    .quill-editor .ql-container {
        border: none;
        border-bottom-left-radius: var(--radius-sm);
        border-bottom-right-radius: var(--radius-sm);
    }
    .quill-editor .ql-editor {
        min-height: 200px;
        font-size: 14px;
        line-height: 1.6;
    }
    #commentsToAuthorEditor .ql-editor { min-height: 260px; }

    /* 用于提交：隐藏 textarea，但仍参与 JS 同步 */
    .quill-sync-textarea {
        position: absolute !important;
        left: -10000px !important;
        top: auto !important;
        width: 1px !important;
        height: 1px !important;
        overflow: hidden !important;
        opacity: 0 !important;
    }
</style>

<%-- Font Awesome 图标（用于评分/提示等） --%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">

<c:choose>
    <c:when test="${not empty param.reviewId}">
        <c:set var="rid" value="${param.reviewId}"/>
    </c:when>
    <c:when test="${not empty param.id}">
        <c:set var="rid" value="${param.id}"/>
    </c:when>
    <c:otherwise>
        <c:set var="rid" value="${reviewId}"/>
    </c:otherwise>
</c:choose>

<h2><i class="fas fa-edit"></i> 提交评审意见</h2>

<c:if test="${empty rid}">
    <div class="alert alert-danger">
        <i class="fas fa-exclamation-circle"></i> 缺少 reviewId 参数，无法提交评审。请从"待审列表"重新进入。
    </div>
</c:if>

<c:if test="${not empty rid}">
<div class="container-fluid">
    <div class="alert alert-info">
        <i class="fas fa-info-circle"></i> 
        <strong>重要提醒：</strong>提交评审意见后，您将无法再查看该稿件的脱密版文件。
    </div>

<form method="post" action="${ctx}/reviewer/submit" id="reviewForm" novalidate onsubmit="return prepareForm()">
    <input type="hidden" name="reviewId" value="${rid}"/>
    <input type="hidden" name="id" value="${rid}"/>

    <div class="card mb-4">
        <div class="card-header bg-primary text-white">
            <h4><i class="fas fa-file-alt"></i> 评审说明</h4>
        </div>
        <div class="card-body">
            <p>请分别填写：<b>给编辑的保密意见（Confidential to Editor）</b> 与 <b>给作者的意见（Comments to Author）</b>。</p>
            <p class="text-muted"><i class="fas fa-lock"></i> 给编辑的意见仅编辑和主编可见，包含是否存在学术不端风险、重要缺陷、建议处理方式等</p>
        </div>
    </div>

    <div class="card mb-4">
        <div class="card-header bg-success text-white">
            <h4><i class="fas fa-edit"></i> 给编辑的保密意见（Confidential to Editor）</h4>
        </div>
        <div class="card-body">
            <div class="mb-4">
                <h5><i class="fas fa-chart-bar"></i> 多维评分</h5>
                <div class="table-responsive">
                    <table class="table table-bordered table-hover">
                        <thead class="table-light">
                            <tr>
                                <th width="25%">维度</th>
                                <th width="25%">分值（0-10，整数）</th>
                                <th width="50%">评判标准</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><strong>原创性（Originality）</strong></td>
                                <td>
                                    <input type="number" name="scoreOriginality" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>开创性工作，提出全新理论/方法<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>显著改进现有方法，有重要创新<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>适度改进，有一定新意<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>微小改进，创新性有限<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>无创新性，简单重复
                                    </small>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>重要性/影响力（Significance）</strong></td>
                                <td>
                                    <input type="number" name="scoreSignificance" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>对领域有重大影响，解决核心问题<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>有重要学术/应用价值<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>有一定价值，但影响有限<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>价值较小，意义不大<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>无实际意义
                                    </small>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>方法/技术质量（Methodology）</strong></td>
                                <td>
                                    <input type="number" name="scoreMethodology" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>方法严谨，设计完善，技术先进<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>方法合理，技术可靠<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>方法基本正确，但可改进<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>方法有明显缺陷<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>方法错误或不可行
                                    </small>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>表达/结构（Presentation）</strong></td>
                                <td>
                                    <input type="number" name="scorePresentation" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>逻辑清晰，文笔优美，图表专业<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>表达清楚，结构合理<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>基本可读，但有改进空间<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>表达混乱，难理解<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>无法阅读，严重错误
                                    </small>
                                </td>
                            </tr>
                            
                            <!-- 新增维度 1: 实验/数据分析 -->
                            <tr>
                                <td><strong>实验/数据分析（Experimentation & Analysis）</strong></td>
                                <td>
                                    <input type="number" name="scoreExperimentation" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>实验设计科学，数据分析全面深入，结论可靠<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>实验合理，数据分析恰当，结论有说服力<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>实验基本合理，数据分析基本正确<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>实验设计或数据分析有明显缺陷<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>实验或数据分析存在严重问题
                                    </small>
                                </td>
                            </tr>
                            
                            <!-- 新增维度 2: 文献综述 -->
                            <tr>
                                <td><strong>文献综述（Literature Review）</strong></td>
                                <td>
                                    <input type="number" name="scoreLiteratureReview" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>全面覆盖相关文献，批判性分析深入，定位准确<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>文献覆盖较全面，分析合理，定位较准确<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>文献基本覆盖，分析基本合理<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>文献覆盖不全，分析不足<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>文献综述严重缺失或错误
                                    </small>
                                </td>
                            </tr>
                            
                            <!-- 新增维度 3: 结论与讨论 -->
                            <tr>
                                <td><strong>结论与讨论（Conclusions & Discussion）</strong></td>
                                <td>
                                    <input type="number" name="scoreConclusions" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>结论明确有力，讨论深入，展望合理<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>结论清晰，讨论充分，展望合理<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>结论基本合理，讨论基本充分<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>结论模糊或讨论不足<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>结论与讨论存在严重问题
                                    </small>
                                </td>
                            </tr>
                            
                            <!-- 新增维度 4: 学术规范性 -->
                            <tr>
                                <td><strong>学术规范性（Academic Integrity）</strong></td>
                                <td>
                                    <input type="number" name="scoreAcademicIntegrity" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>完全符合学术规范，引用准确，无学术不端<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>基本符合学术规范，引用基本准确<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>规范性一般，有少量不规范之处<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>规范性较差，存在明显问题<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>存在严重学术规范问题或学术不端
                                    </small>
                                </td>
                            </tr>
                            
                            <!-- 新增维度 5: 实用性 -->
                            <tr>
                                <td><strong>实用性（Practicality）</strong></td>
                                <td>
                                    <input type="number" name="scorePracticality" class="form-control score-input" 
                                           min="0" max="10" step="1" required value="5"
                                           onchange="updateOverallScore()"/>
                                </td>
                                <td>
                                    <small class="text-muted">
                                        <i class="fas fa-star"></i> <strong>9-10分：</strong>有重大实际应用价值，可直接转化<br>
                                        <i class="fas fa-star-half-alt"></i> <strong>7-8分：</strong>有较好应用前景，易于转化<br>
                                        <i class="fas fa-check-circle"></i> <strong>5-6分：</strong>有一定应用价值，但需进一步开发<br>
                                        <i class="fas fa-exclamation-circle"></i> <strong>3-4分：</strong>应用价值有限，转化困难<br>
                                        <i class="fas fa-times-circle"></i> <strong>0-2分：</strong>无实际应用价值
                                    </small>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="row mb-3">
                <div class="col-md-6">
                    <div class="form-group">
                        <label for="score" class="form-label">
                            <i class="fas fa-calculator"></i> <strong>总体分（自动计算）</strong>
                        </label>
                        <div class="input-group">
                            <input type="number" name="score" id="score" class="form-control" 
                                   min="0" max="10" step="1" required readonly value="5"/>
                            <span class="input-group-text">/10</span>
                        </div>
                        <input type="hidden" name="scoreOverall" id="scoreOverall" value="5"/>
                        <small class="form-text text-muted">基于九项评分自动计算的平均分</small>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="form-group">
                        <label for="recommendation" class="form-label">
                            <i class="fas fa-gavel"></i> <strong>推荐结论</strong>
                        </label>
                        <select name="recommendation" id="recommendation" class="form-select" required>
                            <option value="">-- 请选择 --</option>
                            <option value="ACCEPT">接受（Accept）</option>
                            <option value="MINOR_REVISION">小修后接受（Minor Revision）</option>
                            <option value="MAJOR_REVISION">大修后再审（Major Revision）</option>
                            <option value="REJECT">拒稿（Reject）</option>
                        </select>
                    </div>
                </div>
            </div>

            <div class="form-group">
                <label class="form-label">
                    <i class="fas fa-lock"></i> <strong>给编辑的保密意见（必填）</strong>
                </label>
                <small class="form-text text-muted d-block mb-2">
                    仅编辑/主编可见。请评估：是否存在学术不端风险、重要缺陷、建议处理方式等
                </small>
                <textarea name="confidentialToEditor" id="confidentialToEditor" rows="8" required placeholder="请输入给编辑的保密意见..." class="form-control"></textarea>
                <div id="confidentialToEditorEditor" class="quill-editor" style="display:none;"></div>

                <div id="quillLoadError" class="alert alert-warning mt-2" style="display:none;">
                    <i class="fas fa-exclamation-triangle"></i>
                    富文本编辑器未能加载，已自动切换为普通文本输入。请确认本地资源存在且可访问：
                    <code>${ctx}/static/js/quill.min.js</code> 与 <code>${ctx}/static/css/quill.snow.css</code>。
                </div>
            </div>
        </div>
    </div>

    <div class="card mb-4">
        <div class="card-header bg-info text-white">
            <h4><i class="fas fa-user-edit"></i> 给作者的意见（Comments to Author）</h4>
        </div>
        <div class="card-body">
            <div class="form-group">
                <label class="form-label">
                    <i class="fas fa-comment-dots"></i> <strong>具体修改建议（必填）</strong>
                </label>
                <small class="form-text text-muted d-block mb-2">
                    作者可见。请提供详细的修改建议，帮助作者改进稿件
                </small>
                <textarea name="commentsToAuthor" id="commentsToAuthor" rows="10" required placeholder="请输入给作者的具体修改建议..." class="form-control"></textarea>
                <div id="commentsToAuthorEditor" class="quill-editor" style="display:none;"></div>
                <%-- 兼容旧后端 --%>
                <input type="hidden" name="content" id="contentCompat"/>
                
                <%-- 新增维度的隐藏字段（如果需要兼容旧后端） --%>
                <input type="hidden" name="scoreExperimentation" id="scoreExperimentationCompat" value="5"/>
                <input type="hidden" name="scoreLiteratureReview" id="scoreLiteratureReviewCompat" value="5"/>
                <input type="hidden" name="scoreConclusions" id="scoreConclusionsCompat" value="5"/>
                <input type="hidden" name="scoreAcademicIntegrity" id="scoreAcademicIntegrityCompat" value="5"/>
                <input type="hidden" name="scorePracticality" id="scorePracticalityCompat" value="5"/>
            </div>
        </div>
    </div>

    <div class="card mb-4">
        <div class="card-header bg-warning">
            <h4><i class="fas fa-exclamation-triangle"></i> 提交确认</h4>
        </div>
        <div class="card-body">
            <div class="alert alert-warning">
                <h5><i class="fas fa-bell"></i> 重要提示</h5>
                <ul>
                    <li>提交评审意见后，您将无法再查看该稿件的脱密版文件</li>
                    <li>评审意见一旦提交，将无法修改，请仔细核对</li>
                    <li>请确保评分和意见客观公正</li>
                </ul>
            </div>
            
            <div class="d-flex justify-content-between">
                <a href="${ctx}/reviewer/assigned" class="btn btn-secondary">
                    <i class="fas fa-arrow-left"></i> 返回待审列表
                </a>
                <button type="submit" class="btn btn-primary" id="submitBtn">
                    <i class="fas fa-paper-plane"></i> 提交评审意见
                </button>
            </div>
        </div>
    </div>
</form>
</div>

<script>
    // 初始化 Quill 富文本编辑器（Snow theme，本地资源）
    var quillToolbar = [
        ['bold', 'italic', 'underline', 'strike'],
        [{'list': 'ordered'}, {'list': 'bullet'}],
        [{'indent': '-1'}, {'indent': '+1'}],
        [{'align': []}],
        ['link'],
        ['clean']
    ];

    var confidentialQuill = null;
    var commentsQuill = null;

    function syncQuillToTextarea() {
        if (!confidentialQuill || !commentsQuill) return;
        var confidentialEl = document.getElementById('confidentialToEditor');
        var commentsEl = document.getElementById('commentsToAuthor');
        if (confidentialEl) confidentialEl.value = confidentialQuill.root.innerHTML;
        if (commentsEl) commentsEl.value = commentsQuill.root.innerHTML;
    }

    function initQuillIfAvailable() {
        var warn = document.getElementById('quillLoadError');
        var ta1 = document.getElementById('confidentialToEditor');
        var ta2 = document.getElementById('commentsToAuthor');
        var ed1 = document.getElementById('confidentialToEditorEditor');
        var ed2 = document.getElementById('commentsToAuthorEditor');

        // 未加载 Quill：保留 textarea 作为降级输入
        if (typeof window.Quill === 'undefined') {
            if (warn) warn.style.display = 'block';
            if (ed1) ed1.style.display = 'none';
            if (ed2) ed2.style.display = 'none';
            return;
        }

        try {
            if (ed1) ed1.style.display = 'block';
            if (ed2) ed2.style.display = 'block';

            confidentialQuill = new Quill('#confidentialToEditorEditor', {
                theme: 'snow',
                modules: { toolbar: quillToolbar },
                placeholder: '请输入给编辑的保密意见...'
            });

            commentsQuill = new Quill('#commentsToAuthorEditor', {
                theme: 'snow',
                modules: { toolbar: quillToolbar },
                placeholder: '请输入给作者的具体修改建议...'
            });

            // 若后端回填了历史内容，则带入 Quill
            if (ta1 && ta1.value) confidentialQuill.root.innerHTML = ta1.value;
            if (ta2 && ta2.value) commentsQuill.root.innerHTML = ta2.value;

            // Quill 正常后再隐藏 textarea
            if (ta1) ta1.classList.add('quill-sync-textarea');
            if (ta2) ta2.classList.add('quill-sync-textarea');

            // 同步一次，并绑定变更事件
            syncQuillToTextarea();
            confidentialQuill.on('text-change', syncQuillToTextarea);
            commentsQuill.on('text-change', syncQuillToTextarea);

            if (warn) warn.style.display = 'none';
        } catch (e) {
            console.error('Quill init failed:', e);
            if (warn) warn.style.display = 'block';
            if (ed1) ed1.style.display = 'none';
            if (ed2) ed2.style.display = 'none';
            if (ta1) ta1.classList.remove('quill-sync-textarea');
            if (ta2) ta2.classList.remove('quill-sync-textarea');
            confidentialQuill = null;
            commentsQuill = null;
        }
    }

    // 兼容：如果脚本在 DOMContentLoaded 之后执行，也能初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initQuillIfAvailable);
    } else {
        initQuillIfAvailable();
    }
// 更新总体分 - 基于所有9个维度计算
    function updateOverallScore() {
        console.log('updateOverallScore called');
        
        // 所有评分字段的name
        const scoreFields = [
            'scoreOriginality', 'scoreSignificance', 'scoreMethodology', 'scorePresentation',
            'scoreExperimentation', 'scoreLiteratureReview', 'scoreConclusions', 
            'scoreAcademicIntegrity', 'scorePracticality'
        ];
        
        let total = 0;
        let validCount = 0;
        
        // 遍历所有评分字段
        scoreFields.forEach(fieldName => {
            // 使用jQuery选择器获取所有同名的input
            const inputs = document.querySelectorAll(`input[name="${fieldName}"]`);
            
            inputs.forEach(input => {
                const value = parseFloat(input.value);
                console.log(`Field ${fieldName}: value=${input.value}, parsed=${value}`);
                
                if (!isNaN(value) && value >= 0 && value <= 10) {
                    total += value;
                    validCount++;
                }
            });
        });
        
        console.log(`Total: ${total}, Count: ${validCount}`);
        
        if (validCount > 0) {
            const average = total / validCount;
            const roundedAverage = Math.round(average * 10) / 10; // 保留1位小数
            
            // 更新显示字段和隐藏字段
            const scoreDisplay = document.getElementById('score');
            const scoreHidden = document.getElementById('scoreOverall');
            
            if (scoreDisplay) {
                scoreDisplay.value = roundedAverage;
                console.log(`Updated score display to: ${roundedAverage}`);
            }
            
            if (scoreHidden) {
                scoreHidden.value = roundedAverage;
                console.log(`Updated score hidden to: ${roundedAverage}`);
            }
            
            // 同时更新新增维度的隐藏字段
            const hiddenFields = [
                'scoreExperimentation', 'scoreLiteratureReview', 'scoreConclusions',
                'scoreAcademicIntegrity', 'scorePracticality'
            ];
            
            hiddenFields.forEach(fieldName => {
                const input = document.querySelector(`input[name="${fieldName}"]`);
                const hiddenInput = document.getElementById(`${fieldName}Compat`);
                
                if (input && hiddenInput) {
                    const value = parseFloat(input.value);
                    if (!isNaN(value) && value >= 0 && value <= 10) {
                        hiddenInput.value = value;
                    } else {
                        hiddenInput.value = 5; // 默认值
                    }
                }
            });
        }
    }

    // 准备表单提交（保留原逻辑）
    function prepareForm() {
        console.log('prepareForm called');
        // 同步 Quill 内容到隐藏字段
        if (typeof syncQuillToTextarea === "function") {
            syncQuillToTextarea();
        }

        // 校验【给编辑的保密意见】与【给作者的意见】必填
        var confidentialTa = document.getElementById('confidentialToEditor');
        var commentsTa = document.getElementById('commentsToAuthor');

        if (confidentialQuill) {
            if (confidentialQuill.getText().trim().length === 0) {
                alert("请填写【给编辑的保密意见】");
                return false;
            }
        } else {
            if (!confidentialTa || confidentialTa.value.trim().length === 0) {
                alert("请填写【给编辑的保密意见】");
                return false;
            }
        }

        if (commentsQuill) {
            if (commentsQuill.getText().trim().length === 0) {
                alert("请填写【给作者的意见】");
                return false;
            }
        } else {
            if (!commentsTa || commentsTa.value.trim().length === 0) {
                alert("请填写【给作者的意见】");
                return false;
            }
        }

        // 同步兼容字段
        const commentsEl = document.getElementById('commentsToAuthor');
        const compatEl = document.getElementById('contentCompat');
        if (commentsEl && compatEl) {
            compatEl.value = commentsEl.value;
            console.log('Updated content compat field');
        }
        
        // 验证表单
        const form = document.getElementById('reviewForm');
        if (!form.checkValidity()) {
            // 触发浏览器原生提示（更直观）
            if (form.reportValidity) form.reportValidity();
            else alert('请填写所有必填字段');
            return false;
        }
        
        // 验证评分是否在0-10范围内
        const scoreInputs = document.querySelectorAll('.score-input');
        let allValid = true;
        scoreInputs.forEach(input => {
            const value = parseFloat(input.value);
            if (isNaN(value) || value < 0 || value > 10) {
                allValid = false;
                input.classList.add('is-invalid');
            } else {
                input.classList.remove('is-invalid');
            }
        });
        
        if (!allValid) {
            alert('请确保所有评分都在0-10的范围内');
            return false;
        }
        
        // 确认提交
        if (!confirm('确定提交评审意见吗？提交后将无法修改。')) {
            return false;
        }
        
        // 禁用提交按钮防止重复提交
        const submitBtn = document.getElementById('submitBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 提交中...';
        
        return true;
    }
    
    // 页面加载完成后的初始化
    document.addEventListener('DOMContentLoaded', function() {
        console.log('DOMContentLoaded');
        
        // 初始计算总体分
        updateOverallScore();
        
        // 为所有评分输入框绑定实时计算事件
        const scoreInputs = document.querySelectorAll('.score-input');
        console.log(`Found ${scoreInputs.length} score inputs`);
        
        scoreInputs.forEach(input => {
            // 移除原有的事件监听器，避免重复绑定
            const newInput = input.cloneNode(true);
            input.parentNode.replaceChild(newInput, input);
            
            // 为新input绑定事件
            newInput.addEventListener('input', updateOverallScore);
            newInput.addEventListener('change', updateOverallScore);
            
            // 添加键盘事件，确保输入限制
            newInput.addEventListener('keydown', function(e) {
                // 允许控制键、删除键、箭头键等
                if ([8, 9, 13, 27, 46].includes(e.keyCode) || 
                    (e.keyCode >= 37 && e.keyCode <= 40)) {
                    return;
                }
                
                // 确保输入的是数字
                if ((e.keyCode < 48 || e.keyCode > 57) && 
                    (e.keyCode < 96 || e.keyCode > 105)) {
                    e.preventDefault();
                }
            });
        });
        
        // 为推荐结论添加事件，确保选择了选项
        const recommendationSelect = document.getElementById('recommendation');
        if (recommendationSelect) {
            recommendationSelect.addEventListener('change', function() {
                if (this.value) {
                    this.classList.remove('is-invalid');
                } else {
                    this.classList.add('is-invalid');
                }
            });
        }
        
        // 为表单添加提交前的验证
        const form = document.getElementById('reviewForm');
        if (form) {
            form.addEventListener('submit', function(e) {
                const recommendation = document.getElementById('recommendation');
                if (!recommendation || !recommendation.value) {
                    e.preventDefault();
                    alert('请选择推荐结论');
                    recommendation.classList.add('is-invalid');
                    return false;
                }
            });
        }
            }
);
            }
        }, 1000);
    });
    
    // 页面完全加载后再次计算，确保所有元素已加载
    window.addEventListener('load', function() {
        console.log('Window loaded');
        setTimeout(updateOverallScore, 500);
    });
</script>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>