<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>稿件详情</h2>

<c:if test="${empty manuscript}">
    <p>未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">
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
                <div style="border:1px solid #eee; padding:8px; min-height:60px;">
                    <c:out value="${manuscript.abstractText}" escapeXml="false"/>
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
                    <c:if test="${not empty currentVersion.fileOriginalPath}">
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">Manuscript 预览/下载</a>
                    </c:if>
                    <c:if test="${not empty currentVersion.coverLetterPath}">
                        <span style="margin-left:12px;"></span>
                        <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">Cover Letter 预览/下载</a>
                    </c:if>
                </c:if>
                <c:if test="${empty currentVersion}">
                    暂无版本文件
                </c:if>
            </td>
        </tr>
    </table>

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
        请先在下方对稿件信息进行修改（包括元数据、作者列表、文件等），确认无误后再点击
        <strong>Resubmit（重新提交）</strong>。
    </p>

    <c:if test="${not empty error}">
        <div style="padding:8px;border:1px solid #c00;color:#c00;margin:10px 0;">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form id="resubmitForm" action="${ctx}/manuscripts/resubmit" method="post" enctype="multipart/form-data">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

        <fieldset style="margin:12px 0;">
            <legend><b>1. 元数据</b></legend>

            <p>
                <label>标题：
                    <input type="text" name="title" size="80" value="<c:out value='${manuscript.title}'/>" required/>
                </label>
            </p>

            <p>
                <label>期刊ID：
                    <input type="number" name="journalId" value="<c:out value='${manuscript.journalId}'/>" />
                </label>
            </p>

            <p>
                <label>研究主题：
                    <input type="text" name="subjectArea" size="80" value="<c:out value='${manuscript.subjectArea}'/>"/>
                </label>
            </p>

            <p>
                <label>项目资助情况：</label><br/>
                <textarea name="fundingInfo" rows="3" cols="80"><c:out value="${manuscript.fundingInfo}"/></textarea>
            </p>

            <p>
                <label>关键词：</label><br/>
                <input type="text" name="keywords" size="80" value="<c:out value='${manuscript.keywords}'/>"/>
            </p>

            <p>
                <label>摘要（支持富文本）：</label>
                <div id="abstractEditor2" contenteditable="true"
                     style="border:1px solid #ccc; padding:8px; min-height:120px; background:#fff;">
                    <c:out value="${manuscript.abstractText}" escapeXml="false"/>
                </div>
                <input type="hidden" id="abstractHidden2" name="abstract"/>
            </p>
        </fieldset>

        <fieldset style="margin:12px 0;">
            <legend><b>2. 作者列表</b></legend>

            <table id="authorsTable2" border="1" cellpadding="4" cellspacing="0" style="width:100%; background:#fff;">
                <thead>
                <tr>
                    <th style="width:60px;">顺序</th>
                    <th style="width:90px;">通讯作者</th>
                    <th>姓名</th>
                    <th>单位</th>
                    <th style="width:90px;">学历</th>
                    <th style="width:90px;">职称</th>
                    <th style="width:120px;">职位</th>
                    <th style="width:180px;">邮箱</th>
                    <th style="width:80px;">操作</th>
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
                                <td><input type="text" name="authorName" value="<c:out value='${a.fullName}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="authorAffiliation" value="<c:out value='${a.affiliation}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="authorDegree" value="<c:out value='${a.degree}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="authorTitle" value="<c:out value='${a.title}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="authorPosition" value="<c:out value='${a.position}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="authorEmail" value="<c:out value='${a.email}'/>" style="width:98%;"/></td>
                                <td style="text-align:center;">
                                    <button type="button" onclick="removeRow2(this)">删除</button>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td>1</td>
                            <td style="text-align:center;"><input type="radio" name="correspondingIndex" value="0" checked/></td>
                            <td><input type="text" name="authorName" style="width:98%;" placeholder="作者姓名"/></td>
                            <td><input type="text" name="authorAffiliation" style="width:98%;" placeholder="单位/学院"/></td>
                            <td><input type="text" name="authorDegree" style="width:98%;" placeholder="本科/硕士/博士"/></td>
                            <td><input type="text" name="authorTitle" style="width:98%;" placeholder="讲师/副教授/教授"/></td>
                            <td><input type="text" name="authorPosition" style="width:98%;" placeholder="职位"/></td>
                            <td><input type="text" name="authorEmail" style="width:98%;" placeholder="邮箱"/></td>
                            <td style="text-align:center;"><button type="button" onclick="removeRow2(this)">删除</button></td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>

            <div style="margin-top:8px;">
                <button type="button" onclick="addAuthorRow2()">+ 添加作者</button>
            </div>
        </fieldset>

        <fieldset style="margin:12px 0;">
            <legend><b>3. 文件上传</b></legend>

            <p>
                <label>上传修回后的手稿文件（可选）：</label>
                <input type="file" name="manuscriptFile" accept=".pdf,.doc,.docx"/>
                <c:if test="${not empty currentVersion and not empty currentVersion.fileOriginalPath}">
                    <span style="margin-left:10px;">
                        当前版本：<a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">预览/下载</a>
                    </span>
                </c:if>
            </p>

            <p>
                <label>上传修回后的 Cover Letter（可选）：</label>
                <input type="file" name="coverFile" accept=".pdf,.doc,.docx,.html,.htm"/>
                <c:if test="${not empty currentVersion and not empty currentVersion.coverLetterPath}">
                    <span style="margin-left:10px;">
                        当前版本：<a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">预览/下载</a>
                    </span>
                </c:if>
            </p>

            <p>
                <label>Cover Letter（富文本，可选）：</label>
                <div id="coverEditor2" contenteditable="true"
                     style="border:1px solid #ccc; padding:8px; min-height:120px; background:#fff;"></div>
                <input type="hidden" id="coverHidden2" name="coverLetterHtml"/>
            </p>
        </fieldset>

        <fieldset style="margin:12px 0;">
            <legend><b>4. 推荐审稿人（可选）</b></legend>

            <table id="reviewersTable2" border="1" cellpadding="4" cellspacing="0" style="width:100%; background:#fff;">
                <thead>
                <tr>
                    <th style="width:40px;">#</th>
                    <th style="width:180px;">姓名</th>
                    <th style="width:220px;">邮箱</th>
                    <th>推荐理由</th>
                    <th style="width:80px;">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${not empty recommendedReviewers}">
                        <c:forEach var="r" items="${recommendedReviewers}" varStatus="st">
                            <tr>
                                <td><c:out value="${st.index + 1}"/></td>
                                <td><input type="text" name="recReviewerName" value="<c:out value='${r.fullName}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="recReviewerEmail" value="<c:out value='${r.email}'/>" style="width:98%;"/></td>
                                <td><input type="text" name="recReviewerReason" value="<c:out value='${r.reason}'/>" style="width:98%;"/></td>
                                <td style="text-align:center;"><button type="button" onclick="removeRow2(this)">删除</button></td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td>1</td>
                            <td><input type="text" name="recReviewerName" style="width:98%;" placeholder="姓名"/></td>
                            <td><input type="text" name="recReviewerEmail" style="width:98%;" placeholder="邮箱"/></td>
                            <td><input type="text" name="recReviewerReason" style="width:98%;" placeholder="推荐理由"/></td>
                            <td style="text-align:center;"><button type="button" onclick="removeRow2(this)">删除</button></td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>

            <div style="margin-top:8px;">
                <button type="button" onclick="addReviewerRow2()">+ 添加推荐审稿人</button>
            </div>
        </fieldset>

        <p style="margin-top:14px;">
            <button type="submit" onclick="return confirm('确认 Resubmit？将进入后续处理流程。');">Resubmit（重新提交）</button>
        </p>
    </form>

    <script>
        function removeRow2(btn) {
            var tr = btn.closest('tr');
            if (!tr) return;
            var tbody = tr.parentNode;
            tbody.removeChild(tr);
            renumberTables2();
        }

        function addAuthorRow2() {
            var tbody = document.querySelector('#authorsTable2 tbody');
            var index = tbody.querySelectorAll('tr').length;
            var tr = document.createElement('tr');
            tr.innerHTML = '' +
                '<td>' + (index + 1) + '</td>' +
                '<td style="text-align:center;"><input type="radio" name="correspondingIndex" value="' + index + '"></td>' +
                '<td><input type="text" name="authorName" style="width:98%;" placeholder="作者姓名"></td>' +
                '<td><input type="text" name="authorAffiliation" style="width:98%;" placeholder="单位/学院"></td>' +
                '<td><input type="text" name="authorDegree" style="width:98%;" placeholder="本科/硕士/博士"></td>' +
                '<td><input type="text" name="authorTitle" style="width:98%;" placeholder="讲师/副教授/教授"></td>' +
                '<td><input type="text" name="authorPosition" style="width:98%;" placeholder="职位"></td>' +
                '<td><input type="text" name="authorEmail" style="width:98%;" placeholder="邮箱"></td>' +
                '<td style="text-align:center;"><button type="button" onclick="removeRow2(this)">删除</button></td>';
            tbody.appendChild(tr);
        }

        function addReviewerRow2() {
            var tbody = document.querySelector('#reviewersTable2 tbody');
            var index = tbody.querySelectorAll('tr').length;
            var tr = document.createElement('tr');
            tr.innerHTML = '' +
                '<td>' + (index + 1) + '</td>' +
                '<td><input type="text" name="recReviewerName" style="width:98%;" placeholder="姓名"></td>' +
                '<td><input type="text" name="recReviewerEmail" style="width:98%;" placeholder="邮箱"></td>' +
                '<td><input type="text" name="recReviewerReason" style="width:98%;" placeholder="推荐理由"></td>' +
                '<td style="text-align:center;"><button type="button" onclick="removeRow2(this)">删除</button></td>';
            tbody.appendChild(tr);
        }

        function renumberTables2() {
            var authorRows = document.querySelectorAll('#authorsTable2 tbody tr');
            authorRows.forEach(function(tr, idx) {
                tr.children[0].innerText = (idx + 1);
                var radio = tr.querySelector('input[type=radio][name=correspondingIndex]');
                if (radio) radio.value = idx;
            });

            var reviewerRows = document.querySelectorAll('#reviewersTable2 tbody tr');
            reviewerRows.forEach(function(tr, idx) {
                tr.children[0].innerText = (idx + 1);
            });
        }

        document.getElementById('resubmitForm').addEventListener('submit', function() {
            document.getElementById('abstractHidden2').value = document.getElementById('abstractEditor2').innerHTML;
            document.getElementById('coverHidden2').value = document.getElementById('coverEditor2').innerHTML;
        });
    </script>
