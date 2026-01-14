package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.User;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UserDAO {

    
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

    
    public List<User> searchReviewerPool(String keyword,
                                         Integer minCompleted,
                                         Integer minAvgScore,
                                         Integer limit) throws SQLException {

        
        
        

        String top = (limit != null && limit > 0) ? ("TOP " + limit + " ") : "";

        
        String selectCols = "SELECT " + top +
                "u.UserId, u.Username, u.PasswordHash, u.Email, u.FullName, " +
                "u.Affiliation, u.ResearchArea, u.Status, r.RoleCode ";

        String fromJoin = "FROM dbo.Users u " +
                "JOIN dbo.Roles r ON u.RoleId = r.RoleId ";

        String whereBase = "WHERE r.RoleCode = 'REVIEWER' AND u.Status = 'ACTIVE' ";

        
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

        
        String sql3 = selectCols + ", 0 AS CompletedCount, CAST(NULL AS FLOAT) AS AvgScore " +
                fromJoin + whereBase + kwClause + "ORDER BY u.UserId ASC";
        return runReviewerQuery(sql3, kwParams);
    }

    
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

        
        user.setRoleCode("AUTHOR");
        return user;
    }
    
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

    
    public void updateStatus(int userId, String status) throws SQLException {
        String sql = "UPDATE dbo.Users SET Status = ? WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    
    public void resetPassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE dbo.Users SET PasswordHash = ? WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    

    
    public void updateUsername(int userId, String newUsername) throws SQLException {
        String sql = "UPDATE dbo.Users SET Username = ? WHERE UserId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    
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

        
        try {
            Object completedObj = rs.getObject("CompletedCount");
            if (completedObj != null) {
                u.setCompletedReviewCount(((Number) completedObj).intValue());
            }
        } catch (SQLException ignore) {
            
        }
        try {
            Object avgObj = rs.getObject("AvgScore");
            if (avgObj != null) {
                u.setAvgReviewScore(((Number) avgObj).doubleValue());
            }
        } catch (SQLException ignore) {
            
        }
        return u;
    }


    
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

    
    public void deleteUser(int userId) throws SQLException {
        
        
        
        
        
        
        
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int fallbackUserId = getFallbackUserId(conn, userId);

                
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

                
                execUpdate(conn,
                        "UPDATE dbo.Journals SET CreatedBy = NULL WHERE CreatedBy = ?",
                        userId);

                execUpdate(conn,
                        "UPDATE dbo.Manuscripts SET CurrentEditorId = NULL WHERE CurrentEditorId = ?",
                        userId);

                
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

                
                safeExecUpdate(conn,
                        "DELETE FROM dbo.ManuscriptStageTimestamps WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                
                
                safeExecUpdate(conn,
                        "DELETE FROM dbo.ArticleMetrics WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.ManuscriptVersions WHERE ManuscriptId IN (SELECT ManuscriptId FROM dbo.Manuscripts WHERE SubmitterId = ?)",
                        userId);

                execUpdate(conn,
                        "DELETE FROM dbo.Manuscripts WHERE SubmitterId = ?",
                        userId);

                
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

    
    private void safeExecUpdate(Connection conn, String sql, Object... params) {
        try {
            execUpdate(conn, sql, params);
        } catch (SQLException ignore) {
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

