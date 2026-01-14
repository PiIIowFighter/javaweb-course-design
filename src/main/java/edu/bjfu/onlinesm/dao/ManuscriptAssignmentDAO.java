package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptAssignment;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.time.LocalDateTime;


public class ManuscriptAssignmentDAO {

    
    public void createAssignment(int manuscriptId,
                                 int editorId,
                                 int chiefUserId,
                                 String chiefComment) throws SQLException {

        String sql = "INSERT INTO dbo.ManuscriptAssignments " +
                     "    (ManuscriptId, EditorId, AssignedByChiefId, ChiefComment, AssignedTime) " +
                     "VALUES (?, ?, ?, ?, DATEADD(HOUR, 8, SYSUTCDATETIME()))";

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

    
    public ManuscriptAssignment findLatestByManuscript(int manuscriptId) throws SQLException {

        String sql = "SELECT TOP 1 AssignmentId, ManuscriptId, EditorId, AssignedByChiefId, " +
                "       ChiefComment, AssignedTime " +
                "FROM dbo.ManuscriptAssignments " +
                "WHERE ManuscriptId = ? " +
                "ORDER BY AssignedTime DESC, AssignmentId DESC";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, manuscriptId);

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

