<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<style>
    /* Guide: add a right-side illustration to avoid excessive blank space on wide screens */
    .guide-split {
        display: grid;
        /* Make text and illustration each take half width on wide screens */
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
        gap: 22px;
        align-items: stretch;
    }

    .guide-side { align-self: stretch; }

    .guide-side .illus-box {
        border-radius: 18px;
        overflow: hidden;
        background: linear-gradient(135deg, rgba(16, 120, 255, 0.10), rgba(0, 0, 0, 0.00));
        border: 1px solid rgba(0, 0, 0, 0.06);
        height: 100%;
        display: flex;
        flex-direction: column;
    }

    /* keep illustration filling the right side while preserving visual balance */
    .guide-side .illus-media {
        flex: 1;
        min-height: clamp(360px, 62vh, 660px);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .guide-side img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
        padding: 0;
    }

    .guide-side .illus-caption {
        padding: 10px 12px 12px;
        font-size: 12px;
        color: rgba(0, 0, 0, 0.60);
        line-height: 1.4;
    }

    @media (max-width: 992px) {
        .guide-split {
            grid-template-columns: 1fr;
        }

        .guide-side { order: 2; }

        .guide-side .illus-media { height: 220px; }
    }
</style>

<div class="card">
    <div class="guide-split">
        <div class="stack">
            <div class="card-header">
                <div>
                    <div class="page-head">
                        <h2 class="page-title"><c:choose>
                                    <c:when test="${not empty page && not empty page.title}">
                                        <c:out value="${page.title}"/>
                                    </c:when>
                                    <c:otherwise>用户指南 (Guide for authors)</c:otherwise>
                                </c:choose></h2>
</div>
                </div>
            </div>

            <div class="actions">
                <a class="btn-primary" style="text-decoration:none;" href="${ctx}/manuscripts/submit">
                    <i class="bi bi-upload" aria-hidden="true"></i>
                    Submit your article
                </a>
                <a style="text-decoration:none;" href="${ctx}/news">
                    <i class="bi bi-newspaper" aria-hidden="true"></i>
                    查看新闻
                </a>
            </div>

            <c:if test="${not empty pageLoadError}">
                <div class="notice danger" style="margin-top: 14px;">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                    <div>
                        未找到页面配置数据（JournalPages 中没有对应记录）：pageKey=guide
                    </div>
                </div>
            </c:if>

            <c:choose>
                <c:when test="${not empty page && not empty page.content}">
                    <div class="richtext" style="margin-top: 14px;">
                        <c:out value="${page.content}" escapeXml="false"/>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="margin-top: 14px;">
                        <h3>投稿准备</h3>
                        <ul>
                            <li>确认研究主题符合期刊范围（Aims &amp; Scope）。</li>
                            <li>准备作者信息、单位、基金与通讯作者邮箱。</li>
                            <li>整理正文、图表、补充材料与数据/代码链接（如有）。</li>
                        </ul>
                        <h3>写作与格式</h3>
                        <ul>
                            <li>摘要包含背景/方法/结果/结论四要素；关键词 3–6 个。</li>
                            <li>图表清晰，图题与注释完整；参考文献格式统一。</li>
                        </ul>
                        
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <aside class="guide-side" aria-hidden="true">
            <div class="illus-box">
                <div class="illus-media">
                    <img src="${ctx}/static/img/illustrations/guide-ai-right.png" alt="International Artificial Intelligence Research"/>
                </div>
                <div class="illus-caption">
                    International Artificial Intelligence Research<br/>
                    
                </div>
            </div>
        </aside>
    </div>
</div>

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
