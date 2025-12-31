<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not empty sessionScope.currentUser}">
            </section>
        </div>
</c:if>
</div>
</main>

<footer class="site-footer">
    <div class="container footer-inner">
        <button type="button" class="btn-quiet" onclick="history.back();">
            <i class="bi bi-arrow-left" aria-hidden="true"></i>
            返回上一页
        </button>
        <small>&copy; 2025 科研论文在线投稿及管理系统 · JavaWeb 课程设计</small>
    </div>
</footer>
</body>
</html>