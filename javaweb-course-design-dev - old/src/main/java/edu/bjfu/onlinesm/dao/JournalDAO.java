package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 期刊 DAO（dbo.Journals）
 */
public class JournalDAO {

    
    /**
     * 兼容旧代码：findAll() 等同于 listAll()
     */
    public List<Journal> findAll() throws SQLException {
        return listAll();
    }

public List<Journal> listAll() throws SQLException {
        String sql = "SELECT JournalId, Name, Description, ImpactFactor, Timeline, ISSN " +
                "FROM dbo.Journals ORDER BY JournalId ASC";
        List<Journal> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapFullRow(rs));
            }
        }
        return list;
    }

    /**
     * 取一个“主期刊”（用于前台兜底显示）。
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

    public int insert(Journal j, Integer createdBy) throws SQLException {
        String sql = "INSERT INTO dbo.Journals(Name, Description, ImpactFactor, Timeline, ISSN, CreatedBy) " +
                "VALUES(?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, j.getName());
            ps.setString(2, j.getDescription());
            if (j.getImpactFactor() == null) {
                ps.setNull(3, Types.DECIMAL);
            } else {
                ps.setDouble(3, j.getImpactFactor());
            }
            ps.setString(4, j.getTimeline());
            ps.setString(5, j.getIssn());
            if (createdBy == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, createdBy);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void update(Journal j) throws SQLException {
        String sql = "UPDATE dbo.Journals SET Name=?, Description=?, ImpactFactor=?, Timeline=?, ISSN=? WHERE JournalId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, j.getName());
            ps.setString(2, j.getDescription());
            if (j.getImpactFactor() == null) {
                ps.setNull(3, Types.DECIMAL);
            } else {
                ps.setDouble(3, j.getImpactFactor());
            }
            ps.setString(4, j.getTimeline());
            ps.setString(5, j.getIssn());
            ps.setInt(6, j.getJournalId());

            ps.executeUpdate();
        }
    }

    public void delete(int journalId) throws SQLException {
        String sql = "DELETE FROM dbo.Journals WHERE JournalId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, journalId);
            ps.executeUpdate();
        }
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
