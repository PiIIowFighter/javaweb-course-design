package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.OperationLogger;
import edu.bjfu.onlinesm.dao.ManuscriptDAO;
import edu.bjfu.onlinesm.dao.UserDAO;
import edu.bjfu.onlinesm.dao.ReviewDAO;
import edu.bjfu.onlinesm.dao.NotificationDAO;
import edu.bjfu.onlinesm.dao.EditorSuggestionDAO;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.model.Notification;
import edu.bjfu.onlinesm.model.EditorSuggestion;
import edu.bjfu.onlinesm.dao.ManuscriptAssignmentDAO;
import edu.bjfu.onlinesm.util.mail.MailNotifications;
import edu.bjfu.onlinesm.util.mail.MailService;
import edu.bjfu.onlinesm.util.notify.InAppNotifications;

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
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
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
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final EditorSuggestionDAO editorSuggestionDAO = new EditorSuggestionDAO();
    private final ManuscriptAssignmentDAO assignmentDAO = new ManuscriptAssignmentDAO();
    private final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    private final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);

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

                case "/recommend":
                    // 提出建议：汇总审稿意见并向主编提交编辑建议（无最终决策权）
                    handleEditorRecommendPage(req, resp, current);
                    break;

                case "/review/monitor":
                    // 审稿监控：查看逾期审稿任务并执行催审
                    handleReviewMonitorPage(req, resp, current);
                    break;

                case "/review/remindForm":
                    // 手动催审：进入自定义邮件内容页面
                    handleReviewRemindFormPage(req, resp, current);
                    break;

                case "/review/detail":
                    // 查看审稿意见详情（供“提出建议”页面/主编终审页跳转）
                    handleEditorReviewDetailPage(req, resp, current);
                    break;

                case "/review/select":
                    // 选择审稿人页面（从稿件详情页跳转）
                    handleReviewSelectPage(req, resp, current);
                    break;

                case "/authorComm":
                    // 与作者沟通：按稿件列出沟通入口
                    handleAuthorCommList(req, resp, current);
                    break;

                case "/author/message":
                    // 与作者沟通：发送消息并查看沟通历史时间线
                    handleAuthorMessagePage(req, resp, current);
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

        // 只展示 UNDER_REVIEW（外审进行中）列表。
        // EDITOR_RECOMMENDATION 的稿件请在“提出建议”模块查看。
        List<Manuscript> underReviewList = manuscriptDAO.findByStatuses("UNDER_REVIEW");

        req.setAttribute("underReviewList", underReviewList);

        // 兼容旧 JSP（若还在使用 ${manuscripts}）：
        req.setAttribute("manuscripts", underReviewList);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/under_review_list.jsp")
                .forward(req, resp);
    }

    /**
     * 编辑提出建议页面：
     * - 不带 manuscriptId：展示当前编辑可提交建议的稿件列表（EDITOR_RECOMMENDATION）
     * - 带 manuscriptId：展示审稿意见汇总，并填写总结+建议后提交给主编
     */
    private void handleEditorRecommendPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String role = current.getRoleCode();
        if (!"EDITOR".equals(role) && !"EDITOR_IN_CHIEF".equals(role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑或主编可以访问提出建议页面。");
            return;
        }

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {

            // 列表页：仅展示状态为 EDITOR_RECOMMENDATION 的稿件
            List<Manuscript> ready = manuscriptDAO.findByStatuses("EDITOR_RECOMMENDATION");

            // 若是责任编辑，只展示分配给自己的稿件
            if ("EDITOR".equals(role)) {
                List<Manuscript> filtered = new ArrayList<>();
                for (Manuscript m : ready) {
                    Integer editorId = manuscriptDAO.findCurrentEditorId(m.getManuscriptId());
                    if (editorId != null && editorId == current.getUserId()) {
                        filtered.add(m);
                    }
                }
                ready = filtered;
            }

            req.setAttribute("readyList", ready);
            req.getRequestDispatcher("/WEB-INF/jsp/editor/recommend_list.jsp")
                    .forward(req, resp);
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // 权限：责任编辑只能看自己的；主编可看全部
        if ("EDITOR".equals(role)) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (editorId == null || editorId != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        // 审稿意见（只展示已提交 SUBMITTED）
        List<Review> all = reviewDAO.findByManuscript(manuscriptId);
        List<Review> submitted = new ArrayList<>();
        Map<String, Integer> stats = new HashMap<>();
        for (Review r : all) {
            if ("SUBMITTED".equalsIgnoreCase(r.getStatus())) {
                submitted.add(r);
                String rec = r.getRecommendation();
                if (rec == null) rec = "UNKNOWN";
                stats.put(rec, stats.getOrDefault(rec, 0) + 1);
            }
        }

        EditorSuggestion existing = editorSuggestionDAO.findByManuscriptId(manuscriptId);

        req.setAttribute("manuscript", m);
        req.setAttribute("submittedReviews", submitted);
        req.setAttribute("recommendStats", stats);
        req.setAttribute("editorSuggestion", existing);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/recommend_form.jsp")
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

        // 为列表补充“编辑建议/总结”
        List<Integer> ids = new ArrayList<>();
        for (Manuscript m : finalList) {
            ids.add(m.getManuscriptId());
        }
        Map<Integer, EditorSuggestion> suggestionMap = editorSuggestionDAO.findByManuscriptIds(ids);
        req.setAttribute("suggestionMap", suggestionMap);

        req.setAttribute("manuscripts", finalList);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/final_decision_list.jsp")
                .forward(req, resp);
    }


    /**
     * 主编管理“审稿人库”的页面：仅允许 EDITOR_IN_CHIEF 访问。
     */
    private void handleReviewerPoolPage (HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以管理审稿人库。");
            return;
        }

        String reviewerKeyword = req.getParameter("reviewerKeyword");
        if (reviewerKeyword == null) reviewerKeyword = "";
        String kw = reviewerKeyword.trim().toLowerCase();

        List<User> reviewers = userDAO.findByRoleCode("REVIEWER");
        if (!kw.isEmpty() && reviewers != null) {
            List<User> filtered = new ArrayList<>();
            for (User u : reviewers) {
                if (u == null) continue;
                String un = u.getUsername();
                String em = u.getEmail();
                String fn = u.getFullName();
                if ((un != null && un.toLowerCase().contains(kw))
                        || (em != null && em.toLowerCase().contains(kw))
                        || (fn != null && fn.toLowerCase().contains(kw))) {
                    filtered.add(u);
                }
            }
            reviewers = filtered;
        }

        req.setAttribute("reviewers", reviewers);
        req.setAttribute("reviewerKeyword", reviewerKeyword);
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
                        "DESK_REVIEW_INITIAL",
                        "TO_ASSIGN",
                        "WITH_EDITOR",
                        "UNDER_REVIEW",
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

                case "/special":
                    // 主编特殊权限：更改初审/终审决定、撤稿（仅已发表）
                    handleChiefSpecialPost(req, resp, current);
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
                    // 编辑对某个审稿记录执行催审（默认模板）
                    handleRemindReviewerPost(req, resp, current);
                    break;

                case "/review/remindCustom":
                    // 编辑在“审稿监控 / 手动催审”页面自定义邮件内容后提交
                    handleRemindReviewerCustomPost(req, resp, current);
                    break;

                case "/review/cancel":
                    // 解除/取消审稿人
                    handleCancelReviewerPost(req, resp, current);
                    break;

                case "/author/message":
                    // 与作者沟通：发送消息（站内/邮件）并可抄送主编
                    handleSendAuthorMessagePost(req, resp, current);
                    break;

                case "/review/autoRemindNow":
                    // 手动触发一次“自动催审”，便于模拟后台定时任务
                    handleAutoRemindNowPost(req, resp, current);
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

        // 0. 基本状态校验：仅允许对 WITH_EDITOR 的稿件发起外审邀请
        Manuscript mm = manuscriptDAO.findById(manuscriptId);
        if (mm == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }
        if (!"WITH_EDITOR".equals(mm.getCurrentStatus())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "该稿件当前状态不允许邀请审稿人（仅 WITH_EDITOR 可邀请）。");
            return;
        }

        // 1. 对每个选中的审稿人，插入一条 INVITED 记录
        for (String reviewerIdStr : reviewerIdParams) {
            int reviewerId = Integer.parseInt(reviewerIdStr.trim());
            int reviewId = reviewDAO.inviteReviewerReturnId(manuscriptId, reviewerId, dueAt);
            // 2.1 邀请审稿人：发送“审稿邀请邮件”（带摘要与截止日期）
            if (reviewId > 0) {
                mailNotifications.onReviewerInvited(reviewId);
                // 站内通知
                inAppNotifications.onReviewerInvited(reviewId);
            }
        }

        // 2. 如果稿件当前还在 WITH_EDITOR，就顺便把稿件状态改为 UNDER_REVIEW（送外审）
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m != null && "WITH_EDITOR".equals(m.getCurrentStatus())) {
            manuscriptDAO.updateStatusWithHistory(manuscriptId, "UNDER_REVIEW", "SEND_TO_REVIEW", current.getUserId(), "送外审");
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
        reviewDAO.remindChecked(reviewId);
        // 2.2 催审：发送“催促邮件”给审稿人
        mailNotifications.onReviewerRemind(reviewId);
        // 站内通知
        inAppNotifications.onReviewerRemind(reviewId);

        // 催审后跳回该稿件在送外审列表，可按需改成 detail 页面
        resp.sendRedirect(req.getContextPath()
                + "/editor/underReview?highlight=" + manuscriptId);
    }

    

    /**
     * 审稿监控页面：集中查看逾期审稿任务，并可以从这里进入手动催审。
     */
    private void handleReviewMonitorPage(HttpServletRequest req,
                                         HttpServletResponse resp,
                                         User current)
            throws ServletException, IOException, SQLException {

        // 参数：逾期天数 / 冷却天数 / 最大记录数，提供默认值
        int overdueDays  = parseIntOrDefault(req.getParameter("overdueDays"), 7);
        int cooldownDays = parseIntOrDefault(req.getParameter("cooldownDays"), 3);
        int limit        = parseIntOrDefault(req.getParameter("limit"), 50);

        // 查询符合条件的逾期审稿任务
        List<Review> overdue = reviewDAO.findOverdueForAutoRemind(overdueDays, cooldownDays, limit);

        // 同步查询稿件标题，方便在列表里展示
        Map<Integer, String> titleMap = new HashMap<>();
        for (Review r : overdue) {
            Manuscript m = manuscriptDAO.findById(r.getManuscriptId());
            if (m != null) {
                titleMap.put(r.getReviewId(), m.getTitle());
            }
        }

        // 读取一次性提示信息（比如自动催审后的结果）
        String message = (String) req.getSession().getAttribute("monitorMessage");
        if (message != null) {
            req.setAttribute("monitorMessage", message);
            req.getSession().removeAttribute("monitorMessage");
        }

        req.setAttribute("overdueReviews", overdue);
        req.setAttribute("monitorTitles", titleMap);
        req.setAttribute("monitorOverdueDays", overdueDays);
        req.setAttribute("monitorCooldownDays", cooldownDays);
        req.setAttribute("monitorLimit", limit);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/review_monitor.jsp")
                .forward(req, resp);
    }

    
    /**
     * 安全地解析 int 参数，如果为空或格式错误则返回默认值。
     */
    private int parseIntOrDefault(String s, int defaultValue) {
        if (s == null) {
            return defaultValue;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    
    /**
     * 手动催审：打开一个单独页面，让编辑自定义催审邮件内容。
     */
    private void handleReviewRemindFormPage(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
                && !"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑 / 主编 / 编辑部管理员可以访问审稿监控。");
            return;
        }

        String reviewIdStr = req.getParameter("reviewId");
        if (reviewIdStr == null || reviewIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 reviewId 参数。");
            return;
        }

        int reviewId = Integer.parseInt(reviewIdStr.trim());
        Review review = reviewDAO.findById(reviewId);
        if (review == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的审稿记录。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
        User reviewer = userDAO.findById(review.getReviewerId());

        // 简单默认文案（示例中提到“已逾期 3 天”，这里不强行计算具体天数）
        String defaultText = "请尽快提交您的审稿意见，本稿件的截止日期已过。";

        req.setAttribute("review", review);
        req.setAttribute("reviewManuscript", m);
        req.setAttribute("reviewReviewer", reviewer);
        req.setAttribute("defaultRemindText", defaultText);
        req.setAttribute("back", req.getParameter("back"));

        req.getRequestDispatcher("/WEB-INF/jsp/editor/review_remind_form.jsp")
                .forward(req, resp);
    }

    /**
     * 查看审稿意见详情（编辑/主编使用）。
     * GET: /editor/review/detail?reviewId=xxx
     */
    private void handleEditorReviewDetailPage(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
                && !"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑/主编/编辑部管理员可以查看审稿意见详情。\n");
            return;
        }

        String reviewIdStr = req.getParameter("reviewId");
        if (reviewIdStr == null || reviewIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 reviewId 参数。\n");
            return;
        }

        int reviewId;
        try {
            reviewId = Integer.parseInt(reviewIdStr.trim());
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "reviewId 参数格式不正确。\n");
            return;
        }

        Review review = reviewDAO.findById(reviewId);
        if (review == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的审稿记录。\n");
            return;
        }

        Manuscript m = manuscriptDAO.findById(review.getManuscriptId());
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应的稿件。\n");
            return;
        }

        // 责任编辑权限：只能查看自己负责的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(m.getManuscriptId());
            if (ceid != null && ceid != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。\n");
                return;
            }
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("review", review);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/review_detail.jsp")
                .forward(req, resp);
    }

    /**
     * 手动催审提交：使用自定义内容封装在标准模板中发送邮件。
     */
    private void handleRemindReviewerCustomPost(HttpServletRequest req,
                                                HttpServletResponse resp,
                                                User current)
            throws IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
                && !"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑 / 主编 / 编辑部管理员可以催审。");
            return;
        }

        String reviewIdStr     = req.getParameter("reviewId");
        String manuscriptIdStr = req.getParameter("manuscriptId");
        String back            = req.getParameter("back");
        String message         = req.getParameter("message");

        if (reviewIdStr == null || manuscriptIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int reviewId     = Integer.parseInt(reviewIdStr.trim());
        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());

        // 记录催审时间 / 次数
        reviewDAO.remindChecked(reviewId);
        // 发送自定义模板邮件
        mailNotifications.onReviewerRemindCustom(reviewId, message);
        // 站内通知给编辑 / 主编
        inAppNotifications.onReviewerRemind(reviewId);

        String ctx = req.getContextPath();
        if ("monitor".equals(back)) {
            resp.sendRedirect(ctx + "/editor/review/monitor");
        } else {
            resp.sendRedirect(ctx + "/manuscripts/detail?id=" + manuscriptId);
        }
    }

    /**
     * 执行一次“自动催审”：按当前规则批量给逾期审稿人发送催审邮件。
     */
    private void handleAutoRemindNowPost(HttpServletRequest req,
                                         HttpServletResponse resp,
                                         User current)
            throws IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
                && !"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑 / 主编 / 编辑部管理员可以催审。");
            return;
        }

        int overdueDays  = parseIntOrDefault(req.getParameter("overdueDays"), 7);
        int cooldownDays = parseIntOrDefault(req.getParameter("cooldownDays"), 3);
        int limit        = parseIntOrDefault(req.getParameter("limit"), 50);

        List<Review> overdue = reviewDAO.findOverdueForAutoRemind(overdueDays, cooldownDays, limit);

        int success = 0;
        for (Review r : overdue) {
            try {
                reviewDAO.remindChecked(r.getReviewId());
                mailNotifications.onReviewerRemind(r.getReviewId());
                inAppNotifications.onReviewerRemind(r.getReviewId());
                success++;
            } catch (Exception ignore) {
                // 单条失败不影响整体流程
            }
        }

        String summary = "按当前规则筛选到 " + overdue.size()
                + " 条逾期审稿任务，成功发送催审邮件 " + success + " 封。";

        req.getSession().setAttribute("monitorMessage", summary);

            resp.sendRedirect(req.getContextPath()
                    + "/editor/review/monitor?overdueDays=" + overdueDays
                    + "&cooldownDays=" + cooldownDays
                    + "&limit=" + limit);
    }

