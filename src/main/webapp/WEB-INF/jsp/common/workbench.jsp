<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<!--
  工作台（入口页）响应式优化：
  1) 解除登录态内容区 1160px 的 max-width 限制，避免大屏两侧出现大片空白；
  2) 入口卡片使用自适应栅格；
  3) 卡片内按钮铺满宽度，移动端更易点击。
  为避免影响其它页面，这里采用页面内局部样式。
-->
<style>
  /* 登录态右侧内容区默认 max-width: 1160px，这里仅对工作台页解除限制 */
  .authed .content-inner {
    max-width: none;
    padding-left: clamp(12px, 2vw, 24px);
    padding-right: clamp(12px, 2vw, 24px);
  }

  .workbench-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 14px;
  }

  .workbench-card .btn {
    width: 100%;
    justify-content: center;
  }

  @media (max-width: 520px) {
    .workbench-grid {
      grid-template-columns: 1fr;
    }
    .workbench-card .btn {
      padding-top: 12px;
      padding-bottom: 12px;
    }
  }
</style>
<c:if test="${empty sessionScope.menuPermMap}">
  <div class="card" style="border-left: 4px solid #f0ad4e;">
    <div style="font-weight: 600;">提示</div>
    <div class="muted" style="margin-top: 4px;">
      当前会话未加载菜单权限（menuPermMap 为空）。请先完成登录权限初始化/或在权限管理中保存一次权限，然后刷新页面。
    </div>
  </div>
</c:if>

<!-- 入口区：按权限显示 -->
<div class="workbench-grid">

  <!-- 作者 -->
  <c:if test="${sessionScope.menuPermMap['MENU_AUTHOR_MY_MANUSCRIPTS'] or sessionScope.menuPermMap['MENU_AUTHOR_SUBMIT']}">
    <div class="card workbench-card">
      <div style="font-weight:700; margin-bottom:6px;"><i class="bi bi-upload" aria-hidden="true"></i> 投稿 / 作者</div>
      <div class="muted" style="margin-bottom:10px;">稿件提交、查看与跟踪</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <c:if test="${sessionScope.menuPermMap['MENU_AUTHOR_MY_MANUSCRIPTS']}">
          <a class="btn" href="${ctx}/manuscripts/list">我的稿件</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_AUTHOR_SUBMIT']}">
          <a class="btn" href="${ctx}/manuscripts/submit">提交稿件</a>
        </c:if>
      </div>
    </div>
  </c:if>

  <!-- 审稿人 -->
  <c:if test="${sessionScope.menuPermMap['MENU_REVIEWER_ASSIGNED'] or sessionScope.menuPermMap['MENU_REVIEWER_HISTORY']}">
    <div class="card workbench-card">
      <div style="font-weight:700; margin-bottom:6px;"><i class="bi bi-person-check" aria-hidden="true"></i> 外审 / 审稿人</div>
      <div class="muted" style="margin-bottom:10px;">受邀、评审与历史记录</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <c:if test="${sessionScope.menuPermMap['MENU_REVIEWER_ASSIGNED']}">
          <a class="btn" href="${ctx}/reviewer/assigned">待评审稿件</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_REVIEWER_HISTORY']}">
          <a class="btn" href="${ctx}/reviewer/history">历史评审</a>
        </c:if>
      </div>
    </div>
  </c:if>

  <!-- 编辑 -->
  <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_TODO'] or sessionScope.menuPermMap['MENU_EDITOR_UNDER_REVIEW']
      or sessionScope.menuPermMap['MENU_EDITOR_RECOMMEND'] or sessionScope.menuPermMap['MENU_EDITOR_REVIEW_MONITOR']
      or sessionScope.menuPermMap['MENU_EDITOR_AUTHOR_COMM']}">
    <div class="card workbench-card">
      <div style="font-weight:700; margin-bottom:6px;"><i class="bi bi-person-workspace" aria-hidden="true"></i> 编辑工作台</div>
      <div class="muted" style="margin-bottom:10px;">编辑处理、监控与沟通</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_TODO']}">
          <a class="btn" href="${ctx}/editor/withEditor">编辑待办</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_UNDER_REVIEW']}">
          <a class="btn" href="${ctx}/editor/underReview">审稿中</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_RECOMMEND']}">
          <a class="btn" href="${ctx}/editor/recommend">编辑推荐</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_REVIEW_MONITOR']}">
          <a class="btn" href="${ctx}/editor/review/monitor">审稿监控 / 催审</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EDITOR_AUTHOR_COMM']}">
          <a class="btn" href="${ctx}/editor/authorComm">与作者沟通</a>
        </c:if>
      </div>
    </div>
  </c:if>

  <!-- 主编 -->
  <c:if test="${sessionScope.menuPermMap['MENU_EIC_OVERVIEW'] or sessionScope.menuPermMap['MENU_EIC_DESK']
      or sessionScope.menuPermMap['MENU_EIC_TO_ASSIGN'] or sessionScope.menuPermMap['MENU_EIC_REVIEWERS']
      or sessionScope.menuPermMap['MENU_EIC_FINAL_DECISION'] or sessionScope.menuPermMap['MENU_EIC_SPECIAL']}">
    <div class="card workbench-card">
      <div style="font-weight:700; margin-bottom:6px;"><i class="bi bi-award" aria-hidden="true"></i> 主编工作台</div>
      <div class="muted" style="margin-bottom:10px;">主编总览、分配与终审</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_OVERVIEW']}">
          <a class="btn" href="${ctx}/editor/overview">系统全览</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_DESK']}">
          <a class="btn" href="${ctx}/editor/desk">案头审查</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_TO_ASSIGN']}">
          <a class="btn" href="${ctx}/editor/toAssign">待分配队列</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_REVIEWERS']}">
          <a class="btn" href="${ctx}/editor/reviewers">审稿人库</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_FINAL_DECISION']}">
          <a class="btn" href="${ctx}/editor/finalDecision">终审决策</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['MENU_EIC_SPECIAL']}">
          <a class="btn" href="${ctx}/editor/special">专项工作</a>
        </c:if>
      </div>
    </div>
  </c:if>

  <!-- 编辑部管理员 -->
