package edu.bjfu.onlinesm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class PermissionCatalog {

    public static final String ADMIN_USERS = "ADMIN_USERS";
    public static final String ADMIN_PERMISSIONS = "ADMIN_PERMISSIONS";
    public static final String ADMIN_LOGS = "ADMIN_LOGS";
    public static final String ADMIN_SYSTEM = "ADMIN_SYSTEM";
    public static final String ADMIN_DB_MAINTENANCE = "ADMIN_DB_MAINTENANCE";
    public static final String ADMIN_JOURNALS = "ADMIN_JOURNALS";
    public static final String ADMIN_EDITORIAL = "ADMIN_EDITORIAL";
    public static final String ADMIN_NEWS = "ADMIN_NEWS";




public static final String MENU_AUTHOR_MY_MANUSCRIPTS = "MENU_AUTHOR_MY_MANUSCRIPTS";
public static final String MENU_AUTHOR_SUBMIT = "MENU_AUTHOR_SUBMIT";

public static final String MENU_REVIEWER_ASSIGNED = "MENU_REVIEWER_ASSIGNED";
public static final String MENU_REVIEWER_HISTORY = "MENU_REVIEWER_HISTORY";

public static final String MENU_EDITOR_TODO = "MENU_EDITOR_TODO";
public static final String MENU_EDITOR_UNDER_REVIEW = "MENU_EDITOR_UNDER_REVIEW";
public static final String MENU_EDITOR_RECOMMEND = "MENU_EDITOR_RECOMMEND";
public static final String MENU_EDITOR_REVIEW_MONITOR = "MENU_EDITOR_REVIEW_MONITOR";
public static final String MENU_EDITOR_AUTHOR_COMM = "MENU_EDITOR_AUTHOR_COMM";

public static final String MENU_EIC_OVERVIEW = "MENU_EIC_OVERVIEW";
public static final String MENU_EIC_DESK = "MENU_EIC_DESK";
public static final String MENU_EIC_TO_ASSIGN = "MENU_EIC_TO_ASSIGN";
public static final String MENU_EIC_REVIEWERS = "MENU_EIC_REVIEWERS";
public static final String MENU_EIC_FINAL_DECISION = "MENU_EIC_FINAL_DECISION";
public static final String MENU_EIC_SPECIAL = "MENU_EIC_SPECIAL";

public static final String MENU_EO_FORMAL_CHECK = "MENU_EO_FORMAL_CHECK";
public static final String MENU_EO_FORMAL_HISTORY = "MENU_EO_FORMAL_HISTORY";


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

        
        public String getDesc() {
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



list.add(new Item(MENU_AUTHOR_MY_MANUSCRIPTS, "我的稿件", "作者：查看稿件列表/详情/进度/导出"));
list.add(new Item(MENU_AUTHOR_SUBMIT, "提交稿件", "作者：提交新稿/修回再投"));

list.add(new Item(MENU_REVIEWER_ASSIGNED, "待评审稿件", "审稿人：待处理/受邀/填写评审意见"));
list.add(new Item(MENU_REVIEWER_HISTORY, "历史评审", "审稿人：历史评审记录"));

list.add(new Item(MENU_EDITOR_TODO, "编辑待办", "编辑：待处理稿件（分配审稿/查看进展）"));
list.add(new Item(MENU_EDITOR_UNDER_REVIEW, "审稿中稿件", "编辑：审稿中列表"));
list.add(new Item(MENU_EDITOR_RECOMMEND, "编辑推荐", "编辑：推荐意见与详情"));
list.add(new Item(MENU_EDITOR_REVIEW_MONITOR, "审稿监控", "编辑：催审/查看评审详情"));
list.add(new Item(MENU_EDITOR_AUTHOR_COMM, "作者沟通", "编辑：与作者沟通/发送消息"));

list.add(new Item(MENU_EIC_OVERVIEW, "主编总览", "主编：全局总览"));
list.add(new Item(MENU_EIC_DESK, "案头审稿", "主编：案头稿件列表"));
list.add(new Item(MENU_EIC_TO_ASSIGN, "待分配审稿人", "主编：分配审稿人/责任编辑"));
list.add(new Item(MENU_EIC_REVIEWERS, "审稿人库", "主编：管理/筛选审稿人库"));
list.add(new Item(MENU_EIC_FINAL_DECISION, "终审决策", "主编：终审/录用退稿决策列表"));
list.add(new Item(MENU_EIC_SPECIAL, "专项工作", "主编：专项/特殊任务入口"));

list.add(new Item(MENU_EO_FORMAL_CHECK, "形式审查", "编辑部管理员：稿件形式审查/自动检查"));
list.add(new Item(MENU_EO_FORMAL_HISTORY, "形式审查历史", "编辑部管理员：形式审查历史记录"));
        ALL = Collections.unmodifiableList(list);
    }

    private PermissionCatalog() {
    }

    public static List<Item> all() {
        return ALL;
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

