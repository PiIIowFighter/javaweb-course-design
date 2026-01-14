package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Notification;
import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.SchemaUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class NotificationDAO {

    private void ensureTable() throws SQLException {
        SchemaUtil.ensureNotificationsTable();
    }

    public int create(int recipientUserId,
                      Integer createdByUserId,
                      String type,
                      String category,
                      String title,
                      String content,
                      Integer relatedManuscriptId) throws SQLException {

        ensureTable();

        String sql = "INSERT INTO dbo.Notifications(RecipientUserId, CreatedByUserId, Type, Category, Title, Content, RelatedManuscriptId, IsRead) " +
                "VALUES(?,?,?,?,?,?,?,0)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, recipientUserId);
            if (createdByUserId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, createdByUserId);
            ps.setString(3, type == null ? "SYSTEM" : type);
            ps.setString(4, category);
            ps.setString(5, title == null ? "" : title);
            ps.setString(6, content);
            if (relatedManuscriptId == null) ps.setNull(7, Types.INTEGER);
            else ps.setInt(7, relatedManuscriptId);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Notification> listByRecipient(int recipientUserId, int limit) throws SQLException {
        ensureTable();
        if (limit <= 0) limit = 50;

        
        String sql = "SELECT TOP " + limit + " NotificationId, RecipientUserId, CreatedByUserId, Type, Category, Title, Content, RelatedManuscriptId, IsRead, ReadAt, CreatedAt " +
                "FROM dbo.Notifications WHERE RecipientUserId=? " +
                "ORDER BY IsRead ASC, CreatedAt DESC, NotificationId DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }


    
    public Notification findById(int notificationId) throws SQLException {
        ensureTable();

        String sql = "SELECT NotificationId, RecipientUserId, CreatedByUserId, Type, Category, Title, Content, RelatedManuscriptId, IsRead, ReadAt, CreatedAt " +
                "FROM dbo.Notifications WHERE NotificationId=?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    
    public Notification findById(Integer notificationId) throws SQLException {
        if (notificationId == null) return null;
        return findById(notificationId.intValue());
    }


    
    public List<Notification> listByCreator(int createdByUserId, int limit) throws SQLException {
        ensureTable();
        if (limit <= 0) limit = 50;

        String sql = "SELECT TOP " + limit + " NotificationId, RecipientUserId, CreatedByUserId, Type, Category, Title, Content, RelatedManuscriptId, IsRead, ReadAt, CreatedAt " +
                "FROM dbo.Notifications WHERE CreatedByUserId=? " +
                "ORDER BY CreatedAt DESC, NotificationId DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, createdByUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    
    public List<Notification> listByManuscriptAndCategory(int manuscriptId,
                                                         String category,
                                                         Integer recipientUserId,
                                                         int limit,
                                                         boolean ascendingByTime) throws SQLException {
        ensureTable();
        if (limit <= 0) limit = 100;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TOP ").append(limit)
           .append(" NotificationId, RecipientUserId, CreatedByUserId, Type, Category, Title, Content, RelatedManuscriptId, IsRead, ReadAt, CreatedAt ")
           .append("FROM dbo.Notifications WHERE RelatedManuscriptId=? AND Category=? ");

        if (recipientUserId != null) {
            sql.append("AND RecipientUserId=? ");
        }

        if (ascendingByTime) {
            sql.append("ORDER BY CreatedAt ASC, NotificationId ASC");
        } else {
            sql.append("ORDER BY CreatedAt DESC, NotificationId DESC");
        }

        List<Notification> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            ps.setInt(idx++, manuscriptId);
            ps.setString(idx++, category);
            if (recipientUserId != null) {
                ps.setInt(idx++, recipientUserId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    
    public int countByManuscriptAndCategory(int manuscriptId, String category) throws SQLException {
        ensureTable();
        String sql = "SELECT COUNT(1) AS Cnt FROM dbo.Notifications WHERE RelatedManuscriptId=? AND Category=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, manuscriptId);
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Cnt");
            }
        }
        return 0;
    }

    public int countUnread(int recipientUserId) throws SQLException {
        ensureTable();
        String sql = "SELECT COUNT(1) AS Cnt FROM dbo.Notifications WHERE RecipientUserId=? AND IsRead=0";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Cnt");
            }
        }
        return 0;
    }

    public void markRead(int recipientUserId, int notificationId) throws SQLException {
        ensureTable();
        String sql = "UPDATE dbo.Notifications SET IsRead=1, ReadAt=DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE NotificationId=? AND RecipientUserId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.setInt(2, recipientUserId);
            ps.executeUpdate();
        }
    }

    public void markAllRead(int recipientUserId) throws SQLException {
        ensureTable();
        String sql = "UPDATE dbo.Notifications SET IsRead=1, ReadAt=DATEADD(HOUR, 8, SYSUTCDATETIME()) WHERE RecipientUserId=? AND IsRead=0";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);
            ps.executeUpdate();
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("NotificationId"));
        n.setRecipientUserId(rs.getInt("RecipientUserId"));
        int cb = rs.getInt("CreatedByUserId");
        n.setCreatedByUserId(rs.wasNull() ? null : cb);
        n.setType(rs.getString("Type"));
        n.setCategory(rs.getString("Category"));
        n.setTitle(rs.getString("Title"));
        n.setContent(rs.getString("Content"));
        int mid = rs.getInt("RelatedManuscriptId");
        n.setRelatedManuscriptId(rs.wasNull() ? null : mid);
        n.setRead(rs.getBoolean("IsRead"));

        Timestamp ra = rs.getTimestamp("ReadAt");
        if (ra != null) n.setReadAt(ra.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("CreatedAt");
        if (ca != null) n.setCreatedAt(ca.toLocalDateTime());

        return n;
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

