package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptVersion;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;

/**
 * 稿件版本 DAO：对应 dbo.ManuscriptVersions。
 */
public class ManuscriptVersionDAO {

    /**
     * 在同一事务/连接内读取当前版本。
     * 用于“保存草稿/Resubmit”时：当用户未重新上传文件，仍需沿用上一个当前版本的附件路径。
     */
    public ManuscriptVersion findCurrentByManuscriptId(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT TOP 1 VersionId, ManuscriptId, VersionNumber, IsCurrent, " +
                "FileAnonymousPath, FileOriginalPath, CoverLetterPath, CoverLetterHtml, ResponseLetterPath, CreatedAt, CreatedBy, Remark " +
                "FROM dbo.ManuscriptVersions WHERE ManuscriptId = ? AND IsCurrent = 1 " +
                "ORDER BY VersionNumber DESC, VersionId DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public ManuscriptVersion findCurrentByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT TOP 1 VersionId, ManuscriptId, VersionNumber, IsCurrent, FileAnonymousPath, FileOriginalPath, CoverLetterPath, CoverLetterHtml, ResponseLetterPath, CreatedAt, CreatedBy, Remark " +
                "FROM dbo.ManuscriptVersions WHERE ManuscriptId = ? AND IsCurrent = 1 ORDER BY VersionNumber DESC, VersionId DESC";

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

    public void markAllNotCurrent(Connection conn, int manuscriptId) throws SQLException {
        String sql = "UPDATE dbo.ManuscriptVersions SET IsCurrent = 0 WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    public ManuscriptVersion insert(Connection conn, ManuscriptVersion v) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptVersions " +
                "(ManuscriptId, VersionNumber, IsCurrent, FileAnonymousPath, FileOriginalPath, CoverLetterPath, CoverLetterHtml, ResponseLetterPath, CreatedAt, CreatedBy, Remark) " +
                "VALUES (?,?,?,?,?,?,?,?,SYSUTCDATETIME(),?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getManuscriptId());
            ps.setInt(2, v.getVersionNumber());
            ps.setBoolean(3, v.isCurrent());
            ps.setString(4, v.getFileAnonymousPath());
            ps.setString(5, v.getFileOriginalPath());
            ps.setString(6, v.getCoverLetterPath());
            ps.setString(7, v.getCoverLetterHtml());
            ps.setString(8, v.getResponseLetterPath());
            ps.setInt(9, v.getCreatedBy());
            ps.setString(10, v.getRemark());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setVersionId(rs.getInt(1));
                }
            }
        }
        return v;
    }

    private ManuscriptVersion mapRow(ResultSet rs) throws SQLException {
        ManuscriptVersion v = new ManuscriptVersion();
        v.setVersionId(rs.getInt("VersionId"));
        v.setManuscriptId(rs.getInt("ManuscriptId"));
        v.setVersionNumber(rs.getInt("VersionNumber"));
        v.setCurrent(rs.getBoolean("IsCurrent"));
        v.setFileAnonymousPath(rs.getString("FileAnonymousPath"));
        v.setFileOriginalPath(rs.getString("FileOriginalPath"));
        v.setCoverLetterPath(rs.getString("CoverLetterPath"));
        try {
            v.setCoverLetterHtml(rs.getString("CoverLetterHtml"));
        } catch (SQLException ignored) {}
        v.setResponseLetterPath(rs.getString("ResponseLetterPath"));
        try {
            Timestamp ts = rs.getTimestamp("CreatedAt");
            if (ts != null) {
                v.setCreatedAt(ts.toLocalDateTime());
            }
        } catch (SQLException ignored) {}
        try {
            int createdBy = rs.getInt("CreatedBy");
            if (!rs.wasNull()) {
                v.setCreatedBy(createdBy);
            }
        } catch (SQLException ignored) {}
        try {
            v.setRemark(rs.getString("Remark"));
        } catch (SQLException ignored) {}
        return v;
    }
}
