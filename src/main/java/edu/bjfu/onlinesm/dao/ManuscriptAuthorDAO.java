package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptAuthor;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 稿件作者 DAO：对应 dbo.ManuscriptAuthors。
 */
public class ManuscriptAuthorDAO {

    public void deleteByManuscriptId(Connection conn, int manuscriptId) throws SQLException {
        String sql = "DELETE FROM dbo.ManuscriptAuthors WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    public void insertBatch(Connection conn, int manuscriptId, List<ManuscriptAuthor> authors) throws SQLException {
        if (authors == null || authors.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO dbo.ManuscriptAuthors " +
                "(ManuscriptId, AuthorOrder, FullName, Affiliation, Degree, Title, Position, Email, IsCorresponding) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ManuscriptAuthor a : authors) {
                ps.setInt(1, manuscriptId);
                ps.setInt(2, a.getAuthorOrder() == null ? 1 : a.getAuthorOrder());
                ps.setString(3, a.getFullName());
                ps.setString(4, a.getAffiliation());
                ps.setString(5, a.getDegree());
                ps.setString(6, a.getTitle());
                ps.setString(7, a.getPosition());
                ps.setString(8, a.getEmail());
                ps.setBoolean(9, a.isCorresponding());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<ManuscriptAuthor> findByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT AuthorId, ManuscriptId, AuthorOrder, FullName, Affiliation, Degree, Title, Position, Email, IsCorresponding " +
                "FROM dbo.ManuscriptAuthors WHERE ManuscriptId = ? ORDER BY AuthorOrder ASC, AuthorId ASC";

        List<ManuscriptAuthor> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ManuscriptAuthor a = new ManuscriptAuthor();
                    a.setAuthorId(rs.getInt("AuthorId"));
                    a.setManuscriptId(rs.getInt("ManuscriptId"));
                    a.setAuthorOrder(rs.getInt("AuthorOrder"));
                    a.setFullName(rs.getString("FullName"));
                    a.setAffiliation(rs.getString("Affiliation"));
                    a.setDegree(rs.getString("Degree"));
                    a.setTitle(rs.getString("Title"));
                    a.setPosition(rs.getString("Position"));
                    a.setEmail(rs.getString("Email"));
                    a.setCorresponding(rs.getBoolean("IsCorresponding"));
                    list.add(a);
                }
            }
        }
        return list;
    }
}
