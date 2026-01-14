<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="card stack-lg">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">关于期刊页面</h2>
</div></div>
    </div>

    <p>
        期刊：<b><c:out value="${journal.name}"/></b>
        &nbsp;&nbsp;|&nbsp;&nbsp;
        <a href="${ctx}/admin/journals/list">返回期刊管理</a>
    </p>

    <div class="table-wrap">
        <table class="table-fixed" border="1" cellpadding="6" cellspacing="0" style="background:#fff; width:100%; max-width: 1100px;">
            <thead>
            <tr>
                <th style="width:180px;">页面</th>
                <th style="width:170px;">对应 URL</th>
                <th>标题</th>
                <th style="width:200px;">更新时间</th>
                <th style="width:120px;">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td><b>Publish</b>（论文发表 / 投稿与出版）</td>
                <td><a href="${ctx}/publish" target="_blank">${ctx}/publish</a></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['publish']}">
                            <c:out value="${pageMap['publish'].title}"/>
                        </c:when>
                        <c:otherwise><span style="color:#999;">未配置</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['publish']}"><c:out value="${pageMap['publish'].updatedAt}"/></c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${ctx}/admin/journals/pages/edit?journalId=${journal.journalId}&pageKey=publish">编辑</a>
                </td>
            </tr>

            <tr>
                <td><b>Guide</b>（用户指南 / 投稿指南）</td>
                <td><a href="${ctx}/guide" target="_blank">${ctx}/guide</a></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['guide']}">
                            <c:out value="${pageMap['guide'].title}"/>
                        </c:when>
                        <c:otherwise><span style="color:#999;">未配置</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['guide']}"><c:out value="${pageMap['guide'].updatedAt}"/></c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${ctx}/admin/journals/pages/edit?journalId=${journal.journalId}&pageKey=guide">编辑</a>
                </td>
            </tr>

            <tr>
                <td><b>Aims</b>（办刊宗旨 / Aims &amp; Scope）</td>
                <td><a href="${ctx}/about/aims" target="_blank">${ctx}/about/aims</a></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['aims']}">
                            <c:out value="${pageMap['aims'].title}"/>
                        </c:when>
                        <c:otherwise><span style="color:#999;">未配置</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['aims']}"><c:out value="${pageMap['aims'].updatedAt}"/></c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${ctx}/admin/journals/pages/edit?journalId=${journal.journalId}&pageKey=aims">编辑</a>
                </td>
            </tr>

            <tr>
                <td><b>Policies</b>（政策与流程 / Ethics &amp; Policies）</td>
                <td><a href="${ctx}/about/policies" target="_blank">${ctx}/about/policies</a></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['policies']}">
                            <c:out value="${pageMap['policies'].title}"/>
                        </c:when>
                        <c:otherwise><span style="color:#999;">未配置</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty pageMap['policies']}"><c:out value="${pageMap['policies'].updatedAt}"/></c:when>
                        <c:otherwise>-</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <a href="${ctx}/admin/journals/pages/edit?journalId=${journal.journalId}&pageKey=policies">编辑</a>
                </td>
            </tr>

            </tbody>
        </table>
    </div>

    <p class="card-subtitle" style="margin-top: 10px;">
        提示：保存后，前台对应页面将展示最新内容。
    </p>
</div>



<%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>
<jsp:include page="/WEB-INF/jsp/common/footer.jsp" />

<%--
/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

--%>
