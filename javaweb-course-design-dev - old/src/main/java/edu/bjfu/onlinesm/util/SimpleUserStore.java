package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletContext;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个非常简单的“伪用户仓库”，仅在内存中保存用户信息。
 *
 * 设计目的：
 *  - 让“登录 / 注册 / 跳转各个工作台页面”的流程可以跑通；
 *  - 避免在课程设计的第一阶段就必须完成 JDBC / DAO / Service；
 *  - 方便你后续将这里的实现无缝替换为真正访问 SQL Server 的 UserDAO。
 *
 * 注意：
 *  - 本类不会把数据写入数据库，服务器重启后注册用户会丢失；
 *  - 超级管理员 admin / 123 会在首次访问时自动创建。
 */
public final class SimpleUserStore {

    private static final String CTX_KEY = "SIMPLE_USER_STORE";

    private SimpleUserStore() {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, User> getStore(ServletContext ctx) {
        Object obj = ctx.getAttribute(CTX_KEY);
        if (obj instanceof Map) {
            return (Map<String, User>) obj;
        }
        Map<String, User> map = new ConcurrentHashMap<>();

        // 内置一个和 sqlserver.sql 中一致的超级管理员账号：admin / 123
        User admin = new User();
        admin.setUserId(1);
        admin.setUsername("admin");
        admin.setPasswordHash("123");
        admin.setEmail("admin@example.com");
        admin.setFullName("超级管理员");
        admin.setRoleCode("SUPER_ADMIN");
        admin.setStatus("ACTIVE");
        admin.setRegisterTime(LocalDateTime.now());
        map.put(admin.getUsername(), admin);

        ctx.setAttribute(CTX_KEY, map);
        return map;
    }

    /** 按用户名查找用户（大小写敏感，保持和数据库一致的行为） */
    public static User findByUsername(ServletContext ctx, String username) {
        if (username == null) {
            return null;
        }
        return getStore(ctx).get(username);
    }

    /**
     * 注册新用户并保存到内存。
     * 默认角色为 AUTHOR，状态为 ACTIVE。
     * 若用户名已经存在，则抛出 IllegalArgumentException。
     */
    public static User register(ServletContext ctx,
                                String username,
                                String rawPassword,
                                String email,
                                String fullName,
                                String affiliation,
                                String researchArea) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        Map<String, User> store = getStore(ctx);
        if (store.containsKey(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUserId(store.size() + 1); // 简单自增 ID，仅在内存中使用
        user.setUsername(username);
        user.setPasswordHash(rawPassword); // 课程设计第一阶段先不做加密
        user.setEmail(email);
        user.setFullName(fullName);
        user.setAffiliation(affiliation);
        user.setResearchArea(researchArea);
        user.setRoleCode("AUTHOR");
        user.setStatus("ACTIVE");
        user.setRegisterTime(LocalDateTime.now());

        store.put(username, user);
        return user;
    }
}
