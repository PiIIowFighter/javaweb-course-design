<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">权限管理（按用户 / 菜单入口）</h2>
</div></div>
    </div>

    <!--
      success 参数使用数字码，避免在 URL 中携带中文导致出现“????”乱码。
        1: 保存成功      如仍收到历史字符串参数，则兜底直接输出。
    -->
    <c:if test="${param.success == '1'}">
        <div class="alert alert-success" style="margin-bottom: var(--space-4);">保存成功</div>
    </c:if>
<c:if test="${not empty param.success and param.success != '1'}">
        <div class="alert alert-success" style="margin-bottom: var(--space-4);">
            <c:out value="${param.success}"/>
        </div>
    </c:if>

    <!-- 选择用户（GET） -->
    <form method="get" action="${ctx}/admin/permissions/list" class="stack" style="gap: var(--space-3); margin-bottom: var(--space-5);">
        <div class="grid grid-2" style="align-items:end;">
            <div>
                <label class="label">选择用户</label>
                <select name="userId" class="input" onchange="this.form.submit()">
                    <c:forEach var="u" items="${users}">
                        <option value="${u.userId}" <c:if test="${not empty selectedUser and selectedUser.userId == u.userId}">selected</c:if>>
                            #${u.userId} - <c:out value="${u.username}"/> (<c:out value="${u.roleCode}"/>)
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div style="text-align:right;">
                <small>提示：可按用户配置入口权限；SUPER_ADMIN 也可配置（“权限管理”入口不可取消）。</small>
            </div>
        </div>
    </form>

    <c:if test="${empty selectedUser}">
        <p>未找到目标用户。</p>
    </c:if>

    <c:if test="${not empty selectedUser}">
        <!-- 保存权限（POST） -->
        <form method="post" action="${ctx}/admin/permissions/save" class="stack" style="gap: var(--space-4);">
            <input type="hidden" name="userId" value="${selectedUser.userId}"/>

            <div class="stack" style="gap: var(--space-2);">
                <div>
                    <strong>当前用户：</strong>
                    <c:out value="${selectedUser.username}"/> （<c:out value="${selectedUser.roleCode}"/>）
                <c:if test="${selectedUser.roleCode == 'SUPER_ADMIN'}">
                    <div class="alert alert-warning" style="margin-top: var(--space-2);">该用户为 SUPER_ADMIN：入口权限可编辑；为防止锁死自己，“权限管理”入口不可取消。</div>
                </c:if>
                </div>
