package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptVersion;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;

/**
 * 稿件版本 DAO：对应 dbo.ManuscriptVersions。
 */
public class ManuscriptVersionDAO {

    private boolean hasColumn(Connection conn, String table, String column) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            // SQL Server 通常大小写不敏感；schema 这里用 dbo
            try (ResultSet rs = meta.getColumns(null, "dbo", table, column)) {
                if (rs.next()) return true;
            }
            // 兜底：不带 schema 再查一次
            try (ResultSet rs = meta.getColumns(null, null, table, column)) {
                return rs.next();
            }
        } catch (Exception ignore) {
            return false;
        }
    }

    private boolean hasColumn(ResultSet rs, String col) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) {
                if (col.equalsIgnoreCase(md.getColumnLabel(i)) || col.equalsIgnoreCase(md.getColumnName(i))) {
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * 在同一事务/连接内读取当前版本。
     * 用于“保存草稿/Resubmit”时：当用户未重新上传文件，仍需沿用上一个当前版本的附件路径。
     */
    public ManuscriptVersion findCurrentByManuscriptId(Connection conn, int manuscriptId) throws SQLException {
        boolean hasCoverHtml = hasColumn(conn, "ManuscriptVersions", "CoverLetterHtml");
        String sql = "SELECT TOP 1 VersionId, ManuscriptId, VersionNumber, IsCurrent, " +
                "FileAnonymousPath, FileOriginalPath, CoverLetterPath" +
                (hasCoverHtml ? ", CoverLetterHtml" : "") +
                ", ResponseLetterPath, CreatedAt, CreatedBy, Remark " +
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
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCoverHtml = hasColumn(conn, "ManuscriptVersions", "CoverLetterHtml");
            String sql = "SELECT TOP 1 VersionId, ManuscriptId, VersionNumber, IsCurrent, FileAnonymousPath, FileOriginalPath, CoverLetterPath" +
                    (hasCoverHtml ? ", CoverLetterHtml" : "") +
                    ", ResponseLetterPath, CreatedAt, CreatedBy, Remark " +
                    "FROM dbo.ManuscriptVersions WHERE ManuscriptId = ? AND IsCurrent = 1 ORDER BY VersionNumber DESC, VersionId DESC";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, manuscriptId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
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
        boolean hasCoverHtml = hasColumn(conn, "ManuscriptVersions", "CoverLetterHtml");
        String sql = "INSERT INTO dbo.ManuscriptVersions " +
                "(ManuscriptId, VersionNumber, IsCurrent, FileAnonymousPath, FileOriginalPath, CoverLetterPath" +
                (hasCoverHtml ? ", CoverLetterHtml" : "") +
                ", ResponseLetterPath, CreatedAt, CreatedBy, Remark) " +
                "VALUES (?,?,?,?,?,?" + (hasCoverHtml ? ",?" : "") + ",?,DATEADD(HOUR, 8, SYSUTCDATETIME()),?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setInt(idx++, v.getManuscriptId());
            ps.setInt(idx++, v.getVersionNumber());
            ps.setBoolean(idx++, v.isCurrent());
            ps.setString(idx++, v.getFileAnonymousPath());
            ps.setString(idx++, v.getFileOriginalPath());
            ps.setString(idx++, v.getCoverLetterPath());
            if (hasCoverHtml) {
                ps.setString(idx++, v.getCoverLetterHtml());
            }
            ps.setString(idx++, v.getResponseLetterPath());
            ps.setInt(idx++, v.getCreatedBy());
            ps.setString(idx, v.getRemark());

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
        if (hasColumn(rs, "CoverLetterHtml")) {
            try {
                v.setCoverLetterHtml(rs.getString("CoverLetterHtml"));
            } catch (SQLException ignored) {}
        }
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
