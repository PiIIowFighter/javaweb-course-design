<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>稿件详情</h2>

<c:if test="${empty manuscript}">
    <p>未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">
<c:set var="backToUrl" value="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}#inviteReviewers"/>

<c:if test="${not empty param.cancelMsg}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #b7eb8f; background:#f6ffed; color:#135200; border-radius:6px;">
        <c:out value="${param.cancelMsg}"/>
    </div>
</c:if>

<c:if test="${not empty param.inviteMsg}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #b7eb8f; background:#f6ffed; color:#135200; border-radius:6px;">
        <c:out value="${param.inviteMsg}"/>
    </div>
</c:if>
<c:if test="${not empty param.inviteErr}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #ffa39e; background:#fff1f0; color:#a8071a; border-radius:6px;">
        <c:out value="${param.inviteErr}"/>
    </div>
</c:if>
    <table border="1" cellpadding="4" cellspacing="0" style="background:#fff;">
        <tr>
            <th>稿件编号</th>
            <td><c:out value="${manuscript.manuscriptId}"/></td>
        </tr>
        <tr>
            <th>标题</th>
            <td><c:out value="${manuscript.title}"/></td>
        </tr>
        <tr>
            <th>期刊ID</th>
            <td><c:out value="${manuscript.journalId}"/></td>
        </tr>
        <tr>
            <th>所属专刊</th>
            <td>
                <c:choose>
                    <c:when test="${not empty linkedIssue}">
                        <a href="${pageContext.request.contextPath}/issues?view=detail&id=${linkedIssue.issueId}">
                            <c:out value="${linkedIssue.title}"/>
                        </a>
                    </c:when>
                    <c:when test="${not empty manuscript.issueTitle}">
                        <c:out value="${manuscript.issueTitle}"/>
                    </c:when>
                    <c:otherwise>
                        （默认 / 未关联）
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <th>研究主题</th>
            <td><c:out value="${manuscript.subjectArea}"/></td>
        </tr>
        <tr>
            <th>项目资助情况</th>
            <td><c:out value="${manuscript.fundingInfo}"/></td>
        </tr>
        <tr>
            <th>关键词</th>
            <td><c:out value="${manuscript.keywords}"/></td>
        </tr>
        <tr>
            <th>摘要（HTML）</th>
            <td>
                                    <div class="ql-snow richtext-view">
                        <div class="ql-editor">
                            <c:out value="${manuscript.abstractText}" escapeXml="false"/>
                        </div>
                    </div>
            </td>
        </tr>
        <tr>
            <th>当前状态</th>
            <td><c:out value="${manuscript.currentStatus}"/></td>
        </tr>
        <tr>
            <th>提交时间</th>
            <td><c:out value="${manuscript.submitTime}"/></td>
        </tr>
        <tr>
            <th>决策</th>
            <td><c:out value="${manuscript.decision}"/></td>
        </tr>
        <tr>
            <th>附件</th>
            <td>
                <c:if test="${not empty currentVersion}">
                    <!-- 手稿：审稿人默认查看匿名稿（由 /files/preview 内部根据角色选择文件路径） -->
                    <c:if test="${not empty currentVersion.fileOriginalPath or not empty currentVersion.fileAnonymousPath}">
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">Manuscript 预览/下载</a>
                    </c:if>

                    <!-- 匿名稿：若系统已生成匿名稿，可额外提供入口（工作人员也可用来核查匿名处理是否到位） -->
                    <c:if test="${not empty currentVersion.fileAnonymousPath}">
                        <span style="margin-left:12px;"></span>
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=anonymous">匿名稿 预览/下载</a>
                    </c:if>

                    <!-- Cover/Response：审稿人通常不应看到 -->
                    <c:if test="${sessionScope.currentUser.roleCode != 'REVIEWER'}">
                        <c:if test="${not empty currentVersion.coverLetterPath}">
                            <span style="margin-left:12px;"></span>
                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">Cover Letter 预览/下载</a>
                        </c:if>
                        <c:if test="${not empty currentVersion.responseLetterPath}">
                            <span style="margin-left:12px;"></span>
                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=response">Response Letter 预览/下载</a>
                        </c:if>
                    </c:if>
                </c:if>
                <c:if test="${empty currentVersion}">
                    暂无版本文件
                </c:if>
            </td>
        </tr>
    </table>

    <!-- 形式审查界面 -->
    <c:if test="${sessionScope.currentUser.roleCode == 'EO_ADMIN' and (manuscript.currentStatus == 'SUBMITTED' or manuscript.currentStatus == 'FORMAL_CHECK')}">
        <h3 style="margin-top:14px;">形式审查</h3>
        
        <c:if test="${not empty formalCheckResult}">
            <div style="border:1px solid #ccc; background:#f9f9f9; padding:12px; margin:10px 0;">
                <p><strong>上次审查结果：</strong><c:out value="${formalCheckResult.checkResult}"/></p>
                <p><strong>审查时间：</strong><c:out value="${formalCheckResult.checkTime}"/></p>
                <p><strong>反馈意见：</strong><c:out value="${formalCheckResult.feedback}"/></p>
            </div>
        </c:if>
        
        <form id="formalCheckForm" action="${ctx}/editor/formalCheck" method="post">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
            <input type="hidden" name="op" value="submit"/>
            
            <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; width:100%;">
                <thead>
                    <tr>
                        <th style="width:40%;">检查项目</th>
                        <th style="width:20%;">检查方式</th>
                        <th style="width:40%;">审查状态</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>作者信息（机构邮箱）</td>
                        <td style="text-align:center; color:#666;">系统自动检查</td>
                        <td>
                            <select name="authorInfoValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>摘要字数（150-700字）</td>
                        <td style="text-align:center; color:#666;">系统自动检查</td>
                        <td>
                            <select name="abstractWordCountValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>正文字数（3000-8000字）</td>
                        <td style="text-align:center; color:#666;">系统自动检查</td>
                        <td>
                            <select name="bodyWordCountValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>关键词个数（3-7个）</td>
                        <td style="text-align:center; color:#666;">系统自动检查</td>
                        <td>
                            <select name="keywordsValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>注释编号</td>
                        <td style="text-align:center; color:#666;">人工判断</td>
                        <td>
                            <select name="footnoteNumberingValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>图表格式</td>
                        <td style="text-align:center; color:#666;">人工判断</td>
                        <td>
                            <select name="figureTableFormatValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>参考文献格式</td>
                        <td style="text-align:center; color:#666;">人工判断</td>
                        <td>
                            <select name="referenceFormatValid">
                                <option value="">待检查</option>
                                <option value="true">符合标准</option>
                                <option value="false">不符合标准</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>查重率（阈值：20%）</td>
                        <td style="text-align:center; color:#666;">系统自动检查</td>
                        <td>
                            <div id="plagiarismCheckResult">
                                <span style="color:#999;">未执行查重</span>
                            </div>
                            <button type="button" onclick="performPlagiarismCheck()" style="margin-top:4px;">执行查重</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            
            <div style="margin-top:12px;">
                <label><strong>审查结果：</strong></label>
                <select name="checkResult" id="checkResult" required>
                    <option value="">请选择</option>
                    <option value="PASS">通过</option>
                    <option value="FAIL">不通过</option>
                </select>
            </div>
            
            <div style="margin-top:12px;">
                <label><strong>反馈意见：</strong></label><br/>
                <textarea name="feedback" id="feedback" rows="4" cols="80" placeholder="请填写反馈意见，包含所有不合格的标记"></textarea>
            </div>
            
            <div style="margin-top:12px;">
                <button type="button" onclick="performAutoCheck()">执行自动检查</button>
                <button type="button" onclick="autoDetermineResult()">自动判定结果</button>
                <button type="submit" onclick="return confirm('确认提交形式审查结果？');">提交审查结果</button>
            </div>
        </form>
        
        <script>
            function performAutoCheck() {
                var manuscriptId = parseInt('${manuscript.manuscriptId}', 10);
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '${ctx}/editor/formalCheck', true);
                xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            var response = JSON.parse(xhr.responseText);
                            if (response.success) {
                                document.querySelector('select[name="authorInfoValid"]').value = response.authorInfoValid;
                                document.querySelector('select[name="abstractWordCountValid"]').value = response.abstractWordCountValid;
                                document.querySelector('select[name="bodyWordCountValid"]').value = response.bodyWordCountValid;
                                document.querySelector('select[name="keywordsValid"]').value = response.keywordsValid;
                                alert('自动检查完成！');
                            } else {
                                alert('自动检查失败：' + response.message);
                            }
                        } else {
                            alert('请求失败，状态码：' + xhr.status);
                        }
                    }
                };
                xhr.send('manuscriptId=' + manuscriptId + '&op=autoCheck');
            }
            
            function autoDetermineResult() {
                var checks = [
                    'authorInfoValid',
                    'abstractWordCountValid',
                    'bodyWordCountValid',
                    'keywordsValid',
                    'footnoteNumberingValid',
                    'figureTableFormatValid',
                    'referenceFormatValid'
                ];
                
                var hasInvalid = false;
                var feedback = document.getElementById('feedback');
                var feedbackText = '';
                
                for (var i = 0; i < checks.length; i++) {
                    var select = document.querySelector('select[name="' + checks[i] + '"]');
                    var value = select.value;
                    
                    if (value === 'false') {
                        hasInvalid = true;
                        var labelText = select.closest('tr').querySelector('td').textContent;
                        feedbackText += labelText + '不符合标准；';
                    }
                }
                
                if (hasInvalid) {
                    document.getElementById('checkResult').value = 'FAIL';
                    if (feedback.value === '' || feedback.value === feedbackText) {
                        feedback.value = feedbackText;
                    }
                    alert('已自动判定为"不通过"，请查看反馈意见。');
                } else {
                    var allChecked = true;
                    for (var i = 0; i < checks.length; i++) {
                        var select = document.querySelector('select[name="' + checks[i] + '"]');
                        if (select.value === '') {
                            allChecked = false;
                            break;
                        }
                    }
                    
                    if (allChecked) {
                        document.getElementById('checkResult').value = 'PASS';
                        feedback.value = '所有检查项均符合标准';
                        alert('已自动判定为"通过"。');
                    } else {
                        alert('请先完成所有检查项的审查。');
                    }
                }
            }
            
            function returnForRevision() {
                var checks = [
                    'authorInfoValid',
                    'abstractWordCountValid',
                    'bodyWordCountValid',
                    'keywordsValid',
                    'footnoteNumberingValid',
                    'figureTableFormatValid',
                    'referenceFormatValid'
                ];
                
                var feedbackText = '';
                for (var i = 0; i < checks.length; i++) {
                    var select = document.querySelector('select[name="' + checks[i] + '"]');
                    var value = select.value;
                    
                    if (value === 'false') {
                        var labelText = select.closest('tr').querySelector('td').textContent;
                        feedbackText += labelText + '不符合标准；';
                    }
                }
                
                if (feedbackText === '') {
                    feedbackText = document.getElementById('feedback').value;
                }
                
                if (feedbackText === '') {
                    alert('请填写反馈意见或标记不符合标准的项目。');
                    return;
                }
                
                if (!confirm('确认将稿件退回作者修改？系统将发送邮件通知作者。')) {
                    return;
                }
                
                var manuscriptId = parseInt('${manuscript.manuscriptId}', 10);
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '${ctx}/editor/formalCheck', true);
                xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            var response = JSON.parse(xhr.responseText);
                            if (response.success) {
                                alert('已退回修改，邮件已发送给作者。');
                                window.location.reload();
                            } else {
                                alert('操作失败：' + response.message);
                            }
                        } else {
                            alert('请求失败，状态码：' + xhr.status);
                        }
                    }
                };
                
                var params = 'manuscriptId=' + manuscriptId + '&op=returnForRevision&feedback=' + encodeURIComponent(feedbackText);
                xhr.send(params);
            }
            
            function performPlagiarismCheck() {
                var manuscriptId = parseInt('${manuscript.manuscriptId}', 10);
                var resultDiv = document.getElementById('plagiarismCheckResult');
                resultDiv.innerHTML = '<span style="color:#666;">正在查重中，请稍候...</span>';
                
                var xhr = new XMLHttpRequest();
                xhr.open('POST', '${ctx}/editor/formalCheck', true);
                xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                xhr.onreadystatechange = function() {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            var response = JSON.parse(xhr.responseText);
                            if (response.success) {
                                var similarityScore = response.similarityScore;
                                var highSimilarity = response.highSimilarity;
                                var reportUrl = response.reportUrl;
                                
                                var resultHtml = '<div style="margin-top:4px;">';
                                resultHtml += '<strong>查重率：</strong>' + similarityScore.toFixed(2) + '% ';
                                
                                if (highSimilarity) {
                                    resultHtml += '<span style="color:#d9534f; font-weight:bold;">（超过阈值20%）</span>';
                                } else {
                                    resultHtml += '<span style="color:#5cb85c;">（符合标准）</span>';
                                }
                                
                                resultHtml += '</div>';
                                
                                if (reportUrl) {
                                    resultHtml += '<div style="margin-top:4px;">';
                                    resultHtml += '<a href="' + reportUrl + '" target="_blank">查看查重报告</a>';
                                    resultHtml += '</div>';
                                }
                                
                                resultDiv.innerHTML = resultHtml;
                                alert('查重完成！查重率：' + similarityScore.toFixed(2) + '%');
                            } else {
                                resultDiv.innerHTML = '<span style="color:#d9534f;">查重失败：' + response.message + '</span>';
                                alert('查重失败：' + response.message);
                            }
                        } else {
                            resultDiv.innerHTML = '<span style="color:#d9534f;">请求失败，状态码：' + xhr.status + '</span>';
                            alert('请求失败，状态码：' + xhr.status);
                        }
                    }
                };
                xhr.send('manuscriptId=' + manuscriptId + '&op=plagiarismCheck');
            }
        </script>
    </c:if>

    <h3 style="margin-top:14px;">作者列表</h3>
    <c:if test="${empty authors}">
        <p>（未录入作者信息）</p>
    </c:if>
    <c:if test="${not empty authors}">
        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff;">
            <thead>
            <tr>
                <th>顺序</th>
                <th>姓名</th>
                <th>单位</th>
                <th>邮箱</th>
                <th>通讯作者</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${authors}" var="a">
                <tr>
                    <td><c:out value="${a.authorOrder}"/></td>
                    <td><c:out value="${a.fullName}"/></td>
                    <td><c:out value="${a.affiliation}"/></td>
                    <td><c:out value="${a.email}"/></td>
                    <td><c:out value="${a.corresponding ? '是' : '否'}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

	<c:if test="${(sessionScope.currentUser.roleCode == 'EDITOR' 
               or sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF' 
               or sessionScope.currentUser.roleCode == 'EO_ADMIN')
              and not empty chiefAssignment 
              and not empty chiefAssignment.chiefComment}">
    	<h3 style="margin-top:14px;">主编给编辑的指示</h3>
    	<div style="border:1px solid #ccc; background:#fffff0; padding:8px;">
        	<pre style="white-space:pre-wrap; margin:0;">
           		 <c:out value="${chiefAssignment.chiefComment}"/>
        	</pre>
    	</div>
	</c:if>
	

    <h3 style="margin-top:14px;">推荐审稿人</h3>
    <c:if test="${empty recommendedReviewers}">
        <p>（未推荐审稿人）</p>
    </c:if>
    <c:if test="${not empty recommendedReviewers}">
        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff;">
            <thead>
            <tr>
                <th>姓名</th>
                <th>邮箱</th>
                <th>推荐理由</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${recommendedReviewers}" var="r">
                <tr>
                    <td><c:out value="${r.fullName}"/></td>
                    <td><c:out value="${r.email}"/></td>
                    <td><c:out value="${r.reason}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
