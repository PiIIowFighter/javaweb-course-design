package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.CallForPaper;
import edu.bjfu.onlinesm.util.DbUtil;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 征稿通知（dbo.CallForPapers）DAO（前台 + 管理端）。
 *
 * 说明：
 * - 前台：只读取 IsPublished=1 的记录
 * - 管理端：可 CRUD，兼容旧库可能缺少 CoverImagePath/AttachmentPath 字段（自动降级）
 */
public class CallForPaperDAO {

    // =========================
    // Front-end (public)
    // =========================

    /** 兼容：PublicCallForPaperServlet 调用 listPublished(journalId, 50) */
    public List<CallForPaper> listPublished(Integer journalId, int limit) throws SQLException {
        if (journalId == null) return Collections.emptyList();
        String top = (limit > 0) ? ("TOP " + limit + " ") : "";

        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "CallForPapers", "CoverImagePath");
            boolean hasAttach = hasColumn(conn, "CallForPapers", "AttachmentPath");

            String sql = "SELECT " + top +
                    "CallId, JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished, CreatedAt" +
                    (hasCover ? ", CoverImagePath" : "") +
                    (hasAttach ? ", AttachmentPath" : "") +
                    " FROM dbo.CallForPapers " +
                    "WHERE JournalId=? AND IsPublished=1 " +
                    "ORDER BY ISNULL(Deadline, EndDate) DESC, CallId DESC";

