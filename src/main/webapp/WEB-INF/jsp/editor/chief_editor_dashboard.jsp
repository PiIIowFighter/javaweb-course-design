<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="card">
    <div class="card-header">
        <div>
            <h2 class="card-title">主编工作台</h2>
            <p class="card-subtitle">
                期刊的最高学术负责人，从全局视角统筹稿件流转，负责初审、分配编辑、终审决策与审稿人库管理。
            </p>
        </div>
    </div>

    <div class="grid grid-2">
        <!-- 系统全览 -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/overview">
            <h3><i class="bi bi-eye" aria-hidden="true"></i> 系统全览</h3>
            <p>
                查看系统内所有稿件的当前状态与历史流转轨迹，并可进入稿件详情页，
                可查看版本、附件与评审记录，便于整体把控刊物运行。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 进入系统全览
            </small>
        </a>

        <!-- 案头初审（Desk Review） -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/desk">
            <h3><i class="bi bi-search" aria-hidden="true"></i> 案头审查（Desk Review）</h3>
            <p>
                处理 DESK_REVIEW_INITIAL：对通过形式审查的稿件进行学术初筛，
                决定 Desk Accept（进入分配环节）或 Desk Reject（直接退稿）。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 查看待初审稿件
            </small>
        </a>

        <!-- 待分配队列 -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/toAssign">
            <h3><i class="bi bi-diagram-3" aria-hidden="true"></i> 待分配队列</h3>
            <p>
                处理 TO_ASSIGN：为稿件指派具体责任编辑，并可一并协调后续外审人选，
                推动稿件正式进入外审流程。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 打开待分配列表
            </small>
        </a>

        <!-- 终审决策（Final Decision） -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/finalDecision">
            <h3><i class="bi bi-check2-circle" aria-hidden="true"></i> 终审决策（Final Decision）</h3>
            <p>
                汇总外审意见与责任编辑推荐，做出最终学术决定：
                录用（Accept）、退稿（Reject）或修回（Revision），并形成终审记录。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 查看待终审稿件
            </small>
        </a>

        <!-- 审稿人库管理 -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/reviewers">
            <h3><i class="bi bi-people" aria-hidden="true"></i> 审稿人库管理</h3>
            <p>
                维护期刊审稿人库：邀请新审稿人、启用 / 停用现有审稿人账号，
                管理研究方向、机构信息，为外审环节提供支撑。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 管理审稿人库
            </small>
        </a>

        <!-- 特殊权限操作（如果后端有 /editor/special 对应实现） -->
        <a class="card" style="text-decoration:none;"
           href="${pageContext.request.contextPath}/editor/special">
            <h3><i class="bi bi-exclamation-triangle" aria-hidden="true"></i> 特殊权限操作</h3>
            <p>
                对已录用或已发布稿件执行高风险操作，例如撤稿（Retract）、
                撤销终审决定（Rescind Decision）等，仅主编可用。
            </p>
            <small>
                <i class="bi bi-arrow-right" aria-hidden="true"></i> 进入特殊操作页面
            </small>
        </a>
    </div>

    <div class="stack" style="margin-top: var(--space-6);">
        <small>
            主编工作台围绕稿件状态机（SUBMITTED、FORMAL_CHECK、DESK_REVIEW_INITIAL、
            TO_ASSIGN、WITH_EDITOR、UNDER_REVIEW、REVISION、ACCEPTED、REJECTED 等）
            提供全流程入口，后续可在各列表中补充筛选、统计与图表分析。
        </small>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
