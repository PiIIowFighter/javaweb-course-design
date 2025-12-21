package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.Journal;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 期刊 DAO：仅用于投稿页面加载期刊下拉列表。
 */
public class JournalDAO {

    public List<Journal> findAll() throws SQLException {
        String sql = "SELECT JournalId, Name FROM dbo.Journals ORDER BY JournalId ASC";
        List<Journal> list = new ArrayList<>();

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Journal j = new Journal();
                j.setJournalId(rs.getInt("JournalId"));
                j.setName(rs.getString("Name"));
                list.add(j);
            }
        }
        return list;
    }
}
