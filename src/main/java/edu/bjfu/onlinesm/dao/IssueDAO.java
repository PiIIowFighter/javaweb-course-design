package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Issue;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Issues DAO（dbo.Issues / dbo.IssueManuscripts）。
 *
 * 兼容：如果旧库里 Issues 表缺列 GuestEditors / CoverImagePath / AttachmentPath，会自动退化为不读/不写该列，避免页面 500。
 */
public class IssueDAO {

    // -------------------- 前台读取 --------------------

    public List<Issue> listLatestPublished(int journalId, int limit) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);
            String base = selectColumns(f);
            if (limit > 0) base = base.replaceFirst("SELECT", "SELECT TOP " + limit);
            String sql = base +
                    "WHERE JournalId=? AND IssueType=N'LATEST' AND IsPublished=1 " +
                    "ORDER BY COALESCE(PublishDate, CAST('1900-01-01' AS DATE)) DESC, IssueId DESC";
            return runList(conn, sql, f, journalId);
        }
    }

    public List<Issue> listSpecialPublished(int journalId, int limit) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);
            String base = selectColumns(f);
            if (limit > 0) base = base.replaceFirst("SELECT", "SELECT TOP " + limit);
            String sql = base +
                    "WHERE JournalId=? AND IssueType=N'SPECIAL' AND IsPublished=1 " +
                    "ORDER BY COALESCE(PublishDate, CAST('1900-01-01' AS DATE)) DESC, IssueId DESC";
            return runList(conn, sql, f, journalId);
        }
    }
    /**
     * 前台统一入口：根据 type 返回已发布的 issues 列表。
     * type 支持：latest / special / all（默认 all）。
     */
    public List<Issue> listPublished(Integer journalId, String type, int limit) throws SQLException {
        if (journalId == null) return new ArrayList<>();
        String t = type == null ? "all" : type.trim().toLowerCase();
        switch (t) {
            case "latest":
                return listLatestPublished(journalId, limit);
            case "special":
                return listSpecialPublished(journalId, limit);
            case "all":
            default:
                return listPublishedByJournal(journalId, limit);
        }
    }

    

    public List<Issue> listPublishedByJournal(int journalId) throws SQLException {
        return listPublishedByJournal(journalId, 0);
    }

    public List<Issue> listPublishedByJournal(int journalId, int limit) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);
            String base = selectColumns(f);
            if (limit > 0) base = base.replaceFirst("SELECT", "SELECT TOP " + limit);
            String sql = base +
                    "WHERE JournalId=? AND IsPublished=1 " +
                    "ORDER BY COALESCE(PublishDate, CAST('1900-01-01' AS DATE)) DESC, IssueId DESC";
            return runList(conn, sql, f, journalId);
        }
    }
    

    public Issue findById(int issueId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);
            String sql = selectColumns(f) + "WHERE IssueId=?";
            List<Issue> list = runList(conn, sql, f, issueId);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    public List<Manuscript> listIssueArticles(int issueId) throws SQLException {
        // 关联表 IssueManuscripts + Manuscripts
        String sql = "SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, " +
                "m.ResearchTopic, m.FundingInfo, m.ManuscriptFilePath, m.CoverLetterPath, m.Status, m.SubmitTime, " +
                "m.Decision, m.FinalDecisionTime " +
                "FROM dbo.IssueManuscripts im " +
                "JOIN dbo.Manuscripts m ON m.ManuscriptId = im.ManuscriptId " +
                "WHERE im.IssueId = ? AND m.IsArchived=0 AND m.IsWithdrawn=0 " +
                "ORDER BY im.OrderNo ASC, m.ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Manuscript m = new Manuscript();
                    m.setManuscriptId(rs.getInt("ManuscriptId"));
                    m.setJournalId(rs.getInt("JournalId"));
                    m.setSubmitterId(rs.getInt("SubmitterId"));
                    m.setTitle(rs.getString("Title"));
                    m.setAbstractText(rs.getString("Abstract"));
                    m.setKeywords(rs.getString("Keywords"));
                    m.setSubjectArea(rs.getString("SubjectArea"));
                    m.setFundingInfo(rs.getString("FundingInfo"));                    m.setCurrentStatus(rs.getString("Status"));

                    Timestamp st = rs.getTimestamp("SubmitTime");
                    if (st != null) m.setSubmitTime(st.toLocalDateTime());

                    m.setDecision(rs.getString("Decision"));

                    Timestamp fdt = rs.getTimestamp("FinalDecisionTime");
                    if (fdt != null) m.setFinalDecisionTime(fdt.toLocalDateTime());

                    list.add(m);
                }
            }
        }
        return list;
    }

    // -------------------- 后台维护（admin） --------------------

    public List<Issue> listAllByJournal(int journalId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);
            String sql = selectColumns(f) +
                    "WHERE JournalId=? ORDER BY IssueType ASC, COALESCE(PublishDate, CAST('1900-01-01' AS DATE)) DESC, IssueId DESC";
            return runList(conn, sql, f, journalId);
        }
    }

    public int insertAdmin(Issue i) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO dbo.Issues(JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate");
            if (f.hasGuest) sb.append(", GuestEditors");
            if (f.hasCover) sb.append(", CoverImagePath");
            if (f.hasAtt) sb.append(", AttachmentPath");
            sb.append(") VALUES(?,?,?,?,?,?,?,?,?");
            if (f.hasGuest) sb.append(",?");
            if (f.hasCover) sb.append(",?");
            if (f.hasAtt) sb.append(",?");
            sb.append(")");

            try (PreparedStatement ps = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int idx = 1;
                ps.setInt(idx++, i.getJournalId());
                ps.setString(idx++, i.getIssueType());
                ps.setString(idx++, i.getTitle());
                if (i.getVolume() == null) ps.setNull(idx++, Types.INTEGER); else ps.setInt(idx++, i.getVolume());
                if (i.getNumber() == null) ps.setNull(idx++, Types.INTEGER); else ps.setInt(idx++, i.getNumber());
                if (i.getYear() == null) ps.setNull(idx++, Types.INTEGER); else ps.setInt(idx++, i.getYear());
                ps.setString(idx++, i.getDescription());
                ps.setBoolean(idx++, i.getPublished() != null && i.getPublished());
                if (i.getPublishDate() == null) ps.setNull(idx++, Types.DATE); else ps.setDate(idx++, Date.valueOf(i.getPublishDate()));

                if (f.hasGuest) ps.setString(idx++, i.getGuestEditors());
                if (f.hasCover) ps.setString(idx++, i.getCoverImagePath());
                if (f.hasAtt) ps.setString(idx++, i.getAttachmentPath());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public void updateAdmin(Issue i) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            ColFlags f = detectColumns(conn);

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE dbo.Issues SET IssueType=?, Title=?, Volume=?, Number=?, [Year]=?, Description=?, IsPublished=?, PublishDate=?");
            if (f.hasGuest) sb.append(", GuestEditors=?");
            if (f.hasCover) sb.append(", CoverImagePath=?");
            if (f.hasAtt) sb.append(", AttachmentPath=?");
            sb.append(" WHERE IssueId=?");

            try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                int idx = 1;
                ps.setString(idx++, i.getIssueType());
                ps.setString(idx++, i.getTitle());
                if (i.getVolume() == null) ps.setNull(idx++, Types.INTEGER); else ps.setInt(idx++, i.getVolume());
                if (i.getNumber() == null) ps.setNull(idx++, Types.INTEGER); else ps.setInt(idx++, i.getNumber());
                if (i.getYear() == null) ps.setNull(idx++, Types.INTEGER); else ps.setInt(idx++, i.getYear());
                ps.setString(idx++, i.getDescription());
                ps.setBoolean(idx++, i.getPublished() != null && i.getPublished());
                if (i.getPublishDate() == null) ps.setNull(idx++, Types.DATE); else ps.setDate(idx++, Date.valueOf(i.getPublishDate()));

                if (f.hasGuest) ps.setString(idx++, i.getGuestEditors());
                if (f.hasCover) ps.setString(idx++, i.getCoverImagePath());
                if (f.hasAtt) ps.setString(idx++, i.getAttachmentPath());

                ps.setInt(idx, i.getIssueId());
                ps.executeUpdate();
            }
        }
    }

    public void deleteAdmin(int issueId) throws SQLException {
        String sql = "DELETE FROM dbo.Issues WHERE IssueId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            ps.executeUpdate();
        }
    }

    // -------------------- internals --------------------

    private static final class ColFlags {
        boolean hasGuest;
        boolean hasCover;
        boolean hasAtt;
    }

    private ColFlags detectColumns(Connection conn) throws SQLException {
        ColFlags f = new ColFlags();
        f.hasGuest = hasColumn(conn, "Issues", "GuestEditors");
        f.hasCover = hasColumn(conn, "Issues", "CoverImagePath");
        f.hasAtt = hasColumn(conn, "Issues", "AttachmentPath");
        return f;
    }

    private String selectColumns(ColFlags f) {
        return "SELECT IssueId, JournalId, IssueType, Title, Volume, Number, [Year]"
                + (f.hasGuest ? ", GuestEditors" : "")
                + ", Description, IsPublished, PublishDate"
                + (f.hasCover ? ", CoverImagePath" : "")
                + (f.hasAtt ? ", AttachmentPath" : "")
                + " FROM dbo.Issues ";
    }

    private List<Issue> runList(Connection conn, String sql, ColFlags f, int param) throws SQLException {
        List<Issue> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, f));
                }
            }
        }
        return list;
    }

    private Issue map(ResultSet rs, ColFlags f) throws SQLException {
        Issue i = new Issue();
        i.setIssueId(rs.getInt("IssueId"));
        i.setJournalId(rs.getInt("JournalId"));
        i.setIssueType(rs.getString("IssueType"));
        i.setTitle(rs.getString("Title"));
        i.setVolume((Integer) rs.getObject("Volume"));
        i.setNumber((Integer) rs.getObject("Number"));
        i.setYear((Integer) rs.getObject("Year"));

        if (f.hasGuest) {
            i.setGuestEditors(rs.getString("GuestEditors"));
        }

        i.setDescription(rs.getString("Description"));
        i.setPublished(rs.getBoolean("IsPublished"));

        Date pd = rs.getDate("PublishDate");
        if (pd != null) {
            i.setPublishDate(pd.toLocalDate());
        }

        if (f.hasCover) i.setCoverImagePath(rs.getString("CoverImagePath"));
        if (f.hasAtt) i.setAttachmentPath(rs.getString("AttachmentPath"));

        return i;
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