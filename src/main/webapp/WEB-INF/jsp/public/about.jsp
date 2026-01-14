<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="page-head">
    <h2 class="page-title">关于期刊</h2>
</div>

<div class="about-tabs" role="tablist" aria-label="关于期刊内容切换">
    <button class="about-tab" type="button" data-tab="aims" role="tab" aria-controls="aboutPanelAims">
        论文主旨与投稿范围
    </button>
    <button class="about-tab" type="button" data-tab="board" role="tab" aria-controls="aboutPanelBoard">
        编委成员
    </button>
    <button class="about-tab" type="button" data-tab="insights" role="tab" aria-controls="aboutPanelInsights">
        期刊信息
    </button>
    <button class="about-tab" type="button" data-tab="news" role="tab" aria-controls="aboutPanelNews">
        新闻
    </button>
    <button class="about-tab" type="button" data-tab="policies" role="tab" aria-controls="aboutPanelPolicies">
        政策与指南
    </button>
</div>

<div class="about-panels">
    
    <section class="about-panel" id="aboutPanelAims" data-tab="aims" role="tabpanel">
        <div class="card">
            <div class="card-header">
                <div>
                    <div class="page-head" style="margin:0;">
                        <h2 class="page-title">
                            <c:choose>
                                <c:when test="${not empty aimsPage && not empty aimsPage.title}">
                                    <c:out value="${aimsPage.title}"/>
                                </c:when>
                                <c:otherwise>论文主旨与投稿范围（Aims and scope）</c:otherwise>
                            </c:choose>
                        </h2>
