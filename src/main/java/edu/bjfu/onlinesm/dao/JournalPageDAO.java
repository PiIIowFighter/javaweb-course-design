package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.JournalPage;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * JournalPageDAO：读取“关于期刊”等可配置页面内容。
 * 表：dbo.JournalPages(PageId, JournalId, PageKey, Title, Content, UpdatedAt)
 */
public class JournalPageDAO {

    /**
     * 按期刊 + 页面 key 获取页面内容（例如 aims / policies）。
     * 若不存在返回 null。
     */
    public JournalPage findByJournalAndKey(int journalId, String pageKey) throws SQLException {
        String sql = "SELECT PageId, JournalId, PageKey, Title, Content, UpdatedAt " +
                     "FROM dbo.JournalPages WHERE JournalId = ? AND PageKey = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, journalId);
            ps.setString(2, pageKey);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * 若没有期刊ID时，允许按 key 取“第一个期刊”的页面（用于系统刚初始化的兜底显示）。
     */
    public JournalPage findFirstJournalByKey(String pageKey) throws SQLException {
        String sql = "SELECT TOP 1 PageId, JournalId, PageKey, Title, Content, UpdatedAt " +
                     "FROM dbo.JournalPages WHERE PageKey = ? ORDER BY JournalId ASC, PageId ASC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pageKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private JournalPage mapRow(ResultSet rs) throws SQLException {
        JournalPage p = new JournalPage();
        p.setPageId(rs.getInt("PageId"));
        p.setJournalId(rs.getInt("JournalId"));
        p.setPageKey(rs.getString("PageKey"));
        p.setTitle(rs.getString("Title"));
        p.setContent(rs.getString("Content"));

        Timestamp ts = rs.getTimestamp("UpdatedAt");
        LocalDateTime ldt = (ts == null) ? null : ts.toLocalDateTime();
        p.setUpdatedAt(ldt);

        return p;
    }
}