            List<CallForPaper> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, journalId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs, hasCover, hasAttach));
                    }
                }
            }
            return list;
        }
    }

    /** 兼容重载：避免 Integer/int 重载解析不到 */
    public List<CallForPaper> listPublished(Integer journalId, Integer limit) throws SQLException {
        return listPublished(journalId, limit == null ? 0 : limit);
    }

    /** 兼容重载：避免 journalId 被解析为 int 的情况 */
    public List<CallForPaper> listPublished(int journalId, int limit) throws SQLException {
        return listPublished(Integer.valueOf(journalId), limit);
    }

    public CallForPaper findById(int callId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "CallForPapers", "CoverImagePath");
            boolean hasAttach = hasColumn(conn, "CallForPapers", "AttachmentPath");

            String sql = "SELECT CallId, JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished, CreatedAt" +
                    (hasCover ? ", CoverImagePath" : "") +
                    (hasAttach ? ", AttachmentPath" : "") +
                    " FROM dbo.CallForPapers WHERE CallId=?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, callId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return map(rs, hasCover, hasAttach);
                }
            }
            return null;
        }
    }

    // =========================
    // Admin (journal board)
    // =========================

    /** 管理端：列出某期刊下全部征稿通知（含未发布） */
    public List<CallForPaper> listAllByJournal(Integer journalId) throws SQLException {
        if (journalId == null) return Collections.emptyList();

        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "CallForPapers", "CoverImagePath");
            boolean hasAttach = hasColumn(conn, "CallForPapers", "AttachmentPath");

            String sql = "SELECT CallId, JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished, CreatedAt" +
                    (hasCover ? ", CoverImagePath" : "") +
                    (hasAttach ? ", AttachmentPath" : "") +
                    " FROM dbo.CallForPapers WHERE JournalId=? ORDER BY CallId DESC";

            List<CallForPaper> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, journalId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs, hasCover, hasAttach));
                    }
                }
            }
            return list;
        }
    }

    /** 管理端：新增征稿通知，返回新 CallId */
    public int insertAdmin(CallForPaper call) throws SQLException {
        if (call == null) throw new SQLException("call is null");

        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "CallForPapers", "CoverImagePath");
            boolean hasAttach = hasColumn(conn, "CallForPapers", "AttachmentPath");

            StringBuilder cols = new StringBuilder("JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished");
            StringBuilder vals = new StringBuilder("?, ?, ?, ?, ?, ?, ?");

            if (hasCover) { cols.append(", CoverImagePath"); vals.append(", ?"); }
            if (hasAttach) { cols.append(", AttachmentPath"); vals.append(", ?"); }

            String sql = "INSERT INTO dbo.CallForPapers (" + cols + ") VALUES (" + vals + "); SELECT SCOPE_IDENTITY() AS NewId;";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                ps.setInt(i++, call.getJournalId());
                ps.setString(i++, call.getTitle());
                ps.setString(i++, call.getContent());
                setDateOrNull(ps, i++, call.getStartDate());
                setDateOrNull(ps, i++, call.getDeadline());
                setDateOrNull(ps, i++, call.getEndDate());
                ps.setBoolean(i++, Boolean.TRUE.equals(call.getPublished()));

                if (hasCover) ps.setString(i++, getOptionalString(call, "getCoverImagePath"));
                if (hasAttach) ps.setString(i++, getOptionalString(call, "getAttachmentPath"));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("NewId");
                    }
                }
            }
        }
        throw new SQLException("insert call failed: cannot fetch identity");
    }

    /** 管理端：更新征稿通知 */
    public void updateAdmin(CallForPaper call) throws SQLException {
        if (call == null || call.getCallId() == null) throw new SQLException("callId is null");

        try (Connection conn = DbUtil.getConnection()) {
            boolean hasCover = hasColumn(conn, "CallForPapers", "CoverImagePath");
            boolean hasAttach = hasColumn(conn, "CallForPapers", "AttachmentPath");

            StringBuilder set = new StringBuilder("Title=?, Content=?, StartDate=?, Deadline=?, EndDate=?, IsPublished=?");
            if (hasCover) set.append(", CoverImagePath=?");
            if (hasAttach) set.append(", AttachmentPath=?");

            String sql = "UPDATE dbo.CallForPapers SET " + set + " WHERE CallId=?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, call.getTitle());
                ps.setString(i++, call.getContent());
                setDateOrNull(ps, i++, call.getStartDate());
                setDateOrNull(ps, i++, call.getDeadline());
                setDateOrNull(ps, i++, call.getEndDate());
                ps.setBoolean(i++, Boolean.TRUE.equals(call.getPublished()));

                if (hasCover) ps.setString(i++, getOptionalString(call, "getCoverImagePath"));
                if (hasAttach) ps.setString(i++, getOptionalString(call, "getAttachmentPath"));

                ps.setInt(i, call.getCallId());
                ps.executeUpdate();
            }
        }
    }

    /** 管理端：删除征稿通知 */
    public void deleteAdmin(int callId) throws SQLException {
        String sql = "DELETE FROM dbo.CallForPapers WHERE CallId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, callId);
            ps.executeUpdate();
        }
    }

    // =========================
    // Helpers
    // =========================

    private CallForPaper map(ResultSet rs, boolean hasCover, boolean hasAttach) throws SQLException {
        CallForPaper c = new CallForPaper();
        c.setCallId(rs.getInt("CallId"));
        c.setJournalId(rs.getInt("JournalId"));
        c.setTitle(rs.getString("Title"));
        c.setContent(rs.getString("Content"));
        c.setPublished(rs.getBoolean("IsPublished"));

        Date sd = rs.getDate("StartDate");
        if (sd != null) c.setStartDate(sd.toLocalDate());
        Date dl = rs.getDate("Deadline");
        if (dl != null) c.setDeadline(dl.toLocalDate());
        Date ed = rs.getDate("EndDate");
        if (ed != null) c.setEndDate(ed.toLocalDate());

        Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime());

        if (hasCover) setOptionalString(c, "setCoverImagePath", rs.getString("CoverImagePath"));
        if (hasAttach) setOptionalString(c, "setAttachmentPath", rs.getString("AttachmentPath"));

        return c;
    }

    private boolean hasColumn(Connection conn, String table, String col) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            // SQL Server：schema 通常为 dbo
            try (ResultSet rs = meta.getColumns(null, "dbo", table, col)) {
                if (rs.next()) return true;
            }
            // 兼容：有些 JDBC 可能不给 schema
            try (ResultSet rs = meta.getColumns(null, null, table, col)) {
                return rs.next();
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    private void setDateOrNull(PreparedStatement ps, int idx, java.time.LocalDate d) throws SQLException {
        if (d == null) ps.setNull(idx, Types.DATE);
        else ps.setDate(idx, Date.valueOf(d));
    }

    private String getOptionalString(Object obj, String getterName) {
        try {
            Method m = obj.getClass().getMethod(getterName);
            Object v = m.invoke(obj);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setOptionalString(Object obj, String setterName, String value) {
        try {
            Method m = obj.getClass().getMethod(setterName, String.class);
            m.invoke(obj, value);
        } catch (Exception ignored) {
        }
    }
}
