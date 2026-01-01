<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">超级管理员工作台</h2>
            <p class="card-subtitle">
                最高权限入口：统一管理用户、权限、期刊、系统状态与审计日志。
            </p>
        </div>
    </div>

    <div class="grid grid-2">
        <a class="card" style="text-decoration:none;" href="${ctx}/admin/users/list">
            <h3><i class="bi bi-people" aria-hidden="true"></i> 用户管理</h3>
            <p>创建/修改/删除后台用户，封禁/解封账号，并对关键账号进行只读保护。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开用户管理</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/permissions/list">
            <h3><i class="bi bi-shield-lock" aria-hidden="true"></i> 权限管理</h3>
            <p>为不同角色分配系统访问权限；SUPER_ADMIN 默认全权限且不可修改。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开权限管理</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/system/status">
            <h3><i class="bi bi-activity" aria-hidden="true"></i> 系统状态</h3>
            <p>查看运行环境、JVM 信息与数据库连通性，快速定位部署/连接类问题。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 查看系统状态</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/system/db">
            <h3><i class="bi bi-database-gear" aria-hidden="true"></i> 数据库维护</h3>
            <p>手动检查关键表结构，执行修复/对齐脚本，辅助课程设计的“结构一致性”验证。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 进入数据库维护</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/journals/list">
            <h3><i class="bi bi-journals" aria-hidden="true"></i> 期刊管理</h3>
            <p>按板块维护 Journals / JournalPages / Issues / CallForPapers 等内容结构。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开期刊管理</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/editorial/list">
            <h3><i class="bi bi-person-vcard" aria-hidden="true"></i> 编辑委员会</h3>
            <p>维护编委/岗位信息，支持新增校外编委账号并关联到期刊编委会。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 管理编辑委员会</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/news/list">
            <h3><i class="bi bi-megaphone" aria-hidden="true"></i> 公告 / 新闻</h3>
            <p>维护首页新闻与公告（征稿启事、重要通知等），确保信息及时发布。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开公告/新闻</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${ctx}/admin/logs/list">
            <h3><i class="bi bi-journal-text" aria-hidden="true"></i> 系统日志查询</h3>
            <p>审计关键操作（分配/撤回/发送通知等），用于追踪问题与合规留痕。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 查看系统日志</small>
        </a>
    </div>

    <div class="stack" style="margin-top: var(--space-6);">
        <small>
            建议：日常运营由编辑部管理员/系统管理员分担；超级管理员主要用于权限与安全兜底。
        </small>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
