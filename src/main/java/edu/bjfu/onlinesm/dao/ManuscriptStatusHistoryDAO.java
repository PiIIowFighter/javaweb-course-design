package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.ManuscriptStatusHistory;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ManuscriptStatusHistoryDAO {

    
    public List<ManuscriptStatusHistory> findByManuscriptId(int manuscriptId) throws SQLException {
        String sql = "SELECT h.HistoryId, h.ManuscriptId, h.FromStatus, h.ToStatus, h.Event, " +
                     "h.ChangedBy, h.ChangeTime, h.Remark, u.Username AS ChangedByUsername, u.FullName AS ChangedByFullName " +
                     "FROM dbo.ManuscriptStatusHistory h " +
                     "LEFT JOIN dbo.Users u ON h.ChangedBy = u.UserId " +
                     "WHERE h.ManuscriptId = ? " +
                     "ORDER BY h.ChangeTime ASC, h.HistoryId ASC";

        List<ManuscriptStatusHistory> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    
    public List<ManuscriptStatusHistory> findRecentByManuscriptId(int manuscriptId, int limit) throws SQLException {
        String sql = "SELECT TOP " + limit + " h.HistoryId, h.ManuscriptId, h.FromStatus, h.ToStatus, h.Event, " +
                     "h.ChangedBy, h.ChangeTime, h.Remark, u.Username AS ChangedByUsername, u.FullName AS ChangedByFullName " +
                     "FROM dbo.ManuscriptStatusHistory h " +
                     "LEFT JOIN dbo.Users u ON h.ChangedBy = u.UserId " +
                     "WHERE h.ManuscriptId = ? " +
                     "ORDER BY h.ChangeTime DESC, h.HistoryId DESC";

        List<ManuscriptStatusHistory> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    
    public void insert(int manuscriptId, String fromStatus, String toStatus, 
                       String event, int changedBy, String remark) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStatusHistory " +
                     "(ManuscriptId, FromStatus, ToStatus, Event, ChangedBy, Remark) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            if (fromStatus != null) {
                ps.setString(2, fromStatus);
            } else {
                ps.setNull(2, Types.NVARCHAR);
            }
            ps.setString(3, toStatus);
            ps.setString(4, event);
            ps.setInt(5, changedBy);
            if (remark != null) {
                ps.setString(6, remark);
            } else {
                ps.setNull(6, Types.NVARCHAR);
            }
            ps.executeUpdate();
        }
    }

    
    public void insert(Connection conn, int manuscriptId, String fromStatus, String toStatus,
                       String event, int changedBy, String remark) throws SQLException {
        String sql = "INSERT INTO dbo.ManuscriptStatusHistory " +
                     "(ManuscriptId, FromStatus, ToStatus, Event, ChangedBy, Remark) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            if (fromStatus != null) {
                ps.setString(2, fromStatus);
            } else {
                ps.setNull(2, Types.NVARCHAR);
            }
            ps.setString(3, toStatus);
            ps.setString(4, event);
            ps.setInt(5, changedBy);
            if (remark != null) {
                ps.setString(6, remark);
            } else {
                ps.setNull(6, Types.NVARCHAR);
            }
            ps.executeUpdate();
        }
    }

    
    public String getEstimatedReviewCycle(String currentStatus) {
        if (currentStatus == null) return "未知";
        switch (currentStatus) {
            case "SUBMITTED":
            case "FORMAL_CHECK":
            case "FORMAT_CHECK":
                return "1-2 周（形式审查阶段）";
            case "DESK_REVIEW_INITIAL":
                return "1-2 周（案头初筛阶段）";
            case "TO_ASSIGN":
            case "WITH_EDITOR":
                return "1-2 周（编辑处理阶段）";
            case "UNDER_REVIEW":
            case "REVIEWER_ASSIGNED":
                return "4-6 周（外审阶段）";
            case "EDITOR_RECOMMENDATION":
            case "FINAL_DECISION_PENDING":
                return "1-2 周（终审决策阶段）";
            case "REVISION":
                return "取决于修改内容";
            case "ACCEPTED":
            case "REJECTED":
            case "ARCHIVED":
                return "已完成";
            default:
                return "未知";
        }
    }

    private ManuscriptStatusHistory mapRow(ResultSet rs) throws SQLException {
        ManuscriptStatusHistory h = new ManuscriptStatusHistory();
        h.setHistoryId(rs.getLong("HistoryId"));
        h.setManuscriptId(rs.getInt("ManuscriptId"));
        h.setFromStatus(rs.getString("FromStatus"));
        h.setToStatus(rs.getString("ToStatus"));
        h.setEvent(rs.getString("Event"));
        
        int changedBy = rs.getInt("ChangedBy");
        if (!rs.wasNull()) {
            h.setChangedBy(changedBy);
        }
        
        Timestamp ts = rs.getTimestamp("ChangeTime");
        if (ts != null) {
            
            h.setChangeTime(ts.toLocalDateTime());
        }
        
        h.setRemark(rs.getString("Remark"));
        h.setChangedByUsername(rs.getString("ChangedByUsername"));
        h.setChangedByFullName(rs.getString("ChangedByFullName"));
        
        return h;
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

