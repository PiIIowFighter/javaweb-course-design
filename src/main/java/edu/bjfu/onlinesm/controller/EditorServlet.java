package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.model.Review;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 编辑部 / 编辑视角的稿件与审稿工作台。
 * 供：EDITOR_IN_CHIEF / EDITOR / EO_ADMIN 使用。
 *
 * 当前版本已经实现以下列表页的数据驱动：
 *  - /editor/desk          案头稿件列表（DESK_REVIEW_INITIAL）
 *  - /editor/toAssign      待分配审稿人稿件列表（TO_ASSIGN）
 *  - /editor/underReview   审稿中稿件列表（UNDER_REVIEW）
 *  - /editor/finalDecision 终审 / 录用与退稿决策列表
 */
@WebServlet(name = "EditorServlet", urlPatterns = {"/editor/*"})
public class EditorServlet extends HttpServlet {

    private final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. 必须已登录
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        // 2. 角色校验：主编、编辑、编辑部管理员可以访问
        String role = current.getRoleCode();
        if (role == null
                || (!"EDITOR_IN_CHIEF".equals(role)
                && !"EDITOR".equals(role)
                && !"EO_ADMIN".equals(role))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "只有主编/编辑/编辑部管理员可以访问编辑工作台。");
            return;
        }

        // 3. 解析子路径
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/dashboard";
        }

        try {
            switch (path) {
                case "/dashboard":
                    resp.sendRedirect(req.getContextPath() + "/dashboard");
                    break;

                case "/formalCheck":
                    // SUBMITTED / FORMAL_CHECK 状态稿件列表，供编辑部管理员“形式审查 / 格式检查”
                    handleFormalCheckList(req, resp, current);
                    break;

                case "/desk":
                    // 编辑部“案头稿件”列表（形式审查完毕，进入 DESK_REVIEW_INITIAL）
                    handleDeskList(req, resp, current);
                    break;

                case "/toAssign":
                    // 待分配责任编辑 / 外审专家的稿件列表（TO_ASSIGN）
                    handleToAssignList(req, resp, current);
                    break;

                case "/withEditor":
                    // 责任编辑处理中的稿件列表（WITH_EDITOR）
                    handleWithEditorList(req, resp, current);
                    break;

                case "/underReview":
                    // 审稿人外审中的稿件列表（UNDER_REVIEW）
                    handleUnderReviewList(req, resp, current);
                    break;

                case "/finalDecision":
                    // 终审 / 录用与退稿决策列表
                    handleFinalDecisionList(req, resp, current);
                    break;

                case "/reviewers":
                    // 主编管理“审稿人库”的页面
                    handleReviewerPoolPage(req, resp, current);
                    break;

                case "/overview":
                    // 主编全览：查看系统内全部稿件状态，并可跳转到稿件详情页查看审稿流程
                    handleChiefOverview(req, resp, current);
                    break;

                case "/special":
                    // 主编特殊权限：撤稿 / 撤销决策
                    handleChiefSpecialPage(req, resp, current);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("查询稿件列表时访问数据库出错", e);
        }
    }

    /**
     * SUBMITTED / FORMAL_CHECK 状态稿件列表。
     * 这些稿件由编辑部管理员执行“形式审查 / 格式检查”，
     * 通过后流转到 DESK_REVIEW_INITIAL（案头初审）。
     */
    private void handleFormalCheckList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list = manuscriptDAO.findByStatuses("SUBMITTED", "FORMAL_CHECK");
        req.setAttribute("manuscripts", list);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_list.jsp")
                .forward(req, resp);
    }

    private void handleDeskList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> deskList = manuscriptDAO.findByStatuses("DESK_REVIEW_INITIAL");
        req.setAttribute("manuscripts", deskList);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/desk_list.jsp")
                .forward(req, resp);
    }

    private void handleToAssignList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> toAssignList = manuscriptDAO.findByStatuses("TO_ASSIGN");
        req.setAttribute("manuscripts", toAssignList);

        // 若当前用户是主编，则额外查询所有 EDITOR，供“指派编辑”下拉框使用
        if ("EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            List<User> editors = userDAO.findByRoleCode("EDITOR");
            req.setAttribute("editorList", editors);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/editor/to_assign_list.jsp")
                .forward(req, resp);
    }

    private void handleWithEditorList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list = manuscriptDAO.findByStatuses("WITH_EDITOR");
        req.setAttribute("manuscripts", list);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/with_editor_list.jsp")
                .forward(req, resp);
    }

    private void handleUnderReviewList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {
        // 先尝试自动推进：当某稿件所有“有效邀请”的审稿记录都已 SUBMITTED，则
        // 将稿件状态从 UNDER_REVIEW 推进为 EDITOR_RECOMMENDATION（可提交编辑建议）。
        reviewDAO.promoteAllUnderReviewManuscriptsIfReady();

        // 页面需要同时展示：
        // 1) UNDER_REVIEW（外审进行中）列表
        // 2) EDITOR_RECOMMENDATION（外审完成，可提交编辑建议）下拉选择
        List<Manuscript> underReviewList = manuscriptDAO.findByStatuses("UNDER_REVIEW");
        List<Manuscript> readyList = manuscriptDAO.findByStatuses("EDITOR_RECOMMENDATION");

        req.setAttribute("underReviewList", underReviewList);
        req.setAttribute("readyList", readyList);

        // 兼容旧 JSP（若还在使用 ${manuscripts}）：
        req.setAttribute("manuscripts", underReviewList);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/under_review_list.jsp")
                .forward(req, resp);
    }

    private void handleFinalDecisionList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> finalList = manuscriptDAO.findByStatuses(
                "EDITOR_RECOMMENDATION",
                "FINAL_DECISION_PENDING",
                "ACCEPTED",
                "REJECTED"
        );
        req.setAttribute("manuscripts", finalList);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/final_decision_list.jsp")
                .forward(req, resp);
    }

    /**
     * 主编管理“审稿人库”的页面：仅允许 EDITOR_IN_CHIEF 访问。
     */
    private void handleReviewerPoolPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以管理审稿人库。");
            return;
        }

        List<User> reviewers = userDAO.findByRoleCode("REVIEWER");
        req.setAttribute("reviewers", reviewers);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_pool.jsp")
                .forward(req, resp);
    }

    /**
     * 主编“全览权限”：查看系统内所有稿件的状态，并可跳转到稿件详情页查看审稿流程/版本/附件。
     */
    private void handleChiefOverview(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以查看全览列表。");
            return;
        }

        List<Manuscript> list = manuscriptDAO.findAllForChief();
        req.setAttribute("manuscripts", list);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/chief_overview.jsp")
                .forward(req, resp);
    }

    /**
     * 主编“特殊权限”：撤稿（Retract）/ 撤销终审决定（Rescind Decision）。
     */
    private void handleChiefSpecialPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以访问特殊权限页面。");
            return;
        }

        // 只展示与“决策/撤稿”相关的稿件，避免列表过大
        List<Manuscript> list = manuscriptDAO.findByStatuses(
                "EDITOR_RECOMMENDATION",
                "FINAL_DECISION_PENDING",
                "REVISION",
                "ACCEPTED",
                "REJECTED"
        );
        req.setAttribute("manuscripts", list);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/chief_special.jsp")
                .forward(req, resp);
    }

    private User getCurrentUser(HttpServletRequest req) {
        Object obj = req.getSession().getAttribute("currentUser");
        if (obj instanceof User) {
            return (User) obj;
        }
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) {
            path = "";
        }

        try {
            switch (path) {
                case "/formalCheck":
                    // 编辑部管理员执行形式审查 / 格式检查
                    handleFormalCheckPost(req, resp, current);
                    break;

                case "/desk":
                    // 主编执行案头初审（Desk Accept / Desk Reject）
                    handleDeskDecisionPost(req, resp, current);
                    break;

                case "/toAssign":
                    // 主编为稿件指派责任编辑
                    handleAssignEditorPost(req, resp, current);
                    break;

                case "/finalDecision":
                    // 主编终审：录用 / 退稿 / 要求修回
                    handleFinalDecisionPost(req, resp, current);
                    break;

                case "/reviewers":
                    // 主编管理审稿人库：新增 / 启用 / 禁用
                    handleReviewerPoolPost(req, resp, current);
                    break;

                case "/review/invite":
                    // 编辑为稿件发出审稿邀请
                    handleInviteReviewerPost(req, resp, current);
                    break;

                case "/review/remind":
                    // 编辑对某个审稿记录执行催审
                    handleRemindReviewerPost(req, resp, current);
                    break;

                case "/recommend":
                    // 编辑根据审稿意见提出处理建议（EDITOR_RECOMMENDATION）
                    handleEditorRecommendPost(req, resp, current);
                    break;

                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("处理编辑操作时访问数据库出错", e);
        }
    }
    
    /**
     * 编辑 / 主编为稿件发出审稿邀请。
     * 支持一次邀请多名审稿人（多选），所有被选中的审稿人都会收到邀请记录。
     */
    private void handleInviteReviewerPost(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          User current)
            throws IOException, SQLException {

        // 只有 EDITOR 或 EDITOR_IN_CHIEF 才能邀请审稿人
        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑或主编可以邀请审稿人。");
            return;
        }

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String[] reviewerIdParams = req.getParameterValues("reviewerIds");
        String dueDateStr = req.getParameter("dueDate"); // yyyy-MM-dd，可为空

        // 兼容旧表单：如果没有 reviewerIds，则尝试读取单个 reviewerId
        if (reviewerIdParams == null || reviewerIdParams.length == 0) {
            String singleReviewerId = req.getParameter("reviewerId");
            if (singleReviewerId != null && !singleReviewerId.trim().isEmpty()) {
                reviewerIdParams = new String[]{singleReviewerId.trim()};
            }
        }

        if (manuscriptIdStr == null || reviewerIdParams == null || reviewerIdParams.length == 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());

        LocalDateTime dueAt = null;
        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            LocalDate d = LocalDate.parse(dueDateStr.trim());
            // 截止日期设为当天 23:59:59
            dueAt = d.atTime(23, 59, 59);
        }

        // 1. 对每个选中的审稿人，插入一条 INVITED 记录
        for (String reviewerIdStr : reviewerIdParams) {
            int reviewerId = Integer.parseInt(reviewerIdStr.trim());
            reviewDAO.inviteReviewer(manuscriptId, reviewerId, dueAt);
        }

        // 2. 如果稿件当前还在 WITH_EDITOR，就顺便把稿件状态改为 UNDER_REVIEW（送外审）
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m != null && "WITH_EDITOR".equals(m.getCurrentStatus())) {
            manuscriptDAO.updateStatus(manuscriptId, "UNDER_REVIEW");
        }

        // 回到“送外审稿件列表”
        resp.sendRedirect(req.getContextPath() + "/editor/underReview");
    }

    /**
     * 催审：编辑 / 主编对某个审稿记录执行催审，
     * 更新 RemindCount / LastRemindedAt 字段。
     */
    private void handleRemindReviewerPost(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          User current)
            throws IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑或主编可以催审。");
            return;
        }

        String reviewIdStr     = req.getParameter("reviewId");
        String manuscriptIdStr = req.getParameter("manuscriptId");

        if (reviewIdStr == null || manuscriptIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int reviewId     = Integer.parseInt(reviewIdStr);
        int manuscriptId = Integer.parseInt(manuscriptIdStr);

        // 更新 dbo.Reviews.RemindCount / LastRemindedAt
        reviewDAO.remind(reviewId);

        // 催审后跳回该稿件在送外审列表，可按需改成 detail 页面
        resp.sendRedirect(req.getContextPath()
                + "/editor/underReview?highlight=" + manuscriptId);
    }

    /**
     * 编辑根据外审意见给出“编辑建议”，将稿件状态流转到 EDITOR_RECOMMENDATION，
     * 并在 Manuscripts.Decision 字段记录建议（ACCEPT / MINOR_REVISION / MAJOR_REVISION / REJECT）。
     */
    private void handleEditorRecommendPost(HttpServletRequest req,
                                           HttpServletResponse resp,
                                           User current)
            throws IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑或主编可以提交编辑建议。");
            return;
        }

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String suggestion      = req.getParameter("suggestion");

        if (manuscriptIdStr == null
                || suggestion == null
                || suggestion.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr);
        String decision  = suggestion.trim();   // 直接作为 Decision 存入表中

        // 当稿件状态已进入 EDITOR_RECOMMENDATION（外审完成，可提交编辑建议），
        // 编辑提交建议后应推进到 FINAL_DECISION_PENDING，等待主编终审。
        String sql = "UPDATE dbo.Manuscripts "
                   + "SET Status = 'FINAL_DECISION_PENDING', "
                   + "    Decision = ?, "
                   + "    LastStatusTime = SYSUTCDATETIME() "
                   + "WHERE ManuscriptId = ?";

        try (java.sql.Connection conn = DbUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, decision);
            ps.setInt(2, manuscriptId);
            ps.executeUpdate();
        }

        // 提交编辑建议后，回到终审列表（你如果 GET 路由是 /editor/final 或 /editor/finalDecision，这里改成一致）
        resp.sendRedirect(req.getContextPath() + "/editor/finalDecision");
    }

    
    /**
     * 形式审查 / 格式检查操作（SUBMITTED ↔ FORMAL_CHECK / RETURNED / DESK_REVIEW_INITIAL）。
     * 仅允许 EO_ADMIN 调用。
     */
    private void handleFormalCheckPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以执行形式审查/格式检查操作。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        if (idStr == null || op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);

        switch (op) {
            case "start":
                // SUBMITTED -> FORMAL_CHECK
                manuscriptDAO.updateStatus(manuscriptId, "FORMAL_CHECK");
                break;
            case "approve":
                // FORMAL_CHECK -> DESK_REVIEW_INITIAL
                manuscriptDAO.updateStatus(manuscriptId, "DESK_REVIEW_INITIAL");
                break;
            case "return":
                // SUBMITTED / FORMAL_CHECK -> RETURNED（退回作者修改格式）
                manuscriptDAO.updateStatus(manuscriptId, "RETURNED");
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        resp.sendRedirect(req.getContextPath() + "/editor/formalCheck");
    }

    /**
     * 主编案头初审（Desk Review）：DESK_REVIEW_INITIAL -> TO_ASSIGN / REJECTED。
     */
    private void handleDeskDecisionPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以执行案头初审操作。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        if (idStr == null || op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);
        switch (op) {
            case "deskAccept":
                // DESK_REVIEW_INITIAL -> TO_ASSIGN
                manuscriptDAO.updateStatus(manuscriptId, "TO_ASSIGN");
                break;
            case "deskReject":
                // DESK_REVIEW_INITIAL -> REJECTED
                manuscriptDAO.updateFinalDecision(manuscriptId, "REJECT", "REJECTED");
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        resp.sendRedirect(req.getContextPath() + "/editor/desk");
    }

    /**
     * 主编为稿件指定责任编辑：TO_ASSIGN -> WITH_EDITOR。
     */
    private void handleAssignEditorPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以指派编辑。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        String editorIdStr = req.getParameter("editorId");
        if (idStr == null || editorIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);
        int editorId = Integer.parseInt(editorIdStr);

        manuscriptDAO.assignEditor(manuscriptId, editorId);
        resp.sendRedirect(req.getContextPath() + "/editor/toAssign");
    }

    /**
     * 主编终审：根据 op 参数决定 ACCEPT / REJECT / REVISION。
     */
    private void handleFinalDecisionPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以执行终审操作。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        if (idStr == null || op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);

        // 特殊权限操作需要读取当前状态做最基本校验
        Manuscript currentManuscript = manuscriptDAO.findById(manuscriptId);
        if (currentManuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }

        if ("rescind".equals(op)) {
            // 撤销终审决定：仅允许对已做出最终决定的稿件操作
            String st = currentManuscript.getCurrentStatus();
            if (!"ACCEPTED".equals(st) && !"REJECTED".equals(st) && !"REVISION".equals(st)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "只有已做出最终决定的稿件才可以撤销决策。");
                return;
            }
            manuscriptDAO.rescindDecision(manuscriptId);
            resp.sendRedirect(req.getContextPath() + "/editor/special");
            return;
        }

        if ("retract".equals(op)) {
            // 撤稿：主编可对任意非归档稿件执行撤稿（归档 + 标记撤稿）
            if ("ARCHIVED".equals(currentManuscript.getCurrentStatus())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "该稿件已经归档/撤稿，无需重复操作。");
                return;
            }
            manuscriptDAO.retractManuscript(manuscriptId);
            resp.sendRedirect(req.getContextPath() + "/editor/special");
            return;
        }

        // 常规终审决策：Accept / Reject / Revision
        String decision;
        String newStatus;
        switch (op) {
            case "accept":
                decision = "ACCEPT";
                newStatus = "ACCEPTED";
                break;
            case "reject":
                decision = "REJECT";
                newStatus = "REJECTED";
                break;
            case "revision":
                decision = "REVISION";
                newStatus = "REVISION";
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        manuscriptDAO.updateFinalDecision(manuscriptId, decision, newStatus);
        resp.sendRedirect(req.getContextPath() + "/editor/finalDecision");
    }

    /**
     * 审稿人库管理：邀请（创建待审核）/ 审核通过 / 启用 / 禁用审稿人账号。
     */
    private void handleReviewerPoolPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以管理审稿人库。");
            return;
        }

        // 兼容：op=invite/create/approve/disable/enable
        String op = req.getParameter("op");
        if (op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        if ("create".equals(op) || "invite".equals(op)) {
            // 邀请 / 新增审稿人账号：创建 REVIEWER 角色账号，默认状态为 PENDING（待审核）
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            String fullName = req.getParameter("fullName");
            String email = req.getParameter("email");
            String affiliation = req.getParameter("affiliation");
            String researchArea = req.getParameter("researchArea");

            if (username == null || username.isEmpty()
                    || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "用户名和密码不能为空。");
                return;
            }

            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(password);
            u.setFullName(fullName);
            u.setEmail(email);
            u.setAffiliation(affiliation);
            u.setResearchArea(researchArea);

            // 按结构图：邀请后需“审核资格”，因此先置为 PENDING
            u.setStatus("PENDING");

            userDAO.createUserWithRole(u, "REVIEWER");

        } else {
            // 审核 / 启用 / 禁用审稿人账号
            String userIdStr = req.getParameter("userId");
            if (userIdStr == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少用户 ID 参数。");
                return;
            }
            int userId = Integer.parseInt(userIdStr);

            if ("approve".equals(op)) {
                // 审核通过：PENDING -> ACTIVE
                userDAO.updateStatus(userId, "ACTIVE");
            } else if ("disable".equals(op)) {
                // 移除/禁用：ACTIVE/PENDING -> DISABLED
                userDAO.updateStatus(userId, "DISABLED");
            } else if ("enable".equals(op)) {
                // 重新启用：DISABLED -> ACTIVE
                userDAO.updateStatus(userId, "ACTIVE");
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
            }
        }

        resp.sendRedirect(req.getContextPath() + "/editor/reviewers");
    }


    

}
