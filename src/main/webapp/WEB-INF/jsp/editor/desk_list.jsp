<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="page-head">
        <h2 class="page-title">案头审查</h2>
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
    <p>当前没有需要案头审查的稿件。</p>
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
                            <input type="hidden" name="rejectReason" value=""/>
                            <button type="submit" name="op" value="deskAccept">
                                送外审 / 指派编辑
                            </button>
                            <button type="submit" name="op" value="deskReject"
                                    onclick="return deskReject(this);">
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




<script>
  function deskReject(btn) {
    try {
      var form = btn && btn.closest ? btn.closest('form') : null;
      if (!form) return false;

      var reason = window.prompt('请输入退稿理由（作者可见）：', '');
      if (reason === null) return false; // cancel
      reason = (reason || '').trim();
      if (!reason) {
        alert('退稿理由不能为空。');
        return false;
      }
      var input = form.querySelector('input[name="rejectReason"]');
      if (input) input.value = reason;

      return confirm('确认退稿？退稿理由将同步给作者，并记录在状态历史中。');
    } catch (e) {
      return confirm('确认退稿？');
    }
  }
</script>

<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
