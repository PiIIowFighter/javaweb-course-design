<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%--
  重要：提交评审时必须携带 reviewId。
  你目前从 /reviewer/reviewForm?id=xx 进入，因此这里兼容从 param.id / param.reviewId / requestScope.reviewId 获取。
--%>
<c:choose>
    <c:when test="${not empty param.reviewId}">
        <c:set var="rid" value="${param.reviewId}"/>
    </c:when>
    <c:when test="${not empty param.id}">
        <c:set var="rid" value="${param.id}"/>
    </c:when>
    <c:otherwise>
        <c:set var="rid" value="${reviewId}"/>
    </c:otherwise>
</c:choose>

<h2>提交评审意见</h2>

<c:if test="${empty rid}">
    <p style="color:red;">缺少 reviewId 参数，无法提交评审。请从“待审列表”重新进入。</p>
</c:if>

<c:if test="${not empty rid}">

<form method="post" action="${ctx}/reviewer/submit">
    <%-- 兼容后端可能取 reviewId 或 id --%>
    <input type="hidden" name="reviewId" value="${rid}"/>
    <input type="hidden" name="id" value="${rid}"/>

    <p>请分别填写：<b>给编辑的保密意见（Confidential to Editor）</b> 与 <b>给作者的意见（Comments to Author）</b>。</p>

    <h3>给编辑的保密意见（Confidential to Editor）</h3>

    <p>
        <label>关键评价（Key Evaluation，可选）：<br/>
            <textarea name="keyEvaluation" rows="4" cols="80" placeholder="可简要概括稿件优缺点、关键风险与是否建议送外审/接收等"></textarea>
        </label>
    </p>

    <h4>多维评分（打分表选 2）</h4>
    <table border="1" cellpadding="6" cellspacing="0" style="border-collapse:collapse; min-width: 520px;">
        <thead>
        <tr>
            <th>维度</th>
            <th>分值（0-10，整数）</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td>ScoreOriginality（原创性）</td>
            <td><input type="number" name="scoreOriginality" min="0" max="10" step="1" required value="5"/></td>
        </tr>
        <tr>
            <td>ScoreSignificance（重要性/影响力）</td>
            <td><input type="number" name="scoreSignificance" min="0" max="10" step="1" required value="5"/></td>
        </tr>
        <tr>
            <td>ScoreMethodology（方法/技术质量）</td>
            <td><input type="number" name="scoreMethodology" min="0" max="10" step="1" required value="5"/></td>
        </tr>
        <tr>
            <td>ScorePresentation（表达/结构）</td>
            <td><input type="number" name="scorePresentation" min="0" max="10" step="1" required value="5"/></td>
        </tr>
        </tbody>
    </table>

    <p>
        <label>总体分（Score，必填，0-10）：
            <%--
              兼容后端：有的实现读取 score，有的读取 scoreOverall。
              这里同时提交两份。
            --%>
            <input type="number" name="score" id="score" min="0" max="10" step="1" required value="5"/>
            <input type="hidden" name="scoreOverall" id="scoreOverall" value="5"/>
        </label>
    </p>

    <p>
        <label>推荐结论（必选）：
            <select name="recommendation" required>
                <option value="ACCEPT">接受</option>
                <option value="MINOR_REVISION">小修后接受</option>
                <option value="MAJOR_REVISION">大修后再审</option>
                <option value="REJECT">拒稿</option>
            </select>
        </label>
    </p>

    <p>
        <label>给编辑的保密意见（必填）：<br/>
            <textarea name="confidentialToEditor" rows="6" cols="80" required
                      placeholder="仅编辑/主编可见，包含是否存在学术不端风险、重要缺陷、建议处理方式等"></textarea>
        </label>
    </p>

    <h3>给作者的意见（Comments to Author）</h3>
    <p>
        <label>具体修改建议（必填）：<br/>
            <textarea name="commentsToAuthor" rows="10" cols="80" required
                      placeholder="作者可见：指出需要修改的具体问题、建议补充的实验/引用、表达结构等"></textarea>
        </label>
    </p>

    <%-- 兼容旧后端可能仍然读取 content 字段作为作者意见 --%>
    <input type="hidden" name="content" id="contentCompat" value=""/>

    <p>
        <button type="submit">提交评审</button>
    </p>
</form>

<script>
    (function () {
        function v(id) {
            var el = document.querySelector('[name="' + id + '"]');
            if (!el) return null;
            var val = el.value;
            if (val === null || val === undefined || val === '') return null;
            var n = Number(val);
            return Number.isFinite(n) ? n : null;
        }

        function computeOverallIfEmpty() {
            var s = v('score');
            var o = v('scoreOriginality');
            var g = v('scoreSignificance');
            var m = v('scoreMethodology');
            var p = v('scorePresentation');

            // 同步两个总体分字段
            var scoreEl = document.getElementById('score');
            var scoreOverallEl = document.getElementById('scoreOverall');
            if (scoreEl && scoreOverallEl) {
                scoreOverallEl.value = scoreEl.value;
            }

            // 兼容旧后端：content 可能被当作作者意见
            var cta = document.querySelector('[name="commentsToAuthor"]');
            var compat = document.getElementById('contentCompat');
            if (cta && compat) {
                compat.value = cta.value || '';
            }

            // 若总体分为空，则用四项均值填充
            if (scoreEl && (scoreEl.value === '' || scoreEl.value === null || scoreEl.value === undefined)) {
                if (o !== null && g !== null && m !== null && p !== null) {
                    var avg = Math.round((o + g + m + p) / 4);
                    scoreEl.value = String(avg);
                    if (scoreOverallEl) scoreOverallEl.value = String(avg);
                }
            }
        }

        // 绑定变化事件
        ['score', 'scoreOriginality', 'scoreSignificance', 'scoreMethodology', 'scorePresentation', 'commentsToAuthor']
            .forEach(function (name) {
                var el = document.querySelector('[name="' + name + '"]');
                if (el) {
                    el.addEventListener('input', computeOverallIfEmpty);
                    el.addEventListener('change', computeOverallIfEmpty);
                }
            });

        // 初次同步
        computeOverallIfEmpty();
    })();
</script>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