/**
     * 编辑根据外审意见给出“编辑建议”，将稿件状态流转到 EDITOR_RECOMMENDATION，
     * 并在 Manuscripts.Decision 字段记录建议（ACCEPT / MINOR_REVISION / MAJOR_REVISION / REJECT）。
     */
    private void handleEditorRecommendPost(HttpServletRequest req,
                                           HttpServletResponse resp,
                                           User current)
            throws IOException, SQLException {

        String role = current.getRoleCode();
        if (!"EDITOR".equals(role) && !"EDITOR_IN_CHIEF".equals(role)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑或主编可以提交编辑建议。");
            return;
        }

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String suggestionCode  = req.getParameter("suggestion");
        String summary         = req.getParameter("summary");

        if (manuscriptIdStr == null || suggestionCode == null || suggestionCode.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // 责任编辑只能提交自己负责的稿件
        if ("EDITOR".equals(role)) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (editorId == null || editorId != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        String code = suggestionCode.trim().toUpperCase();
        String decisionText;
        switch (code) {
            case "ACCEPT":
                decisionText = "Suggest Acceptance";
                break;
            case "MINOR_REVISION":
                decisionText = "Suggest Acceptance after Minor Revision";
                break;
            case "MAJOR_REVISION":
                decisionText = "Suggest Major Revision";
                break;
            case "REJECT":
                decisionText = "Suggest Reject";
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "未知建议类型。");
                return;
        }

        // 1) 保存编辑建议（含总结）
        EditorSuggestion s = new EditorSuggestion();
        s.setManuscriptId(manuscriptId);
        s.setEditorId(current.getUserId());
        s.setSuggestion(code);
        s.setSummary(summary);
        editorSuggestionDAO.upsert(s);

        // 2) 推进到待主编终审
        manuscriptDAO.updateStatusWithHistory(
                manuscriptId,
                "FINAL_DECISION_PENDING",
                "EDITOR_RECOMMENDATION_SUBMIT",
                current.getUserId(),
                "编辑提交建议：" + decisionText
        );

        // 兼容旧展示：写入 Manuscripts.Decision（但这不是最终决策）
        String sql = "UPDATE dbo.Manuscripts SET Decision = ? WHERE ManuscriptId = ?";
        try (java.sql.Connection conn = DbUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, decisionText);
            ps.setInt(2, manuscriptId);
            ps.executeUpdate();
        }

        // 3) 通知主编（站内通知）
        inAppNotifications.onEditorRecommendationSubmitted(manuscriptId, current, decisionText, summary);

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
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "FORMAL_CHECK", "FORMAL_CHECK_START", current.getUserId(), "开始形式审查");
                break;
            case "approve":
                // FORMAL_CHECK -> DESK_REVIEW_INITIAL
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "DESK_REVIEW_INITIAL", "FORMAL_CHECK_APPROVE", current.getUserId(), "形式审查通过");
                break;
            case "return":
                // SUBMITTED / FORMAL_CHECK -> RETURNED（退回作者修改格式）
                String issues = req.getParameter("issues");
                String guideUrl = req.getParameter("guideUrl");
                String remark = (issues != null && !issues.trim().isEmpty()) ? "退回原因：" + issues : "形式审查退回";
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(), remark);
                // 1.2 形式审查退回修改：自动邮件通知作者（问题列表/修改指南可选）
                mailNotifications.onFormalCheckReturn(manuscriptId, issues, guideUrl);
                // 站内通知
                inAppNotifications.onFormalCheckReturn(manuscriptId, issues);
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
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "TO_ASSIGN", "DESK_REVIEW_ACCEPT", current.getUserId(), "案头初审通过");
                break;
            case "deskReject":
                // DESK_REVIEW_INITIAL -> REJECTED
                manuscriptDAO.deskReject(manuscriptId);
                // deskReject方法内部已经记录了历史，这里不需要再记录
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
        // 新增：主编给编辑的文字建议
        String chiefComment = req.getParameter("chiefComment");

        if (idStr == null || editorIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr);
        int editorId = Integer.parseInt(editorIdStr);

        // 1）保持原有逻辑：更新稿件当前编辑、状态 -> WITH_EDITOR
        manuscriptDAO.assignEditor(manuscriptId, editorId);

        // 2）新增逻辑：记录“主编指派编辑”的建议
        assignmentDAO.createAssignment(
                manuscriptId,
                editorId,
                current.getUserId(),  // 当前登录用户即主编
                chiefComment
        );

        // 3.3 主编指派编辑：通知被指派编辑（按邮件实现）
        mailNotifications.onEditorAssigned(manuscriptId, current, editorId, chiefComment);
        // 站内通知
        inAppNotifications.onEditorAssigned(manuscriptId, current, editorId, chiefComment);

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

        int manuscriptId = Integer.parseInt(idStr.trim());

        Manuscript currentManuscript = manuscriptDAO.findById(manuscriptId);
        if (currentManuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该稿件。");
            return;
        }

        // 终审列表页仅允许对待终审稿件作出终审决定。
        // 改判/撤稿等“回滚/更改”行为统一走 /editor/special。
        String st = currentManuscript.getCurrentStatus();
        if (!("FINAL_DECISION_PENDING".equals(st) || "EDITOR_RECOMMENDATION".equals(st))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "该稿件当前状态不允许在终审列表页操作；如需改判/撤稿请到“主编特殊权限”页面。");
            return;
        }

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
        // 1.4 主编终审决策：通知作者（同时通知编辑）
        String decisionText = "accept".equals(op) ? "Accepted" : ("reject".equals(op) ? "Rejected" : "Revision Required");
        mailNotifications.onFinalDecision(manuscriptId, decisionText);
        // 站内通知
        inAppNotifications.onFinalDecision(manuscriptId, decisionText);
        resp.sendRedirect(req.getContextPath() + "/editor/finalDecision");
    }

    private void handleChiefSpecialPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        if (!"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有主编可以执行该操作。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        String op = req.getParameter("op");
        String reason = req.getParameter("reason");

        if (idStr == null || op == null || reason == null || reason.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数（manuscriptId/op/reason）。");
            return;
        }

        int manuscriptId = Integer.parseInt(idStr.trim());
        String reasonText = reason.trim();

        try {
            if ("changeDesk".equals(op)) {
                String deskOp = req.getParameter("deskOp");
                if (deskOp == null || deskOp.trim().isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 deskOp 参数。");
                    return;
                }
                manuscriptDAO.changeDeskDecision(manuscriptId, deskOp.trim(), current.getUserId(), reasonText);
                OperationLogger.log(req, "EDITOR_CHIEF", "CHANGE_DESK_DECISION", "manuscriptId=" + manuscriptId + "; deskOp=" + deskOp + "; reason=" + reasonText);

            } else if ("changeFinal".equals(op)) {
                String finalOp = req.getParameter("finalOp");
                if (finalOp == null || finalOp.trim().isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 finalOp 参数。");
                    return;
                }
                manuscriptDAO.changeFinalDecision(manuscriptId, finalOp.trim(), current.getUserId(), reasonText);
                OperationLogger.log(req, "EDITOR_CHIEF", "CHANGE_FINAL_DECISION", "manuscriptId=" + manuscriptId + "; finalOp=" + finalOp + "; reason=" + reasonText);

                // 改判后同步通知作者（可按需要扩展通知编辑/审稿人）
                String decisionText = "accept".equals(finalOp) ? "Accepted" : ("reject".equals(finalOp) ? "Rejected" : "Revision Required");
                mailNotifications.onFinalDecision(manuscriptId, decisionText);
                // 站内通知
                inAppNotifications.onFinalDecision(manuscriptId, decisionText);

            } else if ("retract".equals(op)) {
                manuscriptDAO.retractPublished(manuscriptId, current.getUserId(), reasonText);
                OperationLogger.log(req, "EDITOR_CHIEF", "RETRACT_PUBLISHED", "manuscriptId=" + manuscriptId + "; reason=" + reasonText);
                mailNotifications.onRetract(manuscriptId);
                // 站内通知
                inAppNotifications.onRetract(manuscriptId);

            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
            }
        } catch (IllegalStateException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/editor/special");
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
            // 4.1 主编“邀请新审稿人”：发送邀请邮件（含账号信息）
            mailNotifications.onInviteNewReviewer(u, password);
            // 站内通知
            inAppNotifications.onInviteNewReviewer(u);

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


    


    // ========================= 选择/解除审稿人 =========================

    /**
     * 选择审稿人页面（从稿件详情页进入一个单独页面选择）。
     * URL: /editor/review/select?manuscriptId=xxx
     */
    private void handleReviewSelectPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
                && !"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑/主编/编辑部管理员可以选择审稿人。");
            return;
        }

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && ceid != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        String reviewerKeyword = req.getParameter("reviewerKeyword");
        if (reviewerKeyword == null) reviewerKeyword = "";

        Integer minCompleted = null;
        Integer minAvgScore = null;
        try {
            String mc = req.getParameter("minCompleted");
            if (mc != null && !mc.trim().isEmpty()) minCompleted = Integer.parseInt(mc.trim());
        } catch (Exception ignore) {}
        try {
            String ms = req.getParameter("minAvgScore");
            if (ms != null && !ms.trim().isEmpty()) minAvgScore = Integer.parseInt(ms.trim());
        } catch (Exception ignore) {}

        // 可选：默认用研究主题做一次建议关键词
        if (reviewerKeyword.trim().isEmpty() && m.getSubjectArea() != null) {
            reviewerKeyword = m.getSubjectArea();
        }

