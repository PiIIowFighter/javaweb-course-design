package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class MenuPermissionDAO {

    
    private volatile Boolean grantedColumnExistsCache = null;

    private boolean hasGrantedColumn(Connection conn) throws SQLException {
        if (grantedColumnExistsCache != null) return grantedColumnExistsCache;
        synchronized (this) {
            if (grantedColumnExistsCache != null) return grantedColumnExistsCache;

            
            String metaSql = "SELECT COL_LENGTH('dbo.UserMenuPermissions', 'Granted') AS L";
            try (PreparedStatement ps = conn.prepareStatement(metaSql);
                 ResultSet rs = ps.executeQuery()) {
                boolean exists = false;
                if (rs.next()) {
                    exists = rs.getObject(1) != null;
                }
                grantedColumnExistsCache = exists;
                return exists;
            }
        }
    }

    
    public Set<String> findPermissionsByUser(int userId) throws SQLException {
        Set<String> keys = new HashSet<>();
        try (Connection conn = DbUtil.getConnection()) {
            final boolean hasGranted = hasGrantedColumn(conn);
            final String sql = hasGranted
                    ? "SELECT PermissionKey FROM dbo.UserMenuPermissions WHERE UserId = ? AND Granted = 1"
                    : "SELECT PermissionKey FROM dbo.UserMenuPermissions WHERE UserId = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String k = rs.getString(1);
                        if (k != null && !k.trim().isEmpty()) keys.add(k.trim());
                    }
                }
            }
        }
        return keys;
    }

    
    public void addPermissionsForUser(int userId, Set<String> permissionKeys) throws SQLException {
        if (permissionKeys == null || permissionKeys.isEmpty()) return;

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            boolean hasGranted = hasGrantedColumn(conn);

            final String sql = hasGranted
                    ? "IF NOT EXISTS (SELECT 1 FROM dbo.UserMenuPermissions WHERE UserId=? AND PermissionKey=?) " +
                      "INSERT INTO dbo.UserMenuPermissions(UserId, PermissionKey, Granted) VALUES(?, ?, 1)"
                    : "IF NOT EXISTS (SELECT 1 FROM dbo.UserMenuPermissions WHERE UserId=? AND PermissionKey=?) " +
                      "INSERT INTO dbo.UserMenuPermissions(UserId, PermissionKey) VALUES(?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String k : permissionKeys) {
                    if (k == null) continue;
                    String key = k.trim();
                    if (key.isEmpty()) continue;
                    ps.setInt(1, userId);
                    ps.setString(2, key);
                    ps.setInt(3, userId);
                    ps.setString(4, key);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public void setPermissionsForUser(int userId, Set<String> permissionKeys) throws SQLException {
        replaceUserPermissions(userId, permissionKeys);
    }

    
    public void grantPermission(int userId, String permissionKey) throws SQLException {
        if (permissionKey == null || permissionKey.trim().isEmpty()) return;
        addPermissionsForUser(userId, Collections.singleton(permissionKey.trim()));
    }

    
    public void replaceUserPermissions(int userId, Set<String> permissionKeys) throws SQLException {
        if (permissionKeys == null) permissionKeys = new HashSet<>();

        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM dbo.UserMenuPermissions WHERE UserId = ?")) {
                    del.setInt(1, userId);
                    del.executeUpdate();
                }

                boolean hasGranted = hasGrantedColumn(conn);
                final String insertSql = hasGranted
                        ? "INSERT INTO dbo.UserMenuPermissions(UserId, PermissionKey, Granted) VALUES(?, ?, 1)"
                        : "INSERT INTO dbo.UserMenuPermissions(UserId, PermissionKey) VALUES(?, ?)";

                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (String k : permissionKeys) {
                        if (k == null) continue;
                        String key = k.trim();
                        if (key.isEmpty()) continue;
                        ins.setInt(1, userId);
                        ins.setString(2, key);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
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