</c:if>

<hr/>

<!-- ====================== 作者“待修改” -> 修改并 Resubmit ====================== -->
<c:if test="${not empty manuscript
          and sessionScope.currentUser.roleCode == 'AUTHOR'
          and manuscript.submitterId == sessionScope.currentUser.userId
          and (manuscript.currentStatus == 'RETURNED' or manuscript.currentStatus == 'REVISION')}">

    <h2>待修改</h2>
    <p>
        当前稿件状态为 <strong><c:out value="${manuscript.currentStatus}"/></strong>。
        请在新页面中按“新建投稿”的完整逻辑对元数据、作者列表、附件、Cover Letter（富文本转 PDF）等进行修改，
        然后重新提交（Resubmit）。
    </p>

    <div class="actions" style="margin:12px 0;">
        <a class="btn-primary" href="${ctx}/manuscripts/resubmitEdit?id=${manuscript.manuscriptId}" style="text-decoration:none;">
            进入修改页面（推荐）
        </a>
        <a class="btn-quiet" href="${ctx}/manuscripts/list" style="text-decoration:none;">返回列表</a>
    </div>

</c:if>

<!-- ====================== 只有编辑 / 主编 且 稿件已进入外审相关阶段 时显示审稿相关内容 ====================== -->
<c:if test="${not empty manuscript
          and (sessionScope.currentUser.roleCode == 'EDITOR'
               or sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF')
          and (manuscript.currentStatus == 'WITH_EDITOR'
               or manuscript.currentStatus == 'UNDER_REVIEW'
               or manuscript.currentStatus == 'EDITOR_RECOMMENDATION'
               or manuscript.currentStatus == 'FINAL_DECISION_PENDING')}">

    <!-- 作者推荐审稿人：在稿件工作台直接可见，减少“详情/选择页面”来回跳转的冗余 -->
    <h3>作者推荐审稿人</h3>
    <c:if test="${empty recommendedReviewers}">
        <p>作者未推荐审稿人。</p>
    </c:if>
    <c:if test="${not empty recommendedReviewers}">
        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff;">
            <thead>
            <tr>
                <th>#</th>
                <th>姓名</th>
                <th>邮箱</th>
                <th>推荐理由</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${recommendedReviewers}" var="rr" varStatus="st">
                <tr>
                    <td><c:out value="${st.index + 1}"/></td>
                    <td><c:out value="${rr.reviewerName}"/></td>
                    <td><c:out value="${rr.reviewerEmail}"/></td>
                    <td><c:out value="${rr.reason}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    <h3>当前审稿记录</h3>

    <c:if test="${empty reviews}">
        <p>当前尚未有任何审稿邀请。</p>
    </c:if>

    <c:if test="${not empty reviews}">
        <script type="text/javascript">
            function toggleReviewDetail(id) {
                var el = document.getElementById(id);
                if (!el) return;
                el.style.display = (el.style.display === 'none' || el.style.display === '') ? 'block' : 'none';
            }
        </script>

        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff;">
            <thead>
            <tr>
                <th>审稿人</th>
                <th>状态</th>
                <th>邀请时间</th>
                <th>截止时间</th>
                <th>最后催审时间</th>
                <th>催审次数</th>
                <th>推荐结论</th>
                <th>评审详情</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
