<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="u" value="${user}"/>

<div class="card" style="max-width: 880px; margin: 0 auto;">
    <div class="card-header">
        <div>
            <h2 class="card-title">个人信息</h2>
            <p class="card-subtitle">更新邮箱、资料与附件。信息会用于投稿与审稿流程中的联系与识别。</p>
        </div>
    </div>

    <div class="stack">
        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-success"><c:out value="${message}"/></div>
        </c:if>

        <form action="${ctx}/profile" method="post" enctype="multipart/form-data" class="stack">
            <div class="grid grid-2">
                <div class="form-row">
                    <label>用户名</label>
                    <div>
                        <strong><c:out value="${u.username}"/></strong>
                        <div class="help">用户名不可修改</div>
                    </div>
                </div>

                <div class="form-row">
                    <label for="email">邮箱</label>
                    <input id="email" type="email" name="email" value="${u.email}" placeholder="name@example.com"/>
                </div>

                <div class="form-row">
                    <label for="fullName">姓名</label>
                    <input id="fullName" type="text" name="fullName" value="${u.fullName}" placeholder="可选"/>
                </div>

                <div class="form-row">
                    <label for="affiliation">单位 / 机构</label>
                    <input id="affiliation" type="text" name="affiliation" value="${u.affiliation}" placeholder="可选"/>
                </div>
            </div>

            <div class="form-row">
                <label for="researchArea">研究方向 / 联系方式</label>
                <div>
                    <textarea id="researchArea" name="researchArea" rows="3" placeholder="研究方向、手机号等（可选）">${u.researchArea}</textarea>
                    <div class="help">可在此填写研究方向、手机号等联系方式信息。</div>
                </div>
            </div>

            <div class="grid grid-2">
                <div class="form-row">
                    <label for="avatar">上传头像</label>
                    <div class="stack">
                        <input id="avatar" type="file" name="avatar" accept="image/*"/>
                        <c:if test="${not empty avatarPath}">
                            <div class="help">当前头像</div>
                            <img src="${avatarPath}" alt="头像" style="max-height:96px; border-radius: 16px; border: 1px solid var(--border);"/>
                        </c:if>
                    </div>
                </div>

                <div class="form-row">
                    <label for="resume">上传简历附件</label>
                    <div class="stack">
                        <input id="resume" type="file" name="resume" accept=".pdf,.doc,.docx"/>
                        <c:if test="${not empty resumePath}">
                            <div class="help">当前简历：<a href="${resumePath}" target="_blank">查看已上传简历</a></div>
                        </c:if>
                    </div>
                </div>
            </div>

            <div class="actions">
                <button class="btn-primary" type="submit">
                    <i class="bi bi-save" aria-hidden="true"></i>
                    保存修改
                </button>
                <a class="btn-quiet" href="${ctx}/dashboard" style="text-decoration:none;">返回工作台</a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jsp" %>
