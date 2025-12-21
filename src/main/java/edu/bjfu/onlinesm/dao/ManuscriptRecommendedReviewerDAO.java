package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptRecommendedReviewer;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 推荐审稿人 DAO：对应 dbo.ManuscriptRecommendedReviewers。
 */
public class ManuscriptRecommendedReviewerDAO {

    public void deleteByManuscriptId(Connection conn, int manuscriptId) throws SQLException {
        String sql = "DELETE FROM dbo.ManuscriptRecommendedReviewers WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    public void insertBatch(Connection conn, int manuscriptId, List<ManuscriptRecommendedReviewer> list) throws SQLException {
        if (list == null || list.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO dbo.ManuscriptRecommendedReviewers (ManuscriptId, FullName, Email, Reason) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ManuscriptRecommendedReviewer r : list) {
                ps.setInt(1, manuscriptId);
                ps.setString(2, r.getFullName());
                ps.setString(3, r.getEmail());
                ps.setString(4, r.getReason());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<ManuscriptRecommendedReviewer> findByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT Id, ManuscriptId, FullName, Email, Reason FROM dbo.ManuscriptRecommendedReviewers WHERE ManuscriptId = ? ORDER BY Id ASC";
        List<ManuscriptRecommendedReviewer> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ManuscriptRecommendedReviewer r = new ManuscriptRecommendedReviewer();
                    r.setId(rs.getInt("Id"));
                    r.setManuscriptId(rs.getInt("ManuscriptId"));
                    r.setFullName(rs.getString("FullName"));
                    r.setEmail(rs.getString("Email"));
                    r.setReason(rs.getString("Reason"));
                    list.add(r);
                }
            }
        }
        return list;
    }
}
