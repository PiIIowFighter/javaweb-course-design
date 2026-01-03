package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// 用于记录阶段时间戳
import edu.bjfu.onlinesm.dao.ManuscriptStageTimestampsDAO;

/**
 * 负责访问 dbo.Manuscripts 的简单 DAO，
 * 实现作者投稿和“我的稿件列表”等基础功能。
 */
public class ManuscriptDAO {

    // 阶段时间戳 DAO 实例
    private final ManuscriptStageTimestampsDAO stageTimestampsDAO = new ManuscriptStageTimestampsDAO();

    /**
     * 前台“Latest published”列表：用 ACCEPTED 状态近似已发表。
     * 课程设计中未单独维护 PublishedArticles 表，因此此处仅做展示用途。
     */
    public List<Manuscript> findLatestAccepted(int limit) throws SQLException {
        String top = limit > 0 ? "TOP " + limit + " " : "";
        String sql = "SELECT " + top + " ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                     "FROM dbo.Manuscripts " +
                     "WHERE IsArchived = 0 AND IsWithdrawn = 0 AND Status IN ('ACCEPTED') " +
                     "ORDER BY FinalDecisionTime DESC, ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** 前台详情页：只允许读取 ACCEPTED 状态稿件。 */
    public Manuscript findAcceptedById(int manuscriptId) throws SQLException {
        // 同时读取 dbo.ArticleMetrics（如果存在）用于详情页展示
        String sql = "SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                     "am.ViewCount, am.DownloadCount, am.CitationCount, am.PopularityScore " +
                     "FROM dbo.Manuscripts m " +
                     "LEFT JOIN dbo.ArticleMetrics am ON am.ManuscriptId = m.ManuscriptId " +
                     "WHERE m.ManuscriptId = ? AND m.Status IN ('ACCEPTED')";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowPublic(rs);
                }
            }
        }
        return null;
    }

    /**
     * 新增稿件（可指定状态）。
     * @param conn          事务连接（由调用方控制提交/回滚）
     * @param m             稿件对象
     * @param status        DRAFT / SUBMITTED 等
     * @param setSubmitTime 是否写入 SubmitTime（正式提交时为 true，保存草稿时为 false）
     */
    public Manuscript insertWithStatus(Connection conn, Manuscript m, String status, boolean setSubmitTime) throws SQLException {
        String sql = "INSERT INTO dbo.Manuscripts " +
                "(JournalId, IssueId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?, " + (setSubmitTime ? "SYSUTCDATETIME()" : "NULL") + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }

            if (m.getIssueId() != null) {
                ps.setInt(idx++, m.getIssueId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }
            ps.setInt(idx++, m.getSubmitterId());
            ps.setString(idx++, m.getTitle());
            ps.setString(idx++, m.getAbstractText());
            ps.setString(idx++, m.getKeywords());
            ps.setString(idx++, m.getSubjectArea());
            ps.setString(idx++, m.getFundingInfo());
            ps.setString(idx++, m.getAuthorList());
            ps.setString(idx, status);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    m.setManuscriptId(rs.getInt(1));
                }
            }
        }

        m.setCurrentStatus(status);
        
        // 创建稿件后立即创建时间戳记录
        stageTimestampsDAO.create(conn, m.getManuscriptId());
        
        return m;
    }

    /**
     * 更新稿件元数据，并可指定状态流转（保存草稿/最终提交）。
     * @param conn          事务连接
     * @param m             稿件对象（需包含 ManuscriptId）
     * @param status        新状态
     * @param setSubmitTime 是否写入 SubmitTime（仅当原 SubmitTime 为空时写入）
     */
    public void updateMetadataAndStatus(Connection conn, Manuscript m, String status, boolean setSubmitTime) throws SQLException {
        // 先获取当前状态，以便记录时间戳
        String oldStatus = null;
        String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setInt(1, m.getManuscriptId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    oldStatus = rs.getString("Status");
                }
            }
        }
        
        String sql = "UPDATE dbo.Manuscripts SET " +
                "JournalId = ?, IssueId = ?, Title = ?, Abstract = ?, Keywords = ?, SubjectArea = ?, FundingInfo = ?, AuthorList = ?, " +
                "Status = ?, " +
                (setSubmitTime ? "SubmitTime = ISNULL(SubmitTime, SYSUTCDATETIME()), " : "") +
                "LastStatusTime = SYSUTCDATETIME() " +
                "WHERE ManuscriptId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }
            if (m.getIssueId() != null) {
                ps.setInt(idx++, m.getIssueId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }

            ps.setString(idx++, m.getTitle());
            ps.setString(idx++, m.getAbstractText());
            ps.setString(idx++, m.getKeywords());
            ps.setString(idx++, m.getSubjectArea());
            ps.setString(idx++, m.getFundingInfo());
            ps.setString(idx++, m.getAuthorList());
            ps.setString(idx++, status);
            ps.setInt(idx, m.getManuscriptId());
            ps.executeUpdate();
        }

        m.setCurrentStatus(status);
        
        // 如果状态发生变化，记录时间戳
        if (oldStatus != null && !oldStatus.equals(status)) {
            stageTimestampsDAO.ensureAndUpdateStage(conn, m.getManuscriptId(), oldStatus);
        }
    }

    /**
     * 新增稿件。初始状态设置为 SUBMITTED，提交时间为当前时间。
     * 返回带有主键 ID 的 Manuscript 对象。
     */
    public Manuscript insert(Manuscript m) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(true);
            return insertWithStatus(conn, m, "SUBMITTED", true);
        }
    }

    /**
     * 查询指定作者的所有稿件。
     */
    public List<Manuscript> findBySubmitter(int submitterId) throws SQLException {
        String sql = "SELECT m.ManuscriptId, m.JournalId, m.IssueId, i.Title AS IssueTitle, " +
                     "m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, " +
                     "m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime " +
                     "FROM dbo.Manuscripts m " +
                     "LEFT JOIN dbo.Issues i ON i.IssueId = m.IssueId " +
                     "WHERE m.SubmitterId = ? ORDER BY m.ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, submitterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 按主键查询单个稿件，供详情页使用。
     */
    public Manuscript findById(int manuscriptId) throws SQLException {
        String sql = "SELECT m.ManuscriptId, m.JournalId, m.IssueId, i.Title AS IssueTitle, " +
                     "m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, " +
                     "m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime " +
                     "FROM dbo.Manuscripts m " +
                     "LEFT JOIN dbo.Issues i ON i.IssueId = m.IssueId " +
                     "WHERE m.ManuscriptId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    /**
     * 查询稿件的当前责任编辑ID
     */
    public Integer findCurrentEditorId(int manuscriptId) throws SQLException {
        // 优先从 dbo.Manuscripts.CurrentEditorId 读取（新版脚本）。
        // 但部分同学的旧库没有该列，会导致“Invalid column name 'CurrentEditorId'”从而页面 500。
        String sql1 = "SELECT CurrentEditorId FROM dbo.Manuscripts WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql1)) {

            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int editorId = rs.getInt(1);
                    return rs.wasNull() ? null : editorId;
                }
            }
            return null;

        } catch (SQLException ex) {

            // 仅在“列不存在/老库兼容”场景下回退（避免掩盖真实数据库故障）。
            String msg = ex.getMessage();
            String low = (msg == null) ? "" : msg.toLowerCase();
            boolean columnMissing =
                    (msg != null && msg.contains("CurrentEditorId"))
                            || low.contains("invalid column")
                            || low.contains("unknown column");

            if (!columnMissing) {
                throw ex;
            }

            // 回退：从 dbo.ManuscriptAssignments 取最新一条指派记录的 EditorId（旧库兼容）。
            String sql2 = "SELECT TOP 1 EditorId " +
                          "FROM dbo.ManuscriptAssignments " +
                          "WHERE ManuscriptId = ? " +
                          "ORDER BY AssignedTime DESC, AssignmentId DESC";
            try (Connection conn2 = DbUtil.getConnection();
                 PreparedStatement ps2 = conn2.prepareStatement(sql2)) {

                ps2.setInt(1, manuscriptId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        int editorId = rs2.getInt(1);
                        return rs2.wasNull() ? null : editorId;
                    }
                }
            } catch (SQLException ignore) {
                // 如果旧库也没有该表，则返回 null 让上层决定如何处理（不直接 500）。
            }

            return null;
        }
    }
