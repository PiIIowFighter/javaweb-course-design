<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>审稿意见详情</h2>

<c:if test="${empty review}">
    <p style="color:#d00;">未找到审稿记录。</p>
</c:if>

<c:if test="${not empty review}">
    <div class="card" style="max-width: 1100px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">基本信息</h3>
                <p class="card-subtitle">
                    ReviewId：<strong><c:out value="${review.reviewId}"/></strong>
                    &nbsp;&nbsp;|&nbsp;&nbsp;
                    ManuscriptId：<strong><c:out value="${review.manuscriptId}"/></strong>
                    &nbsp;&nbsp;|&nbsp;&nbsp;
                    状态：<strong><c:out value="${review.status}"/></strong>
                </p>
            </div>
        </div>
        <div class="card-body">
            <p>
                <strong>审稿人：</strong>
                <c:out value="${review.reviewerName}"/>
                <c:if test="${not empty review.reviewerEmail}">
                    &lt;<c:out value="${review.reviewerEmail}"/>&gt;
                </c:if>
            </p>
            <p><strong>提交时间：</strong><c:out value="${review.submittedAt}"/></p>

            <c:if test="${not empty manuscript}">
                <p><strong>稿件标题：</strong><c:out value="${manuscript.title}"/></p>
            </c:if>

            <p style="margin-top: 10px;">
                <a href="${ctx}/manuscripts/detail?id=${review.manuscriptId}">查看稿件详情</a>
                &nbsp;|&nbsp;
                <a href="${ctx}/editor/recommend?manuscriptId=${review.manuscriptId}">返回提出建议</a>
            </p>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">审稿结论</h3>
            </div>
        </div>
        <div class="card-body">
            <p><strong>Recommendation：</strong><c:out value="${review.recommendation}"/></p>
            <p><strong>总体分（Overall Score）：</strong><c:out value="${review.score}"/></p>

            <div style="overflow:auto; margin-top: 10px;">
                <table border="1" cellpadding="6" cellspacing="0" style="width:100%; min-width: 720px;">
                    <thead>
                    <tr>
                        <th style="width: 260px;">评分维度</th>
                        <th style="width: 120px;">分值（0-10）</th>
                        <th>说明</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td><strong>原创性（Originality）</strong></td>
                        <td><c:out value="${review.scoreOriginality}" default="--"/></td>
                        <td>研究是否新颖、是否提出新的观点/方法/发现</td>
                    </tr>
                    <tr>
                        <td><strong>重要性（Significance）</strong></td>
                        <td><c:out value="${review.scoreSignificance}" default="--"/></td>
                        <td>对领域影响、潜在价值与贡献</td>
                    </tr>
                    <tr>
                        <td><strong>方法/技术质量（Methodology）</strong></td>
                        <td><c:out value="${review.scoreMethodology}" default="--"/></td>
                        <td>方法合理性、实验/推导严谨性、可复现性</td>
                    </tr>
                    <tr>
                        <td><strong>表达/结构（Presentation）</strong></td>
                        <td><c:out value="${review.scorePresentation}" default="--"/></td>
                        <td>写作清晰度、逻辑结构、图表与排版质量</td>
                    </tr>
                    <tr>
                        <td><strong>实验设计/实施（Experimentation）</strong></td>
                        <td><c:out value="${review.scoreExperimentation}" default="--"/></td>
                        <td>实验设置是否充分、对比与消融是否到位、实现是否可信</td>
                    </tr>
                    <tr>
                        <td><strong>文献综述（Literature Review）</strong></td>
                        <td><c:out value="${review.scoreLiteratureReview}" default="--"/></td>
                        <td>相关工作覆盖度、对比与定位是否准确</td>
                    </tr>
                    <tr>
                        <td><strong>结论与讨论（Conclusions）</strong></td>
                        <td><c:out value="${review.scoreConclusions}" default="--"/></td>
                        <td>结论是否与证据匹配，讨论是否充分且不夸大</td>
                    </tr>
                    <tr>
                        <td><strong>学术诚信（Academic Integrity）</strong></td>
                        <td><c:out value="${review.scoreAcademicIntegrity}" default="--"/></td>
                        <td>是否存在抄袭/不当引用/数据可疑等风险（仅供参考）</td>
                    </tr>
                    <tr>
                        <td><strong>实用性（Practicality）</strong></td>
                        <td><c:out value="${review.scorePracticality}" default="--"/></td>
                        <td>落地应用价值、可转化性、对实践的帮助</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">关键评价</h3>
            </div>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty review.keyEvaluation}">
                    <%-- 关键评价来自审稿人输入，可能包含富文本；编辑/主编端按富文本展示 --%>
                    <div class="rich-text" style="line-height: 1.7;">
                        <c:out value="${review.keyEvaluation}" escapeXml="false"/>
                    </div>
                </c:when>
                <c:otherwise><p>--</p></c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">给编辑的保密意见</h3>
            </div>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty review.confidentialToEditor}">
                    <%-- CKEditor 富文本：这里不做转义，以便正常显示段落/列表等格式 --%>
                    <div class="rich-text" style="line-height: 1.7;">
                        <c:out value="${review.confidentialToEditor}" escapeXml="false"/>
                    </div>
                </c:when>
                <c:otherwise><p>--</p></c:otherwise>
            </c:choose>
        </div>
    </div>

    <div class="card" style="max-width: 1100px; margin-top: 14px;">
        <div class="card-header">
            <div>
                <h3 class="card-title">给作者的意见（Comments to Author）</h3>
            </div>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty review.content}">
                    <%-- CKEditor 富文本：这里不做转义，以便正常显示段落/列表等格式 --%>
                    <div class="rich-text" style="line-height: 1.7;">
                        <c:out value="${review.content}" escapeXml="false"/>
                    </div>
                </c:when>
                <c:otherwise><p>--</p></c:otherwise>
            </c:choose>
        </div>
    </div>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
