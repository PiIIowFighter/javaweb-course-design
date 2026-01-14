package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.EditorialBoardMember;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class EditorialBoardDAO {

    

    
    public List<EditorialBoardMember> findByJournal(int journalId) throws SQLException {
        return findByJournal(journalId, 0);
    }

    
    public List<EditorialBoardMember> findByJournal(Integer journalId, int limit) throws SQLException {
        if (journalId == null) return new ArrayList<>();
        return findByJournal(journalId.intValue(), limit);
    }

    
    public List<EditorialBoardMember> findByJournalId(Integer journalId) throws SQLException {
        if (journalId == null) return new ArrayList<>();
        return findByJournal(journalId.intValue(), 0);
    }

    public List<EditorialBoardMember> findByJournalId(int journalId) throws SQLException {
        return findByJournal(journalId, 0);
    }

    
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

