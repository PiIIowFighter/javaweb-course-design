package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责访问 dbo.Users / dbo.Roles 的简单 DAO。
 * 仅实现课程设计当前阶段需要的几个方法：
 *  - 按用户名查询用户；
 *  - 注册（插入）新用户（默认 AUTHOR 角色）；
 *  - 查询全部用户（供超级管理员管理）；
 *  - 更新用户状态（封禁 / 解封）；
 *  - 重置用户密码。
 */
public class UserDAO {

    /**
     * 按用户名查询用户，若不存在则返回 null。
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT u.UserId, u.Username, u.PasswordHash, u.Email, u.FullName, " +
                     "u.Affiliation, u.ResearchArea, u.Status, r.RoleCode " +
                     "FROM dbo.Users u JOIN dbo.Roles r ON u.RoleId = r.RoleId " +
                     "WHERE u.Username = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 查询所有用户，按 UserId 升序排列。
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT u.UserId, u.Username, u.PasswordHash, u.Email, u.FullName, " +
                     "u.Affiliation, u.ResearchArea, u.Status, r.RoleCode " +
                     "FROM dbo.Users u JOIN dbo.Roles r ON u.RoleId = r.RoleId " +
                     "ORDER BY u.UserId ASC";

        List<User> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 用于下拉框选择用户（不返回 PasswordHash）。
     * 适用场景：编委会管理等后台配置页面。
     */
    public List<User> findSelectableUsers() throws SQLException {
        String sql = "SELECT u.UserId, u.Username, u.Email, u.FullName, u.Affiliation, u.ResearchArea, u.Status, r.RoleCode " +
                "FROM dbo.Users u JOIN dbo.Roles r ON u.RoleId = r.RoleId " +
                "ORDER BY u.UserId ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("UserId"));
                u.setUsername(rs.getString("Username"));
                u.setEmail(rs.getString("Email"));
                u.setFullName(rs.getString("FullName"));
                u.setAffiliation(rs.getString("Affiliation"));
                u.setResearchArea(rs.getString("ResearchArea"));
                u.setStatus(rs.getString("Status"));
                u.setRoleCode(rs.getString("RoleCode"));
                list.add(u);
            }
        }
        return list;
    }

    /**
     * 根据角色代码查询用户列表，例如 REVIEWER / EDITOR / EDITOR_IN_CHIEF 等。
     * 供主编管理“审稿人库”、分配责任编辑等场景使用。
     */
    public List<User> findByRoleCode(String roleCode) throws SQLException {
        String sql = "SELECT u.UserId, u.Username, u.PasswordHash, u.Email, u.FullName, " +
                     "u.Affiliation, u.ResearchArea, u.Status, r.RoleCode " +
                     "FROM dbo.Users u JOIN dbo.Roles r ON u.RoleId = r.RoleId " +
                     "WHERE r.RoleCode = ? " +
                     "ORDER BY u.UserId ASC";

        List<User> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }



    /**
     * 注册新用户：默认角色为 AUTHOR，状态为 ACTIVE。
     * 若用户名已存在，将抛出 SQLException（唯一键冲突）。
     */
    public User registerAuthor(User user) throws SQLException {
        int roleId = getRoleIdByCode("AUTHOR");
        if (roleId <= 0) {
            throw new IllegalStateException("数据库中找不到 RoleCode = 'AUTHOR' 的记录，请检查初始化脚本。");
        }

        String sql = "INSERT INTO dbo.Users " +
                "(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getAffiliation());
            ps.setString(6, user.getResearchArea());
            ps.setInt(7, roleId);
            ps.setString(8, user.getStatus() != null ? user.getStatus() : "ACTIVE");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                }
            }
        }

        // 补充角色代码，方便后续使用
        user.setRoleCode("AUTHOR");
        return user;
    }
    /**
     * 由超级管理员创建任意非 SUPER_ADMIN 角色的新用户。
     * 该方法不会对权限本身做控制，调用方（例如 UserServlet）需要自行保证
     * 只有 SUPER_ADMIN 可以调用。
     */
    public User createUserWithRole(User user, String roleCode) throws SQLException {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("roleCode 不能为空");
        }
        String normalizedRoleCode = roleCode.trim().toUpperCase();
        if ("SUPER_ADMIN".equals(normalizedRoleCode)) {
            throw new IllegalArgumentException("不允许通过界面创建超级管理员用户。");
        }

        int roleId = getRoleIdByCode(normalizedRoleCode);
        if (roleId <= 0) {
            throw new IllegalStateException("数据库中找不到 RoleCode = '" + normalizedRoleCode + "' 的记录，请检查 dbo.Roles 表。");
        }

        String sql = "INSERT INTO dbo.Users " +
                "(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getAffiliation());
            ps.setString(6, user.getResearchArea());
            ps.setInt(7, roleId);
            ps.setString(8, user.getStatus() != null ? user.getStatus() : "ACTIVE");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                }
            }
        }

        user.setRoleCode(normalizedRoleCode);
        return user;
    }

    /**
     * 更新用户状态，例如 ACTIVE / DISABLED / LOCKED。
     */
    public void updateStatus(int userId, String status) throws SQLException {
        String sql = "UPDATE dbo.Users SET Status = ? WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 重置密码为指定的新密码（当前阶段明文存储）。
     */
    public void resetPassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE dbo.Users SET PasswordHash = ? WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // === 内部工具方法 ===

    private int getRoleIdByCode(String roleCode) throws SQLException {
        String sql = "SELECT RoleId FROM dbo.Roles WHERE RoleCode = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("RoleId");
                }
            }
        }
        return -1;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("UserId"));
        u.setUsername(rs.getString("Username"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setEmail(rs.getString("Email"));
        u.setFullName(rs.getString("FullName"));
        u.setAffiliation(rs.getString("Affiliation"));
        u.setResearchArea(rs.getString("ResearchArea"));
        u.setStatus(rs.getString("Status"));
        u.setRoleCode(rs.getString("RoleCode"));
        return u;
    }


    /**
     * 按主键 ID 查询单个用户。
     */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT u.UserId, u.Username, u.PasswordHash, u.Email, u.FullName, " +
                     "u.Affiliation, u.ResearchArea, u.Status, r.RoleCode " +
                     "FROM dbo.Users u JOIN dbo.Roles r ON u.RoleId = r.RoleId " +
                     "WHERE u.UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * 更新用户个人信息：仅修改 Email / FullName / Affiliation / ResearchArea。
     */
    public void updateProfile(User user) throws SQLException {
        if (user.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String sql = "UPDATE dbo.Users " +
                     "SET Email = ?, FullName = ?, Affiliation = ?, ResearchArea = ? " +
                     "WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getAffiliation());
            ps.setString(4, user.getResearchArea());
            ps.setInt(5, user.getUserId());
            ps.executeUpdate();
        }
    }


    /**
     * 管理员更新用户信息（含角色与状态）。
     *
     * 允许更新字段：Email / FullName / Affiliation / ResearchArea / Status / RoleId。
     * 注意：不在此方法中修改密码。
     */
    public void adminUpdateUser(User user) throws SQLException {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (user.getRoleCode() == null || user.getRoleCode().trim().isEmpty()) {
            throw new IllegalArgumentException("roleCode 不能为空");
        }

        int roleId = getRoleIdByCode(user.getRoleCode().trim().toUpperCase());
        if (roleId <= 0) {
            throw new IllegalStateException("数据库中找不到 RoleCode = '" + user.getRoleCode() + "' 的记录。");
        }

        String sql = "UPDATE dbo.Users SET Email=?, FullName=?, Affiliation=?, ResearchArea=?, Status=?, RoleId=? WHERE UserId=?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getAffiliation());
            ps.setString(4, user.getResearchArea());
            ps.setString(5, user.getStatus());
            ps.setInt(6, roleId);
            ps.setInt(7, user.getUserId());
            ps.executeUpdate();
        }
    }

    /**
     * 管理员删除用户。
     *
     * 提示：真实系统中需要处理外键约束、逻辑删除、审计等；
     * 课程设计阶段先做物理删除。
     */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM dbo.Users WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }


}
