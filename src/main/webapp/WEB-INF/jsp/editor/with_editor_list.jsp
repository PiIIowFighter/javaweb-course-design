<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:choose>
    <c:when test="${sessionScope.currentUser.roleCode == 'EDITOR'}">
        <h2>我的稿件（主编指派给我的稿件）</h2>
        <p>此页面仅展示当前登录编辑被主编指派负责的稿件（状态：WITH_EDITOR）。</p>
    </c:when>
    <c:otherwise>
        <h2>在编辑处处理中的稿件列表（所有编辑）</h2>
        <p>此页面展示所有处于 WITH_EDITOR 状态、正在由责任编辑处理的稿件，方便主编监控进度并催办。</p>
    </c:otherwise>
</c:choose>

<c:if test="${empty manuscripts}">
    <p>当前没有由编辑处理中的稿件。</p>
</c:if>
<c:if test="${not empty manuscripts}">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
    <tr>
        <th>稿件编号</th>
        <th>标题</th>
        <th>当前状态</th>
        <th>责任编辑</th>
        <th>分配时间</th>
        <c:if test="${sessionScope.currentUser.roleCode != 'EDITOR'}">
            <th>主编备注</th>
        </c:if>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${manuscripts}" var="m">
        <tr>
            <td><c:out value="${m.manuscriptId}"/></td>
            <td><c:out value="${m.title}"/></td>
            <td><c:out value="${m.currentStatus}"/></td>
            <td>
                <c:set var="assign" value="${latestAssignments[m.manuscriptId]}"/>
                <c:choose>
                    <c:when test="${not empty assign}">
                        <c:set var="ed" value="${editors[assign.editorId]}"/>
                        <c:choose>
                            <c:when test="${not empty ed}">
                                <c:choose>
                                    <c:when test="${sessionScope.currentUser.roleCode == 'EDITOR'}">
                                        我（<c:out value="${ed.fullName}"/>）
                                    </c:when>
                                    <c:otherwise>
                                        <c:out value="${ed.fullName}"/>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${not empty ed.email}">
                                    <span style="color:#666;">（<c:out value="${ed.email}"/>）</span>
                                </c:if>
                            </c:when>
                            <c:otherwise>
                                用户ID：<c:out value="${assign.editorId}"/>
                            </c:otherwise>
                        </c:choose>
                    </c:when>
                    <c:otherwise>
                        --
                    </c:otherwise>
                </c:choose>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty assign and not empty assign.assignedTime}">
                        <c:out value="${assign.assignedTime}"/>
                    </c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>

            <c:if test="${sessionScope.currentUser.roleCode != 'EDITOR'}">
                <td>
                    <c:choose>
                        <c:when test="${not empty assign and not empty assign.chiefComment}">
                            <c:out value="${assign.chiefComment}"/>
                        </c:when>
                        <c:otherwise>--</c:otherwise>
                    </c:choose>
                </td>
            </c:if>
            <td>
                <!-- 逻辑优化：统一“进入工作台”作为详情入口；审稿相关操作统一为“管理审稿人” -->
                <a class="btn btn-quiet" href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">进入工作台</a>
                <a class="btn" href="${pageContext.request.contextPath}/editor/review/select?manuscriptId=${m.manuscriptId}"
                   style="margin-left:6px;">管理审稿人</a>

                <!-- 主编 / 编辑部管理员可以对单条稿件发送“催办编辑”提醒 -->
                <c:if test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF' || sessionScope.currentUser.roleCode == 'EO_ADMIN'}">
                    <c:if test="${not empty assign}">
                        <form method="post"
                              action="${pageContext.request.contextPath}/editor/remindEditor"
                              style="display:inline;margin-left:8px;">
                            <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                            <input type="hidden" name="editorId" value="${assign.editorId}"/>
                            <button class="btn btn-primary" type="submit">提醒责任编辑</button>
                        </form>
                    </c:if>
                </c:if>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
