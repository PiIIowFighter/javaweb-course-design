package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.dao.MenuPermissionDAO;
import edu.bjfu.onlinesm.model.User;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * “菜单入口”权限服务：
 * <ul>
 *     <li>将“菜单入口”权限缓存到 session（供 header/sidebar JSP 动态显示）</li>
 *     <li>用于 {@link edu.bjfu.onlinesm.util.AdminAuthzFilter} 进行 URL 级访问拦截</li>
 *     <li>当用户没有任何记录时，初始化为“原来侧边栏默认状态”的权限集合</li>
 * </ul>
 */
public class MenuPermissionService {

    /** session 中缓存的权限集合（Set&lt;String&gt;） */
    public static final String SESSION_MENU_PERMS = "menuPermissions";

    /** session 中给 JSP 用的 map（Map&lt;String, Boolean&gt;） */
    public static final String SESSION_MENU_PERM_MAP = "menuPermMap";

    private final MenuPermissionDAO menuPermissionDAO = new MenuPermissionDAO();

    /**
     * 将用户“菜单入口”权限加载进 session（同时生成 menuPermMap）。
     */
    public void loadIntoSession(HttpSession session, User user) {
        if (session == null || user == null) return;

        String role = safeUpper(user.getRoleCode());
        Set<String> keys;        // 从表里读取用户“菜单入口”权限；若没有记录则初始化默认
            // 正常用户：从表里取；若没有记录则初始化默认
            Set<String> assigned = Collections.emptySet();
            Integer uidObj = user.getUserId();
            int uid = uidObj == null ? -1 : uidObj;
            if (uid > 0) {
                try {
                    assigned = menuPermissionDAO.findPermissionsByUser(uid);
                } catch (SQLException e) {
                    // DB 异常时回退默认权限，保证系统可用
                    assigned = Collections.emptySet();
                }

                if (assigned == null || assigned.isEmpty()) {
                    assigned = defaultMenuPermissions(role);
                    try {
                        menuPermissionDAO.addPermissionsForUser(uid, assigned);
                    } catch (SQLException ignore) {
                        // 忽略写入失败，至少让前端能显示
                    }
                }
            } else {
                assigned = defaultMenuPermissions(role);
            }
            keys = assigned;


        // 缓存 Set
        session.setAttribute(SESSION_MENU_PERMS, new HashSet<>(keys));

        // 缓存 Map（供 JSP 用）
        Map<String, Boolean> map = new HashMap<>();
        for (String k : keys) {
            if (k != null && !k.trim().isEmpty()) {
                map.put(k.trim(), Boolean.TRUE);
            }
        }
        session.setAttribute(SESSION_MENU_PERM_MAP, map);
    }

    /**
     * 判断 session 中是否拥有某入口权限。
     */
    public boolean hasPermission(HttpSession session, User user, String permissionKey) {
        if (session == null || user == null || permissionKey == null) return false;

        String role = safeUpper(user.getRoleCode());
        Object obj = session.getAttribute(SESSION_MENU_PERMS);
        if (obj instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) obj;
            return set.contains(permissionKey);
        }

        Object mapObj = session.getAttribute(SESSION_MENU_PERM_MAP);
        if (mapObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> map = (Map<String, Boolean>) mapObj;
            return Boolean.TRUE.equals(map.get(permissionKey));
        }

        return false;
    }

    /**
     * 返回系统所有可勾选的入口权限 key。
     */
    public Set<String> allMenuPermissionKeys() {
        Set<String> set = new HashSet<>();
        for (PermissionCatalog.Item it : PermissionCatalog.all()) {
            if (it != null && it.getKey() != null && !it.getKey().trim().isEmpty()) {
                set.add(it.getKey().trim());
            }
        }
        return set;
    }

    /**
     * 默认入口权限：与原先各角色工作台/侧边栏默认可见入口保持一致。
     */
    public Set<String> defaultMenuPermissions(String roleCode) {
        String role = safeUpper(roleCode);
        Set<String> set = new HashSet<>();

        switch (role) {
            case "SUPER_ADMIN":
                // 默认给满权限（仍可在权限管理中修改）
                set.addAll(allMenuPermissionKeys());
                break;
            case "AUTHOR":
                set.add(PermissionCatalog.MENU_AUTHOR_MY_MANUSCRIPTS);
                set.add(PermissionCatalog.MENU_AUTHOR_SUBMIT);
                break;
            case "REVIEWER":
                set.add(PermissionCatalog.MENU_REVIEWER_ASSIGNED);
                set.add(PermissionCatalog.MENU_REVIEWER_HISTORY);
                break;
            case "EDITOR":
                set.add(PermissionCatalog.MENU_EDITOR_TODO);
                set.add(PermissionCatalog.MENU_EDITOR_UNDER_REVIEW);
                set.add(PermissionCatalog.MENU_EDITOR_RECOMMEND);
                set.add(PermissionCatalog.MENU_EDITOR_REVIEW_MONITOR);
                set.add(PermissionCatalog.MENU_EDITOR_AUTHOR_COMM);
                break;
            case "EDITOR_IN_CHIEF":
                set.add(PermissionCatalog.MENU_EIC_OVERVIEW);
                set.add(PermissionCatalog.MENU_EIC_DESK);
                set.add(PermissionCatalog.MENU_EIC_TO_ASSIGN);
                set.add(PermissionCatalog.MENU_EIC_REVIEWERS);
                set.add(PermissionCatalog.MENU_EIC_FINAL_DECISION);
                set.add(PermissionCatalog.MENU_EIC_SPECIAL);
                break;
            case "EO_ADMIN":
                set.add(PermissionCatalog.MENU_EO_FORMAL_CHECK);
                set.add(PermissionCatalog.MENU_EO_FORMAL_HISTORY);
                // 旧系统里 EO_ADMIN 通常也能发新闻
                set.add(PermissionCatalog.ADMIN_NEWS);
                break;
            case "SYSTEM_ADMIN":
                // 系统管理员默认拥有后台管理入口
                set.add(PermissionCatalog.ADMIN_USERS);
                set.add(PermissionCatalog.ADMIN_PERMISSIONS);
                set.add(PermissionCatalog.ADMIN_LOGS);
                set.add(PermissionCatalog.ADMIN_SYSTEM);
                set.add(PermissionCatalog.ADMIN_DB_MAINTENANCE);
                set.add(PermissionCatalog.ADMIN_JOURNALS);
                set.add(PermissionCatalog.ADMIN_EDITORIAL);
                set.add(PermissionCatalog.ADMIN_NEWS);
                break;
            default:
                break;
        }

        return set;
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}
