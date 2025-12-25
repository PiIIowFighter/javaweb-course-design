package edu.bjfu.onlinesm.util;

import edu.bjfu.onlinesm.dao.PermissionDAO;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限校验服务：
 *  - SUPER_ADMIN 默认拥有全部权限；
 *  - 其他角色权限从 dbo.RolePermissions 读取；
 *  - 若数据库缺表/查询异常，则退回到内置默认权限集合（保证系统可用）。
 */
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
            // 回退默认权限
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

    /**
     * 内置默认权限：用于首次运行或缺表情况下的兜底。
     */
    
private Set<String> defaultPermissions(String roleCode) {
    Set<String> set = new HashSet<>();
    switch (roleCode) {
        case "SYSTEM_ADMIN":
            // 系统管理员：拥有全部后台管理权限 + 全局稿件查看/配置权限
            set.add(PermissionCatalog.ADMIN_USERS);
            set.add(PermissionCatalog.ADMIN_PERMISSIONS);
            set.add(PermissionCatalog.ADMIN_LOGS);
            set.add(PermissionCatalog.ADMIN_SYSTEM);
            set.add(PermissionCatalog.ADMIN_DB_MAINTENANCE);
            set.add(PermissionCatalog.ADMIN_JOURNALS);
            set.add(PermissionCatalog.ADMIN_EDITORIAL);
            set.add(PermissionCatalog.ADMIN_NEWS);
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_ALL);
            set.add(PermissionCatalog.MANUSCRIPT_INVITE_ASSIGN);
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_REVIEWER_ID);
            set.add(PermissionCatalog.DECISION_MAKE_ACCEPT_REJECT);
            set.add(PermissionCatalog.SYSTEM_EDIT_CONFIG);
            break;
        case "EO_ADMIN":
            // 编辑部管理员：公告/新闻管理 + 按表格拥有的稿件/配置相关权限
            set.add(PermissionCatalog.ADMIN_NEWS);
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_ALL);
            set.add(PermissionCatalog.MANUSCRIPT_INVITE_ASSIGN);
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_REVIEWER_ID);
            set.add(PermissionCatalog.SYSTEM_EDIT_CONFIG);
            break;
        case "AUTHOR":
            // 作者：只负责提交新稿件
            set.add(PermissionCatalog.MANUSCRIPT_SUBMIT_NEW);
            break;
        case "REVIEWER":
            // 审稿人：只负责填写审稿意见
            set.add(PermissionCatalog.REVIEW_WRITE_OPINION);
            break;
        case "EDITOR":
            // 编辑：可以邀请/指派人员、查看审稿人身份
            set.add(PermissionCatalog.MANUSCRIPT_INVITE_ASSIGN);
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_REVIEWER_ID);
            break;
        case "EDITOR_IN_CHIEF":
            // 主编（EIC）：查看所有稿件、邀请/指派人员、查看审稿人身份、做出录用/拒稿决定
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_ALL);
            set.add(PermissionCatalog.MANUSCRIPT_INVITE_ASSIGN);
            set.add(PermissionCatalog.MANUSCRIPT_VIEW_REVIEWER_ID);
            set.add(PermissionCatalog.DECISION_MAKE_ACCEPT_REJECT);
            break;
        default:
            break;
    }
    return set;
}

}
