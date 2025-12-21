package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 访问 dbo.Reviews 的简化 DAO。
 *
 * 功能：
 *  - 为某稿件创建审稿邀请（INVITED）；
 *  - 根据稿件 ID 查询所有审稿记录；
 *  - 审稿人提交评审意见；
 *  - 催审（RemindCount++ / LastRemindedAt 更新）。
 */
public class ReviewDAO {

    /**
     * 发出审稿邀请。
     */
    public void inviteReviewer(int manuscriptId, int reviewerId, LocalDateTime dueAt) throws SQLException {
        String sql = "INSERT INTO dbo.Reviews " +
                "(ManuscriptId, ReviewerId, Status, InvitedAt, DueAt, RemindCount) " +
                "VALUES (?,?, 'INVITED', SYSUTCDATETIME(), ?, 0)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setInt(2, reviewerId);
            if (dueAt != null) {
                ps.setTimestamp(3, Timestamp.valueOf(dueAt));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.executeUpdate();
        }
    }

    /**
     * 查询某一稿件的全部审稿记录（按邀请时间倒序）。
     */
    public List<Review> findByManuscript(int manuscriptId) throws SQLException {
        String sql = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                "FROM dbo.Reviews r " +
                "LEFT JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                "WHERE r.ManuscriptId = ? " +
                "ORDER BY r.InvitedAt DESC, r.ReviewId DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /**
     * 审稿人提交评审意见。
     */
    public void submitReview(int reviewId, String content, Double score, String recommendation) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Content = ?, Score = ?, Recommendation = ?, Status = 'SUBMITTED', " +
                "SubmittedAt = SYSUTCDATETIME() " +
                "WHERE ReviewId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, content);
            if (score != null) {
                ps.setDouble(2, score);
            } else {
                ps.setNull(2, Types.DECIMAL);
            }
            ps.setString(3, recommendation);
            ps.setInt(4, reviewId);
            ps.executeUpdate();
        }
    }

    /**
     * 催审：RemindCount + 1, LastRemindedAt 更新为当前时间。
     */
    public void remind(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET RemindCount = ISNULL(RemindCount,0) + 1, LastRemindedAt = SYSUTCDATETIME() " +
                "WHERE ReviewId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }



    /**
     * 按审稿人和状态查询审稿记录（供审稿人查看“待评审稿件”列表）。
     */
    public List<Review> findByReviewerAndStatus(int reviewerId, String manuscriptStatus) throws SQLException {
        String sql = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                "FROM dbo.Reviews r " +
                "JOIN dbo.Manuscripts m ON r.ManuscriptId = m.ManuscriptId " +
                "JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                "WHERE r.ReviewerId = ? AND m.Status = ? " +
                "AND r.Status IN ('INVITED','ACCEPTED') " +
                "ORDER BY r.InvitedAt DESC, r.ReviewId DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewerId);
            ps.setString(2, manuscriptStatus);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    

    /**
     * 审稿人接受审稿邀请：将 Status 从 INVITED 更新为 ACCEPTED，
     * 并记录 AcceptedAt 时间（若此前为空）。
     */
    public void acceptInvitation(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Status = 'ACCEPTED', " +
                "AcceptedAt = ISNULL(AcceptedAt, SYSUTCDATETIME()) " +
                "WHERE ReviewId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    /**
     * 审稿人拒绝审稿邀请：将 Status 从 INVITED 更新为 DECLINED。
     */
    public void declineInvitation(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Status = 'DECLINED' " +
                "WHERE ReviewId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

/**
     * 查询某审稿人的历史评审记录（Status = SUBMITTED）。
     */
    public List<Review> findHistoryByReviewer(int reviewerId) throws SQLException {
        String sql = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                "FROM dbo.Reviews r " +
                "JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                "WHERE r.ReviewerId = ? AND r.Status = 'SUBMITTED' " +
                "ORDER BY r.SubmittedAt DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getInt("ReviewId"));
        r.setManuscriptId(rs.getInt("ManuscriptId"));
        r.setReviewerId(rs.getInt("ReviewerId"));
        r.setContent(rs.getString("Content"));
        double score = rs.getDouble("Score");
        if (!rs.wasNull()) {
            r.setScore(score);
        }
        r.setRecommendation(rs.getString("Recommendation"));
        r.setStatus(rs.getString("Status"));
        Timestamp t;
        t = rs.getTimestamp("InvitedAt");
        if (t != null) {
            r.setInvitedAt(t.toLocalDateTime());
        }
        t = rs.getTimestamp("AcceptedAt");
        if (t != null) {
            r.setAcceptedAt(t.toLocalDateTime());
        }
        t = rs.getTimestamp("SubmittedAt");
        if (t != null) {
            r.setSubmittedAt(t.toLocalDateTime());
        }
        t = rs.getTimestamp("DueAt");
        if (t != null) {
            r.setDueAt(t.toLocalDateTime());
        }
        t = rs.getTimestamp("LastRemindedAt");
        if (t != null) {
            r.setLastRemindedAt(t.toLocalDateTime());
        }
        r.setRemindCount(rs.getInt("RemindCount"));
        return r;
    }
}
