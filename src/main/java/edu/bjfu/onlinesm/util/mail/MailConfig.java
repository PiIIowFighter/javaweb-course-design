package edu.bjfu.onlinesm.util.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 读取 classpath 下的 mail.properties。
 *
 * 说明：本项目未使用 Maven/Gradle，引入 JavaMail 依赖不稳定。
 * 为保证“可发送邮件”的功能可落地，这里实现了一个轻量 SMTP 客户端。
 */
public class MailConfig {

    private final Properties props;

    public MailConfig(Properties props) {
        this.props = props;
    }

    public static MailConfig load() {
        Properties p = new Properties();
        try (InputStream in = MailConfig.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException ignored) {
            // 没有配置文件时，按默认禁用邮件
        }
        return new MailConfig(p);
    }

    private String get(String key, String def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        v = v.trim();
        return v.isEmpty() ? def : v;
    }

    public boolean enabled() {
        return "true".equalsIgnoreCase(get("smtp.enabled", "false"));
    }

    public String host() {
        return get("smtp.host", "");
    }

    public int port() {
        try {
            return Integer.parseInt(get("smtp.port", "25"));
        } catch (NumberFormatException e) {
            return 25;
        }
    }

    public String username() {
        return get("smtp.username", "");
    }

    public String password() {
        return get("smtp.password", "");
    }

    public String from() {
        return get("smtp.from", "");
    }

    public String fromName() {
        return get("smtp.fromName", "OnlineSM期刊系统");
    }

    public boolean startTls() {
        return "true".equalsIgnoreCase(get("smtp.starttls", "true"));
    }

    public boolean ssl() {
        return "true".equalsIgnoreCase(get("smtp.ssl", "false"));
    }

    public boolean debug() {
        return "true".equalsIgnoreCase(get("smtp.debug", "false"));
    }

    public String baseUrl() {
        return get("app.baseUrl", "");
    }
    
 // ====== JavaBean 兼容层：给 MailSender / 旧代码调用 ======
    public boolean isEnabled() { return enabled(); }

    public String getHost() { return host(); }

    public int getPort() { return port(); }

    public String getUsername() { return username(); }

    public String getPassword() { return password(); }

    public String getFrom() {
        String f = from();
        if (f == null || f.trim().isEmpty()) {
            // 有些 SMTP 服务要求 MAIL FROM 必须是登录账号
            return getUsername();
        }
        return f.trim();
    }

    public String getFromName() { return fromName(); }

    // 兼容你 MailSender 里写的 isStarttls() / isStarttls && !isSsl()
    public boolean isStarttls() { return startTls(); }

    // 同时也给更规范的 isStartTls()（可选，但推荐）
    public boolean isStartTls() { return startTls(); }

    public boolean isSsl() { return ssl(); }

    public boolean isDebug() { return debug(); }

    public String getBaseUrl() { return baseUrl(); }

    // 如果你的其它地方用到了 systemName，可加这个（对应 mail.properties 里可填 systemName）
    public String getSystemName() {
        // 兼容之前模板里用到的名字；没有就退回 fromName
        return props == null ? fromName() : props.getProperty("smtp.systemName", fromName());
    }


    // ===== 自动催审（Auto Remind）配置，供 AutoRemindListener 使用 =====
    // 说明：统一从 mail.properties 读取（不再依赖另一套 mail 包）

    public int getAutoRemindOverdueDays() {
        try { return Integer.parseInt(get("mail.autoRemind.overdueDays", "7")); }
        catch (Exception e) { return 7; }
    }

    public int getAutoRemindMinIntervalDays() {
        try { return Integer.parseInt(get("mail.autoRemind.minIntervalDays", "7")); }
        catch (Exception e) { return 7; }
    }

    public int getAutoRemindMaxPerRun() {
        try { return Integer.parseInt(get("mail.autoRemind.maxPerRun", "50")); }
        catch (Exception e) { return 50; }
    }

}
