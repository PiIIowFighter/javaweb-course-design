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

        // 通用：用户字段（审稿统计列在各方案里单独拼接，保证降级方案也能正常返回列名）
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
        String sql1 = selectCols +
                ", ISNULL(s.CompletedCount, 0) AS CompletedCount, s.AvgScore AS AvgScore " +
                fromJoin +
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
        String sql2 = selectCols +
                ", ISNULL(s.CompletedCount, 0) AS CompletedCount, CAST(NULL AS FLOAT) AS AvgScore " +
                fromJoin +
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
        String sql3 = selectCols + ", 0 AS CompletedCount, CAST(NULL AS FLOAT) AS AvgScore " +
                fromJoin + whereBase + kwClause + "ORDER BY u.UserId ASC";
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

    

    /**
     * 用户在个人中心修改用户名（要求唯一）。
     */
    public void updateUsername(int userId, String newUsername) throws SQLException {
        String sql = "UPDATE dbo.Users SET Username = ? WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 用户修改密码：只有当旧密码匹配时才会更新。
     *
     * @return 更新的行数（1=成功，0=旧密码不正确或用户不存在）
     */
    public int updatePasswordIfMatch(int userId, String oldPassword, String newPassword) throws SQLException {
        String sql = "UPDATE dbo.Users SET PasswordHash = ? WHERE UserId = ? AND PasswordHash = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.setString(3, oldPassword);
            return ps.executeUpdate();
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

        // 审稿人绩效统计：并非所有查询都会带这些列（或某些旧数据库不存在 Reviews 表/Score 列）
        try {
            Object completedObj = rs.getObject("CompletedCount");
            if (completedObj != null) {
                u.setCompletedReviewCount(((Number) completedObj).intValue());
            }
        } catch (SQLException ignore) {
            // ignore
        }
        try {
            Object avgObj = rs.getObject("AvgScore");
            if (avgObj != null) {
                u.setAvgReviewScore(((Number) avgObj).doubleValue());
            }
        } catch (SQLException ignore) {
            // ignore
        }
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
        // 你当前库里 Users 被大量业务表外键引用（Notifications / EditorialBoard / Reviews / Manuscripts ...），
        // 直接 DELETE Users 会触发 FK 冲突。
        // 这里用“级联清理 + 必要字段改写”的方式做物理删除，保证管理员“删除用户”按钮可用。
        // 说明：
        // 1) 对于可为空的外键（如 Journals.CreatedBy、Manuscripts.CurrentEditorId）优先置 NULL；
        // 2) 对于必须保留的历史字段（如 ManuscriptStatusHistory.ChangedBy、ManuscriptVersions.CreatedBy）用一个兜底用户替换；
        // 3) 对于与该用户强绑定的数据（如该用户提交的稿件）直接删除整条稿件及其从表记录。
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int fallbackUserId = getFallbackUserId(conn, userId);

                // ============ 1) 先清理“直接引用 Users 的从表” ============
                execUpdate(conn,
                        "DELETE FROM dbo.Notifications WHERE RecipientUserId = ? OR CreatedByUserId = ?",
                        userId, userId);

                execUpdate(conn,
                        "DELETE FROM dbo.EditorialBoard WHERE UserId = ?",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.EditorSuggestions WHERE EditorId = ?",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.FormalCheckResults WHERE ReviewerId = ?",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.Reviews WHERE ReviewerId = ?",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptAssignments WHERE EditorId = ? OR AssignedByChiefId = ?",
                        userId, userId);

                execUpdate(conn,
                        "DELETE FROM dbo.News WHERE AuthorId = ?",
                        userId);

                // 可为空的外键：直接置空即可
                execUpdate(conn,
                        "UPDATE dbo.Journals SET CreatedBy = NULL WHERE CreatedBy = ?",
                        userId);

                execUpdate(conn,
                        "UPDATE dbo.Manuscripts SET CurrentEditorId = NULL WHERE CurrentEditorId = ?",
                        userId);

                // 不可为空但又不希望丢历史：改写为兜底用户
                if (fallbackUserId > 0) {
                    execUpdate(conn,
                            "UPDATE dbo.ManuscriptVersions SET CreatedBy = ? WHERE CreatedBy = ?",
                            fallbackUserId, userId);

                    execUpdate(conn,
                            "UPDATE dbo.ManuscriptStatusHistory SET ChangedBy = ? WHERE ChangedBy = ?",
                            fallbackUserId, userId);

                    execUpdate(conn,
                            "UPDATE dbo.Files SET UploaderId = ? WHERE UploaderId = ?",
                            fallbackUserId, userId);
                }

                // ============ 2) 删除“该用户作为投稿人提交的稿件”及其关联数据 ============
                // 先删与稿件相关的从表，再删 Manuscripts。
                // 说明：Reviews 有 VersionId FK，必须先删 Reviews 再删 Versions。

                // Notifications.RelatedManuscriptId 没有 FK，这里顺手清理一下（可省略）
                execUpdate(conn,
                        "DELETE FROM dbo.Notifications WHERE RelatedManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.EditorSuggestions WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.FormalCheckResults WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.Reviews WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptAssignments WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptAuthors WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptRecommendedReviewers WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.Files WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptStatusHistory WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                // 这些表是补丁里新增的（有的库可能没有）。如果表不存在会报错，所以做 try-catch 降级。
                safeExecUpdate(conn,
                        "DELETE FROM dbo.ManuscriptStageTimestamps WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                // ArticleMetrics 的 FK 有 ON DELETE CASCADE，删除 Manuscripts 时会自动清理；
                // 但为了兼容旧库/不同脚本，这里也尝试显式删除（失败则忽略）。
                safeExecUpdate(conn,
                        "DELETE FROM dbo.ArticleMetrics WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptVersions WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.Manuscripts WHERE SubmitterId = ?",
                        userId);

                // ============ 3) 最后删除用户本身 ============
                execUpdate(conn, "DELETE FROM dbo.Users WHERE UserId = ?", userId);

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 选择一个“兜底用户”用于改写不可为空的外键字段：优先选 admin，否则选任意一个不是被删用户的 UserId。
     */
    private int getFallbackUserId(Connection conn, int deletingUserId) {
        String sqlAdmin = "SELECT TOP 1 UserId FROM dbo.Users WHERE Username = N'admin' AND UserId <> ?";
        String sqlAny = "SELECT TOP 1 UserId FROM dbo.Users WHERE UserId <> ? ORDER BY UserId ASC";
        try (PreparedStatement ps = conn.prepareStatement(sqlAdmin)) {
            ps.setInt(1, deletingUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignore) {
        }
        try (PreparedStatement ps = conn.prepareStatement(sqlAny)) {
            ps.setInt(1, deletingUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignore) {
        }
        return -1;
    }

    private void execUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else if (p == null) {
                    ps.setObject(i + 1, null);
                } else {
                    ps.setObject(i + 1, p);
                }
            }
            ps.executeUpdate();
        }
    }

    /**
     * 兼容不同版本数据库：表/列不存在时忽略。
     */
    private void safeExecUpdate(Connection conn, String sql, Object... params) {
        try {
            execUpdate(conn, sql, params);
        } catch (SQLException ignore) {
        }
    }


}