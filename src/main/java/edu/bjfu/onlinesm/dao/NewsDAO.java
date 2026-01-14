package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.util.DbUtil;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class NewsDAO {

    

    
    public List<News> findPublishedTopN(int n) throws SQLException {
        if (n <= 0) return new ArrayList<>();

        String sql =
                "SELECT TOP " + n + " NewsId, Title, Content, PublishedAt, AuthorId, IsPublished, AttachmentPath " +
                "FROM dbo.News " +
                "WHERE IsPublished = 1 AND (PublishedAt IS NULL OR PublishedAt <= SYSDATETIME()) " +
                "ORDER BY PublishedAt DESC, NewsId DESC";

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

    
    public List<News> findPublishedAll() throws SQLException {
        return findPublished();
    }

    
    public List<News> findPublished() throws SQLException {
        String sql =
                "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished, AttachmentPath " +
                "FROM dbo.News " +
                "WHERE IsPublished = 1 AND (PublishedAt IS NULL OR PublishedAt <= SYSDATETIME()) " +
                "ORDER BY PublishedAt DESC, NewsId DESC";

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

    
    public News findById(int newsId) throws SQLException {
        String sql =
                "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished, AttachmentPath " +
                "FROM dbo.News WHERE NewsId = ?";

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newsId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    

    
    public List<News> findAll() throws SQLException {
        return search(null, null, null);
    }

    
    public List<News> search(String keyword, LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished, AttachmentPath " +
                "FROM dbo.News WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (Title LIKE ? OR Content LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }

        if (fromDate != null) {
            sql.append(" AND PublishedAt >= ?");
            params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
        }

        if (toDate != null) {
            sql.append(" AND PublishedAt < ?");
            params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        }

        sql.append(" ORDER BY PublishedAt DESC, NewsId DESC");

        List<News> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) p);
                } else {
                    ps.setObject(i + 1, p);
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



    
    public int insert(News news) throws SQLException {
        String sql =
                "INSERT INTO dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished, AttachmentPath) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        boolean isPublished = getIsPublishedSafe(news);
        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        
        
        
        
        Timestamp publishedAt;
        LocalDateTime ldt = news.getPublishedAt();
        if (ldt != null) {
            publishedAt = Timestamp.valueOf(ldt);
        } else if (isPublished) {
            publishedAt = nowTs;
        } else {
            publishedAt = null;
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, news.getTitle());
            ps.setString(2, news.getContent());

            if (publishedAt == null) ps.setNull(3, Types.TIMESTAMP);
            else ps.setTimestamp(3, publishedAt);

            ps.setInt(4, news.getAuthorId());
            ps.setBoolean(5, isPublished);
            ps.setString(6, news.getAttachmentPath());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    try {
                        news.setNewsId(id);
                    } catch (Exception ignored) {
                    }
                    return id;
                }
            }
        }
        return 0;
    }


    
    public void update(News news) throws SQLException {
        if (news.getNewsId() == null) {
            throw new SQLException("NewsId 不能为空，无法更新");
        }

        String sql =
                "UPDATE dbo.News " +
                "SET Title = ?, Content = ?, AuthorId = ?, IsPublished = ?, AttachmentPath = ?, PublishedAt = ? " +
                "WHERE NewsId = ?";

        boolean isPublished = getIsPublishedSafe(news);
        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        
        
        
        
        Timestamp publishedAt;
        LocalDateTime ldt = news.getPublishedAt();
        if (ldt != null) {
            publishedAt = Timestamp.valueOf(ldt);
        } else if (isPublished) {
            publishedAt = nowTs;
        } else {
            publishedAt = null;
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, news.getTitle());
            ps.setString(2, news.getContent());
            ps.setInt(3, news.getAuthorId());
            ps.setBoolean(4, isPublished);
            ps.setString(5, news.getAttachmentPath());

            if (publishedAt == null) ps.setNull(6, Types.TIMESTAMP);
            else ps.setTimestamp(6, publishedAt);

            ps.setInt(7, news.getNewsId());

            ps.executeUpdate();
        }
    }


    public void delete(int newsId) throws SQLException {
        String sql = "DELETE FROM dbo.News WHERE NewsId = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newsId);
            ps.executeUpdate();
        }
    }

    

    private News mapRow(ResultSet rs) throws SQLException {
        News n = new News();

        n.setNewsId(rs.getInt("NewsId"));
        n.setTitle(rs.getString("Title"));
        n.setContent(rs.getString("Content"));
        n.setAuthorId(rs.getInt("AuthorId"));
        n.setAttachmentPath(rs.getString("AttachmentPath"));

        Timestamp ts = rs.getTimestamp("PublishedAt");
        LocalDateTime ldt = (ts == null) ? null : ts.toLocalDateTime();
        n.setPublishedAt(ldt);

        
        n.setPublished(rs.getBoolean("IsPublished"));

        return n;
    }


    
    private boolean getIsPublishedSafe(News news) {
        if (news == null) return false;

        Boolean r;

        r = invokeBooleanGetter(news, "isPublished");
        if (r != null) return r;

        r = invokeBooleanGetter(news, "getIsPublished");
        if (r != null) return r;

        r = invokeBooleanGetter(news, "isIsPublished");
        if (r != null) return r;

        
        return false;
    }

    private Boolean invokeBooleanGetter(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            if (v instanceof Boolean) return (Boolean) v;
        } catch (Exception ignored) {
        }
        return null;
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

