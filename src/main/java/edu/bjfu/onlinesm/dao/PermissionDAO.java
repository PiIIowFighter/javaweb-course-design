package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.util.DbUtil;
import edu.bjfu.onlinesm.util.SchemaUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限持久化 DAO。
 *
 * 表：dbo.RolePermissions(RoleCode, PermissionKey)
 */
public class PermissionDAO {

    private final RoleDAO roleDAO = new RoleDAO();

    public List<String> findAllRoles() throws SQLException {
        return roleDAO.findAllRoleCodes();
    }

    public Set<String> findPermissionsByRole(String roleCode) throws SQLException {
        SchemaUtil.ensureRolePermissionsTable();
        String sql = "SELECT PermissionKey FROM dbo.RolePermissions WHERE RoleCode = ?";
        Set<String> set = new HashSet<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getString("PermissionKey"));
                }
            }
        }
        return set;
    }

    public void setPermissionsForRole(String roleCode, Set<String> keys) throws SQLException {
        SchemaUtil.ensureRolePermissionsTable();
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM dbo.RolePermissions WHERE RoleCode = ?")) {
                    del.setString(1, roleCode);
                    del.executeUpdate();
                }
                if (keys != null && !keys.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO dbo.RolePermissions(RoleCode, PermissionKey) VALUES(?,?)")) {
                        for (String k : keys) {
                            if (k == null || k.trim().isEmpty()) continue;
                            ins.setString(1, roleCode);
                            ins.setString(2, k.trim());
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