List<User> reviewers = userDAO.searchReviewerPool(reviewerKeyword, minCompleted, minAvgScore, 100);

// 仅展示“仍然有效/已提交”的审稿记录；撤回/拒绝/过期的不在此处展示，也不应阻止再次邀请
List<Review> allReviews = reviewDAO.findByManuscript(manuscriptId);
List<Review> currentReviews = new ArrayList<>();
Set<Integer> assignedReviewerIds = new HashSet<>(); // 仅 INVITED/ACCEPTED 视为“仍被分配”
for (Review r : allReviews) {
    String st = r.getStatus();
    if ("INVITED".equalsIgnoreCase(st) || "ACCEPTED".equalsIgnoreCase(st)) {
        currentReviews.add(r);
        assignedReviewerIds.add(r.getReviewerId());
    } else if ("SUBMITTED".equalsIgnoreCase(st)) {
        currentReviews.add(r);
    }
}

Map<Integer, User> reviewerMap = new HashMap<>();
for (Review r : currentReviews) {
    User u = userDAO.findById(r.getReviewerId());
    if (u != null) reviewerMap.put(r.getReviewerId(), u);
}

        req.setAttribute("manuscript", m);
        req.setAttribute("reviewers", reviewers);
        req.setAttribute("reviewerKeyword", reviewerKeyword);
        req.setAttribute("currentReviews", currentReviews);
        req.setAttribute("reviewerMap", reviewerMap);
        req.setAttribute("assignedReviewerIds", assignedReviewerIds);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/select_reviewers.jsp").forward(req, resp);
    }

    /**
     * 解除/取消审稿人（编辑在“添加审稿人/详情页”中使用）。
     * POST/GET: /editor/review/cancel
     *
     * 说明：
     * - 撤回=删除该条审稿记录（不写入额外状态），因此被撤回的审稿人可再次邀请；
     * - 为避免“点击后看起来没反应”，撤回后会回跳到 backTo / Referer 并携带 cancelMsg 提示。
     */
    private void handleCancelReviewerPost(HttpServletRequest req, HttpServletResponse resp, User current)
        throws IOException, SQLException {

    if (!"EDITOR".equals(current.getRoleCode())
            && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
            && !"EO_ADMIN".equals(current.getRoleCode())) {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑/主编/编辑部管理员可以解除审稿人。");
        return;
    }

    String reviewIdStr = req.getParameter("reviewId");
    String manuscriptIdStr = req.getParameter("manuscriptId");
    if (reviewIdStr == null || manuscriptIdStr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
        return;
    }

    int reviewId = Integer.parseInt(reviewIdStr.trim());
    int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());

    Review r = reviewDAO.findById(reviewId);
    if (r == null || r.getManuscriptId() != manuscriptId) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应审稿记录。");
        return;
    }

    // EDITOR 只能操作分配给自己的稿件
    if ("EDITOR".equals(current.getRoleCode())) {
        Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
        if (ceid != null && ceid != current.getUserId()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
            return;
        }
    }

    // 1) 先撤回对审稿人的分配（仅允许 INVITED/ACCEPTED 且未提交）
    int updated = reviewDAO.cancelAssignment(reviewId);

    // 2) 撤回后：按需求仅检查是否仍有在审分配；若无则退回上一阶段。
    if (updated > 0) {
        // 通知审稿人（站内）
        notificationDAO.create(
                r.getReviewerId(),
                current.getUserId(),
                "SYSTEM",
                "REVIEW_CANCEL",
                "审稿任务已解除",
                "编辑已解除该稿件的审稿任务。如有疑问请联系编辑部。",
                manuscriptId
        );

        OperationLogger.log(req, "EDITOR", "解除审稿人",
                "解除审稿人 reviewId=" + reviewId + ", manuscriptId=" + manuscriptId);
    }

    // 3) 兜底：若当前稿件仍处于 UNDER_REVIEW，检查是否已没有“仍被分配”的审稿人。
    //    - 若没有：退回上一阶段 WITH_EDITOR
    //    - 若还有：保持在 UNDER_REVIEW
    boolean rolledBack = false;
    try {
        Manuscript curM = manuscriptDAO.findById(manuscriptId);
        if (curM != null && "UNDER_REVIEW".equalsIgnoreCase(curM.getCurrentStatus())) {
            // “仍被分配”的审稿人：INVITED/ACCEPTED；已提交（SUBMITTED）也视为仍然存在审稿记录，不应回退。
            List<Review> still = reviewDAO.findByManuscript(manuscriptId);
            int effectiveCnt = 0;
            for (Review rr : still) {
                String st = rr.getStatus();
                if ("INVITED".equalsIgnoreCase(st)
                        || "ACCEPTED".equalsIgnoreCase(st)
                        || "SUBMITTED".equalsIgnoreCase(st)) {
                    effectiveCnt++;
                }
            }
            if (effectiveCnt <= 0) {
                manuscriptDAO.updateStatusWithHistory(
                        manuscriptId,
                        "WITH_EDITOR",
                        "ROLLBACK_NO_REVIEWER",
                        current.getUserId(),
                        "撤回审稿人后已无在审分配，退回责任编辑阶段"
                );
                rolledBack = true;
            }
        }
    } catch (Exception ignore) {
        // 避免回退检查失败导致 500
    }

    // 4) 回跳：优先 backTo，其次 Referer；并携带 cancelMsg 提示，避免用户误以为“没反应”
    String msg;
    if (updated > 0) {
        msg = rolledBack
                ? "已撤回该审稿人分配，稿件已无在审审稿人，已退回责任编辑阶段。"
                : "已撤回该审稿人分配。";
    } else {
        msg = "撤回未生效：该审稿人当前不可撤回（可能已提交或已撤回）。";
    }

    String ctx = req.getContextPath();
    String target = null;

    // backTo（表单可传当前页面 URL）
    String backTo = req.getParameter("backTo");
    if (backTo != null) {
        backTo = backTo.trim();
        if (!backTo.isEmpty()) {
            if (backTo.startsWith(ctx + "/")) {
                target = backTo;
            } else if (backTo.startsWith("/")) {
                target = ctx + backTo;
            }
        }
    }

    // Referer（兼容 <a href> GET 触发）
    if (target == null) {
        String ref = req.getHeader("Referer");
        if (ref != null) {
            int idx = ref.indexOf(ctx + "/");
            if (idx >= 0) {
                target = ref.substring(idx);
            }
        }
    }

    // 默认回到稿件详情页
    if (target == null) {
        target = ctx + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers";
    }

    // 追加 cancelMsg（注意处理 #fragment）
    String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
    int hash = target.indexOf('#');
    String frag = "";
    if (hash >= 0) {
        frag = target.substring(hash);
        target = target.substring(0, hash);
    }
    target = target + (target.contains("?") ? "&" : "?") + "cancelMsg=" + encoded + frag;

    resp.sendRedirect(target);
}

    // ========================= 与作者沟通 ==========================

    /**
     * 与作者沟通入口（按稿件列出）。
     * GET: /editor/authorComm
     */
    private void handleAuthorCommList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list;
        if ("EDITOR".equals(current.getRoleCode())) {
            list = manuscriptDAO.findByStatusesForEditor(
                    current.getUserId(),
                    "WITH_EDITOR",
                    "UNDER_REVIEW",
                    "EDITOR_RECOMMENDATION",
                    "FINAL_DECISION_PENDING",
                    "REVISION",
                    "RETURNED"
            );
        } else {
            // 主编/编辑部管理员：列出系统中这些状态的稿件
            list = manuscriptDAO.findByStatuses(
                    "WITH_EDITOR",
                    "UNDER_REVIEW",
                    "EDITOR_RECOMMENDATION",
                    "FINAL_DECISION_PENDING",
                    "REVISION",
                    "RETURNED"
            );
        }

        Map<Integer, Integer> countMap = new HashMap<>();
        for (Manuscript m : list) {
            int cnt = notificationDAO.countByManuscriptAndCategory(m.getManuscriptId(), "AUTHOR_MESSAGE");
            countMap.put(m.getManuscriptId(), cnt);
        }

        req.setAttribute("manuscripts", list);
        req.setAttribute("commCountMap", countMap);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/author_comm_list.jsp").forward(req, resp);
    }

    /**
     * 与作者沟通：发送消息并展示沟通历史（时间线）。
     * GET: /editor/author/message?manuscriptId=xxx
     */
    private void handleAuthorMessagePage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && ceid != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        User author = null;
        if (m.getSubmitterId() != null) {
            author = userDAO.findById(m.getSubmitterId());
        }

        List<Notification> history = notificationDAO.listByManuscriptAndCategory(
                manuscriptId,
                "AUTHOR_MESSAGE",
                null,
                200,
                true
        );

        Map<Integer, User> userMap = new HashMap<>();
        // 当前用户
        userMap.put(current.getUserId(), current);
        if (author != null) userMap.put(author.getUserId(), author);
        for (Notification n : history) {
            Integer cb = n.getCreatedByUserId();
            if (cb != null && !userMap.containsKey(cb)) {
                User u = userDAO.findById(cb);
                if (u != null) userMap.put(cb, u);
            }
            int ru = n.getRecipientUserId();
            if (!userMap.containsKey(ru)) {
                User u = userDAO.findById(ru);
                if (u != null) userMap.put(ru, u);
            }
        }

        List<User> chiefs = userDAO.findByRoleCode("EDITOR_IN_CHIEF");

        String flash = (String) req.getSession().getAttribute("authorMessageFlash");
        if (flash != null) {
            req.setAttribute("authorMessageFlash", flash);
            req.getSession().removeAttribute("authorMessageFlash");
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("authorUser", author);
        req.setAttribute("history", history);
        req.setAttribute("userMap", userMap);
        req.setAttribute("chiefEditors", chiefs);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/author_message.jsp").forward(req, resp);
    }

    /**
     * 发送消息给作者（站内消息或邮件），支持抄送主编。
     * POST: /editor/author/message
     */
    private void handleSendAuthorMessagePost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws IOException, SQLException {

        if (!"EDITOR".equals(current.getRoleCode())
                && !"EDITOR_IN_CHIEF".equals(current.getRoleCode())
                && !"EO_ADMIN".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑/主编/编辑部管理员可以给作者发消息。");
            return;
        }

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String title = req.getParameter("title");
        String content = req.getParameter("content");

        boolean sendSystem = req.getParameter("sendSystem") != null;
        boolean sendEmail = req.getParameter("sendEmail") != null;
        boolean ccChief = req.getParameter("ccChief") != null;

        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && ceid != current.getUserId()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        if (content == null || content.trim().isEmpty()) {
            req.getSession().setAttribute("authorMessageFlash", "消息内容不能为空。");
            resp.sendRedirect(req.getContextPath() + "/editor/author/message?manuscriptId=" + manuscriptId);
            return;
        }

        if (!sendSystem && !sendEmail) {
            // 默认至少走站内消息
            sendSystem = true;
        }

        if (title == null) title = "";
        title = title.trim();
        if (title.isEmpty()) {
            title = "稿件沟通（稿件ID=" + manuscriptId + "）";
        }

        User author = null;
        if (m.getSubmitterId() != null) {
            author = userDAO.findById(m.getSubmitterId());
        }
        if (author == null) {
            req.getSession().setAttribute("authorMessageFlash", "未找到作者用户，无法发送。");
            resp.sendRedirect(req.getContextPath() + "/editor/author/message?manuscriptId=" + manuscriptId);
            return;
        }

        List<User> chiefs = userDAO.findByRoleCode("EDITOR_IN_CHIEF");

        // 1) 站内消息
        if (sendSystem) {
            notificationDAO.create(
                    author.getUserId(),
                    current.getUserId(),
                    "SYSTEM",
                    "AUTHOR_MESSAGE",
                    title,
                    content,
                    manuscriptId
            );
            if (ccChief && chiefs != null) {
                for (User ce : chiefs) {
                    if (ce == null) continue;
                    notificationDAO.create(
                            ce.getUserId(),
                            current.getUserId(),
                            "SYSTEM",
                            "AUTHOR_MESSAGE",
                            "[抄送] " + title,
                            "（抄送主编）\n\n" + content,
                            manuscriptId
                    );
                }
            }
        }

        // 2) 邮件
        boolean emailOk = true;
        if (sendEmail) {
            String subject = "[OnlineSM] " + title;
            StringBuilder body = new StringBuilder();
            body.append("稿件ID：").append(manuscriptId).append("\n");
            body.append("稿件标题：").append(m.getTitle() == null ? "" : m.getTitle()).append("\n");
            body.append("发件人：").append(current.getFullName() == null ? current.getUsername() : current.getFullName()).append("\n\n");
            body.append(content);

            try {
                MailService.sendText(author.getEmail(), subject, body.toString());
                if (ccChief && chiefs != null) {
                    for (User ce : chiefs) {
                        if (ce == null || ce.getEmail() == null) continue;
                        MailService.sendText(ce.getEmail(), "[抄送] " + subject, body.toString());
                    }
                }
            } catch (Exception ex) {
                emailOk = false;
            }
        }

        OperationLogger.log(req, "EDITOR", "发送作者消息", "发送消息给作者 manuscriptId=" + manuscriptId + ", sendSystem=" + sendSystem + ", sendEmail=" + sendEmail + ", ccChief=" + ccChief);

        String okText = "消息已发送" + (sendEmail && !emailOk ? "（邮件发送失败，仅站内消息生效）" : "。");
        req.getSession().setAttribute("authorMessageFlash", okText);
        resp.sendRedirect(req.getContextPath() + "/editor/author/message?manuscriptId=" + manuscriptId);
    }


}