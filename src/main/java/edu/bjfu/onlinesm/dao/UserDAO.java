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
     * 审稿人库搜索：按关键词 / 过往绩效筛选 REVIEWER 用户。
     *
     * @param keyword       关键词（可为空），在 FullName / Username / Affiliation / ResearchArea 中模糊匹配
     * @param minCompleted  最低完成评审次数（可为空），基于 Reviews.Status = 'SUBMITTED' 的记录计数
     * @param minAvgScore   最低平均评分（0-10，可为空），基于 Reviews.Score 的平均值
     * @param limit         最多返回多少条记录（可为空或 <=0 表示不限制）
     */
    public List<User> searchReviewerPool(String keyword,
                                         Integer minCompleted,
                                         Integer minAvgScore,
                                         Integer limit) throws SQLException {

        // 注意：部分同学的旧数据库脚本可能没有 dbo.Reviews.Score / dbo.Reviews.InvitedAt 等列，
        // 直接 JOIN/AVG 会导致“Invalid column name …”从而页面 500。
        // 这里按“三段降级”策略查询审稿人池：先带 AvgScore+CompletedCount，再仅 CompletedCount，最后只查用户表。

        String top = (limit != null && limit > 0) ? ("TOP " + limit + " ") : "";

        // 通用：用户字段
        String selectCols = "SELECT " + top +
                "u.UserId, u.Username, u.PasswordHash, u.Email, u.FullName, " +
                "u.Affiliation, u.ResearchArea, u.Status, r.RoleCode ";

        String fromJoin = "FROM dbo.Users u " +
                "JOIN dbo.Roles r ON u.RoleId = r.RoleId ";

        String whereBase = "WHERE r.RoleCode = 'REVIEWER' AND u.Status = 'ACTIVE' ";

        // 关键词过滤（通用）
        java.util.List<Object> kwParams = new java.util.ArrayList<>();
        String kwClause = "";
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            kwClause = "AND (u.FullName LIKE ? OR u.Username LIKE ? OR u.Affiliation LIKE ? OR u.ResearchArea LIKE ?) ";
            kwParams.add(kw);
            kwParams.add(kw);
            kwParams.add(kw);
            kwParams.add(kw);
        }

        // ---------- 方案1：带 AvgScore + CompletedCount ----------
        String sql1 = selectCols + fromJoin +
                "LEFT JOIN ( " +
                "  SELECT ReviewerId, COUNT(*) AS CompletedCount, AVG(CAST(Score AS FLOAT)) AS AvgScore " +
                "  FROM dbo.Reviews WHERE Status = 'SUBMITTED' GROUP BY ReviewerId " +
                ") s ON s.ReviewerId = u.UserId " +
                whereBase +
                kwClause;

        java.util.List<Object> p1 = new java.util.ArrayList<>(kwParams);
        if (minCompleted != null) {
            sql1 += "AND ISNULL(s.CompletedCount, 0) >= ? ";
            p1.add(minCompleted);
        }
        if (minAvgScore != null) {
            sql1 += "AND ISNULL(s.AvgScore, 0) >= ? ";
            p1.add(minAvgScore);
        }
        sql1 += "ORDER BY ISNULL(s.AvgScore, 0) DESC, ISNULL(s.CompletedCount, 0) DESC, u.UserId ASC";

        try {
            return runReviewerQuery(sql1, p1);
        } catch (SQLException ex) {
            String msg = ex.getMessage();
            String low = (msg == null) ? "" : msg.toLowerCase();
            boolean scoreMissing =
                    (msg != null && msg.contains("Score"))
                            || low.contains("invalid column")
                            || low.contains("unknown column");
            if (!scoreMissing) {
                throw ex;
            }
        }

        // ---------- 方案2：仅 CompletedCount（忽略 minAvgScore） ----------
        String sql2 = selectCols + fromJoin +
                "LEFT JOIN ( " +
                "  SELECT ReviewerId, COUNT(*) AS CompletedCount " +
                "  FROM dbo.Reviews WHERE Status = 'SUBMITTED' GROUP BY ReviewerId " +
                ") s ON s.ReviewerId = u.UserId " +
                whereBase +
                kwClause;

        java.util.List<Object> p2 = new java.util.ArrayList<>(kwParams);
        if (minCompleted != null) {
            sql2 += "AND ISNULL(s.CompletedCount, 0) >= ? ";
            p2.add(minCompleted);
        }
        sql2 += "ORDER BY ISNULL(s.CompletedCount, 0) DESC, u.UserId ASC";

        try {
            return runReviewerQuery(sql2, p2);
        } catch (SQLException ex2) {
            String msg = ex2.getMessage();
            String low = (msg == null) ? "" : msg.toLowerCase();
            boolean reviewsMissing =
                    (msg != null && msg.contains("Reviews"))
                            || low.contains("invalid object")
                            || low.contains("does not exist");
            if (!reviewsMissing) {
                throw ex2;
            }
        }

        // ---------- 方案3：不依赖 Reviews 表（忽略 minCompleted/minAvgScore） ----------
        String sql3 = selectCols + fromJoin + whereBase + kwClause + "ORDER BY u.UserId ASC";
        return runReviewerQuery(sql3, kwParams);
    }

    /**
     * 执行审稿人池查询（供 searchReviewerPool 三段降级复用）
     */
    private List<User> runReviewerQuery(String sql, java.util.List<Object> params) throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof String) {
                        ps.setString(i + 1, (String) p);
                    } else if (p instanceof Integer) {
                        ps.setInt(i + 1, (Integer) p);
                    } else {
                        ps.setObject(i + 1, p);
                    }
                }
            }

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