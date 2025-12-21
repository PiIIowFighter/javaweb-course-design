package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责访问 dbo.Manuscripts 的简单 DAO，
 * 实现作者投稿和“我的稿件列表”等基础功能。
 */
public class ManuscriptDAO {

    /**
     * 新增稿件（可指定状态）。
     * @param conn          事务连接（由调用方控制提交/回滚）
     * @param m             稿件对象
     * @param status        DRAFT / SUBMITTED 等
     * @param setSubmitTime 是否写入 SubmitTime（正式提交时为 true，保存草稿时为 false）
     */
    public Manuscript insertWithStatus(Connection conn, Manuscript m, String status, boolean setSubmitTime) throws SQLException {
        String sql = "INSERT INTO dbo.Manuscripts " +
                "(JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime) " +
                "VALUES (?,?,?,?,?,?,?,?,?, " + (setSubmitTime ? "SYSUTCDATETIME()" : "NULL") + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            if (m.getJournalId() != null) {
                ps.setInt(idx++, m.getJournalId());
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
        String sql = "UPDATE dbo.Manuscripts SET " +
                "JournalId = ?, Title = ?, Abstract = ?, Keywords = ?, SubjectArea = ?, FundingInfo = ?, AuthorList = ?, " +
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
        String sql = "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                     "FROM dbo.Manuscripts WHERE SubmitterId = ? ORDER BY ManuscriptId DESC";

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
        String sql = "SELECT ManuscriptId, JournalId, SubmitterId, Title, Abstract, Keywords, SubjectArea, FundingInfo, AuthorList, Status, SubmitTime, Decision, FinalDecisionTime " +
                     "FROM dbo.Manuscripts WHERE ManuscriptId = ?";

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
     * 简单更新稿件状态，并刷新 LastStatusTime。
     * 当前阶段不做复杂的状态机校验，由上层 Servlet 控制调用时机。
     */
    public void updateStatus(int manuscriptId, String newStatus) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts " +
                "SET Status = ?, LastStatusTime = SYSUTCDATETIME() " +
                "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 为稿件分配责任编辑，并将状态变更为 WITH_EDITOR。
     * 由主编在“待指派编辑”列表中调用。
     */
    public void assignEditor(int manuscriptId, int editorUserId) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts " +
                "SET CurrentEditorId = ?, Status = 'WITH_EDITOR', LastStatusTime = SYSUTCDATETIME() " +
                "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, editorUserId);
            ps.setInt(2, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 更新终审决策：录用 / 退稿 / 修回。
     * 同时写入 Decision 字段，便于后续统计。
     */
    public void updateFinalDecision(int manuscriptId, String decision, String newStatus) throws SQLException {
        String sql = "UPDATE dbo.Manuscripts " +
                "SET Status = ?, Decision = ?, FinalDecisionTime = SYSUTCDATETIME(), LastStatusTime = SYSUTCDATETIME() " +
                "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, decision);
            ps.setInt(3, manuscriptId);
            ps.executeUpdate();
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
    }

    /**
     * 兼容旧调用：内部自建连接执行 Resubmit 更新。
     */
    public void updateAndResubmit(Manuscript m, String fromStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            updateAndResubmit(conn, m, fromStatus);
        }
    }

private Manuscript mapRow(ResultSet rs) throws SQLException {
        Manuscript m = new Manuscript();
        m.setManuscriptId(rs.getInt("ManuscriptId"));
        int journalId = rs.getInt("JournalId");
        if (!rs.wasNull()) {
            m.setJournalId(journalId);
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

        return m;
    }
}