<c:forEach items="${reviews}" var="r">
    <tr>
        <td>
            <c:choose>
                <c:when test="${not empty r.reviewerName}">
                    <c:out value="${r.reviewerName}"/>
                    (<c:out value="${r.reviewerEmail}"/>)
                </c:when>
                <c:otherwise>
                    审稿人ID: <c:out value="${r.reviewerId}"/>
                </c:otherwise>
            </c:choose>
        </td>
        <td>
            <c:out value="${r.status}"/>
            <c:if test="${r.status == 'EXPIRED'}">
                <div style="margin:6px 0;padding:6px 10px;border:1px solid #ffc107;border-radius:6px;background:#fff3cd;color:#856404;">
                    <strong>拒绝理由：</strong>
                    <c:out value="${r.rejectionReason}"/>
                    <br/>
                    <small>拒绝时间：<c:out value="${r.declinedAt}"/></small>
                </div>
            </c:if>
        </td>
        <td><c:out value="${r.invitedAt}"/></td>
        <td><c:out value="${r.dueAt}"/></td>
        <td><c:out value="${r.lastRemindedAt}"/></td>
        <td><c:out value="${r.remindCount}"/></td>
        <td><c:out value="${r.recommendation}"/></td>
        <td>
            <c:choose>
                <c:when test="${r.status == 'SUBMITTED'}">
                    <div>
                        总体分：<c:out value="${r.score}"/>
                        <span style="margin:0 8px;">|</span>
                        <a href="javascript:void(0)" onclick="toggleReviewDetail('detail${r.reviewId}')">查看/隐藏</a>
                        &nbsp;|&nbsp;
                        <a href="${ctx}/editor/review/detail?reviewId=${r.reviewId}">单页查看</a>
                    </div>
                    <div id="detail${r.reviewId}" style="display:none; margin-top:8px;">
                        <p><strong>关键评价 KeyEvaluation：</strong></p>
                        <div style="border:1px solid #ddd; padding:8px; background:#fafafa;">
                            <c:out value="${r.keyEvaluation}"/>
                        </div>
                        <p><strong>给编辑的保密意见 ConfidentialToEditor：</strong></p>
                        <div style="border:1px solid #ddd; padding:8px; background:#fafafa;">
                            <c:out value="${r.confidentialToEditor}"/>
                        </div>
                        <p><strong>给作者的意见 Content：</strong></p>
                        <div style="border:1px solid #ddd; padding:8px; background:#fafafa;">
                            <c:out value="${r.content}"/>
                        </div>
                    </div>
                </c:when>
                <c:when test="${r.status == 'EXPIRED'}">
                    <span style="color:#856404;">审稿人已拒绝邀请</span>
                </c:when>
                <c:otherwise>-</c:otherwise>
            </c:choose>
        </td>
        <td>
            <c:choose>
                <c:when test="${r.status == 'INVITED' || r.status == 'ACCEPTED'}">
                    <form method="post" action="${ctx}/editor/review/remind" style="display:inline">
                        <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                        <input type="hidden" name="backTo" value="${backToUrl}"/>
                        <button type="submit">催审</button>
                    </form>
                    <form method="post" action="${ctx}/editor/review/cancel" style="display:inline; margin-left:6px;" onsubmit="return confirm('确认撤回该审稿人分配？')">
                        <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                        <input type="hidden" name="backTo" value="${backToUrl}"/>
                        <button type="submit">撤回分配</button>
                    </form>
                </c:when>
                <c:when test="${r.status == 'SUBMITTED'}">
                    <a href="${ctx}/editor/review/detail?reviewId=${r.reviewId}">查看详细评价</a>
                </c:when>
                <c:otherwise>-</c:otherwise>
            </c:choose>
        </td>
    </tr>
