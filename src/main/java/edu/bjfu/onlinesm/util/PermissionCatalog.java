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
    public static final String ADMIN_JOURNALS = "ADMIN_JOURNALS";
    public static final String ADMIN_EDITORIAL = "ADMIN_EDITORIAL";
    public static final String ADMIN_NEWS = "ADMIN_NEWS";

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
        list.add(new Item(ADMIN_JOURNALS, "期刊管理", "期刊信息维护"));
        list.add(new Item(ADMIN_EDITORIAL, "编委会管理", "编辑委员会维护"));
        list.add(new Item(ADMIN_NEWS, "公告/新闻管理", "发布与维护公告新闻"));
        ALL = Collections.unmodifiableList(list);
    }

    private PermissionCatalog() {
    }

    public static List<Item> all() {
        return ALL;
    }
}
