package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.FormalCheckResult;
import edu.bjfu.onlinesm.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FormalCheckResultDAO {

    private static final String TABLE = "dbo.FormalCheckResults";
    /** 数据库脚本中主键列名为 CheckResultId（不是 CheckId） */
    private static final String PK_COL = "CheckResultId";

    public FormalCheckResult save(FormalCheckResult result) throws SQLException {
        String sql = "INSERT INTO " + TABLE + " " +
                "(ManuscriptId, ReviewerId, AuthorInfoValid, AbstractWordCountValid, " +
                "BodyWordCountValid, KeywordsValid, FootnoteNumberingValid, " +
                "FigureTableFormatValid, ReferenceFormatValid, SimilarityScore, " +
                "HighSimilarity, PlagiarismReportUrl, CheckResult, Feedback, CheckTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, result.getManuscriptId());
            ps.setInt(2, result.getReviewerId());

            setNullableBoolean(ps, 3, result.getAuthorInfoValid());
            setNullableBoolean(ps, 4, result.getAbstractWordCountValid());
            setNullableBoolean(ps, 5, result.getBodyWordCountValid());
            setNullableBoolean(ps, 6, result.getKeywordsValid());
            setNullableBoolean(ps, 7, result.getFootnoteNumberingValid());
            setNullableBoolean(ps, 8, result.getFigureTableFormatValid());
            setNullableBoolean(ps, 9, result.getReferenceFormatValid());

            if (result.getSimilarityScore() != null) {
                ps.setBigDecimal(10, BigDecimal.valueOf(result.getSimilarityScore()));
            } else {
                ps.setNull(10, Types.DECIMAL);
            }

            setNullableBoolean(ps, 11, result.getHighSimilarity());

            ps.setString(12, result.getPlagiarismReportUrl());
            ps.setString(13, result.getCheckResult());
            ps.setString(14, result.getFeedback());

            LocalDateTime ct = (result.getCheckTime() != null) ? result.getCheckTime() : LocalDateTime.now();
            ps.setTimestamp(15, Timestamp.valueOf(ct));
            result.setCheckTime(ct);

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
        String sql = "SELECT * FROM " + TABLE + " WHERE " + PK_COL + " = ?";

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
        String sql = "SELECT TOP 1 * FROM " + TABLE + " " +
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
        String sql = "SELECT * FROM " + TABLE + " " +
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
        String sql = "SELECT * FROM " + TABLE + " " +
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
        String sql = "UPDATE " + TABLE + " SET " +
                "AuthorInfoValid = ?, AbstractWordCountValid = ?, BodyWordCountValid = ?, " +
                "KeywordsValid = ?, FootnoteNumberingValid = ?, FigureTableFormatValid = ?, " +
                "ReferenceFormatValid = ?, SimilarityScore = ?, HighSimilarity = ?, " +
                "PlagiarismReportUrl = ?, CheckResult = ?, Feedback = ?, CheckTime = ? " +
                "WHERE " + PK_COL + " = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setNullableBoolean(ps, 1, result.getAuthorInfoValid());
            setNullableBoolean(ps, 2, result.getAbstractWordCountValid());
            setNullableBoolean(ps, 3, result.getBodyWordCountValid());
            setNullableBoolean(ps, 4, result.getKeywordsValid());
            setNullableBoolean(ps, 5, result.getFootnoteNumberingValid());
            setNullableBoolean(ps, 6, result.getFigureTableFormatValid());
            setNullableBoolean(ps, 7, result.getReferenceFormatValid());

            if (result.getSimilarityScore() != null) {
                ps.setBigDecimal(8, BigDecimal.valueOf(result.getSimilarityScore()));
            } else {
                ps.setNull(8, Types.DECIMAL);
            }

            setNullableBoolean(ps, 9, result.getHighSimilarity());

            ps.setString(10, result.getPlagiarismReportUrl());
            ps.setString(11, result.getCheckResult());
            ps.setString(12, result.getFeedback());

            LocalDateTime ct = (result.getCheckTime() != null) ? result.getCheckTime() : LocalDateTime.now();
            ps.setTimestamp(13, Timestamp.valueOf(ct));

            ps.setInt(14, result.getCheckId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int checkId) throws SQLException {
        String sql = "DELETE FROM " + TABLE + " WHERE " + PK_COL + " = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, checkId);
            return ps.executeUpdate() > 0;
        }
    }

    private FormalCheckResult mapRow(ResultSet rs) throws SQLException {
        FormalCheckResult result = new FormalCheckResult();

        // 注意：数据库列名为 CheckResultId
        int id = rs.getInt(PK_COL);
        if (!rs.wasNull()) {
            result.setCheckId(id);
        }

        result.setManuscriptId(rs.getInt("ManuscriptId"));
        result.setReviewerId(rs.getInt("ReviewerId"));

        result.setAuthorInfoValid(getNullableBoolean(rs, "AuthorInfoValid"));
        result.setAbstractWordCountValid(getNullableBoolean(rs, "AbstractWordCountValid"));
        result.setBodyWordCountValid(getNullableBoolean(rs, "BodyWordCountValid"));
        result.setKeywordsValid(getNullableBoolean(rs, "KeywordsValid"));
        result.setFootnoteNumberingValid(getNullableBoolean(rs, "FootnoteNumberingValid"));
        result.setFigureTableFormatValid(getNullableBoolean(rs, "FigureTableFormatValid"));
        result.setReferenceFormatValid(getNullableBoolean(rs, "ReferenceFormatValid"));

        BigDecimal similarity = rs.getBigDecimal("SimilarityScore");
        if (similarity != null) {
            result.setSimilarityScore(similarity.doubleValue());
        }

        result.setHighSimilarity(getNullableBoolean(rs, "HighSimilarity"));
        result.setPlagiarismReportUrl(rs.getString("PlagiarismReportUrl"));
        result.setCheckResult(rs.getString("CheckResult"));
        result.setFeedback(rs.getString("Feedback"));

        Timestamp checkTime = rs.getTimestamp("CheckTime");
        if (checkTime != null) {
            result.setCheckTime(checkTime.toLocalDateTime());
        }

        return result;
    }

    private static void setNullableBoolean(PreparedStatement ps, int idx, Boolean value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.BIT);
        } else {
            ps.setBoolean(idx, value);
        }
    }

    private static Boolean getNullableBoolean(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return rs.wasNull() ? null : v;
    }
}