</c:forEach>

        </tbody>
        </table>
    </c:if>

    <h3 id="inviteReviewers">审稿人管理</h3>
    <p style="margin:8px 0; color:#666;">本页已合并“稿件详情”与“审稿人选择”页面：可在此直接搜索审稿人、邀请/撤回/催审，并查看已提交的详细评价，避免页面来回跳转与功能重复。</p>

    <!-- ====================== 从审稿人库中搜索并邀请 ====================== -->
    <h4 style="margin-top:12px;">从审稿人库中选择</h4>

    <form method="get" action="${ctx}/manuscripts/detail" style="max-width:960px; background:#fff; border:1px solid #e5e7eb; padding:10px 12px; border-radius:8px;">
        <input type="hidden" name="id" value="${manuscript.manuscriptId}"/>
        <div style="display:flex; flex-wrap:wrap; gap:10px; align-items:flex-end;">
            <div>
                <label><b>关键词</b><br/>
                    <input type="text" name="reviewerKeyword" value="${fn:escapeXml(param.reviewerKeyword)}" style="width:260px;" placeholder="姓名/邮箱/研究方向"/>
                </label>
            </div>
            <div>
                <label><b>最低完成审稿数</b><br/>
                    <input type="number" name="minCompleted" value="${fn:escapeXml(param.minCompleted)}" style="width:140px;" min="0"/>
                </label>
            </div>
            <div>
                <label><b>最低平均分</b><br/>
                    <input type="number" name="minAvgScore" value="${fn:escapeXml(param.minAvgScore)}" style="width:140px;" min="0" max="100"/>
                </label>
            </div>
            <div>
                <button type="submit">搜索</button>
                <a class="btn btn-quiet" href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}#inviteReviewers" style="margin-left:6px;">重置</a>
            </div>
        </div>
        <c:if test="${not empty reviewerSuggestionKeyword}">
            <div style="margin-top:8px; color:#666; font-size:12px;">提示：系统已根据研究主题/关键词进行智能推荐，可直接查看下方候选审稿人。</div>
        </c:if>
    </form>

    <c:if test="${not empty reviewers}">
        <form method="post" action="${ctx}/editor/review/invite" style="max-width:960px; margin-top:10px;">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
            <input type="hidden" name="backTo" value="${backToUrl}"/>

            <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; width:100%;">
                <thead>
                <tr>
                    <th style="width:60px;">选择</th>
                    <th>姓名</th>
                    <th>用户名</th>
                    <th>邮箱</th>
                    <th>研究方向</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${reviewers}" var="u">
                    <tr>
                        <td style="text-align:center;">
                            <input type="checkbox" name="reviewerIds" value="${u.userId}"
                                   <c:if test="${not empty assignedReviewerIds and assignedReviewerIds.contains(u.userId)}">disabled="disabled"</c:if> />
                            <c:if test="${not empty assignedReviewerIds and assignedReviewerIds.contains(u.userId)}">
                                <span style="color:#999; font-size:12px;">已分配</span>
                            </c:if>
                        </td>
                        <td><c:out value="${u.fullName}"/></td>
                        <td><c:out value="${u.username}"/></td>
                        <td><c:out value="${u.email}"/></td>
                        <td><c:out value="${u.researchArea}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <p style="margin-top:10px;">
                <label>截止日期（可选）：
                    <input type="date" name="dueDate"/>
                </label>
            </p>

            <p style="margin-top:10px;">
                <button type="submit">向选中审稿人发出邀请</button>
            </p>
        </form>
    </c:if>

    <c:if test="${empty reviewers}">
        <p style="max-width:960px; color:#666; margin-top:10px;">未命中搜索条件，或审稿人库为空。请调整搜索条件，或由主编在“审稿人库管理”中添加审稿人。</p>
    </c:if>

    <hr style="margin:22px 0; max-width:960px;"/>

    <!-- ====================== 邀请外部审稿人：创建账号并发送邮件 ====================== -->
    <h4 style="margin-top:12px;">邀请外部审稿人（创建账号并邮件邀请）</h4>
    <p style="color:#666; max-width:960px;">当审稿人不在现有审稿人库中时，可在此创建审稿人账号，并向其发送账户信息及本稿件的审稿邀请邮件。</p>

    <form method="post" action="${ctx}/editor/review/inviteExternal" style="max-width:960px; background:#fff; border:1px solid #e5e7eb; padding:12px 14px; border-radius:8px;">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
        <input type="hidden" name="backTo" value="${backToUrl}"/>

        <table cellpadding="6" cellspacing="0" style="width:100%;">
            <tr>
                <td style="width:140px;"><b>用户名 *</b></td>
                <td><input type="text" name="username" required="required" style="width:280px;" placeholder="例如 reviewer_zhang"/></td>
            </tr>
            <tr>
                <td><b>初始密码 *</b></td>
                <td><input type="text" name="password" required="required" style="width:280px;" placeholder="请设置一个初始密码"/></td>
            </tr>
            <tr>
                <td><b>姓名</b></td>
                <td><input type="text" name="fullName" style="width:280px;" placeholder="可选"/></td>
            </tr>
            <tr>
                <td><b>邮箱 *</b></td>
                <td><input type="email" name="email" required="required" style="width:360px;" placeholder="example@university.edu"/></td>
            </tr>
            <tr>
                <td><b>单位/机构</b></td>
                <td><input type="text" name="affiliation" style="width:360px;" placeholder="可选"/></td>
            </tr>
            <tr>
                <td><b>研究方向</b></td>
                <td><input type="text" name="researchArea" style="width:360px;" placeholder="可选"/></td>
            </tr>
            <tr>
                <td><b>截止日期（可选）</b></td>
                <td><input type="date" name="dueDate"/></td>
            </tr>
        </table>

        <div style="margin-top:10px;">
            <button type="submit">创建并邀请外部审稿人</button>
        </div>
    </form>