/**
     * 按单一状态查询所有稿件（编辑部视角简单使用）。
     */
    public List<Manuscript> findByStatus(String status) throws SQLException {
        return findByStatuses(status);
    }

    /**
     * 按多种状态查询所有稿件。
     * 仅用于编辑部工作台的简单列表展示，不区分具体编辑。
     */
    public List<Manuscript> findByStatuses(String... statuses) throws SQLException {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("statuses 不能为空");
        }

        StringBuilder sql = new StringBuilder(
                "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                "FROM dbo.Manuscripts WHERE IsArchived = 0 AND IsWithdrawn = 0 AND Status IN ("
        );
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
        sql.append(") ORDER BY SubmitTime DESC, ManuscriptId DESC");

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < statuses.length; i++) {
                ps.setString(i + 1, statuses[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 根据状态和当前指定的责任编辑筛选稿件列表。
     * 仅返回 IsArchived = 0 且 IsWithdrawn = 0 的记录。
     *
     * 说明：
     *  - 主要用于编辑“我的稿件”列表，保证编辑只能看到主编指派给自己的稿件；
     *  - 这里通过 Manuscripts.CurrentEditorId 做过滤，不直接依赖历史指派记录。
     */
    public List<Manuscript> findByStatusesForEditor(int editorUserId, String... statuses) throws SQLException {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("statuses 不能为空");
        }

        // 1) 优先使用 Manuscripts.CurrentEditorId（如果数据库版本已包含该列）。
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, " +
                    "       SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                    "FROM dbo.Manuscripts WHERE IsArchived = 0 AND IsWithdrawn = 0 " +
                    "  AND CurrentEditorId = ? AND Status IN ("
            );
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
            }
            sql.append(") ORDER BY ISNULL(SubmitTime, LastStatusTime) DESC, ManuscriptId DESC");

            List<Manuscript> list = new ArrayList<>();
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                int idx = 1;
                ps.setInt(idx++, editorUserId);
                for (String s : statuses) {
                    ps.setString(idx++, s);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            // 兼容旧版数据库：没有 CurrentEditorId 列时，降级到 ManuscriptAssignments 或逐条判断。
            String msg = (e.getMessage() == null) ? "" : e.getMessage();
            if (!msg.contains("CurrentEditorId")) {
                throw e; // 不是列缺失导致的错误，继续抛出
            }
        }

        // 2) 尝试通过 ManuscriptAssignments 的“最新指派”来过滤
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("WITH latest AS (\n");
            sql.append("    SELECT ManuscriptId, EditorId,\n");
            sql.append("           ROW_NUMBER() OVER (PARTITION BY ManuscriptId ORDER BY AssignedTime DESC, AssignmentId DESC) AS rn\n");
            sql.append("    FROM dbo.ManuscriptAssignments\n");
            sql.append(")\n");
            sql.append("SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords,\n");
            sql.append("       m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime\n");
            sql.append("FROM dbo.Manuscripts m\n");
            sql.append("JOIN latest la ON la.ManuscriptId = m.ManuscriptId AND la.rn = 1\n");
            sql.append("WHERE m.IsArchived = 0 AND m.IsWithdrawn = 0\n");
            sql.append("  AND la.EditorId = ? AND m.Status IN (");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sql.append(',');
                sql.append('?');
            }
            sql.append(") ORDER BY ISNULL(m.SubmitTime, m.LastStatusTime) DESC, m.ManuscriptId DESC");

            List<Manuscript> list = new ArrayList<>();
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                int idx = 1;
                ps.setInt(idx++, editorUserId);
                for (String s : statuses) {
                    ps.setString(idx++, s);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return list;
        } catch (SQLException ignore) {
            // 3) 兜底：先按状态取出，再逐条比对 current editor
            List<Manuscript> raw = findByStatuses(statuses);
            List<Manuscript> filtered = new ArrayList<>();
            for (Manuscript m : raw) {
                Integer ce = findCurrentEditorId(m.getManuscriptId());
                if (java.util.Objects.equals(ce, editorUserId)) {
                    filtered.add(m);
                }
            }
            return filtered;
        }
    }


    /**
     * 主编“全览权限”使用：查询系统内全部稿件（包含已归档/已撤稿/草稿等）。
     *
     * 说明：当前项目未单独维护“稿件状态历史表”，因此这里的“历史入口”
     * 主要通过跳转到 /manuscripts/detail?id=xxx 查看：
     *  - 当前版本/附件（dbo.ManuscriptVersions 等）
     *  - 审稿流程记录（dbo.Reviews）
     */
    public List<Manuscript> findAllForChief() throws SQLException {
        String sql = "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime "
                   + "FROM dbo.Manuscripts "
                   + "ORDER BY ISNULL(SubmitTime, LastStatusTime) DESC, ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 主编特殊权限：撤销之前的终审决定（Rescind Decision）。
     * 将稿件状态回退到 FINAL_DECISION_PENDING，并清空 Decision/FinalDecisionTime。
     */
    public void rescindDecision(int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts "
                   + "SET Status = 'FINAL_DECISION_PENDING', "
                   + "    Decision = NULL, "
                   + "    FinalDecisionTime = NULL, "
                   + "    LastStatusTime = SYSUTCDATETIME() "
                   + "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 主编特殊权限：撤稿（Retract）。
     * 当前实现采用“归档 + 标记撤稿”方式：IsWithdrawn=1、IsArchived=1、Status=ARCHIVED。
     */
    public void retractManuscript(int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts "
                   + "SET IsWithdrawn = 1, "
                   + "    IsArchived = 1, "
                   + "    Status = 'ARCHIVED', "
                   + "    LastStatusTime = SYSUTCDATETIME() "
                   + "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 案头初审退稿：DESK_REVIEW_INITIAL -> REJECTED。
     * 与“终审退稿”区分点：本操作不会写入 FinalDecisionTime。
     */
    public void deskReject(int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts " +
                "SET Status = 'REJECTED', " +
                "    Decision = 'REJECT', " +
                "    FinalDecisionTime = NULL, " +
                "    CurrentEditorId = NULL, " +
                "    LastStatusTime = SYSUTCDATETIME() " +
                "WHERE ManuscriptId = ? AND Status = 'DESK_REVIEW_INITIAL'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 主编特殊权限：更改案头初审决定。
     * - deskAccept  -> Status=TO_ASSIGN
     * - deskReject  -> Status=REJECTED (Decision='REJECT')
     *
     * 更改后：
     * - 过期该稿件所有 INVITED/ACCEPTED 审稿记录（避免审稿人继续操作）
     * - 写入 ManuscriptStatusHistory
     */
    public void changeDeskDecision(int manuscriptId, String deskOp, int changedBy, String reason) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ManuscriptSnapshot snap = lockAndLoadSnapshot(conn, manuscriptId);
                if (snap == null) {
                    throw new IllegalStateException("未找到该稿件。");
                }
                if (snap.isArchived || snap.isWithdrawn) {
                    throw new IllegalStateException("该稿件已归档/撤稿，无法更改决定。");
                }
                if (snap.finalDecisionTime != null) {
                    throw new IllegalStateException("该稿件已做出终审决定（FinalDecisionTime 非空），不能按“初审决定”改判。");
                }

                String fromStatus = snap.status;
                String toStatus;
                String decision;

                if ("deskAccept".equalsIgnoreCase(deskOp)) {
                    toStatus = "TO_ASSIGN";
                    decision = null;
                } else if ("deskReject".equalsIgnoreCase(deskOp)) {
                    toStatus = "REJECTED";
                    decision = "REJECT";
                } else {
                    throw new IllegalStateException("不支持的 deskOp：" + deskOp);
                }

                String updateSql = "UPDATE dbo.Manuscripts SET Status=?, Decision=?, FinalDecisionTime=NULL, CurrentEditorId=NULL, LastStatusTime=SYSUTCDATETIME() " +
                        "WHERE ManuscriptId=? AND IsArchived=0 AND IsWithdrawn=0";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, toStatus);
                    if (decision == null) ps.setNull(2, java.sql.Types.NVARCHAR);
                    else ps.setString(2, decision);
                    ps.setInt(3, manuscriptId);
                    ps.executeUpdate();
                }

                expireActiveReviews(conn, manuscriptId);
                insertStatusHistory(conn, manuscriptId, fromStatus, toStatus, "CHANGE_DESK_DECISION", changedBy, reason);
                
                // 记录阶段完成时间戳
                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, fromStatus);

                conn.commit();
            } catch (RuntimeException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 主编特殊权限：更改终审决定（仅对已做出终审决定的稿件）。
     * - accept   -> ACCEPTED (Decision='ACCEPT')
     * - reject   -> REJECTED (Decision='REJECT')
     * - revision -> REVISION (Decision='REVISION')
     */
    public void changeFinalDecision(int manuscriptId, String finalOp, int changedBy, String reason) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ManuscriptSnapshot snap = lockAndLoadSnapshot(conn, manuscriptId);
                if (snap == null) {
                    throw new IllegalStateException("未找到该稿件。");
                }
                if (snap.isArchived || snap.isWithdrawn) {
                    throw new IllegalStateException("该稿件已归档/撤稿，无法更改决定。");
                }
                if (snap.finalDecisionTime == null || !("ACCEPTED".equals(snap.status) || "REJECTED".equals(snap.status) || "REVISION".equals(snap.status))) {
                    throw new IllegalStateException("该稿件尚未形成终审决定，不能使用“更改终审决定”。");
                }

                String fromStatus = snap.status;
                String toStatus;
                String decision;

                if ("accept".equalsIgnoreCase(finalOp)) {
                    toStatus = "ACCEPTED";
                    decision = "ACCEPT";
                } else if ("reject".equalsIgnoreCase(finalOp)) {
                    toStatus = "REJECTED";
                    decision = "REJECT";
                } else if ("revision".equalsIgnoreCase(finalOp)) {
                    toStatus = "REVISION";
                    decision = "REVISION";
                } else {
                    throw new IllegalStateException("不支持的 finalOp：" + finalOp);
                }

                String updateSql = "UPDATE dbo.Manuscripts SET Status=?, Decision=?, FinalDecisionTime=SYSUTCDATETIME(), CurrentEditorId=NULL, LastStatusTime=SYSUTCDATETIME() " +
                        "WHERE ManuscriptId=? AND IsArchived=0 AND IsWithdrawn=0";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, toStatus);
                    ps.setString(2, decision);
                    ps.setInt(3, manuscriptId);
                    ps.executeUpdate();
                }

                expireActiveReviews(conn, manuscriptId);
                insertStatusHistory(conn, manuscriptId, fromStatus, toStatus, "CHANGE_FINAL_DECISION", changedBy, reason);
                
                // 记录阶段完成时间戳
                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, fromStatus);

                conn.commit();
            } catch (RuntimeException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 主编特殊权限：撤稿（课程口径：ACCEPTED 即视为已发表）。
     *
     * 说明：原本可按 Issues.IsPublished=1 + IssueManuscripts 关联判定“已发表”，
     * 但本课程要求/系统口径调整为：Manuscripts.Status='ACCEPTED' 即视为已发表。
     */
    public void retractPublished(int manuscriptId, int changedBy, String reason) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ManuscriptSnapshot snap = lockAndLoadSnapshot(conn, manuscriptId);
                if (snap == null) {
                    throw new IllegalStateException("未找到该稿件。");
                }
                if (snap.isArchived || snap.isWithdrawn) {
                    throw new IllegalStateException("该稿件已归档/撤稿，不能重复撤稿。");
                }
                if (!"ACCEPTED".equalsIgnoreCase(snap.status)) {
                    throw new IllegalStateException("仅已发表（ACCEPTED）稿件允许撤稿。");
                }

                String fromStatus = snap.status;
                String toStatus = "ARCHIVED";

                String updateSql = "UPDATE dbo.Manuscripts SET IsWithdrawn=1, IsArchived=1, Status='ARCHIVED', LastStatusTime=SYSUTCDATETIME() WHERE ManuscriptId=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, manuscriptId);
                    ps.executeUpdate();
                }

                expireActiveReviews(conn, manuscriptId);
                insertStatusHistory(conn, manuscriptId, fromStatus, toStatus, "RETRACT_PUBLISHED", changedBy, reason);
                
                // 记录阶段完成时间戳
                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, fromStatus);

                conn.commit();
            } catch (RuntimeException ex) {
                conn.rollback();
                throw ex;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ======== helpers (transaction scoped) ========

    private static class ManuscriptSnapshot {
        final String status;
        final boolean isArchived;
        final boolean isWithdrawn;
        final Timestamp finalDecisionTime;

        ManuscriptSnapshot(String status, boolean isArchived, boolean isWithdrawn, Timestamp finalDecisionTime) {
            this.status = status;
            this.isArchived = isArchived;
            this.isWithdrawn = isWithdrawn;
            this.finalDecisionTime = finalDecisionTime;
        }
    }

    private ManuscriptSnapshot lockAndLoadSnapshot(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT Status, IsArchived, IsWithdrawn, FinalDecisionTime FROM dbo.Manuscripts WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ManuscriptSnapshot(
                        rs.getString("Status"),
                        rs.getBoolean("IsArchived"),
                        rs.getBoolean("IsWithdrawn"),
                        rs.getTimestamp("FinalDecisionTime")
                );
            }
        }
    }

    private void expireActiveReviews(Connection conn, int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.Reviews SET Status='EXPIRED' WHERE ManuscriptId=? AND Status IN ('INVITED','ACCEPTED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    private boolean isPublished(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM dbo.IssueManuscripts im JOIN dbo.Issues i ON i.IssueId = im.IssueId WHERE im.ManuscriptId = ? AND i.IsPublished = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertStatusHistory(Connection conn, int manuscriptId, String fromStatus, String toStatus, String event, int changedBy, String remark) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStatusHistory (ManuscriptId, FromStatus, ToStatus, Event, ChangedBy, Remark) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setString(2, fromStatus);
            ps.setString(3, toStatus);
            ps.setString(4, event);
            ps.setInt(5, changedBy);
            ps.setString(6, remark);
            ps.executeUpdate();
        }
    }

    /**
     * 简单更新稿件状态，并刷新 LastStatusTime。
     * 当前阶段不做复杂的状态机校验，由上层 Servlet 控制调用时机。
     * 同时记录阶段完成时间戳。
     */
    public void updateStatus(int manuscriptId, String newStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 获取当前状态
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }
                
                // 更新状态
                String sql = "UPDATE dbo.Manuscripts " +
                        "SET Status = ?, LastStatusTime = SYSUTCDATETIME() " +
                        "WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }
                
                // 如果状态发生变化，记录时间戳
                if (oldStatus != null && !oldStatus.equals(newStatus)) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 更新稿件状态并记录状态变更历史。
     * @param manuscriptId 稿件ID
     * @param newStatus 新状态
     * @param event 事件类型
     * @param changedBy 操作者用户ID
     * @param remark 备注
     */
    public void updateStatusWithHistory(int manuscriptId, String newStatus, String event, int changedBy, String remark) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 获取当前状态
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }

                // 更新状态
                String updateSql = "UPDATE dbo.Manuscripts SET Status = ?, LastStatusTime = SYSUTCDATETIME() WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }

                // 记录历史
                insertStatusHistory(conn, manuscriptId, oldStatus, newStatus, event, changedBy, remark);
                
                // 记录阶段完成时间戳
                if (oldStatus != null) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 为稿件分配责任编辑，并将状态变更为 WITH_EDITOR。
     * 由主编在"待指派编辑"列表中调用。
     * 同时记录阶段完成时间戳。
     */
    public void assignEditor(int manuscriptId, int editorUserId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 获取当前状态
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }
                
                // 更新状态和编辑
                String sql = "UPDATE dbo.Manuscripts " +
                        "SET CurrentEditorId = ?, Status = 'WITH_EDITOR', LastStatusTime = SYSUTCDATETIME() " +
                        "WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, editorUserId);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }
                
                // 如果状态发生变化，记录时间戳
                if (oldStatus != null && !oldStatus.equals("WITH_EDITOR")) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 为稿件分配责任编辑并记录状态变更历史。
     */
    public void assignEditorWithHistory(int manuscriptId, int editorUserId, int changedBy, String remark) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 获取当前状态
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }

                // 更新状态和编辑
                String updateSql = "UPDATE dbo.Manuscripts SET CurrentEditorId = ?, Status = 'WITH_EDITOR', LastStatusTime = SYSUTCDATETIME() WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, editorUserId);
                    ps.setInt(2, manuscriptId);
                    ps.executeUpdate();
                }

                // 记录历史
                insertStatusHistory(conn, manuscriptId, oldStatus, "WITH_EDITOR", "ASSIGN_EDITOR", changedBy, remark);
                
                // 记录阶段完成时间戳
                if (oldStatus != null) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 更新终审决策：录用 / 退稿 / 修回。
     * 同时写入 Decision 字段，便于后续统计。
     * 同时记录阶段完成时间戳。
     */
    public void updateFinalDecision(int manuscriptId, String decision, String newStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 获取当前状态
                String oldStatus = null;
                String querySql = "SELECT Status FROM dbo.Manuscripts WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                    ps.setInt(1, manuscriptId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("Status");
                        }
                    }
                }
                
                // 更新状态
                String sql = "UPDATE dbo.Manuscripts " +
                        "SET Status = ?, Decision = ?, FinalDecisionTime = SYSUTCDATETIME(), LastStatusTime = SYSUTCDATETIME() " +
                        "WHERE ManuscriptId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, decision);
                    ps.setInt(3, manuscriptId);
                    ps.executeUpdate();
                }
                
                // 如果状态发生变化，记录时间戳
                if (oldStatus != null && !oldStatus.equals(newStatus)) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, oldStatus);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 作者在 RETURNED / REVISION 状态下修改稿件并重新提交时，
     * 同时更新基础字段以及状态流转。
     *
     * @param m           修改后的稿件对象（至少包含 ManuscriptId、Title 等）
     * @param fromStatus  原始状态：RETURNED 或 REVISION
     */
    public void updateAndResubmit(Connection conn, Manuscript m, String fromStatus) throws SQLException {
        if (!"RETURNED".equals(fromStatus) && !"REVISION".equals(fromStatus)) {
            throw new IllegalArgumentException("不支持的 Resubmit 来源状态: " + fromStatus);
        }

        StringBuilder sql = new StringBuilder("UPDATE dbo.Manuscripts SET ");
        sql.append("Title = ?, ");
        sql.append("Abstract = ?, ");
        sql.append("Keywords = ?, ");
        sql.append("SubjectArea = ?, ");
        sql.append("FundingInfo = ?, ");
        sql.append("AuthorList = ?, ");
        sql.append("JournalId = ?, ");

        if ("RETURNED".equals(fromStatus)) {
            // 形式审查退回：重新提交后回到 SUBMITTED，由编辑部管理员再次形式审查
            sql.append("Status = 'SUBMITTED', ");
        } else {
            // 终审“修回”：重新提交后进入 WITH_EDITOR，由责任编辑继续处理
            sql.append("Status = 'WITH_EDITOR', ");
            sql.append("CurrentRound = ISNULL(CurrentRound, 0) + 1, ");
            sql.append("Decision = NULL, ");
            sql.append("FinalDecisionTime = NULL, ");
        }

        sql.append("LastStatusTime = SYSUTCDATETIME() ");
        sql.append("WHERE ManuscriptId = ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, m.getTitle());
            ps.setString(idx++, m.getAbstractText());
            ps.setString(idx++, m.getKeywords());
            ps.setString(idx++, m.getSubjectArea());
            ps.setString(idx++, m.getFundingInfo());
            ps.setString(idx++, m.getAuthorList());

            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
            } else {
                ps.setNull(idx++, Types.INTEGER);
            }

            ps.setInt(idx, m.getManuscriptId());
            ps.executeUpdate();
        }
        
        // 记录原状态的完成时间戳
        stageTimestampsDAO.ensureAndUpdateStage(conn, m.getManuscriptId(), fromStatus);
    }

    /**
     * 兼容旧调用：内部自建连接执行 Resubmit 更新。
     */
    public void updateAndResubmit(Manuscript m, String fromStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            updateAndResubmit(conn, m, fromStatus);
        }
    }

    /**
     * 公共映射方法：用于多个 DAO/Servlet 复用。
     * 兼容：如果 ResultSet 中包含 dbo.ArticleMetrics 的统计列，则一并映射。
     */
    public Manuscript mapRowPublic(ResultSet rs) throws SQLException {
        Manuscript m = new Manuscript();
        m.setManuscriptId(rs.getInt("ManuscriptId"));
        int journalId = rs.getInt("JournalId");
        if (!rs.wasNull()) {
            m.setJournalId(journalId);
        }

        // 专刊（可选列/别名）：IssueId / IssueTitle
        try {
            if (hasColumn(rs, "IssueId")) {
                Object v = rs.getObject("IssueId");
                if (v != null) m.setIssueId(((Number) v).intValue());
            }
            if (hasColumn(rs, "IssueTitle")) {
                m.setIssueTitle(rs.getString("IssueTitle"));
            }
        } catch (SQLException ignored) {
            // ignore
        }
        m.setSubmitterId(rs.getInt("SubmitterId"));
        m.setTitle(rs.getString("Title"));
        m.setAbstractText(rs.getString("Abstract"));
        m.setKeywords(rs.getString("Keywords"));
        try { m.setSubjectArea(rs.getString("SubjectArea")); } catch (SQLException ignored) {}
        try { m.setFundingInfo(rs.getString("FundingInfo")); } catch (SQLException ignored) {}
        try { m.setAuthorList(rs.getString("AuthorList")); } catch (SQLException ignored) {}

        m.setCurrentStatus(rs.getString("Status"));
        Timestamp ts = rs.getTimestamp("SubmitTime");
        if (ts != null) {
            m.setSubmitTime(ts.toLocalDateTime());
        }

        // 终审相关字段
        try {
            String decision = rs.getString("Decision");
            m.setDecision(decision);
        } catch (SQLException ignored) {
        }
        try {
            Timestamp finalTs = rs.getTimestamp("FinalDecisionTime");
            if (finalTs != null) {
                m.setFinalDecisionTime(finalTs.toLocalDateTime());
            }
        } catch (SQLException ignored) {
        }

        // 可选统计列（Articles 页面排序/展示）
        try {
            if (hasColumn(rs, "ViewCount")) {
                m.setViewCount((Integer) rs.getObject("ViewCount"));
            }
            if (hasColumn(rs, "DownloadCount")) {
                m.setDownloadCount((Integer) rs.getObject("DownloadCount"));
            }
            if (hasColumn(rs, "CitationCount")) {
                m.setCitationCount((Integer) rs.getObject("CitationCount"));
            }
            if (hasColumn(rs, "PopularityScore")) {
                Object v = rs.getObject("PopularityScore");
                if (v != null) m.setPopularityScore(((Number) v).doubleValue());
            }
        } catch (SQLException ignored) {
            // ignore
        }

        return m;
    }

    private Manuscript mapRow(ResultSet rs) throws SQLException {
        return mapRowPublic(rs);
    }

    private boolean hasColumn(ResultSet rs, String col) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int count = md.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = md.getColumnLabel(i);
            if (label == null || label.isEmpty()) label = md.getColumnName(i);
            if (col.equalsIgnoreCase(label)) return true;
        }
        return false;
    }

    /**
     * 基于 dbo.ArticleMetrics 排序的“已发表论文”（用 ACCEPTED 近似）。
     */
    public List<Manuscript> findAcceptedByMetric(String type, int limit) throws SQLException {
        if (type == null) type = "popular";
        String metricCol;
        if ("topcited".equalsIgnoreCase(type)) {
            metricCol = "ISNULL(am.CitationCount,0)";
        } else if ("downloaded".equalsIgnoreCase(type)) {
            metricCol = "ISNULL(am.DownloadCount,0)";
        } else {
            metricCol = "ISNULL(am.PopularityScore,0)";
        }

        String sql = "SELECT TOP " + limit + " " +
                "m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime, " +
                "am.ViewCount, am.DownloadCount, am.CitationCount, am.PopularityScore " +
                "FROM dbo.Manuscripts m " +
                "LEFT JOIN dbo.ArticleMetrics am ON am.ManuscriptId = m.ManuscriptId " +
                "WHERE m.IsArchived=0 AND m.IsWithdrawn=0 AND m.Status='ACCEPTED' " +
                "ORDER BY " + metricCol + " DESC, m.FinalDecisionTime DESC, m.ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowPublic(rs));
            }
        }
        return list;
    }

    /**
     * 文章详情页浏览计数（若不存在 metrics 记录则自动补一条）。
     */
    public void incrementViewCount(int manuscriptId) throws SQLException {
        String ensure = "IF NOT EXISTS(SELECT 1 FROM dbo.ArticleMetrics WHERE ManuscriptId=?) " +
                "INSERT INTO dbo.ArticleMetrics(ManuscriptId) VALUES (?)";
        String upd = "UPDATE dbo.ArticleMetrics SET ViewCount = ViewCount + 1, UpdatedAt = SYSUTCDATETIME() WHERE ManuscriptId=?";

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(ensure);
                 PreparedStatement ps2 = conn.prepareStatement(upd)) {
                ps1.setInt(1, manuscriptId);
                ps1.setInt(2, manuscriptId);
                ps1.executeUpdate();

                ps2.setInt(1, manuscriptId);
                ps2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 下载计数（登录后的 /files/preview 也算一次下载）。
     */
    public void incrementDownloadCount(int manuscriptId) throws SQLException {
        String ensure = "IF NOT EXISTS(SELECT 1 FROM dbo.ArticleMetrics WHERE ManuscriptId=?) " +
                "INSERT INTO dbo.ArticleMetrics(ManuscriptId) VALUES (?)";
        String upd = "UPDATE dbo.ArticleMetrics SET DownloadCount = DownloadCount + 1, UpdatedAt = SYSUTCDATETIME() WHERE ManuscriptId=?";
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(ensure);
                 PreparedStatement ps2 = conn.prepareStatement(upd)) {
                ps1.setInt(1, manuscriptId);
                ps1.setInt(2, manuscriptId);
                ps1.executeUpdate();
                ps2.setInt(1, manuscriptId);
                ps2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

}
