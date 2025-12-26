package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptStageTimestamps;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 稿件阶段时间戳 DAO
 * 用于管理稿件各阶段完成时间的数据访问
 */
public class ManuscriptStageTimestampsDAO {

    /**
     * 根据稿件ID查询时间戳记录
     * @param manuscriptId 稿件ID
     * @return 时间戳记录，如果不存在返回null
     */
    public ManuscriptStageTimestamps findByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT ManuscriptId, DraftCompletedAt, SubmittedAt, FormalCheckCompletedAt, " +
                     "DeskReviewInitialCompletedAt, ToAssignCompletedAt, WithEditorCompletedAt, " +
                     "UnderReviewCompletedAt, EditorRecommendationCompletedAt, FinalDecisionPendingCompletedAt " +
                     "FROM dbo.ManuscriptStageTimestamps WHERE ManuscriptId = ?";
        
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
     * 检查记录是否存在
     * @param manuscriptId 稿件ID
     * @return 是否存在
     */
    public boolean exists(int manuscriptId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.ManuscriptStageTimestamps WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 检查记录是否存在（事务版本）
     */
    public boolean exists(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.ManuscriptStageTimestamps WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 创建新记录（仅ManuscriptId，其他字段为NULL）
     * @param manuscriptId 稿件ID
     */
    public void create(int manuscriptId) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStageTimestamps (ManuscriptId) VALUES (?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 创建新记录（事务版本）
     */
    public void create(Connection conn, int manuscriptId) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStageTimestamps (ManuscriptId) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 更新指定阶段的完成时间为当前时间
     * @param manuscriptId 稿件ID
     * @param fromStatus 离开的状态（即完成的阶段）
     */
    public void updateStageCompletedAt(int manuscriptId, String fromStatus) throws SQLException {
        String columnName = getColumnNameByStatus(fromStatus);
        if (columnName == null) {
            return; // 无效状态，静默忽略
        }
        
        String sql = "UPDATE dbo.ManuscriptStageTimestamps SET " + columnName + " = SYSUTCDATETIME() " +
                     "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 更新指定阶段的完成时间（事务版本）
     */
    public void updateStageCompletedAt(Connection conn, int manuscriptId, String fromStatus) throws SQLException {
        String columnName = getColumnNameByStatus(fromStatus);
        if (columnName == null) {
            return; // 无效状态，静默忽略
        }
        
        String sql = "UPDATE dbo.ManuscriptStageTimestamps SET " + columnName + " = SYSUTCDATETIME() " +
                     "WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    /**
     * 确保记录存在并更新时间戳（组合方法）
     * @param manuscriptId 稿件ID
     * @param fromStatus 离开的状态
     */
    public void ensureAndUpdateStage(int manuscriptId, String fromStatus) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensureAndUpdateStage(conn, manuscriptId, fromStatus);
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
     * 确保记录存在并更新时间戳（事务版本）
     */
    public void ensureAndUpdateStage(Connection conn, int manuscriptId, String fromStatus) throws SQLException {
        // 先检查记录是否存在
        if (!exists(conn, manuscriptId)) {
            create(conn, manuscriptId);
        }
        // 更新对应阶段的完成时间
        updateStageCompletedAt(conn, manuscriptId, fromStatus);
    }

    /**
     * 根据状态码获取对应的数据库列名
     */
    private String getColumnNameByStatus(String statusCode) {
        if (statusCode == null) return null;
        switch (statusCode) {
            case "DRAFT":
                return "DraftCompletedAt";
            case "SUBMITTED":
                return "SubmittedAt";
            case "FORMAL_CHECK":
                return "FormalCheckCompletedAt";
            case "DESK_REVIEW_INITIAL":
                return "DeskReviewInitialCompletedAt";
            case "TO_ASSIGN":
                return "ToAssignCompletedAt";
            case "WITH_EDITOR":
                return "WithEditorCompletedAt";
            case "UNDER_REVIEW":
                return "UnderReviewCompletedAt";
            case "EDITOR_RECOMMENDATION":
                return "EditorRecommendationCompletedAt";
            case "FINAL_DECISION_PENDING":
                return "FinalDecisionPendingCompletedAt";
            default:
                return null;
        }
    }

    /**
     * 映射结果集到实体对象
     * 将数据库中的UTC时间转换为北京时间（UTC+8）
     */
    private ManuscriptStageTimestamps mapRow(ResultSet rs) throws SQLException {
        ManuscriptStageTimestamps mst = new ManuscriptStageTimestamps();
        mst.setManuscriptId(rs.getInt("ManuscriptId"));
        
        // 北京时间时区
        ZoneId beijingZone = ZoneId.of("Asia/Shanghai");
        
        Timestamp ts;
        
        ts = rs.getTimestamp("DraftCompletedAt");
        if (ts != null) mst.setDraftCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("SubmittedAt");
        if (ts != null) mst.setSubmittedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("FormalCheckCompletedAt");
        if (ts != null) mst.setFormalCheckCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("DeskReviewInitialCompletedAt");
        if (ts != null) mst.setDeskReviewInitialCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("ToAssignCompletedAt");
        if (ts != null) mst.setToAssignCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("WithEditorCompletedAt");
        if (ts != null) mst.setWithEditorCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("UnderReviewCompletedAt");
        if (ts != null) mst.setUnderReviewCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("EditorRecommendationCompletedAt");
        if (ts != null) mst.setEditorRecommendationCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        ts = rs.getTimestamp("FinalDecisionPendingCompletedAt");
        if (ts != null) mst.setFinalDecisionPendingCompletedAt(convertUtcToBeijing(ts, beijingZone));
        
        return mst;
    }
    
    /**
     * 将UTC时间戳转换为北京时间的LocalDateTime
     * @param ts UTC时间戳（从数据库读取，数据库存储的是UTC时间）
     * @param beijingZone 北京时区
     * @return 北京时间的LocalDateTime
     */
    private java.time.LocalDateTime convertUtcToBeijing(Timestamp ts, ZoneId beijingZone) {
        // Timestamp表示的是UTC时间点，先转换为UTC的ZonedDateTime
        ZonedDateTime utcZoned = ts.toInstant().atZone(ZoneId.of("UTC"));
        // 转换为北京时间
        ZonedDateTime beijingZoned = utcZoned.withZoneSameInstant(beijingZone);
        // 返回LocalDateTime（不包含时区信息，但已经是北京时间）
        return beijingZoned.toLocalDateTime();
    }
}
