package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 期刊 DAO：仅用于投稿页面加载期刊下拉列表。
 */
public class JournalDAO {

    public List<Journal> findAll() throws SQLException {
        String sql = "SELECT JournalId, Name FROM dbo.Journals ORDER BY JournalId ASC";
        List<Journal> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Journal j = new Journal();
                j.setJournalId(rs.getInt("JournalId"));
                j.setName(rs.getString("Name"));
                list.add(j);
            }
        }
        return list;
    }

    /**
     * 获取“主期刊”（课程设计场景通常只有 1 个期刊）。
     * 若存在多条，默认取 JournalId 最小的一条。
     */
    public Journal findPrimary() throws SQLException {
        String sql = "SELECT TOP 1 JournalId, Name, Description, ImpactFactor, Timeline, ISSN " +
                "FROM dbo.Journals ORDER BY JournalId ASC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return mapFullRow(rs);
            }
        }
        return null;
    }

    public Journal findById(int id) throws SQLException {
        String sql = "SELECT JournalId, Name, Description, ImpactFactor, Timeline, ISSN " +
                "FROM dbo.Journals WHERE JournalId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFullRow(rs);
                }
            }
        }
        return null;
    }

    private Journal mapFullRow(ResultSet rs) throws SQLException {
        Journal j = new Journal();
        j.setJournalId(rs.getInt("JournalId"));
        j.setName(rs.getString("Name"));
        j.setDescription(rs.getString("Description"));

        double ifVal = rs.getDouble("ImpactFactor");
        if (rs.wasNull()) {
            j.setImpactFactor(null);
        } else {
            j.setImpactFactor(ifVal);
        }

        j.setTimeline(rs.getString("Timeline"));
        j.setIssn(rs.getString("ISSN"));
        return j;
    }
}
