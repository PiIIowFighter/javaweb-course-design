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
                "    CreatedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()\n" +
                "  );\n" +
                "  CREATE INDEX IX_OperationLogs_CreatedAt ON dbo.OperationLogs(CreatedAt DESC, LogId DESC);\n" +
                "  CREATE INDEX IX_OperationLogs_ActorUsername ON dbo.OperationLogs(ActorUsername);\n" +
                "END";
        exec(sql);
    }

    /**
     * 站内通知表：dbo.Notifications。
     *
     * 说明：
     * - 只做“单向通知”（无对话串），用于任务书中的“通知中心 / 系统通知”。
     * - 为避免首次运行缺表导致 500，DAO 会在调用前兜底创建。
     */
    public static void ensureNotificationsTable() throws SQLException {
        String sql = "IF OBJECT_ID('dbo.Notifications', 'U') IS NULL\n" +
                "BEGIN\n" +
                "  CREATE TABLE dbo.Notifications(\n" +
                "    NotificationId INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "    RecipientUserId INT NOT NULL,\n" +
                "    CreatedByUserId INT NULL,\n" +
                "    Type NVARCHAR(20) NOT NULL DEFAULT N'SYSTEM',\n" +
                "    Category NVARCHAR(50) NULL,\n" +
                "    Title NVARCHAR(200) NOT NULL,\n" +
                "    Content NVARCHAR(MAX) NULL,\n" +
                "    RelatedManuscriptId INT NULL,\n" +
                "    IsRead BIT NOT NULL DEFAULT 0,\n" +
                "    ReadAt DATETIME2(0) NULL,\n" +
                "    CreatedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),\n" +
                "    CONSTRAINT FK_Notifications_Recipient FOREIGN KEY(RecipientUserId) REFERENCES dbo.Users(UserId)\n" +
                "  );\n" +
                "END\n" +
                "IF OBJECT_ID('dbo.Notifications', 'U') IS NOT NULL\n" +
                "BEGIN\n" +
                "  IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Notifications_Recipient_Read' AND object_id=OBJECT_ID('dbo.Notifications'))\n" +
                "  BEGIN\n" +
                "    BEGIN TRY\n" +
                "      CREATE INDEX IX_Notifications_Recipient_Read ON dbo.Notifications(RecipientUserId, IsRead, CreatedAt DESC, NotificationId DESC);\n" +
                "    END TRY\n" +
                "    BEGIN CATCH\n" +
                "    END CATCH\n" +
                "  END\n" +
                "  IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Notifications_CreatedBy' AND object_id=OBJECT_ID('dbo.Notifications'))\n" +
                "  BEGIN\n" +
                "    BEGIN TRY\n" +
                "      CREATE INDEX IX_Notifications_CreatedBy ON dbo.Notifications(CreatedByUserId, CreatedAt DESC, NotificationId DESC);\n" +
                "    END TRY\n" +
                "    BEGIN CATCH\n" +
                "    END CATCH\n" +
                "  END\n" +
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
