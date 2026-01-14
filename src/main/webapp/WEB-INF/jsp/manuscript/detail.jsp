<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="roleCode" value="${sessionScope.currentUser.roleCode}"/>

<div class="page-head">
    <div>
        <h2 class="page-title">稿件详情</h2>
        <div class="chips" style="margin-top:6px;">
            <span class="chip">ManuscriptId：<strong><c:out value="${manuscript.manuscriptId}"/></strong></span>
            <span class="chip">状态：<strong><c:out value="${manuscript.currentStatus}"/></strong></span>

            <c:if test="${roleCode eq 'EO_ADMIN'}">
                <a class="btn btn-quiet" href="${ctx}/editor/formalCheck">返回形式审查列表</a>
            </c:if>
            <c:if test="${roleCode eq 'EDITOR_IN_CHIEF'}">
                <a class="btn btn-quiet" href="${ctx}/editor/toAssign">返回待分配队列</a>
            </c:if>
            <c:if test="${roleCode eq 'EDITOR'}">
                <a class="btn btn-quiet" href="${ctx}/editor/todo">返回编辑待办</a>
            </c:if>
            <c:if test="${roleCode eq 'AUTHOR'}">
                <a class="btn btn-quiet" href="${ctx}/manuscripts/list">返回我的稿件</a>
            </c:if>
        </div>
    </div>
</div>

<c:if test="${empty manuscript}">
    <p style="color:#d00;">未找到稿件或缺少参数。</p>
</c:if>