<h3>与作者沟通</h3>
    <p>
        <a href="${ctx}/editor/author/message?manuscriptId=${manuscript.manuscriptId}">发送消息给作者</a>
        <span style="margin-left:8px; color:#666;">支持站内消息/邮件，可抄送主编；下方按时间线展示沟通历史。</span>
    </p>

    <c:if test="${empty authorMessages}">
        <p>暂无沟通记录。</p>
    </c:if>
    <c:if test="${not empty authorMessages}">
        <div style="border-left:3px solid #ddd; padding-left:12px; margin: 8px 0 16px 0;">
            <c:forEach items="${authorMessages}" var="msg">
                <div style="margin: 10px 0;">
                    <div style="color:#666; font-size:12px;">
                        <c:out value="${msg.createdAt}"/>
                        <span style="margin:0 6px;">·</span>
                        <strong>
                            <c:out value="${authorMessageUserMap[msg.createdByUserId].fullName}"/>
                        </strong>
                        <span style="margin:0 6px;">→</span>
                        <strong>
                            <c:out value="${authorMessageUserMap[msg.recipientUserId].fullName}"/>
                        </strong>
                        <span style="margin-left:8px;">[<c:out value="${msg.category}"/>/<c:out value="${msg.type}"/>]</span>
                    </div>
                    <div style="margin-top:4px;">
                        <div><strong><c:out value="${msg.title}"/></strong></div>
                        <div style="white-space:pre-wrap;"> <c:out value="${msg.content}"/></div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:if>