<c:if test="${sessionScope.menuPermMap['MENU_EO_FORMAL_CHECK'] or sessionScope.menuPermMap['MENU_EO_FORMAL_HISTORY'] or sessionScope.menuPermMap['ADMIN_JOURNALS'] or sessionScope.menuPermMap['ADMIN_NEWS']}">
  <div class="card workbench-card">
    <div style="font-weight:700; margin-bottom:6px;"><i class="bi bi-clipboard-check" aria-hidden="true"></i> 编辑部管理</div>
    <div class="muted" style="margin-bottom:10px;">编辑部管理相关入口</div>
    <div style="display:flex; flex-direction:column; gap:8px;">
      <c:if test="${sessionScope.menuPermMap['MENU_EO_FORMAL_CHECK']}">
        <a class="btn" href="${ctx}/editor/formalCheck">形式审查</a>
      </c:if>
      <c:if test="${sessionScope.menuPermMap['MENU_EO_FORMAL_HISTORY']}">
        <a class="btn" href="${ctx}/editor/formalCheck/history">审查历史</a>
      </c:if>
      <c:if test="${sessionScope.menuPermMap['ADMIN_JOURNALS']}">
        <a class="btn" href="${ctx}/admin/journals/list">期刊管理</a>
      </c:if>
      <c:if test="${sessionScope.menuPermMap['ADMIN_NEWS']}">
        <a class="btn" href="${ctx}/admin/news/list">公告 / 新闻</a>
      </c:if>
    </div>
  </div>
</c:if>

<!-- 后台管理 -->

  <c:if test="${sessionScope.menuPermMap['ADMIN_USERS'] or sessionScope.menuPermMap['ADMIN_PERMISSIONS'] or sessionScope.menuPermMap['ADMIN_LOGS']
      or sessionScope.menuPermMap['ADMIN_SYSTEM'] or sessionScope.menuPermMap['ADMIN_DB_MAINTENANCE']
      or sessionScope.menuPermMap['ADMIN_EDITORIAL']}">
    <div class="card workbench-card">
      <div style="font-weight:700; margin-bottom:6px;"><i class="bi bi-gear" aria-hidden="true"></i> 后台管理</div>
      <div class="muted" style="margin-bottom:10px;">系统配置与维护入口</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <c:if test="${sessionScope.menuPermMap['ADMIN_USERS']}">
          <a class="btn" href="${ctx}/admin/users/list">用户管理</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['ADMIN_PERMISSIONS']}">
          <a class="btn" href="${ctx}/admin/permissions/list">权限管理</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['ADMIN_SYSTEM']}">
          <a class="btn" href="${ctx}/admin/system/status">系统状态</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['ADMIN_DB_MAINTENANCE']}">
          <a class="btn" href="${ctx}/admin/system/db">数据库维护</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['ADMIN_LOGS']}">
          <a class="btn" href="${ctx}/admin/logs/list">系统日志</a>
        </c:if>
        <c:if test="${sessionScope.menuPermMap['ADMIN_EDITORIAL']}">
          <a class="btn" href="${ctx}/admin/editorial/list">编委管理</a>
        </c:if>
      </div>
    </div>
  </c:if>

</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
