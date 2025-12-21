<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">编辑部管理员工作台</h2>
            <p class="card-subtitle">
                负责投稿的形式审查和日常运营支持（如新闻 / 公告维护）。
            </p>
        </div>
    </div>

    <div class="grid grid-2">
        <!-- 形式审查 / 格式检查 -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/formalCheck">
            <h3><i class="bi bi-clipboard-check" aria-hidden="true"></i> 形式审查 / 格式检查</h3>
            <p>
                处理 SUBMITTED / FORMAL_CHECK：检查稿件版式、篇幅、要素是否符合投稿须知，
                决定是否送主编案头审查或退回作者修改。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 进入形式审查工作台
            </small>
        </a>

        <!-- 新闻 / 公告管理 -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/admin/news/list">
            <h3><i class="bi bi-megaphone" aria-hidden="true"></i> 新闻 / 公告管理</h3>
            <p>
                维护期刊网站首页的新闻与公告，例如征稿启事、重要通知等，
                保证作者和审稿人及时获知最新信息。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 打开新闻 / 公告管理
            </small>
        </a>
    </div>

    <div class="stack" style="margin-top: var(--space-6);">
        <small>
            后续如需扩展，可在此工作台增加“退修管理”“统计报表”等卡片入口，
            统一从编辑部管理员视角管理日常运营工作。
        </small>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
