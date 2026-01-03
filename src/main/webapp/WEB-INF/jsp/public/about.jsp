<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="layout">
    <aside class="sidebar">
        <h3 class="side-title">关于期刊</h3>
        <nav class="side-nav" aria-label="关于期刊菜单">
            <a class="side-link ${activeAboutTab == 'aims' ? 'active' : ''}" href="${ctx}/about/aims">
                论文主旨与投稿范围（Aims and scope）
            </a>
            <a class="side-link ${activeAboutTab == 'board' ? 'active' : ''}" href="${ctx}/about/board">
                编委成员（Editorial board）
            </a>
            <a class="side-link ${activeAboutTab == 'insights' ? 'active' : ''}" href="${ctx}/about/insights">
                期刊信息（Journal Insights）
            </a>
            <a class="side-link ${activeAboutTab == 'news' ? 'active' : ''}" href="${ctx}/about/news">
                新闻（News）
            </a>
            <a class="side-link ${activeAboutTab == 'policies' ? 'active' : ''}" href="${ctx}/about/policies">
                政策与指南（Policies and Guidelines）
            </a>
        </nav>
    </aside>

    <section class="content">
        <c:choose>

            <%-- Aims and scope（数据库驱动：dbo.JournalPages） --%>
            <c:when test="${activeAboutTab == 'aims'}">
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h2 class="card-title">
                                <c:choose>
                                    <c:when test="${not empty aimsPage && not empty aimsPage.title}">
                                        <c:out value="${aimsPage.title}"/>
                                    </c:when>
                                    <c:otherwise>论文主旨与投稿范围（Aims and scope）</c:otherwise>
                                </c:choose>
                            </h2>
                            <p class="card-subtitle">介绍期刊的主要研究方向与投稿内容范围。</p>
                        </div>
                    </div>

                    <c:if test="${not empty pageLoadError}">
                        <div class="notice danger">
                            <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                            <div><c:out value="${pageLoadError}"/></div>
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
            </c:when>

            <%-- Editorial board --%>
            <c:when test="${activeAboutTab == 'board'}">
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
            </c:when>

            <%-- Journal insights --%>
            <c:when test="${activeAboutTab == 'insights'}">
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h2 class="card-title">期刊信息（Journal Insights）</h2>
                            <p class="card-subtitle">期刊号、影响因子、发布时间线等信息。</p>
                        </div>
                    </div>

                    <c:if test="${empty journal}">
                        <p>未读取到期刊信息（请检查 dbo.Journals 是否已初始化）。</p>
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
            </c:when>

            <%-- News --%>
            <c:when test="${activeAboutTab == 'news'}">
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
            </c:when>

            <%-- Policies and Guidelines（数据库驱动：dbo.JournalPages） --%>
            <c:when test="${activeAboutTab == 'policies'}">
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

                    <c:if test="${not empty pageLoadError}">
                        <div class="notice danger">
                            <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                            <div><c:out value="${pageLoadError}"/></div>
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
            </c:when>

            <c:otherwise>
                <div class="card">
                    <h2 class="card-title">关于期刊</h2>
                    <p>请选择左侧菜单。</p>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>