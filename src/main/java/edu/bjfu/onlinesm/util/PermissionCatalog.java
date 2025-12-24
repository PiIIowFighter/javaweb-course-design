package edu.bjfu.onlinesm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 系统内置的“权限点”目录。
 *
 * 
 * 仅做模块/URL 级别授权（如：用户管理、权限管理、日志管理等）。
 */
public final class PermissionCatalog {

    public static final String ADMIN_USERS = "ADMIN_USERS";
    public static final String ADMIN_PERMISSIONS = "ADMIN_PERMISSIONS";
    public static final String ADMIN_LOGS = "ADMIN_LOGS";
    public static final String ADMIN_SYSTEM = "ADMIN_SYSTEM";
    public static final String ADMIN_DB_MAINTENANCE = "ADMIN_DB_MAINTENANCE";
    public static final String ADMIN_JOURNALS = "ADMIN_JOURNALS";
    public static final String ADMIN_EDITORIAL = "ADMIN_EDITORIAL";
    public static final String ADMIN_NEWS = "ADMIN_NEWS";

public static final String MANUSCRIPT_SUBMIT_NEW = "MANUSCRIPT_SUBMIT_NEW";
public static final String MANUSCRIPT_VIEW_ALL = "MANUSCRIPT_VIEW_ALL";
public static final String MANUSCRIPT_INVITE_ASSIGN = "MANUSCRIPT_INVITE_ASSIGN";
public static final String MANUSCRIPT_VIEW_REVIEWER_ID = "MANUSCRIPT_VIEW_REVIEWER_ID";
public static final String REVIEW_WRITE_OPINION = "REVIEW_WRITE_OPINION";
public static final String DECISION_MAKE_ACCEPT_REJECT = "DECISION_MAKE_ACCEPT_REJECT";
public static final String SYSTEM_EDIT_CONFIG = "SYSTEM_EDIT_CONFIG";


    public static final class Item {
        private final String key;
        private final String name;
        private final String description;

        public Item(String key, String name, String description) {
            this.key = key;
            this.name = name;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final List<Item> ALL;

    static {
        List<Item> list = new ArrayList<>();
        list.add(new Item(ADMIN_USERS, "用户管理", "查看/新增/修改/删除/禁用用户"));
        list.add(new Item(ADMIN_PERMISSIONS, "权限管理", "为不同角色分配系统访问权限"));
        list.add(new Item(ADMIN_LOGS, "日志管理", "查看系统运行/操作日志"));
        list.add(new Item(ADMIN_SYSTEM, "系统状态", "监控系统运行状态（JVM/DB/时间等）"));
        list.add(new Item(ADMIN_DB_MAINTENANCE, "数据库维护", "检查/修复关键表结构（RolePermissions、OperationLogs 等）"));
        list.add(new Item(ADMIN_JOURNALS, "期刊管理", "期刊信息维护"));
        list.add(new Item(ADMIN_EDITORIAL, "编委会管理", "编辑委员会维护"));
        list.add(new Item(ADMIN_NEWS, "公告/新闻管理", "发布与维护公告新闻"));

// 稿件处理与审稿流程相关权限（按课程设计功能表）
list.add(new Item(MANUSCRIPT_SUBMIT_NEW, "提交新稿件", "作者在系统中提交新稿件"));
list.add(new Item(MANUSCRIPT_VIEW_ALL, "查看所有稿件", "查看系统中所有稿件的总体列表"));
list.add(new Item(MANUSCRIPT_INVITE_ASSIGN, "邀请/指派人员", "为稿件邀请或指派审稿人/编辑"));
list.add(new Item(MANUSCRIPT_VIEW_REVIEWER_ID, "查看审稿人身份", "查看被指派审稿人的身份信息"));
list.add(new Item(REVIEW_WRITE_OPINION, "填写审稿意见", "审稿人填写并提交审稿意见"));
list.add(new Item(DECISION_MAKE_ACCEPT_REJECT, "做出录用/拒稿决定", "主编对稿件做出最终录用/拒稿决定"));
list.add(new Item(SYSTEM_EDIT_CONFIG, "修改系统配置", "修改期刊管理系统的重要配置项"));
        ALL = Collections.unmodifiableList(list);
    }

    private PermissionCatalog() {
    }

    public static List<Item> all() {
        return ALL;
    }
}