<c:if test="${not empty manuscript}">

    <div class="card" style="max-width: 1200px;">
        <div class="card-header">
            <div>
                <h3 class="card-title"><c:out value="${manuscript.title}"/></h3>
                <p class="card-subtitle">
                    <c:if test="${not empty journal}">
                        期刊：<strong><c:out value="${journal.name}"/></strong>
                        <c:if test="${not empty journal.issn}">（ISSN：<c:out value="${journal.issn}"/>）</c:if>
                        &nbsp;|&nbsp;
                    </c:if>
                    投稿人：<strong><c:out value="${empty submitter ? '--' : submitter.fullName}"/></strong>
                    <c:if test="${not empty submitter && not empty submitter.username}">
                        （<c:out value="${submitter.username}"/>）
                    </c:if>
                </p>
            </div>
        </div>

        <div class="card-body">
            <div class="grid grid-2" style="gap:16px;">
                <div>
                    <h4 style="margin:0 0 8px 0;">基本信息</h4>
                    <table border="0" cellpadding="4" cellspacing="0" style="width:100%;">
                        <tr>
                            <td class="muted" style="width:140px;">领域（SubjectArea）</td>
                            <td><c:out value="${empty manuscript.subjectArea ? '--' : manuscript.subjectArea}"/></td>
                        </tr>
                        <tr>
                            <td class="muted">关键词（Keywords）</td>
                            <td><c:out value="${empty manuscript.keywords ? '--' : manuscript.keywords}"/></td>
                        </tr>
                        <tr>
                            <td class="muted">项目资助</td>
                            <td><c:out value="${empty manuscript.fundingInfo ? '--' : manuscript.fundingInfo}"/></td>
                        </tr>
                        <tr>
                            <td class="muted">提交时间</td>
                            <td><c:out value="${manuscript.submitTime}"/></td>
                        </tr>
                        <tr>
                            <td class="muted">终审决策时间</td>
                            <td><c:out value="${manuscript.finalDecisionTime}"/></td>
                        </tr>
                    </table>
                </div>

                <div>
                    <h4 style="margin:0 0 8px 0;">作者信息</h4>
                    <c:if test="${empty authors}">
                        <p class="muted" style="margin:0;">（未维护作者列表）</p>
                    </c:if>
                    <c:if test="${not empty authors}">
                        <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
                            <thead>
                            <tr>
                                <th>作者</th>
                                <th>单位</th>
                                <th>邮箱</th>
                                <th>通讯作者</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${authors}" var="a">
                                <tr>
                                    <td><c:out value="${a.fullName}"/></td>
                                    <td><c:out value="${empty a.affiliation ? '--' : a.affiliation}"/></td>
                                    <td><c:out value="${empty a.email ? '--' : a.email}"/></td>
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
                </div>
            </div>

            <div style="margin-top:16px;">
                <h4 style="margin:0 0 8px 0;">摘要（Abstract）</h4>
                <div class="card" style="padding:12px; max-width: 1200px;">
                    <c:choose>
                        <c:when test="${empty manuscript.abstractText}">
                            <span class="muted">（未填写摘要）</span>
                        </c:when>
                        <c:otherwise>
                            <div style="white-space:pre-wrap;"><c:out value="${manuscript.abstractText}"/></div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <div style="margin-top:16px;">
                <h4 style="margin:0 0 8px 0;">附件与版本文件</h4>

                <c:if test="${not empty currentVersion}">
                    <div class="chips" style="margin-bottom:8px;">
                        <span class="chip">当前版本：V<c:out value="${currentVersion.versionNumber}"/></span>
                        <c:if test="${not empty pdfPageCount}">
                            <span class="chip">PDF页数：<c:out value="${pdfPageCount}"/></span>
                        </c:if>
                        <c:if test="${not empty bodyCount}">
                            <span class="chip">正文估算字数：<c:out value="${bodyCount}"/></span>
                        </c:if>
                        <c:if test="${not empty abstractCount}">
                            <span class="chip">摘要字数：<c:out value="${abstractCount}"/></span>
                        </c:if>
                    </div>

                    <ul style="margin:0; padding-left:18px;">
                        <li><a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=manuscript">Manuscript 预览/下载</a></li>
                        <li><a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=anonymous">匿名稿 预览/下载</a></li>
                        <li><a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=cover">Cover Letter 预览/下载</a></li>
                        <li><a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=response">Response Letter 预览/下载</a></li>

                        <c:if test="${not empty coverAttachments}">
                            <li style="margin-top:8px;">
                                Cover Letter 附件：
                                <ul style="margin:6px 0 0 0; padding-left:18px;">
                                    <c:forEach items="${coverAttachments}" var="f">
                                        <li>
                                            <a target="_blank" href="${ctx}/files/preview?manuscriptId=${manuscript.manuscriptId}&type=attachment&fileId=${f.fileId}">
                                                <c:out value="${f.fileName}"/>
                                            </a>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </li>
                        </c:if>
                    </ul>
                </c:if>

                <c:if test="${empty currentVersion}">
                    <p class="muted" style="margin:0;">（未找到当前版本文件）</p>
                </c:if>
            </div>

            <div style="margin-top:16px;">
                <h4 style="margin:0 0 8px 0;">形式审查（EO）结果</h4>
                <c:choose>
                    <c:when test="${empty formalCheckResult}">
                        <p class="muted" style="margin:0;">（暂无形式审查结果）</p>
                    </c:when>
                    <c:otherwise>
                        <table border="1" cellpadding="4" cellspacing="0" style="width:100%; max-width: 1200px;">
                            <tr>
                                <th style="width:160px;">结论</th>
                                <td><c:out value="${formalCheckResult.checkResult}"/></td>
                            </tr>
                            <tr>
                                <th>反馈</th>
                                <td style="white-space:pre-wrap;"><c:out value="${empty formalCheckResult.feedback ? '--' : formalCheckResult.feedback}"/></td>
                            </tr>
                            <tr>
                                <th>审查时间</th>
                                <td><c:out value="${formalCheckResult.checkTime}"/></td>
                            </tr>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${roleCode eq 'EDITOR' || roleCode eq 'EDITOR_IN_CHIEF'}">
                <div style="margin-top:16px;">
                    <h4 style="margin:0 0 8px 0;">当前审稿记录（仅编辑可见）</h4>
                    <c:if test="${empty reviews}">
                        <p class="muted" style="margin:0;">（暂无审稿记录）</p>
                    </c:if>
                    <c:if test="${not empty reviews}">
                        <table border="1" cellpadding="4" cellspacing="0" style="width:100%; max-width: 1200px;">
                            <thead>
                            <tr>
                                <th>ReviewId</th>
                                <th>审稿人</th>
                                <th>状态</th>
                                <th>邀请时间</th>
                                <th>截止时间</th>
                                <th>详情</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${reviews}" var="r">
                                <tr>
                                    <td><c:out value="${r.reviewId}"/></td>
                                    <td><c:out value="${empty r.reviewerName ? (empty r.reviewerEmail ? '--' : r.reviewerEmail) : r.reviewerName}"/></td>
                                    <td><c:out value="${r.status}"/></td>
                                    <td><c:out value="${r.invitedAt}"/></td>
                                    <td><c:out value="${r.dueAt}"/></td>
                                    <td>
                                        <a class="btn btn-quiet" href="${ctx}/review/detail?reviewId=${r.reviewId}">查看</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:if>

                    <div class="chips" style="margin-top:10px;">
                        <a class="btn" href="${ctx}/editor/review/select?manuscriptId=${manuscript.manuscriptId}">选择/邀请审稿人</a>
                        <a class="btn btn-quiet" href="${ctx}/editor/review/monitor?manuscriptId=${manuscript.manuscriptId}">审稿监控</a>
                    </div>
                </div>
            </c:if>

        </div>
    </div>

</c:if>

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