</div>
                </div>
            </div>

            <c:if test="${not empty aimsLoadError}">
                <div class="notice danger">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                    <div><c:out value="${aimsLoadError}"/></div>
                </div>
            </c:if>

            <c:if test="${not empty journal}">
                <p class="muted">
                    期刊：<b><c:out value="${journal.name}"/></b>
                    <c:if test="${not empty aimsPage && not empty aimsPage.updatedAt}">
                        ｜最近更新：<c:out value="${aimsPage.updatedAt}"/>
                    </c:if>
                </p>
            </c:if>

            <c:choose>
                <c:when test="${not empty aimsPage && not empty aimsPage.content}">
                    <div class="richtext">
                        <c:out value="${aimsPage.content}" escapeXml="false"/>
                    </div>
                </c:when>
                <c:otherwise>
                    <p>暂无内容。</p>
                </c:otherwise>
            </c:choose>
        </div>
    </section>

    
    <section class="about-panel" id="aboutPanelBoard" data-tab="board" role="tabpanel">
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">编委成员（Editorial board）</h2>
                    <p class="card-subtitle">主编、副主编、编辑等成员名单及单位信息。</p>
                </div>
            </div>

            <c:if test="${not empty boardLoadError}">
                <div class="notice danger">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                    <div>加载编辑委员会失败：<c:out value="${boardLoadError}"/></div>
                </div>
            </c:if>

            <c:if test="${empty boardMembers}">
                <p>当前没有可展示的编委成员（请先在后台“编辑委员会管理”中维护数据）。</p>
            </c:if>

            <c:if test="${not empty boardMembers}">
                <div class="grid grid-2">
                    <c:forEach var="m" items="${boardMembers}">
                        <div class="card mini">
                            <div class="media">
                                <div class="avatar" aria-hidden="true" style="overflow:hidden;">
                                    <img src="${ctx}/public/avatar?userId=${m.userId}"
                                         alt="avatar"
                                         style="width:100%;height:100%;object-fit:cover;display:block;"/>
                                </div>
                                <div>
                                    <div><b><c:out value="${m.position}"/></b></div>
                                    <div class="muted"><c:out value="${m.fullName}"/></div>
                                    <c:if test="${not empty m.affiliation}">
                                        <div class="muted"><c:out value="${m.affiliation}"/></div>
                                    </c:if>
                                    <c:if test="${not empty m.section}">
                                        <div class="muted">负责栏目：<c:out value="${m.section}"/></div>
                                    </c:if>
                                </div>
                            </div>
                            <c:if test="${not empty m.bio}">
                                <p class="mt-1"><c:out value="${m.bio}"/></p>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </section>

    
    <section class="about-panel" id="aboutPanelInsights" data-tab="insights" role="tabpanel">
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">期刊信息（Journal Insights）</h2>
                    <p class="card-subtitle">期刊号、影响因子、发布时间线等信息。</p>
                </div>
            </div>

            <c:if test="${empty journal}">
                <p>未读取到期刊信息。</p>
            </c:if>
            <c:if test="${not empty journal}">
                <table class="table">
                    <tr>
                        <th style="width:220px;">期刊名称</th>
                        <td><c:out value="${journal.name}"/></td>
                    </tr>
                    <tr>
                        <th>ISSN</th>
                        <td><c:out value="${journal.issn}"/></td>
                    </tr>
                    <tr>
                        <th>影响因子</th>
                        <td>
                            <c:choose>
                                <c:when test="${not empty journal.impactFactor}">
                                    <c:out value="${journal.impactFactor}"/>
                                </c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <th>发表时间线（Publishing timeline）</th>
                        <td><c:out value="${journal.timeline}"/></td>
                    </tr>
                    <tr>
                        <th>期刊简介</th>
                        <td><c:out value="${journal.description}"/></td>
                    </tr>
                </table>
            </c:if>
        </div>
    </section>

    
    <section class="about-panel" id="aboutPanelNews" data-tab="news" role="tabpanel">
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">新闻（News）</h2>
                    <p class="card-subtitle">期刊最新动态与公告。</p>
                </div>
                <div>
                    <a class="btn" style="text-decoration:none;" href="${ctx}/news/list">
                        <i class="bi bi-newspaper"></i> 查看全部新闻
                    </a>
                </div>
            </div>

            <c:if test="${not empty newsLoadError}">
                <div class="notice danger">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                    <div>加载新闻失败：<c:out value="${newsLoadError}"/></div>
                </div>
            </c:if>

            <c:if test="${empty newsList}">
                <p>暂无已发布新闻。</p>
            </c:if>
            <c:if test="${not empty newsList}">
                <ul class="list">
                    <c:forEach var="n" items="${newsList}">
                        <li class="list-item">
                            <div class="muted">
                                <c:out value="${fn:substring(n.publishedAt, 0, 10)}"/>
                            </div>
                            <div>
                                <a href="${ctx}/news/detail?id=${n.newsId}"><c:out value="${n.title}"/></a>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:if>
        </div>
    </section>

    
    <section class="about-panel" id="aboutPanelPolicies" data-tab="policies" role="tabpanel">
        <div class="card">
            <div class="card-header">
                <div>
                    <h2 class="card-title">
                        <c:choose>
                            <c:when test="${not empty policiesPage && not empty policiesPage.title}">
                                <c:out value="${policiesPage.title}"/>
                            </c:when>
                            <c:otherwise>政策与指南（Policies and Guidelines）</c:otherwise>
                        </c:choose>
                    </h2>
                    <p class="card-subtitle">介绍期刊的相关政策与操作指南。</p>
                </div>
            </div>

            <c:if test="${not empty policiesLoadError}">
                <div class="notice danger">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                    <div><c:out value="${policiesLoadError}"/></div>
                </div>
            </c:if>

            <c:if test="${not empty journal}">
                <p class="muted">
                    期刊：<b><c:out value="${journal.name}"/></b>
                    <c:if test="${not empty policiesPage && not empty policiesPage.updatedAt}">
                        ｜最近更新：<c:out value="${policiesPage.updatedAt}"/>
                    </c:if>
                </p>
            </c:if>

            <c:choose>
                <c:when test="${not empty policiesPage && not empty policiesPage.content}">
                    <div class="richtext">
                        <c:out value="${policiesPage.content}" escapeXml="false"/>
                    </div>
                </c:when>
                <c:otherwise>
                    <p>暂无内容。</p>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</div>

<script>
    (function () {
        function pickInitialTab() {
            var fromHash = (window.location.hash || '').replace('#', '').trim();
            if (fromHash) return fromHash;
            return '${activeAboutTab}';
        }

        function setActive(tab) {
            var tabs = document.querySelectorAll('.about-tab');
            var panels = document.querySelectorAll('.about-panel');

            tabs.forEach(function (btn) {
                var isActive = btn.getAttribute('data-tab') === tab;
                btn.classList.toggle('active', isActive);
                btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
            });

            panels.forEach(function (p) {
                var isActive = p.getAttribute('data-tab') === tab;
                p.style.display = isActive ? 'block' : 'none';
            });
        }

        document.addEventListener('click', function (e) {
            var btn = e.target && e.target.closest ? e.target.closest('.about-tab') : null;
            if (!btn) return;
            var tab = btn.getAttribute('data-tab');
            if (!tab) return;
            setActive(tab);
            try { window.location.hash = tab; } catch (err) { /* ignore */ }
        });

        // init
        var initial = pickInitialTab();
        setActive(initial);
    })();
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
