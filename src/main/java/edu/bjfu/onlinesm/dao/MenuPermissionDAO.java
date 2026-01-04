package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 菜单入口权限（用户级）DAO。
 *
 * <p>兼容两种表结构：</p>
 * <pre>
 *  1) dbo.UserMenuPermissions(UserId, PermissionKey, Granted BIT ...)
 *  2) dbo.UserMenuPermissions(UserId, PermissionKey) —— 以“存在即授权”的方式存储
 * </pre>
 *
 * <p>你之前遇到的报错“列名 'Granted' 无效”，说明当前库属于第 2) 或者缺少 Granted 列。
 * 因此这里会在运行时检测列是否存在，然后自动选择 SQL。</p>
 */
public class MenuPermissionDAO {

    /** 缓存 Granted 列是否存在（首次检测后缓存，避免频繁查询元数据） */
    private volatile Boolean grantedColumnExistsCache = null;

    private boolean hasGrantedColumn(Connection conn) throws SQLException {
        if (grantedColumnExistsCache != null) return grantedColumnExistsCache;
        synchronized (this) {
            if (grantedColumnExistsCache != null) return grantedColumnExistsCache;

            // SQL Server：COL_LENGTH 返回 null 表示列不存在
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

    /**
     * 查询用户已授予的入口权限。
     * - 有 Granted 列：Granted=1
     * - 无 Granted 列：行存在即授权
     */
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

    /**
     * 兼容 PermissionAdminServlet 中的调用：首次初始化默认权限时批量“追加”。
     *
     * <p>注意：在表为空的场景下，追加/覆盖效果相同；这里按“追加且已存在跳过”实现。</p>
     */
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

    /**
     * 覆盖设置用户权限（先清空，再批量插入）。
     *
     * <p>PermissionAdminServlet 保存权限时会调用这个方法名。</p>
     */
    public void setPermissionsForUser(int userId, Set<String> permissionKeys) throws SQLException {
        replaceUserPermissions(userId, permissionKeys);
    }

    /**
     * 兼容旧实现：授予单个入口权限。
     */
    public void grantPermission(int userId, String permissionKey) throws SQLException {
        if (permissionKey == null || permissionKey.trim().isEmpty()) return;
        addPermissionsForUser(userId, Collections.singleton(permissionKey.trim()));
    }

    /**
     * 覆盖设置用户权限（先清空，再批量插入）。
     *
     * <p>保留这个方法名，避免你项目里其它地方调用。</p>
     */
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
