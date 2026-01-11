package edu.bjfu.onlinesm.controller;

import edu.bjfu.onlinesm.util.DbUtil;
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
import edu.bjfu.onlinesm.dao.FormalCheckResultDAO;
import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.service.FormalCheckService;
import edu.bjfu.onlinesm.service.PlagiarismCheckService;
import edu.bjfu.onlinesm.util.OperationLogger;
import edu.bjfu.onlinesm.dao.ManuscriptAssignmentDAO;
import edu.bjfu.onlinesm.dao.ManuscriptRecommendedReviewerDAO;
import edu.bjfu.onlinesm.dao.ManuscriptAuthorDAO;
import edu.bjfu.onlinesm.dao.ManuscriptVersionDAO;
import edu.bjfu.onlinesm.model.ManuscriptRecommendedReviewer;
import edu.bjfu.onlinesm.model.ManuscriptAuthor;
import edu.bjfu.onlinesm.model.ManuscriptVersion;
import edu.bjfu.onlinesm.util.mail.MailNotifications;
import edu.bjfu.onlinesm.util.mail.MailService;
import edu.bjfu.onlinesm.util.notify.InAppNotifications;
import edu.bjfu.onlinesm.util.UploadPathUtil;
import edu.bjfu.onlinesm.util.PdfTextUtil;
import edu.bjfu.onlinesm.util.MenuPermissionGuard;
import edu.bjfu.onlinesm.util.FileTextUtil;
import edu.bjfu.onlinesm.util.PermissionCatalog;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import org.json.JSONObject;
import edu.bjfu.onlinesm.util.PaginationUtil;

/**
 
 * 拆分为 3 个面向角色的 Servlet：
 *  - EditorWorkServlet          （责任编辑 / 编辑功能）
 *  - EditorialOfficeServlet     （编辑部管理员功能）
 *  - ChiefEditorServlet         （主编功能）
 *
 * 该基类不再绑定任何 URL
 */
public abstract class EditorServlet extends HttpServlet {

    protected final ManuscriptDAO manuscriptDAO = new ManuscriptDAO();
    protected final UserDAO userDAO = new UserDAO();
    protected final ReviewDAO reviewDAO = new ReviewDAO();
    protected final NotificationDAO notificationDAO = new NotificationDAO();
    protected final EditorSuggestionDAO editorSuggestionDAO = new EditorSuggestionDAO();
    protected final ManuscriptAssignmentDAO assignmentDAO = new ManuscriptAssignmentDAO();
    protected final ManuscriptRecommendedReviewerDAO recommendedReviewerDAO = new ManuscriptRecommendedReviewerDAO();
    protected final ManuscriptAuthorDAO manuscriptAuthorDAO = new ManuscriptAuthorDAO();
    protected final ManuscriptVersionDAO versionDAO = new ManuscriptVersionDAO();
    protected final FormalCheckResultDAO formalCheckResultDAO = new FormalCheckResultDAO();
    protected final FormalCheckService formalCheckService = new FormalCheckService();
    protected final MailNotifications mailNotifications = new MailNotifications(userDAO, manuscriptDAO, reviewDAO);
    protected final InAppNotifications inAppNotifications = new InAppNotifications(userDAO, manuscriptDAO, reviewDAO);

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. 必须已登录
        User current = getCurrentUser(req);
        if (current == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }


