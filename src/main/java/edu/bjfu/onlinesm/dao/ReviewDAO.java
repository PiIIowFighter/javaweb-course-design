package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewDAO（融合兼容版）
 *
 * 兼容审稿人端 / 编辑端在不同补丁阶段中出现的方法签名：
 * - findById(int)
 * - acceptInvitation(int) / acceptInvitation(int, Integer)
 * - declineInvitation(int) / declineInvitation(int, Integer)
 * - submitReview(...)
 * - submitReviewV2(...)（两种签名）
 * - promoteAllUnderReviewManuscriptsIfReady()（EditorServlet 调用）
 *
 * 以及结构图要求的新字段：
 * ConfidentialToEditor / KeyEvaluation / ScoreOriginality / ScoreSignificance / ScoreMethodology / ScorePresentation
 */
public class ReviewDAO {

    // ========================= 查询/邀请 =========================

    /** 按 ReviewId 查询单条审稿记录（用于“查看稿件摘要/邀请详情”等）。 */
    public Review findById(int reviewId) throws SQLException {
        String sql = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                "FROM dbo.Reviews r " +
                "LEFT JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                "WHERE r.ReviewId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /** 发出审稿邀请。 */
    public void inviteReviewer(int manuscriptId, int reviewerId, LocalDateTime dueAt) throws SQLException {
        String sql = "INSERT INTO dbo.Reviews " +
                "(ManuscriptId, ReviewerId, Status, InvitedAt, DueAt, RemindCount) " +
                "VALUES (?,?, 'INVITED', SYSUTCDATETIME(), ?, 0)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setInt(2, reviewerId);
            if (dueAt != null) ps.setTimestamp(3, Timestamp.valueOf(dueAt));
            else ps.setNull(3, Types.TIMESTAMP);
            ps.executeUpdate();
        }
    }

