package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptAssignment;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * 负责访问 dbo.ManuscriptAssignments 的简单 DAO。
 *
 * 当前阶段只需要两个功能：
 *  1）记录主编指派编辑时的建议（createAssignment）
 *  2）编辑查看最新一条主编建议（findLatestByManuscriptAndEditor）
 */
public class ManuscriptAssignmentDAO {

    /**
     * 新增一条“主编指派编辑”的记录。
     */
    public void createAssignment(int manuscriptId,
                                 int editorUserId,
                                 int chiefUserId,
                                 String chiefComment) throws SQLException {

        String sql = "INSERT INTO dbo.ManuscriptAssignments " +
                "(ManuscriptId, EditorId, AssignedByChiefId, ChiefComment) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, manuscriptId);
            ps.setInt(2, editorUserId);
            ps.setInt(3, chiefUserId);

            if (chiefComment != null && !chiefComment.trim().isEmpty()) {
                ps.setString(4, chiefComment.trim());
            } else {
                ps.setNull(4, Types.NVARCHAR);
            }

            ps.executeUpdate();
        }
    }

    /**
     * 查询某稿件 + 某编辑的最近一条主编指派记录（供编辑端查看建议）。
     */
    public ManuscriptAssignment findLatestByManuscriptAndEditor(int manuscriptId,
                                                                int editorUserId)
            throws SQLException {

        String sql = "SELECT TOP 1 AssignmentId, ManuscriptId, EditorId, " +
                "AssignedByChiefId, ChiefComment, AssignedTime " +
                "FROM dbo.ManuscriptAssignments " +
                "WHERE ManuscriptId = ? AND EditorId = ? " +
                "ORDER BY AssignedTime DESC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, manuscriptId);
            ps.setInt(2, editorUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ManuscriptAssignment ma = new ManuscriptAssignment();
                    ma.setAssignmentId(rs.getInt("AssignmentId"));
                    ma.setManuscriptId(rs.getInt("ManuscriptId"));
                    ma.setEditorId(rs.getInt("EditorId"));
                    ma.setAssignedByChiefId(rs.getInt("AssignedByChiefId"));
                    ma.setChiefComment(rs.getString("ChiefComment"));

                    Timestamp ts = rs.getTimestamp("AssignedTime");
                    if (ts != null) {
                        ma.setAssignedTime(ts.toLocalDateTime());
                    }

                    return ma;
                }
            }
        }

        return null;
    }
}
