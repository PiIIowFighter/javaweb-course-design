package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色 DAO：从 dbo.Roles 读取角色信息（仅 RoleCode）。
 */
public class RoleDAO {

    public List<String> findAllRoleCodes() throws SQLException {
        String sql = "SELECT RoleCode FROM dbo.Roles ORDER BY RoleId ASC";
        List<String> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("RoleCode"));
            }
        }
        return list;
    }
}
