<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}" />

<div class="page-head">
  <h2 class="page-title">筛选并指派责任编辑</h2>
  <div class="chips">
    <span class="chip">支持按研究方向筛选 + 关键词搜索 + 分页</span>
    <a class="btn" href="${ctx}/editor/toAssign">返回待指派列表</a>
  </div>
</div>

<c:if test="${empty manuscript}">
  <p style="color:#d00;">未找到稿件或缺少 manuscriptId 参数。</p>
</c:if>

<c:if test="${not empty manuscript}">

  <div class="card" style="max-width: 1100px;">
    <div class="card-header">
      <div>
        <h3 class="card-title">稿件信息</h3>
        <p class="card-subtitle">
          稿件ID：<strong><c:out value="${manuscript.manuscriptId}"/></strong>
          &nbsp;|&nbsp;
          标题：<strong><c:out value="${manuscript.title}"/></strong>
        </p>
      </div>
    </div>
    <div class="card-body">
      <div style="display:flex; gap:14px; flex-wrap:wrap;">
        <div>
          <div class="muted">领域类型（SubjectArea）</div>
          <div><c:out value="${empty manuscript.subjectArea ? '--' : manuscript.subjectArea}"/></div>
        </div>
        <div>
          <div class="muted">关键词（Keywords）</div>
          <div><c:out value="${empty manuscript.keywords ? '--' : manuscript.keywords}"/></div>
        </div>
      </div>
    </div>
  </div>

  <!-- 筛选条件（GET） -->
  <div class="card" style="max-width: 1100px; margin-top:14px;">
    <div class="card-header">
      <h3 class="card-title">筛选条件</h3>
    </div>
    <div class="card-body">
      <form method="get" action="${ctx}/editor/toAssign/pickEditor" style="display:flex; gap:10px; flex-wrap:wrap; align-items:flex-end;">
        <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}"/>

        <div style="min-width:240px;">
          <label class="muted">关键词（姓名/用户名/邮箱/单位/研究方向）</label>
          <input class="input" type="text" name="q" value="${filterQ}" placeholder="如：张三 / zhangsan / xxx@xx.com" />
        </div>

        <div style="min-width:240px;">
          <label class="muted">研究方向筛选（匹配 ResearchArea）</label>
          <input class="input" type="text" name="area" value="${filterArea}" placeholder="${autoFillArea ? '已默认填入稿件领域，可直接搜索' : '如：人工智能 / 能源 / 图像处理'}" />
        </div>

        <div style="display:flex; gap:10px; align-items:center;">
          <label style="display:flex; align-items:center; gap:6px;">
            <input type="checkbox" name="matchOnly" value="1" <c:if test="${filterMatchOnly}">checked</c:if> />
            仅显示与稿件领域匹配
          </label>
        </div>

        <div>
          <label class="muted">每页条数</label>
          <select class="input" name="pageSize">
            <c:set var="ps" value="${pageSize}" />
            <option value="10" <c:if test="${ps == 10}">selected</c:if>>10</option>
            <option value="20" <c:if test="${ps == 20}">selected</c:if>>20</option>
            <option value="50" <c:if test="${ps == 50}">selected</c:if>>50</option>
          </select>
        </div>

        <button class="btn" type="submit">应用筛选</button>
      </form>

      <div class="muted" style="margin-top:10px;">
        提示：若编辑未维护研究方向（ResearchArea 为空），在“仅显示匹配”开启时可能被过滤掉。
      </div>
    </div>
  </div>

  <!-- 指派表单（POST） -->
  <div class="card" style="max-width: 1100px; margin-top:14px;">
    <div class="card-header">
      <h3 class="card-title">编辑列表</h3>
      <p class="card-subtitle">按“匹配分”排序（分越高越匹配）。选择一个编辑后提交指派。</p>
    </div>
    <div class="card-body">

      <c:if test="${empty editors}">
        <p style="color:#d00;">没有找到符合条件的编辑。请调整筛选条件后重试。</p>
      </c:if>

      <c:if test="${not empty editors}">
        <form method="post" action="${ctx}/editor/toAssign">
          <input type="hidden" name="manuscriptId" value="${manuscript.manuscriptId}" />

          <table border="1" cellpadding="6" cellspacing="0" style="width:100%; border-collapse:collapse;">
            <thead>
              <tr>
                <th style="width:60px;">选择</th>
                <th style="width:80px;">匹配分</th>
                <th style="width:140px;">姓名</th>
                <th style="width:140px;">用户名</th>
                <th style="width:220px;">邮箱</th>
                <th style="width:220px;">单位</th>
                <th>研究方向（ResearchArea）</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach items="${editors}" var="e" varStatus="st">
                <tr>
                  <td style="text-align:center;">
                    <input type="radio" name="editorId" value="${e.userId}" <c:if test="${st.index == 0}">checked</c:if> />
                  </td>
                  <td style="text-align:center; font-weight:700;">
                    <c:out value="${editorScoreMap[e.userId]}"/>
                  </td>
                  <td><c:out value="${e.fullName}"/></td>
                  <td><c:out value="${e.username}"/></td>
                  <td><c:out value="${e.email}"/></td>
                  <td><c:out value="${empty e.affiliation ? '--' : e.affiliation}"/></td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty e.researchArea}">
                        <c:out value="${e.researchArea}"/>
                      </c:when>
                      <c:otherwise>
                        <span class="muted">未填写</span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>

          <%@ include file="/WEB-INF/jsp/common/pagination.jspf" %>

          <div style="margin-top:14px;">
            <label class="muted">写给责任编辑的说明（仅编辑可见，可选）</label>
            <textarea class="input" name="chiefComment" rows="3" style="width:100%;" placeholder="例如：重点关注创新性、实验可重复性、是否需要补充对比实验等。"></textarea>
          </div>

          <div style="display:flex; gap:10px; margin-top:14px;">
            <button class="btn" type="submit">确认指派责任编辑</button>
            <a class="btn" href="${ctx}/editor/toAssign">取消</a>
          </div>

        </form>
      </c:if>

    </div>
  </div>

</c:if>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
