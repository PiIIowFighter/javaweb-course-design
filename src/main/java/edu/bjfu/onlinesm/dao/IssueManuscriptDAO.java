package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class IssueManuscriptDAO {

    
    public Integer findIssueIdByManuscriptId(Connection conn, int manuscriptId) throws SQLException {
        String sql = "SELECT TOP 1 IssueId FROM dbo.IssueManuscripts " +
                "WHERE ManuscriptId=? ORDER BY OrderNo ASC, AddedAt DESC, IssueId ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null;
    }

    
    public Integer findIssueIdByManuscriptId(int manuscriptId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            return findIssueIdByManuscriptId(conn, manuscriptId);
        }
    }

    
    public void setIssueForManuscript(Connection conn, int manuscriptId, Integer issueId) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM dbo.IssueManuscripts WHERE ManuscriptId=?")) {
            del.setInt(1, manuscriptId);
            del.executeUpdate();
        }
        if (issueId == null) return;

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO dbo.IssueManuscripts(IssueId, ManuscriptId, OrderNo) VALUES (?,?,0)")) {
            ins.setInt(1, issueId);
            ins.setInt(2, manuscriptId);
            ins.executeUpdate();
        }
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

