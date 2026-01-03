package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * dbo.IssueManuscripts 关联表 DAO：
 * - 一个稿件选择/关联一个 Issue（卷期/专刊）。
 * - 页面展示/Issue 详情页可通过此表关联稿件列表。
 */
public class IssueManuscriptDAO {

    /**
     * 查询稿件已关联的 IssueId（取第一个关联）。
     */
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

    /**
     * 便捷：独立连接查询稿件已关联 IssueId。
     */
    public Integer findIssueIdByManuscriptId(int manuscriptId) throws SQLException {
        try (Connection conn = DbUtil.getConnection()) {
            return findIssueIdByManuscriptId(conn, manuscriptId);
        }
    }

    /**
     * 设定稿件关联的 Issue（采用“删除旧关联 -> 插入新关联”的方式）。
     * 如果 issueId 为 null，则只删除旧关联。
     */
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
