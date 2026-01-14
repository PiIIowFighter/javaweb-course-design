package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.model.User;

import javax.servlet.ServletContext;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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

    
    public static User findByUsername(ServletContext ctx, String username) {
        if (username == null) {
            return null;
        }
        return getStore(ctx).get(username);
    }

    
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
        user.setUserId(store.size() + 1); 
        user.setUsername(username);
        user.setPasswordHash(rawPassword); 
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

