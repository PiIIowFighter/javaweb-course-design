<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <h2 class="card-title">系统日志查询</h2>
            <p class="card-subtitle">展示系统的操作日志（dbo.OperationLogs），支持按时间范围、用户、模块等条件检索，便于进行日志审计与故障排查。</p>
        </div>
    </div>

    <form action="${pageContext.request.contextPath}/admin/logs/list" method="get" class="stack">
        <div class="form-row">
            <label>时间从</label>
            <div style="display:flex; gap:12px; flex-wrap:wrap;">
                <input type="datetime-local" name="from" value="${from}" style="max-width: 280px;"/>
                <div style="display:flex; align-items:center; color: var(--muted);">到</div>
                <input type="datetime-local" name="to" value="${to}" style="max-width: 280px;"/>
            </div>
        </div>

        <div class="form-row">
            <label>用户名</label>
            <input type="text" name="actor" value="${actor}" placeholder="精确匹配"/>
        </div>

        <div class="form-row">
            <label>模块</label>
            <select name="module" style="max-width: 360px;">
                <option value="" <c:if test="${empty module}">selected</c:if>>全部</option>
                <option value="USER" <c:if test="${module == 'USER'}">selected</c:if>>USER（用户）</option>
                <option value="PERMISSION" <c:if test="${module == 'PERMISSION'}">selected</c:if>>PERMISSION（权限）</option>
                <option value="SYSTEM" <c:if test="${module == 'SYSTEM'}">selected</c:if>>SYSTEM（系统）</option>
                <option value="LOG" <c:if test="${module == 'LOG'}">selected</c:if>>LOG（日志）</option>
                <option value="JOURNAL" <c:if test="${module == 'JOURNAL'}">selected</c:if>>JOURNAL（期刊）</option>
                <option value="EDITORIAL" <c:if test="${module == 'EDITORIAL'}">selected</c:if>>EDITORIAL（编委会）</option>
                <option value="NEWS" <c:if test="${module == 'NEWS'}">selected</c:if>>NEWS（公告）</option>
                <option value="AUTH" <c:if test="${module == 'AUTH'}">selected</c:if>>AUTH（认证）</option>
                <option value="MANUSCRIPT" <c:if test="${module == 'MANUSCRIPT'}">selected</c:if>>MANUSCRIPT（稿件）</option>
                <option value="EDITOR" <c:if test="${module == 'EDITOR'}">selected</c:if>>EDITOR（编辑工作台）</option>
                <option value="EIC" <c:if test="${module == 'EIC'}">selected</c:if>>EIC（主编）</option>
                <option value="REVIEW" <c:if test="${module == 'REVIEW'}">selected</c:if>>REVIEW（审稿）</option>
            </select>
        </div>

        <div class="form-row">
            <label>关键字</label>
            <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
                <input type="text" name="keyword" value="${keyword}" placeholder="用户名/模块/动作/详情（模糊匹配）" style="min-width: 260px; flex: 1;"/>
                <button type="submit">查询</button>
                <a href="${pageContext.request.contextPath}/admin/logs/list" style="color: var(--muted); text-decoration:none;">重置</a>
            </div>
        </div>
    </form>

<c:if test="${empty logs}">
    <p>暂无日志。</p>
</c:if>

<c:if test="${not empty logs}">
    <div class="table-wrap" style="margin-top: 12px;">
    <table class="table-fixed" border="1" cellpadding="6" cellspacing="0" style="min-width: 980px;">
        <thead>
        <tr>
            <th style="width:170px;">时间</th>
            <th style="width:120px;">用户</th>
            <th style="width:120px;">模块</th>
            <th style="width:140px;">动作</th>
            <th>详情</th>
            <th style="width:140px;">IP</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="l" items="${logs}">
            <tr>
                <td>${l.createdAt}</td>
                <td>${l.actorUsername}</td>
                <td>${l.module}</td>
                <td>${l.action}</td>
                <td class="cell-wrap"><span class="pre-wrap">${l.detail}</span></td>
                <td>${l.ip}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    </div>
</c:if>
    
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
