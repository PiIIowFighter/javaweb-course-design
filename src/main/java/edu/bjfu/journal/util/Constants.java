package edu.bjfu.journal.util;

public final class Constants {
    private Constants() {}

    public static final String SESSION_USER = "LOGIN_USER";
    public static final String SESSION_ROLES = "LOGIN_ROLES";
    public static final String SESSION_PERMS = "LOGIN_PERMS";

    // 角色 code（与数据库 sys_role.code 一致）
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_EIC = "EIC";
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_AUTHOR = "AUTHOR";
    public static final String ROLE_CMS = "CMS";
}