        // 3. 解析子路径
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            path = "/dashboard";
        }

        // 按“菜单入口权限”校验（允许跨角色访问）。/dashboard 不拦截
        String requiredPerm = requiredMenuPermission(path);
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        try {
            switch (path) {
                case "/dashboard":
                    resp.sendRedirect(req.getContextPath() + "/dashboard");
                    break;

                case "/formalCheck":
                    // SUBMITTED / FORMAL_CHECK 状态稿件列表，供编辑部管理员“形式审查 / 格式检查”
                    handleFormalCheckList(req, resp, current);
                    break;

                case "/formalCheck/review":
                    // 编辑部管理员：进入单篇稿件的形式审查页面
                    handleFormalCheckReviewPage(req, resp, current);
                    break;

                case "/formalCheck/history":
                    // 编辑部管理员：查看历史形式审查记录
                    handleFormalCheckHistoryPage(req, resp, current);
                    break;

                case "/formalCheck/history/detail":
                    // 编辑部管理员：查看单条形式审查记录详情
                    handleFormalCheckHistoryDetailPage(req, resp, current);
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

                case "/recommend/detail":
                    // 提出建议模块下的“查看稿件详情”（独立 JSP，展示更完整信息）
                    handleRecommendManuscriptDetailPage(req, resp, current);
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
    protected void handleFormalCheckList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list = manuscriptDAO.findByStatuses("SUBMITTED", "FORMAL_CHECK");
        PaginationUtil.apply(req, list, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_list.jsp")
                .forward(req, resp);
    }

    protected void handleFormalCheckReviewPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!MenuPermissionGuard.has(req, PermissionCatalog.MENU_EO_FORMAL_CHECK)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以进入形式审查页面。");
            return;
        }

        String midStr = req.getParameter("manuscriptId");
        if (midStr == null || midStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/editor/formalCheck");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(midStr.trim());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 参数无效。");
            return;
        }

        Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
        if (manuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // 若直接访问审查页，且仍为 SUBMITTED，则自动推进到 FORMAL_CHECK（与“开始审查”按钮效果一致）
        if ("SUBMITTED".equalsIgnoreCase(manuscript.getCurrentStatus())) {
            manuscript.setCurrentStatus("FORMAL_CHECK");
        }

        FormalCheckResult latest = formalCheckResultDAO.findByManuscriptId(manuscriptId);

        // 旁边显示“字数”
        // 需求变更：编辑部管理员（形式审查）的正文字数仅统计稿件 PDF，不再统计 Cover Letter。
        int bodyCount = 0;
        try {
            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);

            String bodyText = "";

            // 仅从 manuscript PDF 统计（与 /files/preview?type=manuscript 一致：优先原稿，否则匿名稿）
            if (currentVer != null) {
                String pdfPath = null;
                if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileOriginalPath();
                } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileAnonymousPath();
                }
                if (pdfPath != null) {
                    bodyText = FileTextUtil.extractText(new File(pdfPath));
                    bodyCount = formalCheckService.computeBodyCount(bodyText);
                }
            }
        } catch (Exception ignore) {
            bodyCount = 0;
        }

        // 摘要字数：直接从摘要文本统计（中文字符 + 英文单词）
        int abstractCount = 0;
        try {
            abstractCount = formalCheckService.computeAbstractCount(manuscript.getAbstractText());
        } catch (Exception ignore) {
            abstractCount = 0;
        }

        req.setAttribute("manuscript", manuscript);
        req.setAttribute("formalCheckResult", latest);
        req.setAttribute("bodyCount", bodyCount);
        req.setAttribute("abstractCount", abstractCount);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_review.jsp").forward(req, resp);
    }

    protected void handleFormalCheckHistoryPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!MenuPermissionGuard.has(req, PermissionCatalog.MENU_EO_FORMAL_HISTORY)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以查看审查历史。");
            return;
        }

        List<FormalCheckResult> history = formalCheckResultDAO.findByReviewerId(current.getUserId());
        java.util.Map<Integer, Manuscript> manuscriptMap = new java.util.HashMap<>();
        if (history != null) {
            for (FormalCheckResult r : history) {
                if (r == null || r.getManuscriptId() == null) continue;
                int mid = r.getManuscriptId();
                if (!manuscriptMap.containsKey(mid)) {
                    try {
                        Manuscript m = manuscriptDAO.findById(mid);
                        if (m != null) manuscriptMap.put(mid, m);
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        }

        PaginationUtil.apply(req, history, "history");
        req.setAttribute("manuscriptMap", manuscriptMap);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_history.jsp").forward(req, resp);
    }

    protected void handleFormalCheckHistoryDetailPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (!MenuPermissionGuard.has(req, PermissionCatalog.MENU_EO_FORMAL_HISTORY)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "只有编辑部管理员可以查看审查记录详情。");
            return;
        }

        String idStr = req.getParameter("checkId");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/editor/formalCheck/history");
            return;
        }

        int checkId;
        try {
            checkId = Integer.parseInt(idStr.trim());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "checkId 参数无效。");
            return;
        }

        FormalCheckResult result = formalCheckResultDAO.findById(checkId);
        Manuscript manuscript = null;
        if (result != null && result.getManuscriptId() != null) {
            try {
                manuscript = manuscriptDAO.findById(result.getManuscriptId());
            } catch (Exception ignore) {
            }
        }

        req.setAttribute("result", result);
        req.setAttribute("manuscript", manuscript);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/formal_check_history_detail.jsp").forward(req, resp);
    }




    protected void handleDeskList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> deskList = manuscriptDAO.findByStatuses("DESK_REVIEW_INITIAL");
        PaginationUtil.apply(req, deskList, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/desk_list.jsp")
                .forward(req, resp);
    }

    protected void handleToAssignList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> toAssignList = manuscriptDAO.findByStatuses("TO_ASSIGN");
        PaginationUtil.apply(req, toAssignList, "manuscripts");
        // 若当前用户是主编，则额外查询所有 EDITOR，供“指派编辑”下拉框使用
        if ("EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            List<User> editors = userDAO.findByRoleCode("EDITOR");
            req.setAttribute("editorList", editors);

            // ✅ 智能推荐：按“稿件领域/关键词”与“编辑研究方向”匹配，生成每篇稿件的推荐编辑列表
            Map<Integer, List<User>> recommendedEditorsMap = new HashMap<>();
            for (Manuscript m : toAssignList) {
                recommendedEditorsMap.put(m.getManuscriptId(), rankEditorsByResearchArea(editors, m));
            }
            req.setAttribute("recommendedEditorsMap", recommendedEditorsMap);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/editor/to_assign_list.jsp")
                .forward(req, resp);
    }



    /**
     * 主编：指派责任编辑（高级筛选页面）
     * URL: /editor/toAssign/pickEditor?manuscriptId=...
     *
     * 功能：
     *  - 按编辑研究方向（ResearchArea）筛选
     *  - 支持关键词搜索（姓名/用户名/邮箱/单位/研究方向）
     *  - 支持“仅显示与稿件领域匹配”的过滤（基于 token 交集与子串加权）
     *  - 支持分页（复用 PaginationUtil，避免编辑数量过多页面放不下）
     */
    protected void handlePickEditorPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        if (current == null || !"EDITOR_IN_CHIEF".equals(current.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "仅主编可使用该功能。");
            return;
        }

        String idStr = req.getParameter("manuscriptId");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少参数 manuscriptId。");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(idStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
        if (manuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // --- 读取筛选条件 ---
        String q = req.getParameter("q");
        if (q != null) q = q.trim();
        String areaParam = req.getParameter("area");
        String area = (areaParam == null) ? null : areaParam.trim();

        // 初次进入（未传 area）时，默认用稿件 SubjectArea 作为筛选提示/默认值
        boolean autoFillArea = false;
        if (areaParam == null) {
            String msArea = manuscript.getSubjectArea();
            if (msArea != null && !msArea.trim().isEmpty()) {
                area = msArea.trim();
                autoFillArea = true;
            }
        }
        if (area == null) area = "";

        String mo = req.getParameter("matchOnly");
        boolean matchOnly = "1".equals(mo) || "true".equalsIgnoreCase(mo) || "on".equalsIgnoreCase(mo);

        // --- 准备稿件 token ---
        String msArea = manuscript.getSubjectArea();
        String msKw = manuscript.getKeywords();
        Set<String> msTokens = tokenizeKeywords((msArea == null ? "" : msArea) + " " + (msKw == null ? "" : msKw));

        // --- 拉取编辑并过滤 ---
        List<User> editors = userDAO.findByRoleCode("EDITOR");
        List<User> filtered = new ArrayList<>();
        Map<Integer, Integer> scoreMap = new HashMap<>();

        String qLower = (q == null) ? "" : q.toLowerCase();
        String areaLower = area.toLowerCase();

        for (User e : editors) {
            if (e == null) continue;

            // 默认只显示 ACTIVE（兼容旧数据：Status 为空也放行）
            String st = e.getStatus();
            if (st != null && !"ACTIVE".equalsIgnoreCase(st)) continue;

            // keyword match
            if (q != null && !q.isEmpty()) {
                if (!containsIgnoreCase(e.getFullName(), qLower)
                        && !containsIgnoreCase(e.getUsername(), qLower)
                        && !containsIgnoreCase(e.getEmail(), qLower)
                        && !containsIgnoreCase(e.getAffiliation(), qLower)
                        && !containsIgnoreCase(e.getResearchArea(), qLower)) {
                    continue;
                }
            }

            // area filter: ResearchArea 包含（或 token 命中）
            if (!area.isEmpty()) {
                String ra = e.getResearchArea();
                String raLower = (ra == null) ? "" : ra.toLowerCase();
                if (!raLower.contains(areaLower)) {
                    // token fallback：area 被拆成 token，任意 token 命中 ResearchArea 即认为通过
                    Set<String> areaTokens = tokenizeKeywords(area);
                    if (!areaTokens.isEmpty()) {
                        boolean hit = false;
                        for (String t : areaTokens) {
                            if (t == null) continue;
                            String tl = t.toLowerCase();
                            if (!tl.isEmpty() && raLower.contains(tl)) {
                                hit = true;
                                break;
                            }
                        }
                        if (!hit) continue;
                    } else {
                        continue;
                    }
                }
            }

            int score = editorMatchScore(e, manuscript.getSubjectArea(), msTokens);
            scoreMap.put(e.getUserId(), score);

            if (matchOnly && score <= 0) continue;

            filtered.add(e);
        }

        // --- 排序：先按匹配分（降序），再按是否填写研究方向，再按 UserId ---
        filtered.sort((a, b) -> {
            int sa = scoreMap.getOrDefault(a.getUserId(), 0);
            int sb = scoreMap.getOrDefault(b.getUserId(), 0);
            if (sa != sb) return Integer.compare(sb, sa);

            boolean ha = a.getResearchArea() != null && !a.getResearchArea().trim().isEmpty();
            boolean hb = b.getResearchArea() != null && !b.getResearchArea().trim().isEmpty();
            if (ha != hb) return hb ? 1 : -1;

            return Integer.compare(a.getUserId(), b.getUserId());
        });

        // --- 分页 ---
        PaginationUtil.apply(req, filtered, "editors");

        req.setAttribute("manuscript", manuscript);
        req.setAttribute("filterQ", q == null ? "" : q);
        req.setAttribute("filterArea", area);
        req.setAttribute("filterMatchOnly", matchOnly);
        req.setAttribute("autoFillArea", autoFillArea);
        req.setAttribute("editorScoreMap", scoreMap);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/pick_editor.jsp")
                .forward(req, resp);
    }

    private boolean containsIgnoreCase(String s, String qLower) {
        if (qLower == null || qLower.isEmpty()) return true;
        if (s == null) return false;
        return s.toLowerCase().contains(qLower);
    }


    protected void handleWithEditorList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        // 责任编辑：仅展示分配给自己的稿件；主编/编辑部管理员：展示全部 WITH_EDITOR
        List<Manuscript> list;
        if ("EDITOR".equals(current.getRoleCode())) {
            list = manuscriptDAO.findByStatusesForEditor(current.getUserId(), "WITH_EDITOR");
        } else {
            list = manuscriptDAO.findByStatuses("WITH_EDITOR");
        }

        // 为列表页准备“最新指派记录/编辑信息”（主编/编辑部管理员视图会用到；对编辑视图也无副作用）
        Map<Integer, edu.bjfu.onlinesm.model.ManuscriptAssignment> latestAssignments = new HashMap<>();
        Map<Integer, User> editors = new HashMap<>();
        for (Manuscript m : list) {
            int mid = m.getManuscriptId();
            try {
                edu.bjfu.onlinesm.model.ManuscriptAssignment ma = assignmentDAO.findLatestByManuscript(mid);
                if (ma != null) {
                    latestAssignments.put(mid, ma);
                    int eid = ma.getEditorId();
                    if (!editors.containsKey(eid)) {
                        User u = userDAO.findById(eid);
                        if (u != null) editors.put(eid, u);
                    }
                } else {
                    // 没有指派记录时，尝试从稿件表/兼容逻辑取当前编辑（不强制显示 comment）
                    Integer eid = manuscriptDAO.findCurrentEditorId(mid);
                    if (eid != null && !editors.containsKey(eid)) {
                        User u = userDAO.findById(eid);
                        if (u != null) editors.put(eid, u);
                    }
                }
            } catch (Exception ignore) {
                // 避免列表页因历史数据/表结构差异导致 500
            }
        }

        PaginationUtil.apply(req, list, "manuscripts");
        req.setAttribute("latestAssignments", latestAssignments);
        req.setAttribute("editors", editors);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/with_editor_list.jsp")
                .forward(req, resp);
    }

    protected void handleUnderReviewList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {
        // 先尝试自动推进：当某稿件所有“有效邀请”的审稿记录都已 SUBMITTED，则
        // 将稿件状态从 UNDER_REVIEW 推进为 EDITOR_RECOMMENDATION（可提交编辑建议）。
        reviewDAO.promoteAllUnderReviewManuscriptsIfReady();

        // 只展示 UNDER_REVIEW（外审进行中）列表。
        // EDITOR_RECOMMENDATION 的稿件请在“提出建议”模块查看。
        List<Manuscript> underReviewList;
        if ("EDITOR".equals(current.getRoleCode())) {
            underReviewList = manuscriptDAO.findByStatusesForEditor(current.getUserId(), "UNDER_REVIEW");
        } else {
            underReviewList = manuscriptDAO.findByStatuses("UNDER_REVIEW");
        }

        req.setAttribute("underReviewList", underReviewList);

        // 兼容旧 JSP（若还在使用 ${manuscripts}）：
        PaginationUtil.apply(req, underReviewList, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/under_review_list.jsp")
                .forward(req, resp);
    }

    /**
     * 编辑提出建议页面：
     * - 不带 manuscriptId：展示当前编辑可提交建议的稿件列表（EDITOR_RECOMMENDATION）
     * - 带 manuscriptId：展示审稿意见汇总，并填写总结+建议后提交给主编
     */
    protected void handleEditorRecommendPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {

            // 列表页：仅展示状态为 EDITOR_RECOMMENDATION 的稿件
            List<Manuscript> ready = manuscriptDAO.findByStatuses("EDITOR_RECOMMENDATION");

            // 若是责任编辑，只展示分配给自己的稿件
            if ("EDITOR".equals(current.getRoleCode())) {
                List<Manuscript> filtered = new ArrayList<>();
                for (Manuscript m : ready) {
                    Integer editorId = manuscriptDAO.findCurrentEditorId(m.getManuscriptId());
                    if (java.util.Objects.equals(editorId, current.getUserId())) {
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
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // 权限：责任编辑只能看自己的；主编可看全部
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (!java.util.Objects.equals(editorId, current.getUserId())) {
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

protected void handleFinalDecisionList(HttpServletRequest req, HttpServletResponse resp, User current)
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

        PaginationUtil.apply(req, finalList, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/final_decision_list.jsp")
                .forward(req, resp);
    }

    /**
     * 提出建议模块下的“查看稿件详情”页面。
     * 说明：该页是从“提出建议”入口进入的独立详情 JSP，
     * 用于展示更完整的稿件信息（作者列表、版本文件、形式审查结果、外审记录与已提交意见等）。
     */
    protected void handleRecommendManuscriptDetailPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String midStr = req.getParameter("manuscriptId");
        if (midStr == null || midStr.trim().isEmpty()) {
            midStr = req.getParameter("id");
        }
        if (midStr == null || midStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/editor/recommend");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(midStr);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 参数无效。");
            return;
        }

        Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
        if (manuscript == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // 责任编辑只能查看分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (!java.util.Objects.equals(editorId, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "您无权查看该稿件的详情。");
                return;
            }
        }

        // 基本信息 + 作者列表
        List<ManuscriptAuthor> authors = manuscriptAuthorDAO.findByManuscriptId(manuscriptId);

        // 当前版本与形式审查结果
        ManuscriptVersion currentVersion = versionDAO.findCurrentByManuscriptId(manuscriptId);
        FormalCheckResult formalCheckResult = formalCheckResultDAO.findByManuscriptId(manuscriptId);

        // 外审记录与作者推荐审稿人
        List<Review> reviews = reviewDAO.findByManuscript(manuscriptId);
        List<ManuscriptRecommendedReviewer> recommendedReviewers = recommendedReviewerDAO.findByManuscriptId(manuscriptId);

        req.setAttribute("manuscript", manuscript);
        req.setAttribute("authors", authors);
        req.setAttribute("currentVersion", currentVersion);
        req.setAttribute("formalCheckResult", formalCheckResult);
        req.setAttribute("reviews", reviews);
        req.setAttribute("recommendedReviewers", recommendedReviewers);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/recommend_manuscript_detail.jsp")
                .forward(req, resp);
    }

    /**
     * 责任编辑查看稿件详情（合并页）：
     * - 稿件详情
     * - 作者列表
     * - 推荐审稿人
     * - 当前审稿记录
     * 页面底部提供“添加邀请审稿人”按钮跳转到审稿人选择页。
     *
     * GET: /editor/withEditor/detail?manuscriptId=xx
     * GET: /editor/underReview/detail?manuscriptId=xx
     */
    protected void handleEditorManuscriptDetailPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            // 兼容部分旧链接使用 id
            manuscriptIdStr = req.getParameter("id");
        }
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能查看/操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        List<ManuscriptAuthor> authors = manuscriptAuthorDAO.findByManuscriptId(manuscriptId);
        List<ManuscriptRecommendedReviewer> recReviewers = recommendedReviewerDAO.findByManuscriptId(manuscriptId);
        List<Review> reviews = reviewDAO.findByManuscript(manuscriptId);

        Map<Integer, User> reviewerMap = new HashMap<>();
        for (Review r : reviews) {
            int rid = r.getReviewerId();
            if (!reviewerMap.containsKey(rid)) {
                User u = userDAO.findById(rid);
                if (u != null) {
                    reviewerMap.put(rid, u);
                }
            }
        }

        // 当前详情页的“回跳地址”：用于邀请/撤回/催审等动作完成后返回本详情页
        String backToUrl = req.getContextPath() + req.getServletPath() + "/detail?manuscriptId=" + manuscriptId;

        req.setAttribute("manuscript", m);
        req.setAttribute("authors", authors);
        req.setAttribute("recommendedReviewers", recReviewers);
        req.setAttribute("reviews", reviews);
        req.setAttribute("reviewerMap", reviewerMap);
        req.setAttribute("backToUrl", backToUrl);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/editor_manuscript_detail.jsp")
                .forward(req, resp);
    }

    /**
     * 外部审稿人邀请页面（创建账号并邮件邀请）。
     * GET: /editor/review/externalInvite?manuscriptId=xx&backTo=...
     */
    protected void handleExternalInviteReviewerPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }

        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        String backToUrl = req.getParameter("backTo");
        if (backToUrl == null || backToUrl.trim().isEmpty()) {
            // 默认回到合并后的详情页
            String detailSp;
            if ("WITH_EDITOR".equals(m.getCurrentStatus())) {
                detailSp = "/editor/withEditor";
            } else if ("UNDER_REVIEW".equals(m.getCurrentStatus())) {
                detailSp = "/editor/underReview";
            } else {
                detailSp = "/editor/withEditor";
            }
            backToUrl = req.getContextPath() + detailSp + "/detail?manuscriptId=" + manuscriptId;
        }

        // 返回“审稿人库选择页”链接（同样携带 backTo）
        String selectUrl = req.getContextPath() + "/editor/review/select?manuscriptId=" + manuscriptId;
        selectUrl = appendQueryParam(selectUrl, "backTo", URLEncoder.encode(backToUrl, "UTF-8"));

        req.setAttribute("manuscript", m);
        req.setAttribute("backToUrl", backToUrl);
        req.setAttribute("selectUrl", selectUrl);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_invite_external.jsp")
                .forward(req, resp);
    }


    /**
     * 主编管理“审稿人库”的页面：仅允许 EDITOR_IN_CHIEF 访问。
     */
    protected void handleReviewerPoolPage (HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

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

        // 兼容页面提示
        req.setAttribute("msg", req.getParameter("msg"));
        req.setAttribute("error", req.getParameter("error"));

        req.setAttribute("reviewers", reviewers);
        req.setAttribute("reviewerKeyword", reviewerKeyword);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_pool.jsp")
                .forward(req, resp);
    }

    /**
     * 主编“全览权限”：查看系统内所有稿件的状态，并可跳转到稿件详情页查看审稿流程/版本/附件。
     */
    protected void handleChiefOverview(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list = manuscriptDAO.findAllForChief();
        PaginationUtil.apply(req, list, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/chief_overview.jsp")
                .forward(req, resp);
    }

    /**
     * 主编“特殊权限”：撤稿（Retract）/ 撤销终审决定（Rescind Decision）。
     */
    protected void handleChiefSpecialPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        // 只展示与“决策/撤稿”相关的稿件，避免列表过大
        List<Manuscript> list = manuscriptDAO.findByStatuses(
                "EDITOR_RECOMMENDATION",
                "FINAL_DECISION_PENDING",
                "REVISION",
                "ACCEPTED",
                "REJECTED"
        );
        PaginationUtil.apply(req, list, "manuscripts");
        req.getRequestDispatcher("/WEB-INF/jsp/editor/chief_special.jsp")
                .forward(req, resp);
    }

    protected User getCurrentUser(HttpServletRequest req) {
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

        // 按“菜单入口权限”校验（允许跨角色访问）
        String requiredPerm = requiredMenuPermission(path);
        if (requiredPerm != null && !MenuPermissionGuard.require(req, resp, requiredPerm)) return;

        try {
            switch (path) {
                case "/formalCheck":
                case "/formalCheck/autoCheck":
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

                case "/review/inviteExternal":
                    // 编辑为稿件邀请外部审稿人（创建账号并发送邮件邀请）
                    handleInviteExternalReviewerPost(req, resp, current);
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
    protected void handleInviteReviewerPost(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          User current)
            throws IOException, SQLException {

        // 只有 EDITOR 或 EDITOR_IN_CHIEF 才能邀请审稿人

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String[] reviewerIdParams = req.getParameterValues("reviewerIds");
        String dueDateStr = req.getParameter("dueDate"); // yyyy-MM-dd，可为空
        String backTo = req.getParameter("backTo");      // 可为空：合并页会传回跳地址

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
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        LocalDateTime dueAt = null;
        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            LocalDate d = LocalDate.parse(dueDateStr.trim());
            // 截止日期设为当天 23:59:59
            dueAt = d.atTime(23, 59, 59);
        }

        // 1) 防御：拒绝(DECLINED)的审稿人不允许再次邀请（UI 已置灰，但仍需后端校验）
        Set<Integer> declinedReviewerIds = new HashSet<>();
        Set<Integer> alreadyAssignedReviewerIds = new HashSet<>();
        try {
            List<Review> existing = reviewDAO.findByManuscript(manuscriptId);
            for (Review r : existing) {
                if (r == null) continue;
                String st = r.getStatus();
                boolean isDeclined = "DECLINED".equals(st)
                        || ("EXPIRED".equals(st) && (r.getDeclinedAt() != null
                        || (r.getRejectionReason() != null && !r.getRejectionReason().trim().isEmpty())));
                if (isDeclined) {
                    declinedReviewerIds.add(r.getReviewerId());
                } else {
                    alreadyAssignedReviewerIds.add(r.getReviewerId());
                }
            }
        } catch (Exception ignore) {
            // 若读取失败，仍继续走邀请流程（inviteReviewerReturnId 内部也有 DECLINED 防线）
        }

        int invitedCount = 0;
        List<Integer> skippedDecline = new ArrayList<>();
        List<Integer> skippedAssigned = new ArrayList<>();

        // 2) 对每个选中的审稿人，插入一条 INVITED 记录，并发送邀请通知（站内 + 邮件）
        for (String reviewerIdStr : reviewerIdParams) {
            int reviewerId = Integer.parseInt(reviewerIdStr.trim());

            if (declinedReviewerIds.contains(reviewerId)) {
                skippedDecline.add(reviewerId);
                continue;
            }
            if (alreadyAssignedReviewerIds.contains(reviewerId)) {
                skippedAssigned.add(reviewerId);
                continue;
            }

            // 返回 reviewId，便于发送通知（避免只插入但未通知导致“没反应”）
            int reviewId = reviewDAO.inviteReviewerReturnId(manuscriptId, reviewerId, dueAt);

            // 站内通知 + 邮件通知（若邮箱未配置或为空会自动跳过）
            inAppNotifications.onReviewerInvited(reviewId);
            mailNotifications.onReviewerInvited(reviewId);
            invitedCount++;
        }

        // 2. 如果稿件当前还在 WITH_EDITOR，就顺便把稿件状态改为 UNDER_REVIEW（送外审）
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m != null && "WITH_EDITOR".equals(m.getCurrentStatus())) {
            manuscriptDAO.updateStatusWithHistory(manuscriptId, "UNDER_REVIEW", "SEND_TO_REVIEW", current.getUserId(), "送外审");
        }

        // 回到合并后的详情页（或 backTo 指定的页面），并给出提示
        String base = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        if (invitedCount == 0) {
            String err = URLEncoder.encode("所选审稿人均不可邀请（可能已分配或已拒绝）。", "UTF-8");
            resp.sendRedirect(appendQueryParam(base, "inviteErr", err));
            return;
        }

        String msg = URLEncoder.encode("已向所选审稿人发送邀请（" + invitedCount + "人）。", "UTF-8");
        String target = appendQueryParam(base, "inviteMsg", msg);

        if (!skippedDecline.isEmpty()) {
            String err = URLEncoder.encode("部分审稿人已拒绝邀请（已拒绝：" + skippedDecline.size() + "人），已自动跳过。","UTF-8");
            target = appendQueryParam(target, "inviteErr", err);
        } else if (!skippedAssigned.isEmpty()) {
            String err = URLEncoder.encode("部分审稿人已被分配/已评审（已跳过：" + skippedAssigned.size() + "人）。", "UTF-8");
            target = appendQueryParam(target, "inviteErr", err);
        }

        resp.sendRedirect(target);
    }

    /**
 * 邀请外部审稿人：由编辑在“审稿人选择”页面底部填写信息，系统将：
 *  1) 创建 REVIEWER 账号（状态 ACTIVE，可直接登录）；
 *  2) 发送“新审稿人账户邀请邮件”（含用户名/初始密码）；
 *  3) 为指定稿件创建一条 INVITED 的 Reviews 记录，并发送“审稿邀请邮件”。
 */
protected void handleInviteExternalReviewerPost(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              User current)
        throws IOException, SQLException {

    String manuscriptIdStr = req.getParameter("manuscriptId");
    String dueDateStr = req.getParameter("dueDate"); // yyyy-MM-dd，可为空
    String backTo = req.getParameter("backTo");      // 可为空：合并页会传回跳地址

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    String fullName = req.getParameter("fullName");
    String email = req.getParameter("email");
    String affiliation = req.getParameter("affiliation");
    String researchArea = req.getParameter("researchArea");

    username = username == null ? "" : username.trim();
    password = password == null ? "" : password.trim();
    fullName = fullName == null ? "" : fullName.trim();
    email = email == null ? "" : email.trim();
    affiliation = affiliation == null ? "" : affiliation.trim();
    researchArea = researchArea == null ? "" : researchArea.trim();

    int manuscriptId;
    try {
        manuscriptId = Integer.parseInt((manuscriptIdStr == null ? "" : manuscriptIdStr.trim()));
    } catch (Exception e) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
        return;
    }

    if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
        String base = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        String msg = URLEncoder.encode("用户名/初始密码/邮箱均不能为空。", "UTF-8");
        resp.sendRedirect(appendQueryParam(base, "inviteErr", msg));
        return;
    }

    // EDITOR 只能操作分配给自己的稿件（防止越权创建账号并邀请）
    if ("EDITOR".equals(current.getRoleCode())) {
        Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
        if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
            return;
        }
    }

    // 截止时间（可选）
    LocalDateTime dueAt = null;
    if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
        LocalDate d = LocalDate.parse(dueDateStr.trim());
        dueAt = d.atTime(23, 59, 59);
    }

    // 如果用户名已存在，直接提示
    User existed = userDAO.findByUsername(username);
    if (existed != null) {
        String base = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        String msg = URLEncoder.encode("用户名已存在，请更换用户名后再邀请。", "UTF-8");
        resp.sendRedirect(appendQueryParam(base, "inviteErr", msg));
        return;
    }

    // 1) 创建审稿人账号（ACTIVE：可直接登录）
    User reviewer = new User();
    reviewer.setUsername(username);
    reviewer.setPasswordHash(password); // 本项目示例为明文对比，沿用原逻辑
    reviewer.setFullName(fullName.isEmpty() ? username : fullName);
    reviewer.setEmail(email);
    reviewer.setAffiliation(affiliation);
    reviewer.setResearchArea(researchArea);
    reviewer.setStatus("ACTIVE");

    User createdReviewer = userDAO.createUserWithRole(reviewer, "REVIEWER");
    int newReviewerId = createdReviewer.getUserId();
    reviewer.setUserId(newReviewerId);

    // 2) 发送“新审稿人账户邀请邮件”（含账号信息）
    mailNotifications.onInviteNewReviewer(reviewer, password);

    // 3) 创建审稿邀请记录并发送邀请邮件
    int reviewId = reviewDAO.inviteReviewerReturnId(manuscriptId, newReviewerId, dueAt);
    inAppNotifications.onReviewerInvited(reviewId);
    mailNotifications.onReviewerInvited(reviewId);

    // 4) 若稿件仍在 WITH_EDITOR，则送外审
    Manuscript m = manuscriptDAO.findById(manuscriptId);
    if (m != null && "WITH_EDITOR".equals(m.getCurrentStatus())) {
        manuscriptDAO.updateStatusWithHistory(manuscriptId, "UNDER_REVIEW", "SEND_TO_REVIEW", current.getUserId(), "送外审");
    }

    String base = (backTo != null && !backTo.trim().isEmpty())
            ? backTo.trim()
            : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
    String msg = URLEncoder.encode("外部审稿人账号已创建，并已发送邮件邀请。", "UTF-8");
    resp.sendRedirect(appendQueryParam(base, "inviteMsg", msg));
}