</c:if>

<!-- ====================== 只有编辑 / 主编 且 稿件已进入外审相关阶段 时显示审稿相关内容 ====================== -->
<c:if test="${not empty manuscript
          and (sessionScope.currentUser.roleCode == 'EDITOR'
               or sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF')
          and (manuscript.currentStatus == 'WITH_EDITOR'
               or manuscript.currentStatus == 'UNDER_REVIEW'
               or manuscript.currentStatus == 'EDITOR_RECOMMENDATION'
               or manuscript.currentStatus == 'FINAL_DECISION_PENDING')}">


    <h3>当前审稿记录</h3>

    <c:if test="${empty reviews}">
        <p>当前尚未有任何审稿邀请。</p>
    </c:if>

    <c:if test="${not empty reviews}">
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
                    <td><c:out value="${r.status}"/></td>
                    <td><c:out value="${r.invitedAt}"/></td>
                    <td><c:out value="${r.dueAt}"/></td>
                    <td><c:out value="${r.lastRemindedAt}"/></td>
                    <td><c:out value="${r.remindCount}"/></td>
                    <td><c:out value="${r.recommendation}"/></td>
                    <td>
                        <c:if test="${r.status != 'SUBMITTED'}">
                            <form method="post" action="${ctx}/editor/review/remind" style="display:inline">
                                <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                                <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                                <button type="submit">催审</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    <h3>邀请新的审稿人</h3>
    <c:if test="${empty reviewers}">
        <p>当前审稿人库为空，请先由主编在“审稿人库管理”中添加审稿人。</p>
    </c:if>
    <c:if test="${not empty reviewers}">
        <form method="post" action="${ctx}/editor/review/invite">
            <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

            <label>选择审稿人（可多选，按 Ctrl/Shift 多选）：
                <select name="reviewerIds" multiple="multiple" size="5" required>
                    <option value="">-- 请选择 --</option>
                    <c:forEach items="${reviewers}" var="u">
                        <option value="${u.userId}">
                            <c:out value="${u.fullName}"/>
                            （用户名：<c:out value="${u.username}"/>）
                        </option>
                    </c:forEach>
                </select>
            </label>

            <label>截止日期（可选）：
                <input type="date" name="dueDate"/>
            </label>

            <button type="submit">发出审稿邀请</button>
        </form>
    </c:if>

    <h3>与作者和审稿人沟通（备注说明）</h3>
    <p>课程设计阶段不实现真实邮件发送，可在操作时通过“备注 / 日志”记录沟通要点，
        教师检查数据库中的 Logs / ManuscriptStatusHistory 表即可了解沟通情况。</p>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