    /** 查询某一稿件的全部审稿记录（按邀请时间倒序）。 */
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
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    /** 按审稿人 + 稿件状态 查询“待评审稿件”列表（只取 INVITED/ACCEPTED）。 */
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
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    /** 查询某审稿人的历史评审记录（Status = SUBMITTED）。 */
    public List<Review> findHistoryByReviewer(int reviewerId) throws SQLException {
        // 增强版：获取稿件标题信息
        String sql = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail, " +
                     "m.Title AS ManuscriptTitle " +
                     "FROM dbo.Reviews r " +
                     "JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                     "LEFT JOIN dbo.Manuscripts m ON r.ManuscriptId = m.ManuscriptId " +
                     "WHERE r.ReviewerId = ? AND r.Status = 'SUBMITTED' " +
                     "ORDER BY r.SubmittedAt DESC";
        
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> list = new ArrayList<>();
                while (rs.next()) {
                    Review review = mapRow(rs);
                    // 额外获取稿件标题
                    try {
                        review.setManuscriptTitle(rs.getString("ManuscriptTitle"));
                    } catch (SQLException e) {
                        // 如果列不存在，忽略
                    }
                    list.add(review);
                }
                return list;
            }
        }
    }
    /**
     * 查找逾期需要催审的审稿任务
     * 
     * @param overdueDays 逾期天数阈值（超过DueAt多少天算逾期）
     * @param minIntervalDays 最小提醒间隔天数（避免频繁催审）
     * @param maxPerRun 每次运行最多处理的数量
     */
    public List<Review> findOverdueForAutoRemind(int overdueDays, int minIntervalDays, int maxPerRun) throws SQLException {
        String sql = "SELECT TOP (?) r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                     "FROM dbo.Reviews r " +
                     "LEFT JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                     "WHERE r.Status IN ('INVITED', 'ACCEPTED') " +
                     "  AND r.DueAt IS NOT NULL " +
                     "  AND r.DueAt < DATEADD(day, -?, SYSUTCDATETIME()) " + // 已逾期overdueDays天
                     "  AND (r.LastRemindedAt IS NULL " +
                     "       OR r.LastRemindedAt < DATEADD(day, -?, SYSUTCDATETIME())) " + // 上次提醒至少minIntervalDays天前
                     "ORDER BY r.DueAt ASC"; // 最逾期的优先
        
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxPerRun);
            ps.setInt(2, overdueDays);
            ps.setInt(3, minIntervalDays);
            
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }
    // ========================= 接受/拒绝邀请 =========================

 // ========================= 接受/拒绝邀请 =========================

    /** 旧版：审稿人接受审稿邀请（仅按 reviewId）。 */
    public void acceptInvitation(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Status = 'ACCEPTED', AcceptedAt = ISNULL(AcceptedAt, SYSUTCDATETIME()) " +
                "WHERE ReviewId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    /** 兼容新版：审稿人接受审稿邀请（带 reviewerId 校验，防越权）。 */
    public void acceptInvitation(int reviewId, Integer reviewerId) throws SQLException {
        if (reviewerId == null) {
            acceptInvitation(reviewId);
            return;
        }
        String sql = "UPDATE dbo.Reviews " +
                "SET Status = 'ACCEPTED', AcceptedAt = ISNULL(AcceptedAt, SYSUTCDATETIME()) " +
                "WHERE ReviewId = ? AND ReviewerId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.setInt(2, reviewerId);
            ps.executeUpdate();
        }
    }

    /** 旧版：审稿人拒绝审稿邀请（仅按 reviewId）。 */
    public void declineInvitation(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews SET Status = 'DECLINED' WHERE ReviewId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    /** 兼容新版：审稿人拒绝审稿邀请（带 reviewerId 校验，防越权）。 */
    public void declineInvitation(int reviewId, Integer reviewerId) throws SQLException {
        if (reviewerId == null) {
            declineInvitation(reviewId);
            return;
        }
        String sql = "UPDATE dbo.Reviews SET Status = 'DECLINED' WHERE ReviewId = ? AND ReviewerId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.setInt(2, reviewerId);
            ps.executeUpdate();
        }
    }

    /** 新版：审稿人拒绝审稿邀请（带拒绝理由）。 */
    public void declineInvitation(int reviewId, Integer reviewerId, String rejectionReason) throws SQLException {
        if (reviewerId == null) {
            declineInvitationWithReason(reviewId, rejectionReason);
            return;
        }
        String sql = "UPDATE dbo.Reviews SET Status = 'DECLINED', RejectionReason = ?, DeclinedAt = SYSUTCDATETIME() WHERE ReviewId = ? AND ReviewerId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rejectionReason);
            ps.setInt(2, reviewId);
            ps.setInt(3, reviewerId);
            ps.executeUpdate();
        }
    }

    /** 旧版：审稿人拒绝审稿邀请（带拒绝理由，不带reviewerId）。 */
    public void declineInvitationWithReason(int reviewId, String rejectionReason) throws SQLException {
        String sql = "UPDATE dbo.Reviews SET Status = 'DECLINED', RejectionReason = ?, DeclinedAt = SYSUTCDATETIME() WHERE ReviewId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rejectionReason);
            ps.setInt(2, reviewId);
            ps.executeUpdate();
        }
    }

    // ========================= 提交评审 =========================

    /** 基础提交（老功能）。 */
    public void submitReview(int reviewId, String content, Double score, String recommendation) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Content = ?, Score = ?, Recommendation = ?, Status = 'SUBMITTED', SubmittedAt = SYSUTCDATETIME() " +
                "WHERE ReviewId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, content);
            if (score != null) ps.setDouble(2, score);
            else ps.setNull(2, Types.DECIMAL);
            ps.setString(3, recommendation);
            ps.setInt(4, reviewId);
            ps.executeUpdate();
        }

        // 提交后尝试推进稿件状态
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }

    /**
     * v2 提交评审（老签名）：结构图字段 + 总体分 + 推荐结论 + 给作者的意见。
     * 兼容旧库：若新列不存在，自动降级为 submitReview。
     */
    public void submitReviewV2(int reviewId,
                               String confidentialToEditor,
                               String keyEvaluation,
                               Integer scoreOriginality,
                               Integer scoreSignificance,
                               Integer scoreMethodology,
                               Integer scorePresentation,
                               Double totalScore,
                               String recommendation,
                               String commentsToAuthor) throws SQLException {

        String sqlV2 = "UPDATE dbo.Reviews SET " +
                "ConfidentialToEditor = ?, " +
                "KeyEvaluation = ?, " +
                "ScoreOriginality = ?, " +
                "ScoreSignificance = ?, " +
                "ScoreMethodology = ?, " +
                "ScorePresentation = ?, " +
                "Content = ?, " +
                "Score = ?, " +
                "Recommendation = ?, " +
                "Status = 'SUBMITTED', " +
                "SubmittedAt = SYSUTCDATETIME() " +
                "WHERE ReviewId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlV2)) {
            ps.setString(1, confidentialToEditor);
            ps.setString(2, keyEvaluation);
            if (scoreOriginality != null) ps.setInt(3, clampScore(scoreOriginality));
            else ps.setNull(3, Types.INTEGER);
            if (scoreSignificance != null) ps.setInt(4, clampScore(scoreSignificance));
            else ps.setNull(4, Types.INTEGER);
            if (scoreMethodology != null) ps.setInt(5, clampScore(scoreMethodology));
            else ps.setNull(5, Types.INTEGER);
            if (scorePresentation != null) ps.setInt(6, clampScore(scorePresentation));
            else ps.setNull(6, Types.INTEGER);
            ps.setString(7, commentsToAuthor);
            if (totalScore != null) ps.setDouble(8, totalScore);
            else ps.setNull(8, Types.DECIMAL);
            ps.setString(9, recommendation);
            ps.setInt(10, reviewId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("ConfidentialToEditor") || msg.contains("KeyEvaluation") || msg.contains("ScoreOriginality"))) {
                submitReview(reviewId, commentsToAuthor, totalScore, recommendation);
                return;
            }
            throw ex;
        }

        // 提交后尝试推进稿件状态
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }

    /**
     * v2 提交评审（新签名）：ReviewerServlet 常用的 11 参数版本
     * (reviewId, reviewerId, commentsToAuthor, confidentialToEditor, keyEvaluation,
     *  scoreOverall, scoreOriginality, scoreSignificance, scoreMethodology, scorePresentation, recommendation)
     *
     * 说明：四项分数通常来自前端数字输入，可能是 Double；这里会四舍五入并限制到 0~10。
     * 兼容旧库：若新列不存在，自动降级为 submitReview（只写 Content/Score/Recommendation）。
     */
    public void submitReviewV2(int reviewId,
                               Integer reviewerId,
                               String commentsToAuthor,
                               String confidentialToEditor,
                               String keyEvaluation,
                               Double scoreOverall,
                               Double scoreOriginality,
                               Double scoreSignificance,
                               Double scoreMethodology,
                               Double scorePresentation,
                               String recommendation) throws SQLException {

        Integer so = roundToInt(scoreOriginality);
        Integer ss = roundToInt(scoreSignificance);
        Integer sm = roundToInt(scoreMethodology);
        Integer sp = roundToInt(scorePresentation);

        // 如果总体分没传，默认取四项均值（存在项才参与）
        Double overall = scoreOverall;
        if (overall == null) {
            double sum = 0;
            int cnt = 0;
            if (so != null) { sum += so; cnt++; }
            if (ss != null) { sum += ss; cnt++; }
            if (sm != null) { sum += sm; cnt++; }
            if (sp != null) { sum += sp; cnt++; }
            if (cnt > 0) overall = sum / cnt;
        }

        String sqlV2 = "UPDATE dbo.Reviews SET " +
                "ConfidentialToEditor = ?, " +
                "KeyEvaluation = ?, " +
                "ScoreOriginality = ?, " +
                "ScoreSignificance = ?, " +
                "ScoreMethodology = ?, " +
                "ScorePresentation = ?, " +
                "Content = ?, " +
                "Score = ?, " +
                "Recommendation = ?, " +
                "Status = 'SUBMITTED', " +
                "SubmittedAt = SYSUTCDATETIME() " +
                "WHERE ReviewId = ? AND ReviewerId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlV2)) {
            ps.setString(1, confidentialToEditor);
            ps.setString(2, keyEvaluation);
            if (so != null) ps.setInt(3, clampScore(so)); else ps.setNull(3, Types.INTEGER);
            if (ss != null) ps.setInt(4, clampScore(ss)); else ps.setNull(4, Types.INTEGER);
            if (sm != null) ps.setInt(5, clampScore(sm)); else ps.setNull(5, Types.INTEGER);
            if (sp != null) ps.setInt(6, clampScore(sp)); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, commentsToAuthor);
            if (overall != null) ps.setDouble(8, overall); else ps.setNull(8, Types.DECIMAL);
            ps.setString(9, recommendation);
            ps.setInt(10, reviewId);
            if (reviewerId != null) ps.setInt(11, reviewerId);
            else ps.setNull(11, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("ConfidentialToEditor") || msg.contains("KeyEvaluation") || msg.contains("ScoreOriginality"))) {
                // 降级：旧库仅写作者意见+总体分+推荐结论
                submitReview(reviewId, commentsToAuthor, overall, recommendation);
                return;
            }
            throw ex;
        }

        // 提交后尝试推进稿件状态
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }
    /** 催审：RemindCount + 1, LastRemindedAt 更新为当前时间。 */
    public void remind(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews SET RemindCount = ISNULL(RemindCount,0) + 1, LastRemindedAt = SYSUTCDATETIME() WHERE ReviewId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    // ========================= 状态推进（EditorServlet 调用） =========================

    /**
     * 批量推进：当稿件处于 UNDER_REVIEW，且该稿件“有效邀请”(INVITED/ACCEPTED/SUBMITTED)中
     * 不存在未提交项（即不存在 INVITED/ACCEPTED），并且至少存在 1 条 SUBMITTED，
     * 则推进为 EDITOR_RECOMMENDATION。
     *
     * DECLINED/EXPIRED 不阻塞推进（不算有效邀请）。
     */
    public void promoteAllUnderReviewManuscriptsIfReady() throws SQLException {
        String sql = "UPDATE m SET m.Status = 'EDITOR_RECOMMENDATION' " +
                "FROM dbo.Manuscripts m " +
                "WHERE m.Status = 'UNDER_REVIEW' " +
                "AND EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = m.ManuscriptId AND r.Status = 'SUBMITTED') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = m.ManuscriptId AND r.Status IN ('INVITED','ACCEPTED'))";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * 单稿件推进：通过 reviewId 反查 ManuscriptId 后按同样规则推进。
     * 若 reviewId 不存在或稿件不满足条件，则不做任何事。
     */
    public void promoteManuscriptToEditorRecommendationIfReadyByReviewId(int reviewId) {
        String getManuscriptSql = "SELECT ManuscriptId FROM dbo.Reviews WHERE ReviewId = ?";
        String promoteSql = "UPDATE dbo.Manuscripts SET Status = 'EDITOR_RECOMMENDATION' " +
                "WHERE ManuscriptId = ? AND Status = 'UNDER_REVIEW' " +
                "AND EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = ? AND r.Status = 'SUBMITTED') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = ? AND r.Status IN ('INVITED','ACCEPTED'))";
        try (Connection conn = DbUtil.getConnection()) {
            Integer manuscriptId = null;
            try (PreparedStatement ps = conn.prepareStatement(getManuscriptSql)) {
                ps.setInt(1, reviewId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) manuscriptId = rs.getInt(1);
                }
            }
            if (manuscriptId == null) return;
            try (PreparedStatement ps2 = conn.prepareStatement(promoteSql)) {
                ps2.setInt(1, manuscriptId);
                ps2.setInt(2, manuscriptId);
                ps2.setInt(3, manuscriptId);
                ps2.executeUpdate();
            }
        } catch (Exception ignore) {
            // 不影响主流程（避免提交成功后因为推进失败而报 500）
        }
    }


    // ========================= 映射/工具 =========================

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getInt("ReviewId"));
        r.setManuscriptId(rs.getInt("ManuscriptId"));
        r.setReviewerId(rs.getInt("ReviewerId"));
        r.setContent(rs.getString("Content"));

        double score = rs.getDouble("Score");
        if (!rs.wasNull()) r.setScore(score);

        r.setRecommendation(rs.getString("Recommendation"));
        r.setStatus(rs.getString("Status"));

        // 只有一个 Timestamp t 声明
        Timestamp t;
        t = rs.getTimestamp("InvitedAt");
        if (t != null) r.setInvitedAt(t.toLocalDateTime());
        t = rs.getTimestamp("AcceptedAt");
        if (t != null) r.setAcceptedAt(t.toLocalDateTime());
        t = rs.getTimestamp("SubmittedAt");
        if (t != null) r.setSubmittedAt(t.toLocalDateTime());
        t = rs.getTimestamp("DueAt");
        if (t != null) r.setDueAt(t.toLocalDateTime());
        t = rs.getTimestamp("LastRemindedAt");
        if (t != null) r.setLastRemindedAt(t.toLocalDateTime());
        
        // 新增的拒绝时间字段
        t = rs.getTimestamp("DeclinedAt");  // 使用同一个变量 t
        if (t != null) r.setDeclinedAt(t.toLocalDateTime());

        try { r.setRemindCount(rs.getInt("RemindCount")); } catch (Exception ignore) {}

        // join 字段
        try { r.setReviewerName(rs.getString("ReviewerName")); } catch (Exception ignore) {}
        try { r.setReviewerEmail(rs.getString("ReviewerEmail")); } catch (Exception ignore) {}

        // v2 字段（老库可能不存在，需容错）
        try { r.setConfidentialToEditor(rs.getString("ConfidentialToEditor")); } catch (Exception ignore) {}
        try { r.setKeyEvaluation(rs.getString("KeyEvaluation")); } catch (Exception ignore) {}

        try { int v = rs.getInt("ScoreOriginality"); if (!rs.wasNull()) r.setScoreOriginality(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreSignificance"); if (!rs.wasNull()) r.setScoreSignificance(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreMethodology"); if (!rs.wasNull()) r.setScoreMethodology(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScorePresentation"); if (!rs.wasNull()) r.setScorePresentation(v); } catch (Exception ignore) {}
        // 新增5个字段映射
        try { int v = rs.getInt("ScoreExperimentation"); if (!rs.wasNull()) r.setScoreExperimentation(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreLiteratureReview"); if (!rs.wasNull()) r.setScoreLiteratureReview(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreConclusions"); if (!rs.wasNull()) r.setScoreConclusions(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreAcademicIntegrity"); if (!rs.wasNull()) r.setScoreAcademicIntegrity(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScorePracticality"); if (!rs.wasNull()) r.setScorePracticality(v); } catch (Exception ignore) {}
        
        // 新增拒绝理由字段映射
        try { r.setRejectionReason(rs.getString("RejectionReason")); } catch (Exception ignore) {}
        
        return r;
    }

    private static Integer roundToInt(Double v) {
        if (v == null) return null;
        return (int) Math.round(v);
    }

    private static int clampScore(int v) {
        if (v < 0) return 0;
        if (v > 10) return 10;
        return v;
    }
}