/**
     * 催审：编辑 / 主编对某个审稿记录执行催审，
     * 更新 RemindCount / LastRemindedAt 字段。
     */
    protected void handleRemindReviewerPost(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          User current)
            throws IOException, SQLException {

        String reviewIdStr     = req.getParameter("reviewId");
        String manuscriptIdStr = req.getParameter("manuscriptId");
        String backTo          = req.getParameter("backTo");

        if (reviewIdStr == null || manuscriptIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int reviewId     = Integer.parseInt(reviewIdStr);
        int manuscriptId = Integer.parseInt(manuscriptIdStr);

        // 更新 dbo.Reviews.RemindCount / LastRemindedAt
        reviewDAO.remind(reviewId);

        // 催审后跳回合并后的详情页（或 backTo 指定页面），避免在多个列表之间来回跳转
        String target = (backTo != null && !backTo.trim().isEmpty())
                ? backTo.trim()
                : (req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId + "#inviteReviewers");
        resp.sendRedirect(target);
    }

    

    /**
     * 审稿监控页面：集中查看逾期审稿任务，并可以从这里进入手动催审。
     */
    protected void handleReviewMonitorPage(HttpServletRequest req,
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
    protected int parseIntOrDefault(String s, int defaultValue) {
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
    protected void handleReviewRemindFormPage(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            User current)
            throws ServletException, IOException, SQLException {

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
    protected void handleEditorReviewDetailPage(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              User current)
            throws ServletException, IOException, SQLException {

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
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
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
    protected void handleRemindReviewerCustomPost(HttpServletRequest req,
                                                HttpServletResponse resp,
                                                User current)
            throws IOException, SQLException {

        String reviewIdStr     = req.getParameter("reviewId");
        String manuscriptIdStr = req.getParameter("manuscriptId");
        String back            = req.getParameter("back");
        String message         = req.getParameter("message");

        if (reviewIdStr == null || manuscriptIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        int reviewId     = Integer.parseInt(reviewIdStr.trim());
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

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
    protected void handleAutoRemindNowPost(HttpServletRequest req,
                                         HttpServletResponse resp,
                                         User current)
            throws IOException, SQLException {

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
     * 编辑根据外审意见给出“编辑建议”。
     * - 编辑建议保存到 dbo.EditorSuggestions（含总结报告）；
     * - 稿件状态推进到 FINAL_DECISION_PENDING 交由主编终审；
     * - Manuscripts.Decision 仅用于“最终决策”（ACCEPT/REJECT/REVISION），这里不写入，避免字段截断/语义混淆。
     */
    protected void handleEditorRecommendPost(HttpServletRequest req,
                                           HttpServletResponse resp,
                                           User current)
            throws IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        String suggestionCode  = req.getParameter("suggestion");
        String summary         = req.getParameter("summary");

        if (manuscriptIdStr == null || suggestionCode == null || suggestionCode.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // 责任编辑只能提交自己负责的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer editorId = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (!java.util.Objects.equals(editorId, current.getUserId())) {
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

        // 注意：不要写入 Manuscripts.Decision。
        // 该字段用于最终决策（且列宽通常较小，例如 NVARCHAR(30)），
        // 将长英文建议写入会触发 SQL Server “字符串或二进制数据将被截断”。

        // 3) 通知主编（站内通知）
        inAppNotifications.onEditorRecommendationSubmitted(manuscriptId, current, decisionText, summary);

        // 提交后回到“提出建议”模块，避免被跳转到主编专属页面导致 403。
        resp.sendRedirect(req.getContextPath() + "/editor/recommend?msg=提交成功");
    }

    
    /**
     * 形式审查 / 格式检查操作（SUBMITTED ↔ FORMAL_CHECK / RETURNED / DESK_REVIEW_INITIAL）。
     * 仅允许 EO_ADMIN 调用。
     */
    protected void handleFormalCheckPost(HttpServletRequest req, HttpServletResponse resp, User current)
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
                // SUBMITTED/RETURNED -> FORMAL_CHECK
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "FORMAL_CHECK", "FORMAL_CHECK_START", current.getUserId(), "编辑部开始进行形式审查");
                inAppNotifications.onFormalCheckStarted(manuscriptId);
                resp.sendRedirect(req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId);
                return;
            case "autoCheck":
                handleAutoCheck(req, resp, manuscriptId);
                return;
            case "plagiarismCheck":
                handlePlagiarismCheck(req, resp, manuscriptId);
                return;
            case "submit":
                handleSubmitFormalCheck(req, resp, current, manuscriptId);
                return;
            case "returnForRevision":
                handleReturnForRevision(req, resp, current, manuscriptId);
                return;
            case "approve":
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "DESK_REVIEW_INITIAL", "FORMAL_CHECK_PASS", current.getUserId(), "形式审查通过");
                inAppNotifications.onFormalCheckPassed(manuscriptId);
                break;
            case "return":
                String issues = req.getParameter("issues");
                if (issues != null) issues = issues.trim();
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(),
                        (issues == null || issues.isEmpty()) ? "形式审查未通过，已退回修改" : issues);

                String guideUrl = req.getParameter("guideUrl");
                mailNotifications.onFormalCheckReturn(manuscriptId, issues, guideUrl);
                inAppNotifications.onFormalCheckReturn(manuscriptId, issues);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        resp.sendRedirect(req.getContextPath() + "/editor/formalCheck");
    }

    protected void handleAutoCheck(HttpServletRequest req, HttpServletResponse resp, int manuscriptId)
            throws SQLException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                out.print(jsonResponse.toString());
                return;
            }

            // 正文字数来源：
            // - 编辑部管理员（EO_ADMIN）的形式审查：仅统计稿件 PDF
            // - 兼容历史逻辑：其他入口仍可优先从 Cover Letter 提取，若为 0 再回退稿件 PDF
            User current = getCurrentUser(req);
            boolean eoAdminOnlyPdf = (current != null && "EO_ADMIN".equals(current.getRoleCode()));

            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);
            String bodyText = "";

            if (!eoAdminOnlyPdf) {
                // 1) Cover Letter
                if (currentVer != null) {
                    String coverPath = currentVer.getCoverLetterPath();
                    if (coverPath != null && !coverPath.trim().isEmpty()) {
                        bodyText = FileTextUtil.extractText(new File(coverPath));
                    }
                }
            }

            // 2) manuscript PDF（与 /files/preview?type=manuscript 一致：优先原稿，否则匿名稿）
            if (currentVer != null && (eoAdminOnlyPdf || formalCheckService.computeBodyCount(bodyText) == 0)) {
                String pdfPath = null;
                if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileOriginalPath();
                } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileAnonymousPath();
                }
                if (pdfPath != null) {
                    bodyText = FileTextUtil.extractText(new File(pdfPath));
                }
            }

            // 详细作者信息在 dbo.ManuscriptAuthors：以此为准校验邮箱，避免 authorList(冗余展示字段) 不含邮箱导致误判
            java.util.List<ManuscriptAuthor> authors = null;
            try {
                authors = manuscriptAuthorDAO.findByManuscriptId(manuscriptId);
            } catch (Exception ignore) {
                authors = null;
            }

            FormalCheckResult result = formalCheckService.performAutomaticChecks(manuscript, bodyText, authors);
            int bodyCount = formalCheckService.computeBodyCount(bodyText);
            int abstractCount = formalCheckService.computeAbstractCount(manuscript.getAbstractText());

            
            jsonResponse.put("success", true);
            jsonResponse.put("authorInfoValid", result.getAuthorInfoValid() != null ? result.getAuthorInfoValid().toString() : "");
            jsonResponse.put("abstractWordCountValid", result.getAbstractWordCountValid() != null ? result.getAbstractWordCountValid().toString() : "");
            jsonResponse.put("bodyWordCountValid", result.getBodyWordCountValid() != null ? result.getBodyWordCountValid().toString() : "");
            jsonResponse.put("keywordsValid", result.getKeywordsValid() != null ? result.getKeywordsValid().toString() : "");
            jsonResponse.put("bodyCount", bodyCount);
            jsonResponse.put("abstractCount", abstractCount);
            String bodyOk = Boolean.TRUE.equals(result.getBodyWordCountValid()) ? "通过" : (Boolean.FALSE.equals(result.getBodyWordCountValid()) ? "不通过" : "未检查");
            jsonResponse.put("message", "自动检查完成：正文字数 " + bodyCount + "（3000-8000，" + bodyOk + "），摘要字数 " + abstractCount + "");
            
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "自动检查失败：" + e.getMessage());
        }

        out.print(jsonResponse.toString());
    }

    protected void handlePlagiarismCheck(HttpServletRequest req, HttpServletResponse resp, int manuscriptId)
            throws SQLException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                out.print(jsonResponse.toString());
                return;
            }

            // 尽量从当前版本 PDF 中提取正文文本，便于在查重报告中显示“字数”等信息
            ManuscriptVersion currentVer = versionDAO.findCurrentByManuscriptId(manuscriptId);
            String pdfPath = null;
            if (currentVer != null) {
                if (currentVer.getFileOriginalPath() != null && !currentVer.getFileOriginalPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileOriginalPath();
                } else if (currentVer.getFileAnonymousPath() != null && !currentVer.getFileAnonymousPath().trim().isEmpty()) {
                    pdfPath = currentVer.getFileAnonymousPath();
                }
            }
            String bodyText = "";
            if (pdfPath != null) {
                bodyText = PdfTextUtil.extractText(new File(pdfPath));
            }

            PlagiarismCheckService.PlagiarismReport plagiarismReport =
                    formalCheckService.performPlagiarismCheck(manuscript, bodyText);
            
            jsonResponse.put("success", true);
            jsonResponse.put("similarityScore", plagiarismReport.getSimilarityScore());
            jsonResponse.put("highSimilarity", plagiarismReport.isHighSimilarity());
            String reportUrl = plagiarismReport.getReportUrl();
            if (reportUrl != null && reportUrl.startsWith("/")) {
                reportUrl = req.getContextPath() + reportUrl;
            }
            jsonResponse.put("reportUrl", reportUrl);
            jsonResponse.put("message", "查重完成");
            
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "查重失败：" + e.getMessage());
        }

        out.print(jsonResponse.toString());
    }

    protected void handleSubmitFormalCheck(HttpServletRequest req, HttpServletResponse resp, User current, int manuscriptId)
            throws SQLException, IOException {

        // 兼容两类调用：
        // 1) /editor/formalCheck/review 页面：fetch 提交，期望 JSON
        // 2) /manuscripts/detail 页面：普通 form 提交，不应把 JSON 显示在浏览器上，应跳转到“审查历史”
        String ajax = req.getParameter("ajax");
        boolean wantJson = "1".equals(ajax) || "true".equalsIgnoreCase(ajax);

        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                if (wantJson) {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.getWriter().print(jsonResponse.toString());
                } else {
                    resp.sendRedirect(req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId);
                }
                return;
            }

            FormalCheckResult result = new FormalCheckResult();
            result.setManuscriptId(manuscriptId);
            result.setReviewerId(current.getUserId());
            result.setCheckTime(LocalDateTime.now());

            String authorInfoValidStr = req.getParameter("authorInfoValid");
            if (authorInfoValidStr != null && !authorInfoValidStr.isEmpty()) {
                result.setAuthorInfoValid(Boolean.valueOf(authorInfoValidStr));
            }

            String abstractWordCountValidStr = req.getParameter("abstractWordCountValid");
            if (abstractWordCountValidStr != null && !abstractWordCountValidStr.isEmpty()) {
                result.setAbstractWordCountValid(Boolean.valueOf(abstractWordCountValidStr));
            }

            String bodyWordCountValidStr = req.getParameter("bodyWordCountValid");
            if (bodyWordCountValidStr != null && !bodyWordCountValidStr.isEmpty()) {
                result.setBodyWordCountValid(Boolean.valueOf(bodyWordCountValidStr));
            }

            String keywordsValidStr = req.getParameter("keywordsValid");
            if (keywordsValidStr != null && !keywordsValidStr.isEmpty()) {
                result.setKeywordsValid(Boolean.valueOf(keywordsValidStr));
            }

            String footnoteNumberingValidStr = req.getParameter("footnoteNumberingValid");
            if (footnoteNumberingValidStr != null && !footnoteNumberingValidStr.isEmpty()) {
                result.setFootnoteNumberingValid(Boolean.valueOf(footnoteNumberingValidStr));
            }

            String figureTableFormatValidStr = req.getParameter("figureTableFormatValid");
            if (figureTableFormatValidStr != null && !figureTableFormatValidStr.isEmpty()) {
                result.setFigureTableFormatValid(Boolean.valueOf(figureTableFormatValidStr));
            }

            String referenceFormatValidStr = req.getParameter("referenceFormatValid");
            if (referenceFormatValidStr != null && !referenceFormatValidStr.isEmpty()) {
                result.setReferenceFormatValid(Boolean.valueOf(referenceFormatValidStr));
            }

            String checkResult = req.getParameter("checkResult");

            boolean hasInvalid = false;
            if (result.getAuthorInfoValid() != null && !result.getAuthorInfoValid()) {
                hasInvalid = true;
            }
            if (result.getAbstractWordCountValid() != null && !result.getAbstractWordCountValid()) {
                hasInvalid = true;
            }
            if (result.getBodyWordCountValid() != null && !result.getBodyWordCountValid()) {
                hasInvalid = true;
            }
            if (result.getKeywordsValid() != null && !result.getKeywordsValid()) {
                hasInvalid = true;
            }
            if (result.getFootnoteNumberingValid() != null && !result.getFootnoteNumberingValid()) {
                hasInvalid = true;
            }
            if (result.getFigureTableFormatValid() != null && !result.getFigureTableFormatValid()) {
                hasInvalid = true;
            }
            if (result.getReferenceFormatValid() != null && !result.getReferenceFormatValid()) {
                hasInvalid = true;
            }

            if (hasInvalid) {
                checkResult = "FAIL";
            }

            result.setCheckResult(checkResult);

            String feedback = req.getParameter("feedback");
            if (feedback == null || feedback.trim().isEmpty()) {
                feedback = formalCheckService.generateFeedback(result);
            }
            result.setFeedback(feedback);

            formalCheckResultDAO.save(result);


            if ("PASS".equals(checkResult)) {
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "DESK_REVIEW_INITIAL", "FORMAL_CHECK_PASS", current.getUserId(),
                        "形式审查通过");
                inAppNotifications.onFormalCheckPassed(manuscriptId);
            } else if ("FAIL".equals(checkResult)) {
                manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(),
                        feedback);
                String guideUrl = req.getContextPath() + "/static/guides/format_guide.pdf";
                mailNotifications.onFormalCheckReturn(manuscriptId, feedback, guideUrl);
                inAppNotifications.onFormalCheckReturn(manuscriptId, feedback);
            }

            jsonResponse.put("success", true);
            jsonResponse.put("message", "形式审查结果已提交");

        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "提交失败：" + e.getMessage());
        }

        if (wantJson) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print(jsonResponse.toString());
        } else {
            // 普通 form 提交：不展示 JSON，直接进入“审查历史”
            if (jsonResponse.optBoolean("success", false)) {
                resp.sendRedirect(req.getContextPath() + "/editor/formalCheck/history");
            } else {
                // 失败则回到稿件详情页（避免吞掉错误）
                resp.sendRedirect(req.getContextPath() + "/manuscripts/detail?id=" + manuscriptId);
            }
        }
    }

    protected void handleReturnForRevision(HttpServletRequest req, HttpServletResponse resp, User current, int manuscriptId)
            throws SQLException, IOException {
        
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        JSONObject jsonResponse = new JSONObject();

        try {
            Manuscript manuscript = manuscriptDAO.findById(manuscriptId);
            if (manuscript == null) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "稿件不存在");
                out.print(jsonResponse.toString());
                return;
            }

            String feedback = req.getParameter("feedback");
            if (feedback == null || feedback.trim().isEmpty()) {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "请填写反馈意见");
                out.print(jsonResponse.toString());
                return;
            }

            FormalCheckResult result = new FormalCheckResult();
            result.setManuscriptId(manuscriptId);
            result.setReviewerId(current.getUserId());
            result.setCheckTime(LocalDateTime.now());
            result.setCheckResult("FAIL");
            result.setFeedback(feedback);

            formalCheckResultDAO.save(result);

            manuscriptDAO.updateStatusWithHistory(manuscriptId, "RETURNED", "FORMAL_CHECK_RETURN", current.getUserId(), feedback);

            String guideUrl = req.getContextPath() + "/static/guides/format_guide.pdf";
            mailNotifications.onFormalCheckReturn(manuscriptId, feedback, guideUrl);
            inAppNotifications.onFormalCheckReturn(manuscriptId, feedback);

            jsonResponse.put("success", true);
            jsonResponse.put("message", "已退回修改，邮件已发送给作者，站内消息已同步发送");
            
        } catch (Exception e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "操作失败：" + e.getMessage());
        }

        out.print(jsonResponse.toString());
    }

    /**
     * 主编案头初审（Desk Review）：DESK_REVIEW_INITIAL -> TO_ASSIGN / REJECTED。
     */
    protected void handleDeskDecisionPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

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

                // ✅ 站内消息：通知作者“已通过案头初审”
                inAppNotifications.onDeskAccepted(manuscriptId);
                break;
            case "deskReject":
                // DESK_REVIEW_INITIAL -> REJECTED（需要退稿理由，作者可见）
                String rejectReason = req.getParameter("rejectReason");
                if (rejectReason == null || rejectReason.trim().isEmpty()) {
                    req.getSession().setAttribute("errorMsg", "退稿理由不能为空。请填写退稿理由后再提交。");
                    resp.sendRedirect(req.getContextPath() + "/editor/desk");
                    return;
                }
                rejectReason = rejectReason.trim();

                manuscriptDAO.deskRejectWithReason(manuscriptId, current.getUserId(), rejectReason);

                // ✅ 站内消息 + 邮件：通知作者退稿原因
                inAppNotifications.onDeskRejected(manuscriptId, rejectReason);
                mailNotifications.onDeskRejected(manuscriptId, rejectReason);
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
    protected void handleAssignEditorPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

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

        // 1）更新稿件当前编辑、状态 -> WITH_EDITOR，并写入状态历史
        String historyRemark = (chiefComment == null || chiefComment.trim().isEmpty())
                ? "主编指派责任编辑"
                : ("主编指派责任编辑：" + chiefComment.trim());
        manuscriptDAO.assignEditorWithHistory(manuscriptId, editorId, current.getUserId(), historyRemark);

        // 2）新增逻辑：记录“主编指派编辑”的建议
        assignmentDAO.createAssignment(
                manuscriptId,
                editorId,
                current.getUserId(),  // 当前登录用户即主编
                chiefComment
        );

        // 主编指派编辑：站内 + 邮件通知（不影响主流程）
        inAppNotifications.onEditorAssigned(manuscriptId, current, editorId, chiefComment);
        mailNotifications.onEditorAssigned(manuscriptId, current, editorId, chiefComment);

        // ✅ 同步通知作者：稿件已分配责任编辑
        inAppNotifications.onEditorAssignedToAuthor(manuscriptId, current, editorId);

        resp.sendRedirect(req.getContextPath() + "/editor/toAssign");
    }


    /**
     * 主编终审：根据 op 参数决定 ACCEPT / REJECT / REVISION。
     */
    protected void handleFinalDecisionPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

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

            // 撤稿：站内 + 邮件通知（不影响主流程）
            inAppNotifications.onRetract(manuscriptId);
            mailNotifications.onRetract(manuscriptId);

            resp.sendRedirect(req.getContextPath() + "/editor/special");
            return;
        }

        // 常规终审决策：Accept / Reject / Revision
        String decision;
        String newStatus;
        String decisionText;
        switch (op) {
            case "accept":
                decision = "ACCEPT";
                newStatus = "ACCEPTED";
                decisionText = "录用";
                break;
            case "reject":
                decision = "REJECT";
                newStatus = "REJECTED";
                decisionText = "退稿";
                break;
            case "revision":
                decision = "REVISION";
                newStatus = "REVISION";
                decisionText = "修改后再审";
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
        }

        manuscriptDAO.updateFinalDecision(manuscriptId, decision, newStatus);

        // 终审结果：站内 + 邮件通知（不影响主流程）
        inAppNotifications.onFinalDecision(manuscriptId, decisionText);
        mailNotifications.onFinalDecision(manuscriptId, decisionText);
        resp.sendRedirect(req.getContextPath() + "/editor/finalDecision");
    }

    /**
     * 审稿人库管理：邀请（创建待审核）/ 审核通过 / 启用 / 禁用审稿人账号。
     */
    protected void handleReviewerPoolPost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws SQLException, IOException {

        // 兼容：op=invite/create/approve/disable/enable
        String op = req.getParameter("op");
        if (op == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
            return;
        }

        String redirectMsg = null;

        if ("create".equals(op) || "invite".equals(op)) {
            // 新建审稿人账号（主编审稿人库）：参考编辑“创建外部审稿人账号”的流程，但这里不复用代码
            String username = trim(req.getParameter("username"));
            String password = trim(req.getParameter("password"));
            String fullName = trim(req.getParameter("fullName"));
            String email = trim(req.getParameter("email"));
            String affiliation = trim(req.getParameter("affiliation"));
            String researchArea = trim(req.getParameter("researchArea"));

            if (username == null || username.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "用户名/初始密码/邮箱不能为空。");
                return;
            }

            // 用户名存在则拒绝，避免覆盖
            User existed = userDAO.findByUsername(username);
            if (existed != null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "用户名已存在，请更换后重试。");
                return;
            }

            User reviewer = new User();
            reviewer.setUsername(username);
            reviewer.setPasswordHash(password); // 本项目示例为明文对比，沿用原逻辑
            reviewer.setFullName((fullName == null || fullName.isEmpty()) ? username : fullName);
            reviewer.setEmail(email);
            reviewer.setAffiliation(affiliation);
            reviewer.setResearchArea(researchArea);
            reviewer.setStatus("ACTIVE");

            User created = userDAO.createUserWithRole(reviewer, "REVIEWER");
            reviewer.setUserId(created.getUserId());

            // 邀请新审稿人：邮件 + 站内（均不影响主流程）
            inAppNotifications.onInviteNewReviewer(reviewer);
            mailNotifications.onInviteNewReviewer(reviewer, password);

            redirectMsg = "已创建审稿人账号并发送邀请：" + reviewer.getUsername();

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
                redirectMsg = "已审核通过：ID=" + userId;
            } else if ("disable".equals(op)) {
                // 移除/禁用：ACTIVE/PENDING -> DISABLED
                userDAO.updateStatus(userId, "DISABLED");
                redirectMsg = "已禁用：ID=" + userId;
            } else if ("enable".equals(op)) {
                // 重新启用：DISABLED -> ACTIVE
                userDAO.updateStatus(userId, "ACTIVE");
                redirectMsg = "已启用：ID=" + userId;
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "不支持的操作类型：" + op);
                return;
            }
        }

        String target = req.getContextPath() + "/editor/reviewers";
        if (redirectMsg != null && !redirectMsg.trim().isEmpty()) {
            target += "?msg=" + URLEncoder.encode(redirectMsg, "UTF-8");
        }
        resp.sendRedirect(target);
    }

    /**
     * 参数安全 trim：null -> null。
     * 避免直接调用 String#trim 造成 NPE。
     */
    private String trim(String s) {
        return s == null ? null : s.trim();
    }


    


    // ========================= 选择/解除审稿人 =========================

    /**
     * 选择审稿人页面（从稿件详情页进入一个单独页面选择）。
     * URL: /editor/review/select?manuscriptId=xxx
     */
    protected void handleReviewSelectPage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "该稿件不属于当前编辑。");
                return;
            }
        }

        // 回跳地址：优先使用参数 backTo，其次根据稿件状态给一个合理默认值
        String backToUrl = req.getParameter("backTo");
        if (backToUrl == null || backToUrl.trim().isEmpty()) {
            String detailSp;
            if ("WITH_EDITOR".equals(m.getCurrentStatus())) {
                detailSp = "/editor/withEditor";
            } else if ("UNDER_REVIEW".equals(m.getCurrentStatus())) {
                detailSp = "/editor/underReview";
            } else {
                detailSp = "/editor/withEditor";
            }
            backToUrl = req.getContextPath() + detailSp + "/detail?manuscriptId=" + manuscriptId;
        }

        // 搜索条件
        String reviewerKeyword = req.getParameter("reviewerKeyword");
        Integer minCompleted = null;
        Integer minAvgScore = null;
        try {
            String mc = req.getParameter("minCompleted");
            if (mc != null && !mc.trim().isEmpty()) minCompleted = Integer.parseInt(mc.trim());
        } catch (Exception ignore) {}
        try {
            String mas = req.getParameter("minAvgScore");
            if (mas != null && !mas.trim().isEmpty()) minAvgScore = Integer.parseInt(mas.trim());
        } catch (Exception ignore) {}

        // 审稿人池（带降级策略：UserDAO.searchReviewerPool 内部已处理）
        List<User> reviewers = userDAO.searchReviewerPool(reviewerKeyword, minCompleted, minAvgScore, 200);

        // 已经存在审稿记录的审稿人（用于禁用重复邀请）
        // - DECLINED：显示“已拒绝”，且勾选框置灰
        // - 其他状态：显示“已分配”，且勾选框置灰
        Set<Integer> assignedReviewerIds = new HashSet<>();
        Set<Integer> declinedReviewerIds = new HashSet<>();
        List<Review> existing = reviewDAO.findByManuscript(manuscriptId);
        for (Review r : existing) {
            if (r == null) continue;
            String st = r.getStatus();

            // 兼容旧数据：以前拒绝可能写为 EXPIRED 且带 DeclinedAt / RejectionReason。
            boolean isDeclined = "DECLINED".equals(st)
                    || ("EXPIRED".equals(st) && (r.getDeclinedAt() != null
                    || (r.getRejectionReason() != null && !r.getRejectionReason().trim().isEmpty())));

            if (isDeclined) {
                declinedReviewerIds.add(r.getReviewerId());
            } else {
                assignedReviewerIds.add(r.getReviewerId());
            }
        }

        req.setAttribute("manuscript", m);
        req.setAttribute("backToUrl", backToUrl);
        req.setAttribute("reviewers", reviewers);
        req.setAttribute("assignedReviewerIds", assignedReviewerIds);
        req.setAttribute("declinedReviewerIds", declinedReviewerIds);
        req.setAttribute("reviewerKeyword", reviewerKeyword);
        req.setAttribute("minCompleted", minCompleted);
        req.setAttribute("minAvgScore", minAvgScore);

        req.getRequestDispatcher("/WEB-INF/jsp/editor/reviewer_select_pool.jsp")
                .forward(req, resp);
    }

    /**
     * 解除/取消审稿人（编辑在“添加审稿人/详情页”中使用）。
     * POST/GET: /editor/review/cancel
     *
     * 说明：
     * - 撤回=删除该条审稿记录（不写入额外状态），因此被撤回的审稿人可再次邀请；
     * - 为避免“点击后看起来没反应”，撤回后会回跳到 backTo / Referer 并携带 cancelMsg 提示。
     */
    protected void handleCancelReviewerPost(HttpServletRequest req, HttpServletResponse resp, User current)
        throws IOException, SQLException {

    String reviewIdStr = req.getParameter("reviewId");
    String manuscriptIdStr = req.getParameter("manuscriptId");
    if (reviewIdStr == null || manuscriptIdStr == null) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少必要参数。");
        return;
    }

    int reviewId = Integer.parseInt(reviewIdStr.trim());
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }

    Review r = reviewDAO.findById(reviewId);
    if (r == null || r.getManuscriptId() != manuscriptId) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到对应审稿记录。");
        return;
    }

    // EDITOR 只能操作分配给自己的稿件
    if ("EDITOR".equals(current.getRoleCode())) {
        Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
        if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
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
    String encoded = URLEncoder.encode(msg, "UTF-8");
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
    protected void handleAuthorCommList(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        List<Manuscript> list;
        if ("EDITOR".equals(current.getRoleCode())) {
            list = manuscriptDAO.findByStatusesForEditor(
                    current.getUserId(),
                    // 仅显示：属于该编辑且处于以下状态的稿件
                    "UNDER_REVIEW",
                    "WITH_EDITOR",
                    "EDITOR_RECOMMENDATION"
            );
        } else {
            // 其他角色（主编/编辑部管理员）：列出系统中这些状态的稿件
            list = manuscriptDAO.findByStatuses(
                    "UNDER_REVIEW",
                    "WITH_EDITOR",
                    "EDITOR_RECOMMENDATION"
            );
        }

        Map<Integer, Integer> countMap = new HashMap<>();
        for (Manuscript m : list) {
            int cnt = notificationDAO.countByManuscriptAndCategory(m.getManuscriptId(), "AUTHOR_MESSAGE");
            countMap.put(m.getManuscriptId(), cnt);
        }

        PaginationUtil.apply(req, list, "manuscripts");
        req.setAttribute("commCountMap", countMap);
        req.getRequestDispatcher("/WEB-INF/jsp/editor/author_comm_list.jsp").forward(req, resp);
    }

    /**
     * 与作者沟通：发送消息并展示沟通历史（时间线）。
     * GET: /editor/author/message?manuscriptId=xxx
     */
    protected void handleAuthorMessagePage(HttpServletRequest req, HttpServletResponse resp, User current)
            throws ServletException, IOException, SQLException {

        String manuscriptIdStr = req.getParameter("manuscriptId");
        if (manuscriptIdStr == null || manuscriptIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 manuscriptId 参数。");
            return;
        }
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
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
    protected void handleSendAuthorMessagePost(HttpServletRequest req, HttpServletResponse resp, User current)
            throws IOException, SQLException {

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
        int manuscriptId;
        try {
            manuscriptId = Integer.parseInt(manuscriptIdStr.trim());
        } catch (NumberFormatException nfe) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "manuscriptId 必须为整数。");
            return;
        }
        Manuscript m = manuscriptDAO.findById(manuscriptId);
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到稿件。");
            return;
        }

        // EDITOR 只能操作分配给自己的稿件
        if ("EDITOR".equals(current.getRoleCode())) {
            Integer ceid = manuscriptDAO.findCurrentEditorId(manuscriptId);
            if (ceid != null && !Objects.equals(ceid, current.getUserId())) {
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

    // ========================= URL 工具（用于合并页跳转） =========================

    /**
     * 在原 URL 后追加 query 参数。valueEncoded 必须已进行 URL 编码。
     * 支持 URL 带有锚点（#...），会自动把参数插入到锚点前。
     */
    protected String appendQueryParam(String url, String key, String valueEncoded) {
        if (url == null) return null;
        String base = url;
        String fragment = "";
        int idx = url.indexOf('#');
        if (idx >= 0) {
            base = url.substring(0, idx);
            fragment = url.substring(idx);
        }
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + key + "=" + valueEncoded + fragment;
    }

    /**
     * 若请求中存在某个参数（非空），则把它透传到目标 URL 上。
     * @throws UnsupportedEncodingException 
     */
    protected String appendQueryParamIfPresent(HttpServletRequest req, String url, String key) throws UnsupportedEncodingException {
        String v = req.getParameter(key);
        if (v == null || v.trim().isEmpty()) return url;
        String enc = URLEncoder.encode(v, "UTF-8");
        return appendQueryParam(url, key, enc);
    }


    // =========================
    // 编辑指派：按领域/关键词匹配的推荐排序
    // =========================
    private List<User> rankEditorsByResearchArea(List<User> editors, Manuscript manuscript) {
        if (editors == null) return Collections.emptyList();
        if (manuscript == null) return new ArrayList<>(editors);

        String msArea = manuscript.getSubjectArea();
        String msKeywords = manuscript.getKeywords();
        Set<String> msTokens = tokenizeKeywords((msArea == null ? "" : msArea) + " " + (msKeywords == null ? "" : msKeywords));

        List<User> copy = new ArrayList<>(editors);
        copy.sort((a, b) -> {
            int sa = editorMatchScore(a, msArea, msTokens);
            int sb = editorMatchScore(b, msArea, msTokens);
            if (sa != sb) return Integer.compare(sb, sa); // 降序
            boolean ha = a != null && a.getResearchArea() != null && !a.getResearchArea().trim().isEmpty();
            boolean hb = b != null && b.getResearchArea() != null && !b.getResearchArea().trim().isEmpty();
            if (ha != hb) return hb ? 1 : -1;
            String ua = a == null ? "" : String.valueOf(a.getUsername());
            String ub = b == null ? "" : String.valueOf(b.getUsername());
            return ua.compareToIgnoreCase(ub);
        });
        return copy;
    }

    private int editorMatchScore(User editor, String manuscriptSubjectArea, Set<String> manuscriptTokens) {
        if (editor == null) return 0;
        String ra = editor.getResearchArea();
        if (ra == null) ra = "";
        ra = ra.trim();
        if (ra.isEmpty()) return 0;

        int score = 0;

        // token 交集
        Set<String> eTokens = tokenizeKeywords(ra);
        if (manuscriptTokens != null && !manuscriptTokens.isEmpty()) {
            for (String t : eTokens) {
                if (manuscriptTokens.contains(t)) score++;
            }
        }

        // 额外：subjectArea 与 researchArea 互为子串时加权
        String ms = manuscriptSubjectArea == null ? "" : manuscriptSubjectArea.trim();
        if (!ms.isEmpty()) {
            String msLower = ms.toLowerCase();
            String raLower = ra.toLowerCase();
            if (msLower.contains(raLower) || raLower.contains(msLower)) score += 2;
        }

        return score;
    }

    private Set<String> tokenizeKeywords(String s) {
        if (s == null) return Collections.emptySet();
        String normalized = s.toLowerCase()
                .replaceAll("[\u3000\t\r\n]+", " ")
                .replaceAll("[，、；;|/\\\\]+", " ")   // 注意这里是 /\\\\
                .replaceAll("[,]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) return Collections.emptySet();

        String[] parts = normalized.split(" ");
        Set<String> set = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (t.length() == 1 && t.charAt(0) <= 127) continue; // 过滤英文单字符
            set.add(t);
        }
        return set;
    }





    /**
     * /editor/* 路径与“菜单入口权限”映射（与 MenuAuthzFilter 保持一致）。
     */
    protected String requiredMenuPermission(String path) {
        if (path == null) return null;

        // 编辑部管理员：形式审查
        if (path.startsWith("/formalCheck/history")) return PermissionCatalog.MENU_EO_FORMAL_HISTORY;
        if (path.startsWith("/formalCheck/autoCheck")) return PermissionCatalog.MENU_EO_FORMAL_CHECK;
        if (path.startsWith("/formalCheck")) return PermissionCatalog.MENU_EO_FORMAL_CHECK;

        // 主编
        if (path.startsWith("/overview")) return PermissionCatalog.MENU_EIC_OVERVIEW;
        if (path.startsWith("/desk")) return PermissionCatalog.MENU_EIC_DESK;
        if (path.startsWith("/toAssign")) return PermissionCatalog.MENU_EIC_TO_ASSIGN;
        if (path.startsWith("/reviewers")) return PermissionCatalog.MENU_EIC_REVIEWERS;
        if (path.startsWith("/finalDecision")) return PermissionCatalog.MENU_EIC_FINAL_DECISION;
        if (path.startsWith("/special")) return PermissionCatalog.MENU_EIC_SPECIAL;

        // 编辑：列表/推荐
        if (path.startsWith("/withEditor")) return PermissionCatalog.MENU_EDITOR_TODO;
        if (path.startsWith("/underReview")) return PermissionCatalog.MENU_EDITOR_UNDER_REVIEW;
        if (path.startsWith("/recommend")) return PermissionCatalog.MENU_EDITOR_RECOMMEND;

        // 审稿动作：选择/邀请/外部邀请/取消 -> “编辑待办”
        if (path.startsWith("/review/select") || path.startsWith("/review/externalInvite")
                || path.startsWith("/review/invite") || path.startsWith("/review/inviteExternal") || path.startsWith("/review/cancel")) {
            return PermissionCatalog.MENU_EDITOR_TODO;
        }
        // 催审/监控 -> “审稿监控”
        if (path.startsWith("/review/remind") || path.startsWith("/review/remindCustom") || path.startsWith("/review/autoRemindNow")
                || path.startsWith("/review/monitor")) {
            return PermissionCatalog.MENU_EDITOR_REVIEW_MONITOR;
        }

        // 作者沟通（页面 + 发消息动作）
        if (path.startsWith("/author/message") || path.startsWith("/authorComm")) {
            return PermissionCatalog.MENU_EDITOR_AUTHOR_COMM;
        }

        return null;
    }

}