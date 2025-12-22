package edu.bjfu.onlinesm.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 课程设计阶段的轻量级“建表兜底”。
 *
 * 注意：
 * 1) 在生产系统中应使用正式迁移工具（Liquibase/Flyway）；
 * 2) 此处只在管理员模块需要时尝试创建必需的表，避免首次运行因缺表导致页面 500。
 */
public final class SchemaUtil {

    private SchemaUtil() {
    }

    public static void ensureRolePermissionsTable() throws SQLException {
        String sql = "IF OBJECT_ID('dbo.RolePermissions', 'U') IS NULL\n" +
                "BEGIN\n" +
                "  CREATE TABLE dbo.RolePermissions(\n" +
                "    RoleCode NVARCHAR(50) NOT NULL,\n" +
                "    PermissionKey NVARCHAR(100) NOT NULL,\n" +
                "    CONSTRAINT PK_RolePermissions PRIMARY KEY(RoleCode, PermissionKey)\n" +
                "  );\n" +
                "END";
        exec(sql);
    }

    /**
     * 编委会表：dbo.EditorialBoard。
     * 课程设计阶段用于“编委会管理”模块，避免首次运行因缺表导致 500。
     */
    public static void ensureEditorialBoardTable() throws SQLException {
        String sql = "IF OBJECT_ID('dbo.EditorialBoard', 'U') IS NULL\n" +
                "BEGIN\n" +
                "  CREATE TABLE dbo.EditorialBoard(\n" +
                "    BoardMemberId INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "    UserId INT NOT NULL,\n" +
                "    JournalId INT NOT NULL,\n" +
                "    Position NVARCHAR(50) NOT NULL,\n" +
                "    Section NVARCHAR(100) NULL,\n" +
                "    Bio NVARCHAR(MAX) NULL\n" +
                "  );\n" +
                "END";
        exec(sql);
    }

    public static void ensureOperationLogsTable() throws SQLException {
        String sql = "IF OBJECT_ID('dbo.OperationLogs', 'U') IS NULL\n" +
                "BEGIN\n" +
                "  CREATE TABLE dbo.OperationLogs(\n" +
                "    LogId INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "    ActorUserId INT NULL,\n" +
                "    ActorUsername NVARCHAR(100) NULL,\n" +
                "    Module NVARCHAR(100) NOT NULL,\n" +
                "    Action NVARCHAR(200) NOT NULL,\n" +
                "    Detail NVARCHAR(MAX) NULL,\n" +
                "    Ip NVARCHAR(64) NULL,\n" +
                "    CreatedAt DATETIME NOT NULL DEFAULT(GETDATE())\n" +
                "  );\n" +
                "END";
        exec(sql);
    }

    private static void exec(String sql) throws SQLException {
        try (Connection conn = DbUtil.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }
}
