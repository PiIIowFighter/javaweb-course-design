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


public class MenuPermissionService {

    
    public static final String SESSION_MENU_PERMS = "menuPermissions";

    
    public static final String SESSION_MENU_PERM_MAP = "menuPermMap";

    private final MenuPermissionDAO menuPermissionDAO = new MenuPermissionDAO();

    
    public void loadIntoSession(HttpSession session, User user) {
        if (session == null || user == null) return;

        String role = safeUpper(user.getRoleCode());
        Set<String> keys;        
            
            Set<String> assigned = Collections.emptySet();
            Integer uidObj = user.getUserId();
            int uid = uidObj == null ? -1 : uidObj;
            if (uid > 0) {
                try {
                    assigned = menuPermissionDAO.findPermissionsByUser(uid);
                } catch (SQLException e) {
                    
                    assigned = Collections.emptySet();
                }

                if (assigned == null || assigned.isEmpty()) {
                    assigned = defaultMenuPermissions(role);
                    try {
                        menuPermissionDAO.addPermissionsForUser(uid, assigned);
                    } catch (SQLException ignore) {
                        
                    }
                }
            } else {
                assigned = defaultMenuPermissions(role);
            }
            keys = assigned;

        
        if ("SUPER_ADMIN".equals(role)) {
            Set<String> fixed = defaultMenuPermissions(role);
            keys = fixed;
            Integer uidObj2 = user.getUserId();
            int uid2 = uidObj2 == null ? -1 : uidObj2;
            if (uid2 > 0) {
                try {
                    menuPermissionDAO.setPermissionsForUser(uid2, fixed);
                } catch (SQLException ignore) {
                }
            }
        }



        
        session.setAttribute(SESSION_MENU_PERMS, new HashSet<>(keys));

        
        Map<String, Boolean> map = new HashMap<>();
        for (String k : keys) {
            if (k != null && !k.trim().isEmpty()) {
                map.put(k.trim(), Boolean.TRUE);
            }
        }
        session.setAttribute(SESSION_MENU_PERM_MAP, map);
    }

    
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

    
    public Set<String> allMenuPermissionKeys() {
        Set<String> set = new HashSet<>();
        for (PermissionCatalog.Item it : PermissionCatalog.all()) {
            if (it != null && it.getKey() != null && !it.getKey().trim().isEmpty()) {
                set.add(it.getKey().trim());
            }
        }
        return set;
    }

    
    public Set<String> defaultMenuPermissions(String roleCode) {
        String role = safeUpper(roleCode);
        Set<String> set = new HashSet<>();

        switch (role) {
            case "SUPER_ADMIN":
                
                set.add(PermissionCatalog.ADMIN_USERS);
                set.add(PermissionCatalog.ADMIN_PERMISSIONS);
                set.add(PermissionCatalog.ADMIN_LOGS);
                set.add(PermissionCatalog.ADMIN_SYSTEM);
                set.add(PermissionCatalog.ADMIN_DB_MAINTENANCE);
                set.add(PermissionCatalog.ADMIN_EDITORIAL);
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
                set.add(PermissionCatalog.ADMIN_USERS);
                set.add(PermissionCatalog.ADMIN_PERMISSIONS);
                set.add(PermissionCatalog.ADMIN_LOGS);
                set.add(PermissionCatalog.ADMIN_SYSTEM);
                set.add(PermissionCatalog.ADMIN_DB_MAINTENANCE);
                set.add(PermissionCatalog.ADMIN_EDITORIAL);
                set.add(PermissionCatalog.ADMIN_JOURNALS);
                break;
            case "SYSTEM_ADMIN":
                
                set.add(PermissionCatalog.ADMIN_USERS);
                set.add(PermissionCatalog.ADMIN_PERMISSIONS);
                set.add(PermissionCatalog.ADMIN_LOGS);
                set.add(PermissionCatalog.ADMIN_SYSTEM);
                set.add(PermissionCatalog.ADMIN_DB_MAINTENANCE);
                set.add(PermissionCatalog.ADMIN_EDITORIAL);
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

