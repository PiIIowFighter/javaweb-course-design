package edu.bjfu.onlinesm.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 简单的 JDBC 工具类：
 *  - 从 classpath 下的 db.properties 加载 SQL Server 配置；
 *  - 注册 JDBC 驱动；
 *  - 提供获取 Connection 的静态方法。
 *
 * 在 MyEclipse / Tomcat 中部署时，只要 db.properties 位于 WEB-INF/classes 下，
 * 本工具类即可正常读取。
 */
public final class DbUtil {

    private static String url;
    private static String username;
    private static String password;

    static {
        loadConfig();
    }

    private DbUtil() {
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("找不到 db.properties，请确认它位于 src/main/resources 下。");
            }
            props.load(in);
            String driverClass = props.getProperty("jdbc.driver");
            url = props.getProperty("jdbc.url");
            username = props.getProperty("jdbc.username");
            password = props.getProperty("jdbc.password");

            if (driverClass == null || url == null) {
                throw new IllegalStateException("db.properties 中缺少 jdbc.driver 或 jdbc.url 配置。");
            }

            Class.forName(driverClass);
        } catch (IOException e) {
            throw new RuntimeException("读取 db.properties 失败", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("加载数据库驱动失败，请确认已在 lib 中加入 SQL Server JDBC 驱动。", e);
        }
    }

    /**
     * 获取一个新的数据库连接。
     * 调用者使用后需要自行关闭：
     * try (Connection conn = DbUtil.getConnection()) { ... }
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}