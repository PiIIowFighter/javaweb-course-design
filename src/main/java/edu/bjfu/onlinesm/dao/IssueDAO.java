package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Issue;
import edu.bjfu.onlinesm.model.Manuscript;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 前台 Issues 数据读取（dbo.Issues / dbo.IssueManuscripts）。
 *
 * 兼容：如果旧库里 Issues 表缺列 GuestEditors，会自动退化为不读该列，避免页面 500。
 */
public class IssueDAO {

    public List<Issue> listPublished(Integer journalId, String type, int limit) throws SQLException {
        if (journalId == null) return Collections.emptyList();

        String top = limit > 0 ? "TOP " + limit + " " : "";
        String typeFilter;
        if ("latest".equalsIgnoreCase(type)) {
            typeFilter = " AND IssueType='LATEST' ";
        } else if ("special".equalsIgnoreCase(type)) {
            typeFilter = " AND IssueType='SPECIAL' ";
        } else {
            typeFilter = ""; // all
        }

        String sqlWithGuest = "SELECT " + top + "IssueId, JournalId, IssueType, Title, Volume, Number, [Year], GuestEditors, Description, IsPublished, PublishDate " +
                "FROM dbo.Issues WHERE JournalId=? AND IsPublished=1 " + typeFilter +
                "ORDER BY PublishDate DESC, IssueId DESC";

        String sqlWithoutGuest = "SELECT " + top + "IssueId, JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate " +
                "FROM dbo.Issues WHERE JournalId=? AND IsPublished=1 " + typeFilter +
                "ORDER BY PublishDate DESC, IssueId DESC";

        try {
            return runList(sqlWithGuest, true, journalId);
        } catch (SQLException ex) {
            // Fix: "列名 GuestEditors 无效" (SQLServer)
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("guesteditor")) {
                return runList(sqlWithoutGuest, false, journalId);
            }
            throw ex;
        }
    }

    public Issue findById(int issueId) throws SQLException {
        String sqlWithGuest = "SELECT IssueId, JournalId, IssueType, Title, Volume, Number, [Year], GuestEditors, Description, IsPublished, PublishDate " +
                "FROM dbo.Issues WHERE IssueId=?";
        String sqlWithoutGuest = "SELECT IssueId, JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate " +
                "FROM dbo.Issues WHERE IssueId=?";

        try {
            return runOne(sqlWithGuest, true, issueId);
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("guesteditor")) {
                return runOne(sqlWithoutGuest, false, issueId);
            }
            throw ex;
        }
    }

    public List<Manuscript> listIssueArticles(int issueId) throws SQLException {
        // 关联表 IssueManuscripts + Manuscripts
        String sql = "SELECT m.ManuscriptId, m.JournalId, m.SubmitterId, m.Title, m.Abstract, m.Keywords, m.SubjectArea, m.FundingInfo, m.AuthorList, m.Status, m.SubmitTime, m.Decision, m.FinalDecisionTime " +
                "FROM dbo.IssueManuscripts im " +
                "JOIN dbo.Manuscripts m ON m.ManuscriptId = im.ManuscriptId " +
                "WHERE im.IssueId = ? " +
                "ORDER BY im.OrderNo ASC, m.ManuscriptId DESC";

        List<Manuscript> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                ManuscriptDAO mdao = new ManuscriptDAO();
                while (rs.next()) {
                    list.add(mdao.mapRowPublic(rs));
                }
            }
        }
        return list;
    }

    private List<Issue> runList(String sql, boolean hasGuest, int journalId) throws SQLException {
        List<Issue> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, hasGuest));
                }
            }
        }
        return list;
    }

    private Issue runOne(String sql, boolean hasGuest, int issueId) throws SQLException {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs, hasGuest);
                }
            }
        }
        return null;
    }

    private Issue map(ResultSet rs, boolean hasGuest) throws SQLException {
        Issue i = new Issue();
        i.setIssueId(rs.getInt("IssueId"));
        i.setJournalId(rs.getInt("JournalId"));
        i.setIssueType(rs.getString("IssueType"));
        i.setTitle(rs.getString("Title"));
        i.setVolume((Integer) rs.getObject("Volume"));
        i.setNumber((Integer) rs.getObject("Number"));
        i.setYear((Integer) rs.getObject("Year"));
        if (hasGuest) {
            i.setGuestEditors(rs.getString("GuestEditors"));
        }
        i.setDescription(rs.getString("Description"));
        i.setPublished(rs.getBoolean("IsPublished"));

        Date pd = rs.getDate("PublishDate");
        if (pd != null) {
            i.setPublishDate(pd.toLocalDate());
        }
        return i;
    }
}
