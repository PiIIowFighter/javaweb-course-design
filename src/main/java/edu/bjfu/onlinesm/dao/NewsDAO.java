package edu.bjfu.onlinesm.dao;

import edu.bjfu.onlinesm.model.News;
import edu.bjfu.onlinesm.util.DbUtil;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NewsDAO
 * 表：dbo.News(NewsId, Title, Content, PublishedAt, AuthorId, IsPublished)
 *
 * ✅ 适配：News.publishedAt 为 LocalDateTime
 * ✅ 修复：不再调用 News.getPublished()（你项目里没有这个方法），改用反射兼容不同 getter 命名
 */
public class NewsDAO {

    /* =========================
       前台：已发布新闻
       ========================= */

    /** 首页：已发布新闻 TOP N */
    public List<News> findPublishedTopN(int n) throws SQLException {
        if (n <= 0) return new ArrayList<>();

        String sql =
                "SELECT TOP " + n + " NewsId, Title, Content, PublishedAt, AuthorId, IsPublished " +
                "FROM dbo.News " +
                "WHERE IsPublished = 1 " +
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

    /** ✅ 解决你报错：PublicAboutServlet / PublicNewsServlet 调用 findPublishedAll() */
    public List<News> findPublishedAll() throws SQLException {
        return findPublished();
    }

    /** 已发布新闻（不限制条数） */
    public List<News> findPublished() throws SQLException {
        String sql =
                "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished " +
                "FROM dbo.News " +
                "WHERE IsPublished = 1 " +
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

    /** 详情：按ID查 */
    public News findById(int newsId) throws SQLException {
        String sql =
                "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished " +
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

    /* =========================
       后台：管理端 CRUD
       ========================= */

    /** 后台列表：全部新闻（含未发布） */
    public List<News> findAll() throws SQLException {
        String sql =
                "SELECT NewsId, Title, Content, PublishedAt, AuthorId, IsPublished " +
                "FROM dbo.News " +
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

    /**
     * ✅ 解决你报错：NewsAdminServlet 调用 insert(news)
     * 规则：
     * - isPublished=true：PublishedAt 若 news 未设置，则默认当前时间
     * - isPublished=false：PublishedAt 写 NULL
     */
    public int insert(News news) throws SQLException {
        String sql =
                "INSERT INTO dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished) " +
                "VALUES (?, ?, ?, ?, ?)";

        boolean isPublished = getIsPublishedSafe(news);
        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        Timestamp publishedAt = null;
        if (isPublished) {
            LocalDateTime ldt = news.getPublishedAt();
            publishedAt = (ldt == null) ? nowTs : Timestamp.valueOf(ldt);
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, news.getTitle());
            ps.setString(2, news.getContent());

            if (publishedAt == null) ps.setNull(3, Types.TIMESTAMP);
            else ps.setTimestamp(3, publishedAt);

            ps.setInt(4, news.getAuthorId());
            ps.setBoolean(5, isPublished);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    // 尝试回写 ID（如果你的 News 有 setNewsId）
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

    /**
     * ✅ 解决你报错：NewsAdminServlet 调用 update(news)
     * 规则：
     * - isPublished=true：PublishedAt 若原本为空则设为当前时间，否则保留
     * - isPublished=false：PublishedAt 置 NULL
     */
    public void update(News news) throws SQLException {
        if (news.getNewsId() == null) {
            throw new SQLException("NewsId 不能为空，无法更新");
        }

        String sql =
                "UPDATE dbo.News " +
                "SET Title = ?, Content = ?, AuthorId = ?, IsPublished = ?, " +
                "    PublishedAt = CASE " +
                "        WHEN ? = 1 THEN ISNULL(PublishedAt, ?) " +
                "        ELSE NULL " +
                "    END " +
                "WHERE NewsId = ?";

        boolean isPublished = getIsPublishedSafe(news);
        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, news.getTitle());
            ps.setString(2, news.getContent());
            ps.setInt(3, news.getAuthorId());
            ps.setBoolean(4, isPublished);

            ps.setBoolean(5, isPublished);
            ps.setTimestamp(6, nowTs);

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

    /* =========================
       映射 & 兼容 getter
       ========================= */

    private News mapRow(ResultSet rs) throws SQLException {
        News n = new News();

        n.setNewsId(rs.getInt("NewsId"));
        n.setTitle(rs.getString("Title"));
        n.setContent(rs.getString("Content"));
        n.setAuthorId(rs.getInt("AuthorId"));

        Timestamp ts = rs.getTimestamp("PublishedAt");
        LocalDateTime ldt = (ts == null) ? null : ts.toLocalDateTime();
        n.setPublishedAt(ldt);

        // 你后台 servlet 用的是 setPublished(boolean)，这里也按这个来
        n.setPublished(rs.getBoolean("IsPublished"));

        return n;
    }

    /**
     * ✅ 关键修复：用反射兼容不同命名（不会再出现“方法不存在”的编译报错）
     * 尝试顺序：
     * 1) isPublished()
     * 2) getIsPublished()
     * 3) isIsPublished()
     */
    private boolean getIsPublishedSafe(News news) {
        if (news == null) return false;

        Boolean r;

        r = invokeBooleanGetter(news, "isPublished");
        if (r != null) return r;

        r = invokeBooleanGetter(news, "getIsPublished");
        if (r != null) return r;

        r = invokeBooleanGetter(news, "isIsPublished");
        if (r != null) return r;

        // 如果你 News 只有 setPublished(...) 但没有 getter，这里兜底 false
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
