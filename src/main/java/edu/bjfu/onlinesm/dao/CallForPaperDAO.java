package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.CallForPaper;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 征稿通知（dbo.CallForPapers）前台读取 DAO。
 */
public class CallForPaperDAO {

    public List<CallForPaper> listPublished(Integer journalId, int limit) throws SQLException {
        if (journalId == null) return Collections.emptyList();

        String top = limit > 0 ? "TOP " + limit + " " : "";
        String sql = "SELECT " + top + "CallId, JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished, CreatedAt " +
                "FROM dbo.CallForPapers " +
                "WHERE JournalId=? AND IsPublished=1 " +
                "ORDER BY ISNULL(Deadline, EndDate) DESC, CallId DESC";

        List<CallForPaper> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public CallForPaper findById(int callId) throws SQLException {
        String sql = "SELECT CallId, JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished, CreatedAt " +
                "FROM dbo.CallForPapers WHERE CallId=?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, callId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    private CallForPaper map(ResultSet rs) throws SQLException {
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

        return c;
    }
}
