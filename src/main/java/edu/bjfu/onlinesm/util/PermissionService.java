package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.dao.PermissionDAO;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class PermissionService {

    private final PermissionDAO permissionDAO = new PermissionDAO();

    public boolean hasPermission(String roleCode, String permissionKey) {
        if (roleCode == null || permissionKey == null) return false;
        String rc = roleCode.trim().toUpperCase();
        String pk = permissionKey.trim();

        if ("SUPER_ADMIN".equals(rc)) {
            return true;
        }

        try {
            Set<String> set = permissionDAO.findPermissionsByRole(rc);
            return set.contains(pk);
        } catch (SQLException e) {
            
            return defaultPermissions(rc).contains(pk);
        }
    }

    public Set<String> getPermissionsForRole(String roleCode) {
        if (roleCode == null) return Collections.emptySet();
        String rc = roleCode.trim().toUpperCase();
        try {
            return permissionDAO.findPermissionsByRole(rc);
        } catch (SQLException e) {
            return defaultPermissions(rc);
        }
    }

    
    private Set<String> defaultPermissions(String roleCode) {
        Set<String> set = new HashSet<>();
        switch (roleCode) {
            case "SYSTEM_ADMIN":
                set.add(PermissionCatalog.ADMIN_USERS);
                set.add(PermissionCatalog.ADMIN_PERMISSIONS);
                set.add(PermissionCatalog.ADMIN_LOGS);
                set.add(PermissionCatalog.ADMIN_SYSTEM);
                set.add(PermissionCatalog.ADMIN_DB_MAINTENANCE);
                set.add(PermissionCatalog.ADMIN_JOURNALS);
                set.add(PermissionCatalog.ADMIN_EDITORIAL);
                set.add(PermissionCatalog.ADMIN_NEWS);
                break;
            case "EO_ADMIN":
                set.add(PermissionCatalog.ADMIN_NEWS);
                break;
            default:
                break;
        }
        return set;
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

