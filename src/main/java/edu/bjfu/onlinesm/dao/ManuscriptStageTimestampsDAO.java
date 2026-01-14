package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptStageTimestamps;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;


public class ManuscriptStageTimestampsDAO {

    
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

    
    public boolean exists(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.ManuscriptStageTimestamps WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    
    public void create(int manuscriptId) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStageTimestamps (ManuscriptId) VALUES (?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
    public void create(Connection conn, int manuscriptId) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStageTimestamps (ManuscriptId) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
    public void updateStageCompletedAt(int manuscriptId, String fromStatus) throws SQLException {
        String columnName = getColumnNameByStatus(fromStatus);
        if (columnName == null) {
            return; 
        }
        
        String sql = "UPDATE dbo.ManuscriptStageTimestamps SET " + columnName + " = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                     "WHERE ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
    public void updateStageCompletedAt(Connection conn, int manuscriptId, String fromStatus) throws SQLException {
        String columnName = getColumnNameByStatus(fromStatus);
        if (columnName == null) {
            return; 
        }
        
        String sql = "UPDATE dbo.ManuscriptStageTimestamps SET " + columnName + " = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                     "WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    
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

    
    public void ensureAndUpdateStage(Connection conn, int manuscriptId, String fromStatus) throws SQLException {
        
        if (!exists(conn, manuscriptId)) {
            create(conn, manuscriptId);
        }
        
        updateStageCompletedAt(conn, manuscriptId, fromStatus);
    }

    
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

    
    private ManuscriptStageTimestamps mapRow(ResultSet rs) throws SQLException {
        ManuscriptStageTimestamps mst = new ManuscriptStageTimestamps();
        mst.setManuscriptId(rs.getInt("ManuscriptId"));
        
        Timestamp ts;
        
        ts = rs.getTimestamp("DraftCompletedAt");
        if (ts != null) mst.setDraftCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("SubmittedAt");
        if (ts != null) mst.setSubmittedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("FormalCheckCompletedAt");
        if (ts != null) mst.setFormalCheckCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("DeskReviewInitialCompletedAt");
        if (ts != null) mst.setDeskReviewInitialCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("ToAssignCompletedAt");
        if (ts != null) mst.setToAssignCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("WithEditorCompletedAt");
        if (ts != null) mst.setWithEditorCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("UnderReviewCompletedAt");
        if (ts != null) mst.setUnderReviewCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("EditorRecommendationCompletedAt");
        if (ts != null) mst.setEditorRecommendationCompletedAt(ts.toLocalDateTime());
        
        ts = rs.getTimestamp("FinalDecisionPendingCompletedAt");
        if (ts != null) mst.setFinalDecisionPendingCompletedAt(ts.toLocalDateTime());
        
        return mst;
    }
}

/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

