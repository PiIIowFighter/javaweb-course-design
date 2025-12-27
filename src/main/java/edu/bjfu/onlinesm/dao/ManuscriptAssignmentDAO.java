package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptAssignment;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * 负责访问 dbo.ManuscriptAssignments 的简单 DAO。
 *
 * 功能：
 *  1）记录主编指派编辑时的建议（createAssignment）；
 *  2）编辑/主编查看最新一条指派记录（findLatestByManuscriptAndEditor）。
 */
public class ManuscriptAssignmentDAO {

    /**
     * 新建一条指派记录。
     *
     * @param manuscriptId  稿件 ID
     * @param editorId      被指派的编辑用户 ID
     * @param chiefUserId   指派该编辑的主编用户 ID
     * @param chiefComment  主编给编辑的备注/指示（可为空）
     */
    public void createAssignment(int manuscriptId,
                                 int editorId,
                                 int chiefUserId,
                                 String chiefComment) throws SQLException {

        String sql = "INSERT INTO dbo.ManuscriptAssignments " +
                     "    (ManuscriptId, EditorId, AssignedByChiefId, ChiefComment, AssignedTime) " +
                     "VALUES (?, ?, ?, ?, SYSUTCDATETIME())";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, manuscriptId);
            ps.setInt(2, editorId);
            ps.setInt(3, chiefUserId);
            if (chiefComment == null || chiefComment.trim().isEmpty()) {
                ps.setNull(4, Types.NVARCHAR);
            } else {
                ps.setString(4, chiefComment.trim());
            }

            ps.executeUpdate();
        }
    }

    /**
     * 查找某稿件针对某一位编辑的“最新一条指派记录”。
     * 主要用在稿件详情页中展示“主编给该编辑的指示”。
     *
     * @param manuscriptId 稿件 ID
     * @param editorId     编辑用户 ID
     * @return 最新一条 ManuscriptAssignment；如不存在，返回 null。
     */
    public ManuscriptAssignment findLatestByManuscriptAndEditor(int manuscriptId,
                                                                int editorId) throws SQLException {

        String sql = "SELECT TOP 1 AssignmentId, ManuscriptId, EditorId, AssignedByChiefId, " +
                     "       ChiefComment, AssignedTime " +
                     "FROM dbo.ManuscriptAssignments " +
                     "WHERE ManuscriptId = ? AND EditorId = ? " +
                     "ORDER BY AssignedTime DESC, AssignmentId DESC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, manuscriptId);
            ps.setInt(2, editorId);

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
