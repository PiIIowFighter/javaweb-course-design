package edu.bjfu.onlinesm.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.TimeZone;

/**
 * 全局时区初始化：统一将系统默认时区设置为中国标准时间（UTC+8）。
 *
 * 目的：
 * 1) LocalDateTime.now()/ZonedDateTime.now() 等默认以东八区取“当前时间”
 * 2) JSTL fmt:formatDate 等默认以东八区格式化时间
 *
 * 说明：
 * - 本项目大量使用 LocalDateTime（无时区信息）与 DATETIME2（无时区信息）。
 *   因此需要确保“当前时间”的生成与展示都落在统一的系统默认时区下。
 */
@WebListener
public class TimeZoneInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 中国标准时间
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(tz);
        // 部分库/组件会读取 user.timezone
        System.setProperty("user.timezone", "Asia/Shanghai");
        System.out.println("[TimeZoneInit] default timezone set to " + tz.getID());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // no-op
    }
}