</c:if>

<!-- ====================== 作者查看：仅展示给作者的意见（Content） ====================== -->
<c:if test="${not empty manuscript
          and sessionScope.currentUser.roleCode == 'AUTHOR'
          and manuscript.currentStatus != 'DRAFT'}">

    <h3>审稿意见（给作者的意见）</h3>

    <c:set var="hasSubmitted" value="false"/>
    <c:forEach items="${reviews}" var="rr">
        <c:if test="${rr.status == 'SUBMITTED'}">
            <c:set var="hasSubmitted" value="true"/>
        </c:if>
    </c:forEach>

    <c:if test="${empty reviews || !hasSubmitted}">
        <p>目前暂无可展示的审稿意见（审稿尚未提交）。</p>
    </c:if>

    <c:if test="${not empty reviews && hasSubmitted}">
        <table border="1" cellpadding="4" cellspacing="0" style="background:#fff; max-width: 980px; width:100%;">
            <thead>
            <tr>
                <th style="width: 120px;">推荐结论</th>
                <th style="width: 80px;">总体分</th>
                <th>给作者的意见（Content）</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${reviews}" var="r">
                <c:if test="${r.status == 'SUBMITTED'}">
                    <tr>
                        <td><c:out value="${r.recommendation}"/></td>
                        <td><c:out value="${r.score}"/></td>
                        <td style="white-space:pre-wrap;"><c:out value="${r.content}"/></td>
                    </tr>
                </c:if>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
