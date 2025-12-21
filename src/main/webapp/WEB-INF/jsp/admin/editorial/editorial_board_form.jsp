<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<h2>编辑委员会成员编辑</h2>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<form action="${ctx}/admin/editorial/save" method="post" style="max-width: 800px;">
    <c:if test="${not empty member.boardMemberId}">
        <input type="hidden" name="boardMemberId" value="${member.boardMemberId}"/>
    </c:if>

    <table border="1" cellpadding="6" cellspacing="0" style="width: 100%;">
        <tr>
            <th style="width: 160px;">期刊</th>
            <td>
                <select name="journalId" required>
                    <c:forEach var="j" items="${journals}">
                        <option value="${j.journalId}" <c:if test="${member.journalId == j.journalId}">selected</c:if>>
                            ${j.name}
                        </option>
                    </c:forEach>
                </select>
            </td>
        </tr>
        <tr>
            <th>成员（系统用户）</th>
            <td>
                <select name="userId" required>
                    <c:forEach var="u" items="${users}">
                        <option value="${u.userId}" <c:if test="${member.userId == u.userId}">selected</c:if>>
                            <c:choose>
                                <c:when test="${not empty u.fullName}">${u.fullName}</c:when>
                                <c:otherwise>${u.username}</c:otherwise>
                            </c:choose>
                            (${u.username}) - ${u.roleCode} - ${u.status}
                        </option>
                    </c:forEach>
                </select>
                <div style="color:#666; font-size: 12px; margin-top: 4px;">
                    提示：如需添加校外编委，可先在“用户管理”中创建账号（可设置为 EDITOR/REVIEWER 等），再在此处加入编委会。
                </div>
            </td>
        </tr>
        <tr>
            <th>职务</th>
            <td>
                <input type="text" name="position" value="${member.position}" placeholder="如：主编 / 副主编 / 编委" required style="width: 100%;"/>
            </td>
        </tr>
        <tr>
            <th>栏目/分工</th>
            <td>
                <input type="text" name="section" value="${member.section}" placeholder="可选，如：AI/数据科学/林业信息化" style="width: 100%;"/>
            </td>
        </tr>
        <tr>
            <th>简介</th>
            <td>
                <textarea name="bio" rows="6" style="width: 100%;" placeholder="可选：个人简介/研究方向/单位等">${member.bio}</textarea>
            </td>
        </tr>
    </table>

    <div style="margin-top: 12px;">
        <button type="submit"><i class="bi bi-save"></i> 保存</button>
        <a style="margin-left: 10px;" href="${ctx}/admin/editorial/list?journalId=${member.journalId}">返回列表</a>
    </div>
</form>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
