package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptFunding;
import edu.bjfu.onlinesm.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ManuscriptFundingDAO {

    
    public List<ManuscriptFunding> findByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT FundingId, ManuscriptId, FundingName, FundingLevel, FundingAmount " +
                "FROM dbo.ManuscriptFundings WHERE ManuscriptId = ? ORDER BY FundingId ASC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ManuscriptFunding> list = new ArrayList<>();
                while (rs.next()) {
                    ManuscriptFunding f = new ManuscriptFunding();
                    f.setFundingId(rs.getInt("FundingId"));
                    f.setManuscriptId(rs.getInt("ManuscriptId"));
                    f.setFundingName(rs.getString("FundingName"));
                    f.setFundingLevel(rs.getString("FundingLevel"));
                    BigDecimal amt = rs.getBigDecimal("FundingAmount");
                    f.setFundingAmount(amt);
                    list.add(f);
                }
                return list;
            }
        } catch (SQLException e) {
            
            if (looksLikeMissingTable(e)) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    public void deleteByManuscriptId(Connection conn, int manuscriptId) throws SQLException {
        String sql = "DELETE FROM dbo.ManuscriptFundings WHERE ManuscriptId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.executeUpdate();
        }
    }

    public void insertBatch(Connection conn, int manuscriptId, List<ManuscriptFunding> fundings) throws SQLException {
        if (fundings == null || fundings.isEmpty()) return;
        String sql = "INSERT INTO dbo.ManuscriptFundings(ManuscriptId, FundingName, FundingLevel, FundingAmount) " +
                "VALUES(?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ManuscriptFunding f : fundings) {
                ps.setInt(1, manuscriptId);
                ps.setString(2, f.getFundingName());
                ps.setString(3, f.getFundingLevel());
                if (f.getFundingAmount() == null) {
                    ps.setNull(4, Types.DECIMAL);
                } else {
                    ps.setBigDecimal(4, f.getFundingAmount());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    
    public void replaceByManuscriptId(Connection conn, int manuscriptId, List<ManuscriptFunding> fundings) throws SQLException {
        deleteByManuscriptId(conn, manuscriptId);
        insertBatch(conn, manuscriptId, fundings);
    }

    private boolean looksLikeMissingTable(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("invalid object name") && msg.contains("manuscriptfundings");
    }
}

/**
 *　　　　　　　　┏┓　　　┏┓+ +
 *　　　　　　　┏┛┻━━━┛┻┓ + +
 *　　　　　　　┃　　　　　　　┃
 *　　　　　　　┃　　　━　　　┃ ++ + + +
 *　　　　　　 ████━████ ┃+
 *　　　　　　　┃　　　　　　　┃ +
 *　　　　　　　┃　　　┻　　　┃
 *　　　　　　　┃　　　　　　　┃ + +
 *　　　　　　　┗━┓　　　┏━┛
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃ + + + +
 *　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 *　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 *　　　　　　　　　┃　　　┃
 *　　　　　　　　　┃　　　┃　　+
 *　　　　　　　　　┃　 　　┗━━━┓ + +
 *　　　　　　　　　┃ 　　　　　　　┣┓
 *　　　　　　　　　┃ 　　　　　　　┏┛
 *　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　　　　　　　┃┫┫　┃┫┫
 *　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 */

