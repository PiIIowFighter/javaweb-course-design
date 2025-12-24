package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.JournalPage;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JournalPageDAO：读取/维护“关于期刊”等可配置页面内容。
 * 表：dbo.JournalPages(PageId, JournalId, PageKey, Title, Content, UpdatedAt, CoverImagePath?, AttachmentPath?)
 */
public class JournalPageDAO {

    public JournalPage findByJournalAndKey(int journalId, String pageKey) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "JournalPages", "CoverImagePath");
            boolean hasAtt = hasColumn(conn, "JournalPages", "AttachmentPath");
            String sql = "SELECT PageId, JournalId, PageKey, Title, Content, UpdatedAt"
                    + (hasCover ? ", CoverImagePath" : "")
                    + (hasAtt ? ", AttachmentPath" : "")
                    + " FROM dbo.JournalPages WHERE JournalId=? AND PageKey=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, journalId);
                ps.setString(2, pageKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapRow(rs, hasCover, hasAtt);
                }
            }
        }
        return null;
    }

    /**
     * 若没有期刊ID时，允许按 key 取“第一个期刊”的页面（用于系统初始化的兜底显示）。
     */
    public JournalPage findFirstJournalByKey(String pageKey) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "JournalPages", "CoverImagePath");
            boolean hasAtt = hasColumn(conn, "JournalPages", "AttachmentPath");

            String sql = "SELECT TOP 1 PageId, JournalId, PageKey, Title, Content, UpdatedAt"
                    + (hasCover ? ", CoverImagePath" : "")
                    + (hasAtt ? ", AttachmentPath" : "")
                    + " FROM dbo.JournalPages WHERE PageKey=? ORDER BY JournalId ASC, PageId ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pageKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapRow(rs, hasCover, hasAtt);
                }
            }
        }
        return null;
    }

    public List<JournalPage> listByJournal(int journalId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "JournalPages", "CoverImagePath");
            boolean hasAtt = hasColumn(conn, "JournalPages", "AttachmentPath");

            String sql = "SELECT PageId, JournalId, PageKey, Title, Content, UpdatedAt"
                    + (hasCover ? ", CoverImagePath" : "")
                    + (hasAtt ? ", AttachmentPath" : "")
                    + " FROM dbo.JournalPages WHERE JournalId=? ORDER BY PageKey ASC, PageId ASC";

            List<JournalPage> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, journalId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs, hasCover, hasAtt));
                    }
                }
            }
            return list;
        }
    }

    public JournalPage findById(int pageId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "JournalPages", "CoverImagePath");
            boolean hasAtt = hasColumn(conn, "JournalPages", "AttachmentPath");

            String sql = "SELECT PageId, JournalId, PageKey, Title, Content, UpdatedAt"
                    + (hasCover ? ", CoverImagePath" : "")
                    + (hasAtt ? ", AttachmentPath" : "")
                    + " FROM dbo.JournalPages WHERE PageId=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pageId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapRow(rs, hasCover, hasAtt);
                }
            }
        }
        return null;
    }

    public int insert(JournalPage p) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "JournalPages", "CoverImagePath");
            boolean hasAtt = hasColumn(conn, "JournalPages", "AttachmentPath");

            String sql = "INSERT INTO dbo.JournalPages(JournalId, PageKey, Title, Content, UpdatedAt"
                    + (hasCover ? ", CoverImagePath" : "")
                    + (hasAtt ? ", AttachmentPath" : "")
                    + ") VALUES(?,?,?,?,?"
                    + (hasCover ? ",?" : "")
                    + (hasAtt ? ",?" : "")
                    + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int idx = 1;
                ps.setInt(idx++, p.getJournalId());
                ps.setString(idx++, p.getPageKey());
                ps.setString(idx++, p.getTitle());
                ps.setString(idx++, p.getContent());
                Timestamp ts = Timestamp.valueOf(p.getUpdatedAt() != null ? p.getUpdatedAt() : LocalDateTime.now());
                ps.setTimestamp(idx++, ts);

                if (hasCover) ps.setString(idx++, p.getCoverImagePath());
                if (hasAtt) ps.setString(idx++, p.getAttachmentPath());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(JournalPage p) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "JournalPages", "CoverImagePath");
            boolean hasAtt = hasColumn(conn, "JournalPages", "AttachmentPath");

            String sql = "UPDATE dbo.JournalPages SET PageKey=?, Title=?, Content=?, UpdatedAt=?"
                    + (hasCover ? ", CoverImagePath=?" : "")
                    + (hasAtt ? ", AttachmentPath=?" : "")
                    + " WHERE PageId=?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                ps.setString(idx++, p.getPageKey());
                ps.setString(idx++, p.getTitle());
                ps.setString(idx++, p.getContent());
                Timestamp ts = Timestamp.valueOf(p.getUpdatedAt() != null ? p.getUpdatedAt() : LocalDateTime.now());
                ps.setTimestamp(idx++, ts);

                if (hasCover) ps.setString(idx++, p.getCoverImagePath());
                if (hasAtt) ps.setString(idx++, p.getAttachmentPath());

                ps.setInt(idx, p.getPageId());
                ps.executeUpdate();
            }
        }
    }

    public void delete(int pageId) throws SQLException {
        String sql = "DELETE FROM dbo.JournalPages WHERE PageId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageId);
            ps.executeUpdate();
        }
    }

    private JournalPage mapRow(ResultSet rs, boolean hasCover, boolean hasAtt) throws SQLException {
        JournalPage p = new JournalPage();
        p.setPageId(rs.getInt("PageId"));
        p.setJournalId(rs.getInt("JournalId"));
        p.setPageKey(rs.getString("PageKey"));
        p.setTitle(rs.getString("Title"));
        p.setContent(rs.getString("Content"));

        Timestamp ts = rs.getTimestamp("UpdatedAt");
        p.setUpdatedAt(ts == null ? null : ts.toLocalDateTime());

        if (hasCover) p.setCoverImagePath(rs.getString("CoverImagePath"));
        if (hasAtt) p.setAttachmentPath(rs.getString("AttachmentPath"));

        return p;
    }

    private boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT COUNT(*) AS Cnt FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME=? AND COLUMN_NAME=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Cnt") > 0;
            }
        }
        return false;
    }
}
