<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="page-head">
        <h2 class="page-title">案头稿件列表（FORMAL_CHECK 之后、待进一步处理）</h2>
        <div class="chips">
            <span class="chip">此页面展示已经通过形式审查、进入编辑部案头阶段（状态：DESK_REVIEW_INITIAL）的稿件。</span>
        </div>
    </div><!--
  响应式优化：
  - 表格外层使用 table-wrap，窄屏可横向滚动；
  - 操作区按钮使用 flex-wrap，避免挤压导致大片空白/错位。
  （不改全局 CSS，仅在本页做轻量样式增强）
-->
<style>
  .desk-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
  }
  .desk-actions form { display: inline-flex; flex-wrap: wrap; gap: 10px; margin: 0; }
  .desk-actions a.btn { text-decoration: none; }

  @media (max-width: 520px) {
    .desk-actions { gap: 8px; }
    .desk-actions form { width: 100%; }
    .desk-actions form button { flex: 1 1 auto; }
    .desk-actions a.btn { width: 100%; justify-content: center; }
  }
</style>

<c:if test="${empty manuscripts}">
    <p>当前没有需要处理的案头稿件。</p>
</c:if>
<c:if test="${not empty manuscripts}">
<div class="table-wrap">
<table border="1" cellpadding="4" cellspacing="0">
    <thead>
    <tr>
        <th>稿件编号</th>
        <th>标题</th>
        <th>当前状态</th>
        <th>提交时间</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${manuscripts}" var="m">
        <tr>
            <td>
                <a href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">
                    <c:out value="${m.manuscriptId}"/>
                </a>
            </td>
            <td><c:out value="${m.title}"/></td>
            <td><c:out value="${m.currentStatus}"/></td>
            <td>
                <c:choose>
                    <c:when test="${m.submitTime != null}">
                        <c:out value="${m.submitTime}"/>
                    </c:when>
                    <c:otherwise>--</c:otherwise>
                </c:choose>
            </td>
            <td>
                <div class="desk-actions">
                    <a class="btn" href="${pageContext.request.contextPath}/manuscripts/detail?id=${m.manuscriptId}">
                        查看详情
                    </a>

                    <c:if test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                        <form method="post" action="${pageContext.request.contextPath}/editor/desk">
                            <input type="hidden" name="manuscriptId" value="${m.manuscriptId}"/>
                            <button type="submit" name="op" value="deskAccept">
                                送外审 / 指派编辑
                            </button>
                            <button type="submit" name="op" value="deskReject"
                                    onclick="return confirm('确认直接退稿？该操作将把稿件状态标记为 REJECTED。');">
                                退稿
                            </button>
                        </form>
                    </c:if>
                </div>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</div>
</c:if>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
