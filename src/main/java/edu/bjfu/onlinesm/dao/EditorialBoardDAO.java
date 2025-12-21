package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 编委 DAO：同时支持
 * - 前台首页/关于期刊展示（findByJournal...）
 * - 后台编委管理（create/update/delete/findById/findAll...）
 */
public class EditorialBoardDAO {

    /* =========================
       前台展示：按期刊查询
       ========================= */

    /** 兼容：只传 journalId，不限制条数 */
    public List<EditorialBoardMember> findByJournal(int journalId) throws SQLException {
        return findByJournal(journalId, 0);
    }

    /** 兼容：Integer journalId + limit（你之前 PublicHomeServlet 可能用到） */
    public List<EditorialBoardMember> findByJournal(Integer journalId, int limit) throws SQLException {
        if (journalId == null) return new ArrayList<>();
        return findByJournal(journalId.intValue(), limit);
    }

    /** 兼容：后台列表里常见调用 findByJournalId(Integer) */
    public List<EditorialBoardMember> findByJournalId(Integer journalId) throws SQLException {
        if (journalId == null) return new ArrayList<>();
        return findByJournal(journalId.intValue(), 0);
    }

    public List<EditorialBoardMember> findByJournalId(int journalId) throws SQLException {
        return findByJournal(journalId, 0);
    }

    /**
     * 主实现：按 JournalId 查询编委成员
     * @param limit <=0 表示不限制
     */
    public List<EditorialBoardMember> findByJournal(int journalId, int limit) throws SQLException {
        String top = (limit > 0) ? ("TOP " + limit + " ") : "";
        String sql =
                "SELECT " + top +
                " e.BoardMemberId, e.UserId, e.JournalId, e.Position, e.Section, e.Bio, " +
                " u.FullName, u.Affiliation, u.Email " +
                "FROM dbo.EditorialBoard e " +
                "JOIN dbo.Users u ON e.UserId = u.UserId " +
                "WHERE e.JournalId = ? " +
                "ORDER BY e.Position ASC, e.BoardMemberId ASC";

        List<EditorialBoardMember> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, journalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /* =========================
       后台管理：CRUD
       ========================= */

    /** ✅ 解决你截图报错：create(EditorialBoardMember) */
    public int create(EditorialBoardMember m) throws SQLException {
        String sql =
                "INSERT INTO dbo.EditorialBoard(UserId, JournalId, Position, Section, Bio) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, m.getUserId());
            ps.setInt(2, m.getJournalId());
            ps.setString(3, m.getPosition());
            ps.setString(4, m.getSection());
            ps.setString(5, m.getBio());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return 0;
    }

    /** ✅ 解决你截图报错：update(EditorialBoardMember) */
    public void update(EditorialBoardMember m) throws SQLException {
        String sql =
                "UPDATE dbo.EditorialBoard " +
                "SET UserId = ?, JournalId = ?, Position = ?, Section = ?, Bio = ? " +
                "WHERE BoardMemberId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, m.getUserId());
            ps.setInt(2, m.getJournalId());
            ps.setString(3, m.getPosition());
            ps.setString(4, m.getSection());
            ps.setString(5, m.getBio());
            ps.setInt(6, m.getBoardMemberId());

            ps.executeUpdate();
        }
    }

    /** delete：给 int + Integer 两个版本，避免你后续再被装箱/拆箱坑 */
    public void delete(int boardMemberId) throws SQLException {
        String sql = "DELETE FROM dbo.EditorialBoard WHERE BoardMemberId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, boardMemberId);
            ps.executeUpdate();
        }
    }

    public void delete(Integer boardMemberId) throws SQLException {
        if (boardMemberId == null) return;
        delete(boardMemberId.intValue());
    }

    /** 后台编辑页常用：按 id 查询 */
    public EditorialBoardMember findById(int boardMemberId) throws SQLException {
        String sql =
                "SELECT e.BoardMemberId, e.UserId, e.JournalId, e.Position, e.Section, e.Bio, " +
                " u.FullName, u.Affiliation, u.Email " +
                "FROM dbo.EditorialBoard e " +
                "JOIN dbo.Users u ON e.UserId = u.UserId " +
                "WHERE e.BoardMemberId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, boardMemberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /** 后台列表常用：全部成员（不按期刊过滤） */
    public List<EditorialBoardMember> findAll() throws SQLException {
        String sql =
                "SELECT e.BoardMemberId, e.UserId, e.JournalId, e.Position, e.Section, e.Bio, " +
                " u.FullName, u.Affiliation, u.Email " +
                "FROM dbo.EditorialBoard e " +
                "JOIN dbo.Users u ON e.UserId = u.UserId " +
                "ORDER BY e.JournalId ASC, e.Position ASC, e.BoardMemberId ASC";

        List<EditorialBoardMember> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private EditorialBoardMember mapRow(ResultSet rs) throws SQLException {
        EditorialBoardMember m = new EditorialBoardMember();
        // 下面 setter 要与你的 EditorialBoardMember 模型一致；若字段名不同，改这里即可。
        m.setBoardMemberId(rs.getInt("BoardMemberId"));
        m.setUserId(rs.getInt("UserId"));
        m.setJournalId(rs.getInt("JournalId"));
        m.setPosition(rs.getString("Position"));
        m.setSection(rs.getString("Section"));
        m.setBio(rs.getString("Bio"));
        m.setFullName(rs.getString("FullName"));
        m.setAffiliation(rs.getString("Affiliation"));
        m.setEmail(rs.getString("Email"));
        return m;
    }
}
