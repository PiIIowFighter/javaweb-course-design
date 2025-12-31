package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormalCheckResultDAO {

    public FormalCheckResult save(FormalCheckResult result) throws SQLException {
        String sql = "INSERT INTO dbo.FormalCheckResults " +
                "(ManuscriptId, ReviewerId, AuthorInfoValid, AbstractWordCountValid, " +
                "BodyWordCountValid, KeywordsValid, FootnoteNumberingValid, " +
                "FigureTableFormatValid, ReferenceFormatValid, SimilarityScore, " +
                "HighSimilarity, PlagiarismReportUrl, CheckResult, Feedback, CheckTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, result.getManuscriptId());
            ps.setInt(2, result.getReviewerId());
            
            if (result.getAuthorInfoValid() != null) {
                ps.setBoolean(3, result.getAuthorInfoValid());
            } else {
                ps.setNull(3, Types.BIT);
            }
            
            if (result.getAbstractWordCountValid() != null) {
                ps.setBoolean(4, result.getAbstractWordCountValid());
            } else {
                ps.setNull(4, Types.BIT);
            }
            
            if (result.getBodyWordCountValid() != null) {
                ps.setBoolean(5, result.getBodyWordCountValid());
            } else {
                ps.setNull(5, Types.BIT);
            }
            
            if (result.getKeywordsValid() != null) {
                ps.setBoolean(6, result.getKeywordsValid());
            } else {
                ps.setNull(6, Types.BIT);
            }
            
            if (result.getFootnoteNumberingValid() != null) {
                ps.setBoolean(7, result.getFootnoteNumberingValid());
            } else {
                ps.setNull(7, Types.BIT);
            }
            
            if (result.getFigureTableFormatValid() != null) {
                ps.setBoolean(8, result.getFigureTableFormatValid());
            } else {
                ps.setNull(8, Types.BIT);
            }
            
            if (result.getReferenceFormatValid() != null) {
                ps.setBoolean(9, result.getReferenceFormatValid());
            } else {
                ps.setNull(9, Types.BIT);
            }
            
            if (result.getSimilarityScore() != null) {
                ps.setDouble(10, result.getSimilarityScore());
            } else {
                ps.setNull(10, Types.DOUBLE);
            }
            
            if (result.getHighSimilarity() != null) {
                ps.setBoolean(11, result.getHighSimilarity());
            } else {
                ps.setNull(11, Types.BIT);
            }
            
            ps.setString(12, result.getPlagiarismReportUrl());
            ps.setString(13, result.getCheckResult());
            ps.setString(14, result.getFeedback());
            ps.setTimestamp(15, Timestamp.valueOf(result.getCheckTime()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    result.setCheckId(rs.getInt(1));
                }
            }
        }

        return result;
    }

    public FormalCheckResult findById(int checkId) throws SQLException {
        String sql = "SELECT * FROM dbo.FormalCheckResults WHERE CheckId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, checkId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }

        return null;
    }

    public FormalCheckResult findByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT TOP 1 * FROM dbo.FormalCheckResults " +
                "WHERE ManuscriptId = ? ORDER BY CheckTime DESC";

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

    public List<FormalCheckResult> findByManuscriptIdAll(int manuscriptId) throws SQLException {
        String sql = "SELECT * FROM dbo.FormalCheckResults " +
                "WHERE ManuscriptId = ? ORDER BY CheckTime DESC";

        List<FormalCheckResult> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    public List<FormalCheckResult> findByReviewerId(int reviewerId) throws SQLException {
        String sql = "SELECT * FROM dbo.FormalCheckResults " +
                "WHERE ReviewerId = ? ORDER BY CheckTime DESC";

        List<FormalCheckResult> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reviewerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    public boolean update(FormalCheckResult result) throws SQLException {
        String sql = "UPDATE dbo.FormalCheckResults SET " +
                "AuthorInfoValid = ?, AbstractWordCountValid = ?, BodyWordCountValid = ?, " +
                "KeywordsValid = ?, FootnoteNumberingValid = ?, FigureTableFormatValid = ?, " +
                "ReferenceFormatValid = ?, SimilarityScore = ?, HighSimilarity = ?, " +
                "PlagiarismReportUrl = ?, CheckResult = ?, Feedback = ?, CheckTime = ? " +
                "WHERE CheckId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, result.getAuthorInfoValid());
            ps.setBoolean(2, result.getAbstractWordCountValid());
            ps.setBoolean(3, result.getBodyWordCountValid());
            ps.setBoolean(4, result.getKeywordsValid());
            ps.setBoolean(5, result.getFootnoteNumberingValid());
            ps.setBoolean(6, result.getFigureTableFormatValid());
            ps.setBoolean(7, result.getReferenceFormatValid());
            
            if (result.getSimilarityScore() != null) {
                ps.setDouble(8, result.getSimilarityScore());
            } else {
                ps.setNull(8, Types.DOUBLE);
            }
            
            if (result.getHighSimilarity() != null) {
                ps.setBoolean(9, result.getHighSimilarity());
            } else {
                ps.setNull(9, Types.BIT);
            }
            
            ps.setString(10, result.getPlagiarismReportUrl());
            ps.setString(11, result.getCheckResult());
            ps.setString(12, result.getFeedback());
            ps.setTimestamp(13, Timestamp.valueOf(result.getCheckTime()));
            ps.setInt(14, result.getCheckId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int checkId) throws SQLException {
        String sql = "DELETE FROM dbo.FormalCheckResults WHERE CheckId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, checkId);
            return ps.executeUpdate() > 0;
        }
    }

    private FormalCheckResult mapRow(ResultSet rs) throws SQLException {
        FormalCheckResult result = new FormalCheckResult();
        result.setCheckId(rs.getInt("CheckId"));
        result.setManuscriptId(rs.getInt("ManuscriptId"));
        result.setReviewerId(rs.getInt("ReviewerId"));
        result.setAuthorInfoValid(rs.getBoolean("AuthorInfoValid"));
        result.setAbstractWordCountValid(rs.getBoolean("AbstractWordCountValid"));
        result.setBodyWordCountValid(rs.getBoolean("BodyWordCountValid"));
        result.setKeywordsValid(rs.getBoolean("KeywordsValid"));
        result.setFootnoteNumberingValid(rs.getBoolean("FootnoteNumberingValid"));
        result.setFigureTableFormatValid(rs.getBoolean("FigureTableFormatValid"));
        result.setReferenceFormatValid(rs.getBoolean("ReferenceFormatValid"));
        
        double similarityScore = rs.getDouble("SimilarityScore");
        if (!rs.wasNull()) {
            result.setSimilarityScore(similarityScore);
        }
        
        result.setHighSimilarity(rs.getBoolean("HighSimilarity"));
        result.setPlagiarismReportUrl(rs.getString("PlagiarismReportUrl"));
        
        result.setCheckResult(rs.getString("CheckResult"));
        result.setFeedback(rs.getString("Feedback"));
        
        Timestamp checkTime = rs.getTimestamp("CheckTime");
        if (checkTime != null) {
            result.setCheckTime(checkTime.toLocalDateTime());
        }

        return result;
    }
}
