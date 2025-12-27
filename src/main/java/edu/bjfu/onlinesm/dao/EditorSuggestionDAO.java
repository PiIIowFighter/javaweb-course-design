package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.EditorSuggestion;
import edu.bjfu.onlinesm.util.DbUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 编辑建议 DAO：dbo.EditorSuggestions
 */
public class EditorSuggestionDAO {

        /** 需要时自动建表（便于本地跑起来；正式环境仍建议先跑 sqlserver.sql）。 */
    private void ensureTable() throws SQLException {
        String ddl = ""
                + "IF OBJECT_ID('dbo.EditorSuggestions', 'U') IS NULL\n"
                + "BEGIN\n"
                + "  CREATE TABLE dbo.EditorSuggestions (\n"
                + "    ManuscriptId INT NOT NULL PRIMARY KEY,\n"
                + "    EditorId INT NOT NULL,\n"
                + "    Suggestion NVARCHAR(50) NOT NULL,\n"
                + "    Summary NVARCHAR(MAX) NULL,\n"
                + "    SubmittedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),\n"
                + "    UpdatedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),\n"
                + "    CONSTRAINT FK_EditorSuggestions_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),\n"
                + "    CONSTRAINT FK_EditorSuggestions_Editor FOREIGN KEY(EditorId) REFERENCES dbo.Users(UserId)\n"
                + "  );\n"
                + "  CREATE INDEX IX_EditorSuggestions_EditorId ON dbo.EditorSuggestions(EditorId);\n"
                + "END";
        try (Connection conn = DbUtil.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    public EditorSuggestion findByManuscriptId(int manuscriptId) throws SQLException {
        ensureTable();
        String sql = "SELECT es.ManuscriptId, es.EditorId, es.Suggestion, es.Summary, es.SubmittedAt, es.UpdatedAt,"
                + " u.FullName AS EditorName "
                + "FROM dbo.EditorSuggestions es "
                + "LEFT JOIN dbo.Users u ON es.EditorId = u.UserId "
                + "WHERE es.ManuscriptId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * 批量查询：按稿件ID映射成 Map<ManuscriptId, EditorSuggestion>。
     */
    public Map<Integer, EditorSuggestion> findByManuscriptIds(List<Integer> manuscriptIds) throws SQLException {
        ensureTable();
        Map<Integer, EditorSuggestion> map = new HashMap<>();
        if (manuscriptIds == null || manuscriptIds.isEmpty()) return map;

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < manuscriptIds.size(); i++) {
            if (i > 0) in.append(',');
            in.append('?');
        }

        String sql = "SELECT es.ManuscriptId, es.EditorId, es.Suggestion, es.Summary, es.SubmittedAt, es.UpdatedAt,"
                + " u.FullName AS EditorName "
                + "FROM dbo.EditorSuggestions es "
                + "LEFT JOIN dbo.Users u ON es.EditorId = u.UserId "
                + "WHERE es.ManuscriptId IN (" + in + ")";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < manuscriptIds.size(); i++) {
                ps.setInt(i + 1, manuscriptIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EditorSuggestion s = mapRow(rs);
                    map.put(s.getManuscriptId(), s);
                }
            }
        }
        return map;
    }

    /**
     * 插入或更新（按 ManuscriptId 唯一）。
     */
    public void upsert(EditorSuggestion s) throws SQLException {
        ensureTable();
        if (s == null) throw new IllegalArgumentException("EditorSuggestion 不能为空");

        String check = "SELECT COUNT(1) FROM dbo.EditorSuggestions WHERE ManuscriptId = ?";
        boolean exists;
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, s.getManuscriptId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                exists = rs.getInt(1) > 0;
            }
        }

        if (exists) {
            String sql = "UPDATE dbo.EditorSuggestions SET EditorId=?, Suggestion=?, Summary=?, UpdatedAt=SYSUTCDATETIME() "
                    + "WHERE ManuscriptId=?";
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, s.getEditorId());
                ps.setString(2, s.getSuggestion());
                ps.setString(3, s.getSummary());
                ps.setInt(4, s.getManuscriptId());
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO dbo.EditorSuggestions(ManuscriptId, EditorId, Suggestion, Summary, SubmittedAt, UpdatedAt) "
                    + "VALUES(?,?,?,?,SYSUTCDATETIME(),SYSUTCDATETIME())";
            try (Connection conn = DbUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, s.getManuscriptId());
                ps.setInt(2, s.getEditorId());
                ps.setString(3, s.getSuggestion());
                ps.setString(4, s.getSummary());
                ps.executeUpdate();
            }
        }
    }

    private EditorSuggestion mapRow(ResultSet rs) throws SQLException {
        EditorSuggestion s = new EditorSuggestion();
        s.setManuscriptId(rs.getInt("ManuscriptId"));
        s.setEditorId(rs.getInt("EditorId"));
        s.setSuggestion(rs.getString("Suggestion"));
        s.setSummary(rs.getString("Summary"));

        Timestamp t1 = rs.getTimestamp("SubmittedAt");
        if (t1 != null) s.setSubmittedAt(t1.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("UpdatedAt");
        if (t2 != null) s.setUpdatedAt(t2.toLocalDateTime());

        try { s.setEditorName(rs.getString("EditorName")); } catch (SQLException ignored) {}

        return s;
    }
}