</div>

            <!-- 入口权限分组（与工作台 / 侧边栏一致） -->
            <div class="stack" style="gap: var(--space-4);">

                <!-- 系统管理（管理员类入口） -->
                <div>
                    <h3 style="margin: 0 0 var(--space-2);">系统管理（管理员）</h3>
                    <div class="grid grid-2" style="align-items:start;">
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${fn:startsWith(p.key, 'ADMIN_') and p.key != 'ADMIN_JOURNALS' and p.key != 'ADMIN_NEWS'}">
                                <label class="card" style="display:flex; gap: var(--space-3); align-items:flex-start; cursor:pointer;">
                                    <input type="checkbox" name="permissions" value="${p.key}"
                                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                                           <c:if test="${lockedMap[p.key]}">disabled</c:if>
                                           style="margin-top: 4px;"/>
                                    <span>
                                        <div style="font-weight:600;"><c:out value="${p.name}"/></div>
                                        <div style="opacity:.75; font-size: 12px;"><c:out value="${p.desc}"/>（<c:out value="${p.key}"/>）</div>
                                    </span>
                                </label>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>

                <!-- 投稿 / 作者 -->
                <div>
                    <h3 style="margin: 0 0 var(--space-2);">投稿 / 作者</h3>
                    <div class="grid grid-2" style="align-items:start;">
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${fn:startsWith(p.key, 'MENU_AUTHOR_')}">
                                <label class="card" style="display:flex; gap: var(--space-3); align-items:flex-start; cursor:pointer;">
                                    <input type="checkbox" name="permissions" value="${p.key}"
                                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                                           <c:if test="${lockedMap[p.key]}">disabled</c:if>
                                           style="margin-top: 4px;"/>
                                    <span>
                                        <div style="font-weight:600;"><c:out value="${p.name}"/></div>
                                        <div style="opacity:.75; font-size: 12px;"><c:out value="${p.desc}"/>（<c:out value="${p.key}"/>）</div>
                                    </span>
                                </label>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>

                <!-- 外审 / 审稿人 -->
                <div>
                    <h3 style="margin: 0 0 var(--space-2);">外审 / 审稿人</h3>
                    <div class="grid grid-2" style="align-items:start;">
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${fn:startsWith(p.key, 'MENU_REVIEWER_')}">
                                <label class="card" style="display:flex; gap: var(--space-3); align-items:flex-start; cursor:pointer;">
                                    <input type="checkbox" name="permissions" value="${p.key}"
                                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                                           <c:if test="${lockedMap[p.key]}">disabled</c:if>
                                           style="margin-top: 4px;"/>
                                    <span>
                                        <div style="font-weight:600;"><c:out value="${p.name}"/></div>
                                        <div style="opacity:.75; font-size: 12px;"><c:out value="${p.desc}"/>（<c:out value="${p.key}"/>）</div>
                                    </span>
                                </label>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>

                <!-- 编辑 -->
                <div>
                    <h3 style="margin: 0 0 var(--space-2);">编辑工作台</h3>
                    <div class="grid grid-2" style="align-items:start;">
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${fn:startsWith(p.key, 'MENU_EDITOR_')}">
                                <label class="card" style="display:flex; gap: var(--space-3); align-items:flex-start; cursor:pointer;">
                                    <input type="checkbox" name="permissions" value="${p.key}"
                                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                                           <c:if test="${lockedMap[p.key]}">disabled</c:if>
                                           style="margin-top: 4px;"/>
                                    <span>
                                        <div style="font-weight:600;"><c:out value="${p.name}"/></div>
                                        <div style="opacity:.75; font-size: 12px;"><c:out value="${p.desc}"/>（<c:out value="${p.key}"/>）</div>
                                    </span>
                                </label>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>

                <!-- 主编 -->
                <div>
                    <h3 style="margin: 0 0 var(--space-2);">主编工作台</h3>
                    <div class="grid grid-2" style="align-items:start;">
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${fn:startsWith(p.key, 'MENU_EIC_')}">
                                <label class="card" style="display:flex; gap: var(--space-3); align-items:flex-start; cursor:pointer;">
                                    <input type="checkbox" name="permissions" value="${p.key}"
                                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                                           <c:if test="${lockedMap[p.key]}">disabled</c:if>
                                           style="margin-top: 4px;"/>
                                    <span>
                                        <div style="font-weight:600;"><c:out value="${p.name}"/></div>
                                        <div style="opacity:.75; font-size: 12px;"><c:out value="${p.desc}"/>（<c:out value="${p.key}"/>）</div>
                                    </span>
                                </label>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>

                <!-- 编辑部管理员 -->
                <div>
                    <h3 style="margin: 0 0 var(--space-2);">编辑部管理员</h3>
                    <div class="grid grid-2" style="align-items:start;">
                        <c:forEach var="p" items="${permissions}">
                            <c:if test="${(fn:startsWith(p.key, 'MENU_EO_') or p.key == 'ADMIN_JOURNALS' or p.key == 'ADMIN_NEWS')}">
                                <label class="card" style="display:flex; gap: var(--space-3); align-items:flex-start; cursor:pointer;">
                                    <input type="checkbox" name="permissions" value="${p.key}"
                                           <c:if test="${assignedMap[p.key]}">checked</c:if>
                                           <c:if test="${lockedMap[p.key]}">disabled</c:if>
                                           style="margin-top: 4px;"/>
                                    <span>
                                        <div style="font-weight:600;"><c:out value="${p.name}"/></div>
                                        <div style="opacity:.75; font-size: 12px;"><c:out value="${p.desc}"/>（<c:out value="${p.key}"/>）</div>
                                    </span>
                                </label>
                            </c:if>
                        </c:forEach>
                    </div>
                </div>

            </div>

            <div style="display:flex; gap: var(--space-3); justify-content:flex-end;">
                <a class="btn btn-ghost" href="${ctx}/dashboard">返回工作台</a>
                <button class="btn btn-primary" type="submit" <c:if test="${lockedMap[p.key]}">disabled</c:if>>保存</button>
            </div>
        </form>
    </c:if>
</div>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
