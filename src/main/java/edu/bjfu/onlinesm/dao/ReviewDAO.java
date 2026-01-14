package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Review;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ReviewDAO {

    
    private final ManuscriptStageTimestampsDAO stageTimestampsDAO = new ManuscriptStageTimestampsDAO();

    

    
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

    
    public void inviteReviewer(int manuscriptId, int reviewerId, LocalDateTime dueAt) throws SQLException {
        inviteReviewerReturnId(manuscriptId, reviewerId, dueAt);
    }

    
    public int inviteReviewerReturnId(int manuscriptId, int reviewerId, LocalDateTime dueAt) throws SQLException {
        
        String checkDecline = "SELECT COUNT(1) AS Cnt FROM dbo.Reviews WHERE ManuscriptId = ? AND ReviewerId = ? AND Status = 'DECLINED'";

        
        
        String cleanup = "DELETE FROM dbo.Reviews WHERE ManuscriptId = ? AND ReviewerId = ? AND Status IN ('INVITED','ACCEPTED','EXPIRED')";

        try (Connection conn = DbUtil.getConnection()) {
            
            try (PreparedStatement ps0 = conn.prepareStatement(checkDecline)) {
                ps0.setInt(1, manuscriptId);
                ps0.setInt(2, reviewerId);
                try (ResultSet rs0 = ps0.executeQuery()) {
                    if (rs0.next() && rs0.getInt("Cnt") > 0) {
                        throw new SQLException("该审稿人已拒绝本稿件邀请（DECLINED），不允许再次邀请。");
                    }
                }
            }

            
            try (PreparedStatement ps = conn.prepareStatement(cleanup)) {
                ps.setInt(1, manuscriptId);
                ps.setInt(2, reviewerId);
                ps.executeUpdate();
            }

            
            String insert1 = "INSERT INTO dbo.Reviews (ManuscriptId, ReviewerId, Status, InvitedAt, DueAt, RemindCount) " +
                    "OUTPUT INSERTED.ReviewId VALUES (?,?, 'INVITED', DATEADD(HOUR, 8, SYSUTCDATETIME()), ?, 0)";
            try {
                return execInsertReturnId(conn, insert1, manuscriptId, reviewerId, dueAt);
            } catch (SQLException ex1) {
                if (!looksLikeMissingColumnOrObject(ex1)) {
                    throw ex1;
                }
            }

            
            String insert2 = "INSERT INTO dbo.Reviews (ManuscriptId, ReviewerId, Status, DueAt, RemindCount) " +
                    "OUTPUT INSERTED.ReviewId VALUES (?,?, 'INVITED', ?, 0)";
            try {
                return execInsertReturnId(conn, insert2, manuscriptId, reviewerId, dueAt);
            } catch (SQLException ex2) {
                if (!looksLikeMissingColumnOrObject(ex2)) {
                    throw ex2;
                }
            }

            
            String insert3 = "INSERT INTO dbo.Reviews (ManuscriptId, ReviewerId, Status) OUTPUT INSERTED.ReviewId VALUES (?,?, 'INVITED')";
            return execInsertReturnId(conn, insert3, manuscriptId, reviewerId, null);
        }
    }

    private int execInsertReturnId(Connection conn, String sql, int manuscriptId, int reviewerId, LocalDateTime dueAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setInt(2, reviewerId);
            
            if (sql.contains("?") && sql.contains("DueAt")) {
                if (dueAt != null) ps.setTimestamp(3, Timestamp.valueOf(dueAt));
                else ps.setNull(3, Types.TIMESTAMP);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("插入审稿邀请失败：未返回 ReviewId");
    }

    private boolean looksLikeMissingColumnOrObject(SQLException ex) {
        if (ex == null) return false;
        String msg = ex.getMessage();
        if (msg == null) return false;
        String low = msg.toLowerCase();
        return low.contains("invalid column")
                || low.contains("unknown column")
                || low.contains("invalid object")
                || low.contains("does not exist");
    }


    
    public List<Review> findByManuscript(int manuscriptId) throws SQLException {
        
        String sql1 = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                "FROM dbo.Reviews r " +
                "LEFT JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                "WHERE r.ManuscriptId = ? " +
                "ORDER BY r.InvitedAt DESC, r.ReviewId DESC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Review> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            String low = (msg == null) ? "" : msg.toLowerCase();
            boolean columnMissing =
                    (msg != null && msg.contains("InvitedAt"))
                            || low.contains("invalid column")
                            || low.contains("unknown column");
            if (!columnMissing) {
                throw ex;
            }

            String sql2 = "SELECT r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                    "FROM dbo.Reviews r " +
                    "LEFT JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                    "WHERE r.ManuscriptId = ? " +
                    "ORDER BY r.ReviewId DESC";

            try (Connection conn2 = DbUtil.getConnection();
                 PreparedStatement ps2 = conn2.prepareStatement(sql2)) {
                ps2.setInt(1, manuscriptId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    List<Review> list2 = new ArrayList<>();
                    while (rs2.next()) list2.add(mapRow(rs2));
                    return list2;
                }
            }
        }
    }



    
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

    
    public List<Review> findHistoryByReviewer(int reviewerId) throws SQLException {
        
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
                    
                    try {
                        review.setManuscriptTitle(rs.getString("ManuscriptTitle"));
                    } catch (SQLException e) {
                        
                    }
                    list.add(review);
                }
                return list;
            }
        }
    }

    
    public List<Review> findOverdueForAutoRemind(int overdueDays, int minIntervalDays, int maxPerRun) throws SQLException {
        String sql = "SELECT TOP (?) r.*, u.FullName AS ReviewerName, u.Email AS ReviewerEmail " +
                "FROM dbo.Reviews r " +
                "LEFT JOIN dbo.Users u ON r.ReviewerId = u.UserId " +
                "WHERE r.Status IN ('INVITED', 'ACCEPTED') " +
                "  AND r.DueAt IS NOT NULL " +
                "  AND r.DueAt < DATEADD(day, -?, DATEADD(HOUR, 8, SYSUTCDATETIME())) " +
                "  AND (r.LastRemindedAt IS NULL " +
                "       OR r.LastRemindedAt < DATEADD(day, -?, DATEADD(HOUR, 8, SYSUTCDATETIME()))) " +
                "ORDER BY r.DueAt ASC";

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

    

    
    public void acceptInvitation(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Status = 'ACCEPTED', AcceptedAt = ISNULL(AcceptedAt, DATEADD(HOUR, 8, SYSUTCDATETIME())) " +
                "WHERE ReviewId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    
    public void acceptInvitation(int reviewId, Integer reviewerId) throws SQLException {
        if (reviewerId == null) {
            acceptInvitation(reviewId);
            return;
        }
        String sql = "UPDATE dbo.Reviews " +
                "SET Status = 'ACCEPTED', AcceptedAt = ISNULL(AcceptedAt, DATEADD(HOUR, 8, SYSUTCDATETIME())) " +
                "WHERE ReviewId = ? AND ReviewerId = ? AND Status = 'INVITED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.setInt(2, reviewerId);
            ps.executeUpdate();
        }
    }

    
    public void declineInvitation(int reviewId) throws SQLException {
        declineInvitation(reviewId, null, null);
    }

    
    public void declineInvitation(int reviewId, Integer reviewerId) throws SQLException {
        declineInvitation(reviewId, reviewerId, null);
    }

    
    public void declineInvitation(int reviewId, Integer reviewerId, String rejectionReason) throws SQLException {
        
        

        String sqlWithReason;
        String sqlFallback;
        if (reviewerId == null) {
            sqlWithReason = "UPDATE dbo.Reviews SET Status = 'DECLINED', RejectionReason = ?, DeclinedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                    "WHERE ReviewId = ? AND Status = 'INVITED' AND SubmittedAt IS NULL";
            sqlFallback = "UPDATE dbo.Reviews SET Status = 'DECLINED' " +
                    "WHERE ReviewId = ? AND Status = 'INVITED' AND SubmittedAt IS NULL";
        } else {
            sqlWithReason = "UPDATE dbo.Reviews SET Status = 'DECLINED', RejectionReason = ?, DeclinedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                    "WHERE ReviewId = ? AND ReviewerId = ? AND Status = 'INVITED' AND SubmittedAt IS NULL";
            sqlFallback = "UPDATE dbo.Reviews SET Status = 'DECLINED' " +
                    "WHERE ReviewId = ? AND ReviewerId = ? AND Status = 'INVITED' AND SubmittedAt IS NULL";
        }

        try (Connection conn = DbUtil.getConnection()) {
            
            try (PreparedStatement ps = conn.prepareStatement(sqlWithReason)) {
                ps.setString(1, rejectionReason);
                ps.setInt(2, reviewId);
                if (reviewerId != null) {
                    ps.setInt(3, reviewerId);
                }
                ps.executeUpdate();
                return;
            } catch (SQLException ex) {
                
                String msg = (ex.getMessage() == null ? "" : ex.getMessage());
                boolean missingNewColumns = msg.toLowerCase().contains("rejectionreason")
                        || msg.toLowerCase().contains("declinedat")
                        || msg.toLowerCase().contains("invalid column");
                if (!missingNewColumns) {
                    throw ex;
                }
            }

            
            try (PreparedStatement ps2 = conn.prepareStatement(sqlFallback)) {
                ps2.setInt(1, reviewId);
                if (reviewerId != null) {
                    ps2.setInt(2, reviewerId);
                }
                ps2.executeUpdate();
            }
        }
    }

    
    public void cancelInvitation(int reviewId) throws SQLException {
        String sql = "DELETE FROM dbo.Reviews WHERE ReviewId = ? AND Status = 'INVITED' AND SubmittedAt IS NULL";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    
    public int cancelAssignment(int reviewId) throws SQLException {
        String sql = "DELETE FROM dbo.Reviews WHERE ReviewId = ? AND Status IN ('INVITED','ACCEPTED') AND SubmittedAt IS NULL";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate();
        }
    }

    
    public int countActiveAssignmentsByManuscript(int manuscriptId) throws SQLException {
        String sql = "SELECT COUNT(1) AS Cnt FROM dbo.Reviews WHERE ManuscriptId = ? AND Status IN ('INVITED','ACCEPTED')";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Cnt");
            }
        }
        return 0;
    }

    
    public int countSubmittedByManuscript(int manuscriptId) throws SQLException {
        String sql = "SELECT COUNT(1) AS Cnt FROM dbo.Reviews WHERE ManuscriptId = ? AND Status = 'SUBMITTED'";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Cnt");
            }
        }
        return 0;
    }

    

    
    public void submitReview(int reviewId, String content, Double score, String recommendation) throws SQLException {
        String sql = "UPDATE dbo.Reviews " +
                "SET Content = ?, Score = ?, Recommendation = ?, Status = 'SUBMITTED', SubmittedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
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

        
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }

    
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
                "SubmittedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
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

        
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }

    
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
                "SubmittedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
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
                
                submitReview(reviewId, commentsToAuthor, overall, recommendation);
                return;
            }
            throw ex;
        }

        
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }

    
    public void submitReviewV3(int reviewId,
                              Integer reviewerId,
                              String commentsToAuthor,
                              String confidentialToEditor,
                              String keyEvaluation,
                              Double scoreOverall,
                              Double scoreOriginality,
                              Double scoreSignificance,
                              Double scoreMethodology,
                              Double scorePresentation,
                              Double scoreExperimentation,
                              Double scoreLiteratureReview,
                              Double scoreConclusions,
                              Double scoreAcademicIntegrity,
                              Double scorePracticality,
                              String recommendation) throws SQLException {

        Integer so = roundToInt(scoreOriginality);
        Integer ss = roundToInt(scoreSignificance);
        Integer sm = roundToInt(scoreMethodology);
        Integer sp = roundToInt(scorePresentation);
        Integer se = roundToInt(scoreExperimentation);
        Integer sl = roundToInt(scoreLiteratureReview);
        Integer sc = roundToInt(scoreConclusions);
        Integer sai = roundToInt(scoreAcademicIntegrity);
        Integer spr = roundToInt(scorePracticality);

        
        Double overall = scoreOverall;
        if (overall == null) {
            double sum = 0;
            int cnt = 0;
            Integer[] vals = new Integer[]{so, ss, sm, sp, se, sl, sc, sai, spr};
            for (Integer v : vals) {
                if (v != null) {
                    sum += v;
                    cnt++;
                }
            }
            if (cnt > 0) overall = sum / cnt;
        }

        String sqlV3 = "UPDATE dbo.Reviews SET " +
                "ConfidentialToEditor = ?, " +
                "KeyEvaluation = ?, " +
                "ScoreOriginality = ?, " +
                "ScoreSignificance = ?, " +
                "ScoreMethodology = ?, " +
                "ScorePresentation = ?, " +
                "ScoreExperimentation = ?, " +
                "ScoreLiteratureReview = ?, " +
                "ScoreConclusions = ?, " +
                "ScoreAcademicIntegrity = ?, " +
                "ScorePracticality = ?, " +
                "Content = ?, " +
                "Score = ?, " +
                "Recommendation = ?, " +
                "Status = 'SUBMITTED', " +
                "SubmittedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                "WHERE ReviewId = ? AND ReviewerId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlV3)) {
            ps.setString(1, confidentialToEditor);
            ps.setString(2, keyEvaluation);
            if (so != null) ps.setInt(3, clampScore(so)); else ps.setNull(3, Types.INTEGER);
            if (ss != null) ps.setInt(4, clampScore(ss)); else ps.setNull(4, Types.INTEGER);
            if (sm != null) ps.setInt(5, clampScore(sm)); else ps.setNull(5, Types.INTEGER);
            if (sp != null) ps.setInt(6, clampScore(sp)); else ps.setNull(6, Types.INTEGER);
            if (se != null) ps.setInt(7, clampScore(se)); else ps.setNull(7, Types.INTEGER);
            if (sl != null) ps.setInt(8, clampScore(sl)); else ps.setNull(8, Types.INTEGER);
            if (sc != null) ps.setInt(9, clampScore(sc)); else ps.setNull(9, Types.INTEGER);
            if (sai != null) ps.setInt(10, clampScore(sai)); else ps.setNull(10, Types.INTEGER);
            if (spr != null) ps.setInt(11, clampScore(spr)); else ps.setNull(11, Types.INTEGER);
            ps.setString(12, commentsToAuthor);
            if (overall != null) ps.setDouble(13, overall); else ps.setNull(13, Types.DECIMAL);
            ps.setString(14, recommendation);
            ps.setInt(15, reviewId);
            if (reviewerId != null) ps.setInt(16, reviewerId); else ps.setNull(16, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            
            if (msg != null && (
                    msg.contains("ScoreExperimentation") || msg.contains("ScoreLiteratureReview") ||
                    msg.contains("ScoreConclusions") || msg.contains("ScoreAcademicIntegrity") ||
                    msg.contains("ScorePracticality") || msg.contains("ConfidentialToEditor") ||
                    msg.contains("KeyEvaluation") || msg.contains("ScoreOriginality")
            )) {
                
                submitReviewV2(reviewId, reviewerId, commentsToAuthor, confidentialToEditor, keyEvaluation,
                        overall, scoreOriginality, scoreSignificance, scoreMethodology, scorePresentation, recommendation);
                return;
            }
            throw ex;
        }

        
        promoteManuscriptToEditorRecommendationIfReadyByReviewId(reviewId);
    }

    
    public void remind(int reviewId) throws SQLException {
        String sql = "UPDATE dbo.Reviews SET RemindCount = ISNULL(RemindCount,0) + 1, LastRemindedAt = DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE ReviewId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.executeUpdate();
        }
    }

    
    public void remindChecked(int reviewId) throws SQLException {
        remind(reviewId);
    }


    

    
    public void promoteAllUnderReviewManuscriptsIfReady() throws SQLException {
        
        
        
        

        String selectSql = "SELECT m.ManuscriptId " +
                "FROM dbo.Manuscripts m " +
                "WHERE m.Status = 'UNDER_REVIEW' " +
                "AND EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = m.ManuscriptId AND r.Status = 'SUBMITTED') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = m.ManuscriptId AND r.Status IN ('INVITED','ACCEPTED'))";

        String updateSql = "UPDATE dbo.Manuscripts SET Status='EDITOR_RECOMMENDATION', LastStatusTime=DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                "WHERE ManuscriptId=? AND Status='UNDER_REVIEW' " +
                "AND EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = ? AND r.Status = 'SUBMITTED') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = ? AND r.Status IN ('INVITED','ACCEPTED'))";

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Integer> ids = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }

                if (!ids.isEmpty()) {
                    try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
                        for (Integer manuscriptId : ids) {
                            ps2.setInt(1, manuscriptId);
                            ps2.setInt(2, manuscriptId);
                            ps2.setInt(3, manuscriptId);
                            int updated = ps2.executeUpdate();
                            if (updated > 0) {
                                
                                stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, "UNDER_REVIEW");
                            }
                        }
                    }
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

    
    public void promoteManuscriptToEditorRecommendationIfReadyByReviewId(int reviewId) {
        String getManuscriptSql = "SELECT ManuscriptId FROM dbo.Reviews WHERE ReviewId = ?";
        String promoteSql = "UPDATE dbo.Manuscripts SET Status='EDITOR_RECOMMENDATION', LastStatusTime=DATEADD(HOUR, 8, SYSUTCDATETIME()) " +
                "WHERE ManuscriptId = ? AND Status = 'UNDER_REVIEW' " +
                "AND EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = ? AND r.Status = 'SUBMITTED') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.Reviews r WHERE r.ManuscriptId = ? AND r.Status IN ('INVITED','ACCEPTED'))";
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer manuscriptId = null;
                try (PreparedStatement ps = conn.prepareStatement(getManuscriptSql)) {
                    ps.setInt(1, reviewId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) manuscriptId = rs.getInt(1);
                    }
                }
                if (manuscriptId == null) {
                    conn.commit();
                    return;
                }
                int updated;
                try (PreparedStatement ps2 = conn.prepareStatement(promoteSql)) {
                    ps2.setInt(1, manuscriptId);
                    ps2.setInt(2, manuscriptId);
                    ps2.setInt(3, manuscriptId);
                    updated = ps2.executeUpdate();
                }

                if (updated > 0) {
                    stageTimestampsDAO.ensureAndUpdateStage(conn, manuscriptId, "UNDER_REVIEW");
                }

                conn.commit();
            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignore) {}
                
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {
            
        }
    }

    

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getInt("ReviewId"));
        r.setManuscriptId(rs.getInt("ManuscriptId"));
        r.setReviewerId(rs.getInt("ReviewerId"));
        r.setContent(rs.getString("Content"));
        try {
            double score = rs.getDouble("Score");
            if (!rs.wasNull()) r.setScore(score);
        } catch (SQLException ignore) {
            
        }
        r.setRecommendation(rs.getString("Recommendation"));
        r.setStatus(rs.getString("Status"));

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

        
        try {
            t = rs.getTimestamp("DeclinedAt");
            if (t != null) r.setDeclinedAt(t.toLocalDateTime());
        } catch (Exception ignore) {
        }

        try { r.setRemindCount(rs.getInt("RemindCount")); } catch (Exception ignore) {}

        
        try { r.setReviewerName(rs.getString("ReviewerName")); } catch (Exception ignore) {}
        try { r.setReviewerEmail(rs.getString("ReviewerEmail")); } catch (Exception ignore) {}

        
        try { r.setConfidentialToEditor(rs.getString("ConfidentialToEditor")); } catch (Exception ignore) {}
        try { r.setKeyEvaluation(rs.getString("KeyEvaluation")); } catch (Exception ignore) {}

        try { int v = rs.getInt("ScoreOriginality"); if (!rs.wasNull()) r.setScoreOriginality(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreSignificance"); if (!rs.wasNull()) r.setScoreSignificance(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreMethodology"); if (!rs.wasNull()) r.setScoreMethodology(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScorePresentation"); if (!rs.wasNull()) r.setScorePresentation(v); } catch (Exception ignore) {}

        
        try { int v = rs.getInt("ScoreExperimentation"); if (!rs.wasNull()) r.setScoreExperimentation(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreLiteratureReview"); if (!rs.wasNull()) r.setScoreLiteratureReview(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreConclusions"); if (!rs.wasNull()) r.setScoreConclusions(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScoreAcademicIntegrity"); if (!rs.wasNull()) r.setScoreAcademicIntegrity(v); } catch (Exception ignore) {}
        try { int v = rs.getInt("ScorePracticality"); if (!rs.wasNull()) r.setScorePracticality(v); } catch (Exception ignore) {}

        
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

