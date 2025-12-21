package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.util.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 新闻 / 公告 DAO，对应 dbo.News。
 */
public class NewsDAO {

    public List<News> findAll() throws SQLException {
        String sql = "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished " +
                     "FROM dbo.News ORDER BY PublishedAt DESC";

        List<News> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public News findById(int id) throws SQLException {
        String sql = "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished " +
                     "FROM dbo.News WHERE NewsId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public void insert(News news) throws SQLException {
        // PublishedAt 使用表中的默认值（SYSUTCDATETIME），这里不显式写
        String sql = "INSERT INTO dbo.News(Title, Content, AuthorId, IsPublished) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, news.getTitle());
            ps.setString(2, news.getContent());
            ps.setInt(3, news.getAuthorId());
            ps.setBoolean(4, news.isPublished());
            ps.executeUpdate();
        }
    }

    public void update(News news) throws SQLException {
        String sql = "UPDATE dbo.News " +
                     "SET Title = ?, Content = ?, IsPublished = ? " +
                     "WHERE NewsId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, news.getTitle());
            ps.setString(2, news.getContent());
            ps.setBoolean(3, news.isPublished());
            ps.setInt(4, news.getNewsId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM dbo.News WHERE NewsId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private News mapRow(ResultSet rs) throws SQLException {
        News n = new News();
        n.setNewsId(rs.getInt("NewsId"));
        n.setTitle(rs.getString("Title"));
        n.setContent(rs.getString("Content"));

        Timestamp ts = rs.getTimestamp("PublishedAt");
        if (ts != null) {
            n.setPublishedAt(ts.toLocalDateTime());
        }

        n.setAuthorId(rs.getInt("AuthorId"));
        n.setPublished(rs.getBoolean("IsPublished"));
        return n;
    }
}
