<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:set var="backToUrl" value="${ctx}/editor/recommend/detail?manuscriptId=${manuscript.manuscriptId}#reviews"/>

<c:if test="${not empty param.cancelMsg}">
    <div style="margin:10px 0; padding:10px 12px; border:1px solid #b7eb8f; background:#f6ffed; color:#135200; border-radius:6px;">
        <c:out value="${param.cancelMsg}"/>
    </div>
</c:if>

<h2>稿件详情（提出建议参考）</h2>

<p style="margin:6px 0 12px; color:#666;">
    该页面专用于“提出建议”模块中的“查看稿件详情”，展示稿件元数据、作者列表、版本文件、形式审查结果及外审信息。
</p>

<p style="margin:8px 0 14px;">
    <a class="btn btn-quiet" href="${ctx}/editor/recommend">返回提出建议列表</a>
    <a class="btn" href="${ctx}/manuscripts/detail?id=${manuscript.manuscriptId}">打开合并后的工作台详情页</a>
</p>

<c:if test="${empty manuscript}">
    <p>未找到稿件记录。</p>
</c:if>

<c:if test="${not empty manuscript}">

    <h3>基本信息</h3>
    <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
        <tr><th style="width:160px;">稿件编号</th><td><c:out value="${manuscript.manuscriptId}"/></td></tr>
        <tr><th>标题</th><td><c:out value="${manuscript.title}"/></td></tr>
        <tr><th>研究主题</th><td><c:out value="${manuscript.subjectArea}"/></td></tr>
        <tr><th>关键词</th><td><c:out value="${manuscript.keywords}"/></td></tr>
        <tr><th>资助信息</th><td><c:out value="${manuscript.fundingInfo}"/></td></tr>
        <tr><th>当前状态</th><td><c:out value="${manuscript.currentStatus}"/></td></tr>
        <tr><th>投稿时间</th><td><c:out value="${manuscript.submitTime}"/></td></tr>
        <tr><th>决策</th><td><c:out value="${manuscript.decision}"/></td></tr>
        <tr><th>摘要</th><td>
                <div class="ql-snow richtext-view">
                    <div class="ql-editor">
                        <c:out value="${manuscript.abstractText}" escapeXml="false"/>
                    </div>
                </div>
            </td></tr>
    </table>

    <h3>作者列表</h3>
    <c:if test="${empty authors}">
        <p>暂无作者信息。</p>
    </c:if>
    <c:if test="${not empty authors}">
        <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
            <thead>
            <tr>
                <th style="width:64px;">序号</th>
                <th>姓名</th>
                <th>单位</th>
                <th>Email</th>
                <th style="width:90px;">通讯作者</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${authors}" var="a" varStatus="st">
                <tr>
                    <td><c:out value="${st.index + 1}"/></td>
                    <td><c:out value="${a.fullName}"/></td>
                    <td><c:out value="${a.affiliation}"/></td>
                    <td><c:out value="${a.email}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${a.corresponding}">是</c:when>
                            <c:otherwise>否</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    <h3>当前版本文件</h3>
    <c:if test="${empty currentVersion}">
        <p>未找到版本文件。</p>
    </c:if>
    <c:if test="${not empty currentVersion}">
        <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
            <tr><th style="width:160px;">版本号</th><td><c:out value="${currentVersion.versionNumber}"/></td></tr>
            <tr><th>匿名稿路径</th><td><c:out value="${currentVersion.fileAnonymousPath}"/></td></tr>
            <tr><th>原稿路径</th><td><c:out value="${currentVersion.fileOriginalPath}"/></td></tr>
            <tr><th>Cover Letter</th><td><c:out value="${currentVersion.coverLetterPath}"/></td></tr>
            <tr><th>Response Letter</th><td><c:out value="${currentVersion.responseLetterPath}"/></td></tr>
            <tr><th>创建时间</th><td><c:out value="${currentVersion.createdAt}"/></td></tr>
            <tr><th>备注</th><td><c:out value="${currentVersion.remark}"/></td></tr>
        </table>
    </c:if>

    <h3>形式审查结果</h3>
    <c:if test="${empty formalCheckResult}">
        <p>暂无形式审查记录。</p>
    </c:if>
    <c:if test="${not empty formalCheckResult}">
        <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
            <tr><th style="width:160px;">结果</th><td><c:out value="${formalCheckResult.checkResult}"/></td></tr>
            <tr><th>查重率(%)</th><td><c:out value="${formalCheckResult.similarityScore}"/></td></tr>
            <tr><th>高相似度</th>
                <td>
                    <c:choose>
                        <c:when test="${formalCheckResult.highSimilarity}">是</c:when>
                        <c:otherwise>否</c:otherwise>
                    </c:choose>
                </td>
            </tr>
            <tr><th>查重报告</th><td><c:out value="${formalCheckResult.plagiarismReportUrl}"/></td></tr>
            <tr><th>反馈意见</th><td><c:out value="${formalCheckResult.feedback}"/></td></tr>
            <tr><th>审查时间</th><td><c:out value="${formalCheckResult.checkTime}"/></td></tr>
        </table>

        <p style="margin:8px 0 0; color:#666;">（下表为系统/人工检查项明细）</p>
        <table border="1" cellpadding="4" cellspacing="0" style="width:100%; margin-top:6px;">
            <tr><th style="width:220px;">作者信息</th><td><c:out value="${formalCheckResult.authorInfoValid}"/></td></tr>
            <tr><th>摘要字数</th><td><c:out value="${formalCheckResult.abstractWordCountValid}"/></td></tr>
            <tr><th>正文字数</th><td><c:out value="${formalCheckResult.bodyWordCountValid}"/></td></tr>
            <tr><th>关键词</th><td><c:out value="${formalCheckResult.keywordsValid}"/></td></tr>
            <tr><th>注释编号</th><td><c:out value="${formalCheckResult.footnoteNumberingValid}"/></td></tr>
            <tr><th>图表格式</th><td><c:out value="${formalCheckResult.figureTableFormatValid}"/></td></tr>
            <tr><th>参考文献格式</th><td><c:out value="${formalCheckResult.referenceFormatValid}"/></td></tr>
        </table>
    </c:if>

    <h3>作者推荐审稿人</h3>
    <c:if test="${empty recommendedReviewers}">
        <p>作者未推荐审稿人。</p>
    </c:if>
    <c:if test="${not empty recommendedReviewers}">
        <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
            <thead>
            <tr>
                <th style="width:64px;">序号</th>
                <th>姓名</th>
                <th>Email</th>
                <th>推荐理由</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${recommendedReviewers}" var="rr" varStatus="st">
                <tr>
                    <td><c:out value="${st.index + 1}"/></td>
                    <td><c:out value="${rr.fullName}"/></td>
                    <td><c:out value="${rr.email}"/></td>
                    <td><c:out value="${rr.reason}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    <h3 id="reviews">外审记录与已提交意见</h3>
    <c:if test="${empty reviews}">
        <p>暂无审稿记录。</p>
    </c:if>
    <c:if test="${not empty reviews}">
        <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
            <thead>
            <tr>
                <th style="width:64px;">ID</th>
                <th>审稿人</th>
                <th>状态</th>
                <th>邀请时间</th>
                <th>接受时间</th>
                <th>截止时间</th>
                <th>提交时间</th>
                <th style="width:90px;">得分</th>
                <th style="width:140px;">推荐意见</th>
                <th style="width:140px;">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${reviews}" var="r">
                <tr>
                    <td><c:out value="${r.reviewId}"/></td>
                    <td>
                        <c:out value="${r.reviewerName}"/>
                        <c:if test="${not empty r.reviewerEmail}">
                            <span style="color:#666;">（<c:out value="${r.reviewerEmail}"/>）</span>
                        </c:if>
                    </td>
                    <td><c:out value="${r.status}"/></td>
                    <td><c:out value="${r.invitedAt}"/></td>
                    <td><c:out value="${r.acceptedAt}"/></td>
                    <td><c:out value="${r.dueAt}"/></td>
                    <td><c:out value="${r.submittedAt}"/></td>
                    <td><c:out value="${r.score}"/></td>
                    <td><c:out value="${r.recommendation}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${r.status == 'INVITED' || r.status == 'ACCEPTED'}">
                                <form method="post" action="${ctx}/editor/review/cancel" style="display:inline" onsubmit="return confirm('确认取消该审稿人邀请/分配？')">
                                    <input type="hidden" name="reviewId" value="${r.reviewId}"/>
                                    <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>
                                    <input type="hidden" name="backTo" value="${backToUrl}"/>
                                    <button type="submit">取消邀请</button>
                                </form>
                            </c:when>
                            <c:when test="${r.status == 'SUBMITTED'}">
                                <a href="${ctx}/editor/review/detail?reviewId=${r.reviewId}">查看详细评价</a>
                            </c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <c:if test="${r.status == 'SUBMITTED'}">
                    <tr>
                        <td colspan="10" style="background:#fafafa;">
                            <div style="margin:4px 0;"><strong>关键评价：</strong><c:out value="${r.keyEvaluation}"/></div>
                            <div style="margin:4px 0;"><strong>给编辑保密意见：</strong><c:out value="${r.confidentialToEditor}"/></div>
                            <div style="margin:4px 0;"><strong>评审内容：</strong><c:out value="${r.content}"/></div>
                        </td>
                    </tr>
                </c:if>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

</c:if>
