<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<h2>历史评审记录</h2>
<p>以下为您已经完成的所有评审记录。</p>

<c:if test="${empty reviews}">
    <div style="padding: 20px; background-color: #f9f9f9; border: 1px solid #ddd;">
        <p>目前还没有完成的评审记录。</p>
    </div>
</c:if>

<c:if test="${not empty reviews}">
<table border="1" cellpadding="4" cellspacing="0" style="width:100%; border-collapse: collapse;">
    <thead>
    <tr style="background-color: #f2f2f2;">
        <th style="padding: 8px; text-align: center;">审稿记录ID</th>
        <th style="padding: 8px; text-align: center;">稿件编号</th>
        <th style="padding: 8px; text-align: center;">提交时间</th>
        <th style="padding: 8px; text-align: center;">总体评分</th>
        <th style="padding: 8px; text-align: center;">多维评分</th>
        <th style="padding: 8px; text-align: center;">推荐意见</th>
        <th style="padding: 8px; text-align: center;">状态</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${reviews}" var="r">
        <tr>
            <td style="padding: 8px; text-align: center;"><c:out value="${r.reviewId}"/></td>
            <td style="padding: 8px; text-align: center;">MS-<c:out value="${r.manuscriptId}"/></td>
            <td style="padding: 8px; text-align: center;">
                <c:if test="${not empty r.submittedAt}">
                    <%-- 兼容LocalDateTime类型，先转字符串处理，避免格式转换异常 --%>
                    <c:set var="dateStr" value="${r.submittedAt.toString()}"/>
                    <c:choose>
                        <c:when test="${fn:contains(dateStr, 'T')}">
                            <c:set var="datePart" value="${fn:substring(dateStr, 0, 10)}"/>
                            <c:set var="timePart" value="${fn:substring(dateStr, 11, 16)}"/>
                            ${datePart} ${timePart}
                        </c:when>
                        <c:otherwise>
                            <fmt:formatDate value="${r.submittedAt}" pattern="yyyy-MM-dd HH:mm"/>
                        </c:otherwise>
                    </c:choose>
                </c:if>
                <c:if test="${empty r.submittedAt}">-</c:if>
            </td>
            <td style="padding: 8px; text-align: center;">
                <c:choose>
                    <c:when test="${not empty r.score}">
                        <span style="font-weight:bold; color:#0066cc;">
                            <fmt:formatNumber value="${r.score}" pattern="0.0"/>/10
                        </span>
                    </c:when>
                    <c:otherwise>未评分</c:otherwise>
                </c:choose>
            </td>
            <td style="padding: 8px; text-align: center;">
                <small>
                    创新: <c:out value="${r.scoreOriginality != null ? r.scoreOriginality : 'N/A'}"/><br>
                    重要: <c:out value="${r.scoreSignificance != null ? r.scoreSignificance : 'N/A'}"/><br>
                    方法: <c:out value="${r.scoreMethodology != null ? r.scoreMethodology : 'N/A'}"/><br>
                    呈现: <c:out value="${r.scorePresentation != null ? r.scorePresentation : 'N/A'}"/>
                </small>
            </td>
            <td style="padding: 8px; text-align: center;">
                <c:choose>
                    <c:when test="${r.recommendation == 'ACCEPT'}">
                        <span style="color:green; font-weight:bold;">接受</span>
                    </c:when>
                    <c:when test="${r.recommendation == 'MINOR_REVISION'}">
                        <span style="color:orange; font-weight:bold;">小修</span>
                    </c:when>
                    <c:when test="${r.recommendation == 'MAJOR_REVISION'}">
                        <span style="color:#cc6600; font-weight:bold;">大修</span>
                    </c:when>
                    <c:when test="${r.recommendation == 'REJECT'}">
                        <span style="color:red; font-weight:bold;">拒稿</span>
                    </c:when>
                    <c:otherwise><c:out value="${r.recommendation}"/></c:otherwise>
                </c:choose>
            </td>
            <td style="padding: 8px; text-align: center;">
                <span style="color:green; font-weight:bold;">
                    <c:choose>
                        <c:when test="${r.status == 'SUBMITTED'}">已提交</c:when>
                        <c:otherwise><c:out value="${r.status}"/></c:otherwise>
                    </c:choose>
                </span>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</c:if>

<div style="margin-top:20px; padding:15px; background-color:#f9f9f9; border-left:4px solid #ccc;">
    <h4>说明：</h4>
    <ul>
        <li>评审意见提交后，系统将自动锁定稿件文件，无法再次下载</li>
        <li>如需查看评审详情，请联系编辑或主编</li>
        <li>评分标准：10分制（0-10分），5分为及格线</li>
        <li>多维评分中的 N/A 表示该维度未评分</li>
    </ul>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>