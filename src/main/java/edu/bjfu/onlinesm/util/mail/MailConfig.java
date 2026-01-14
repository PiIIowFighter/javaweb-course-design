package edu.bjfu.onlinesm.util.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


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
    
 
    public boolean isEnabled() { return enabled(); }

    public String getHost() { return host(); }

    public int getPort() { return port(); }

    public String getUsername() { return username(); }

    public String getPassword() { return password(); }

    public String getFrom() {
        String f = from();
        if (f == null || f.trim().isEmpty()) {
            
            return getUsername();
        }
        return f.trim();
    }

    public String getFromName() { return fromName(); }

    
    public boolean isStarttls() { return startTls(); }

    
    public boolean isStartTls() { return startTls(); }

    public boolean isSsl() { return ssl(); }

    public boolean isDebug() { return debug(); }

    public String getBaseUrl() { return baseUrl(); }

    
    public String getSystemName() {
        
        return props == null ? fromName() : props.getProperty("smtp.systemName", fromName());
    }


    
    

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

