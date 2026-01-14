<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card">
    <div class="card-header">
        <div>
            <div class="page-head">
        <h2 class="page-title">编辑部工作台</h2>
</div></div>
    </div>

    <div class="grid grid-2">
        
        

        <c:choose>
            <c:when test="${sessionScope.currentUser.roleCode == 'EDITOR_IN_CHIEF'}">
                
            </c:when>
            <c:otherwise>
                <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/withEditor">
                    <h3><i class="bi bi-person-workspace" aria-hidden="true"></i> 我的待处理稿件</h3>
                    <p>处理 WITH_EDITOR：为稿件选择审稿人并发起外审。</p>
                    <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
                </a>
            </c:otherwise>
        </c:choose>

        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/underReview">
            <h3><i class="bi bi-hourglass-split" aria-hidden="true"></i> 审稿中</h3>
            <p>处理 UNDER_REVIEW：跟踪审稿进度与回收评审意见。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>
        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/review/monitor">
            <h3><i class="bi bi-bell" aria-hidden="true"></i> 审稿监控 / 催审</h3>
            <p>集中查看逾期审稿任务，手动或自动发送催审提醒。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开监控面板</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/recommend">
            <h3><i class="bi bi-lightbulb" aria-hidden="true"></i> 提出建议</h3>
            <p>综合审稿意见，向主编提交建议（如 Suggest Acceptance）。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 进入提出建议</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/authorComm">
            <h3><i class="bi bi-chat-dots" aria-hidden="true"></i> 与作者沟通</h3>
            <p>通过站内消息或邮件与作者沟通，解释审稿意见/要求澄清，并可抄送主编。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 查看沟通入口</small>
        </a>

        <a class="card" style="text-decoration:none;" href="${pageContext.request.contextPath}/editor/finalDecision">
            <h3><i class="bi bi-check2-circle" aria-hidden="true"></i> 终审决策</h3>
            <p>编辑推荐意见与主编终审：录用 / 退稿 / 退修。</p>
            <small><i class="bi bi-arrow-right" aria-hidden="true"></i> 打开列表</small>
        </a>
       
    </div>

    <div class="stack" style="margin-top: var(--space-6);">
        <small>后续可在列表页中接入更丰富的筛选与统计，按状态机（DRAFT、SUBMITTED、FORMAL_CHECK、UNDER_REVIEW、REVISION、ACCEPTED、REJECTED 等）展示不同环节。</small>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>

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
