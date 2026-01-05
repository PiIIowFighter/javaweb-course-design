-- ============================================================
-- 合并脚本：sqlserver_patched.sql + sync_formal_check_database.sql
-- 生成时间: 2026-01-02
-- 说明：先执行主建库/补丁脚本，再执行形式审查同步脚本（已包含存在性判断）。
-- ============================================================

/* ============================================================
   Online_SMSystem4SP 课程设计 - 重整版 SQL Server 建库脚本（v2025-12-21）
   目标：
   1) 与当前 src.zip 代码一致（尤其是 dbo.OperationLogs / dbo.RolePermissions）；
   2) 修复“日志管理 500 / 列名 LogId 无效”等由表结构不一致导致的问题；
   3) 给出默认角色、默认账号、默认权限映射，保证后台可直接登录演示。
   ------------------------------------------------------------
   说明：
   - 若你希望“全量重建数据库”，请先取消下面 DROP DATABASE 的注释；
   - 若你希望“在现有库上修复 OperationLogs / RolePermissions”，脚本也提供了
     “检测列是否存在 -> DROP & 重新创建”的兼容逻辑（不会自动迁移旧日志数据）。
   ============================================================ */

/* ========= 可选：全量重建数据库（会清空所有数据，请谨慎） =========
IF DB_ID(N'Online_SMSystem4SP') IS NOT NULL
    DROP DATABASE [Online_SMSystem4SP];
GO
================================================================ */

-- 若数据库不存在则创建
IF DB_ID(N'Online_SMSystem4SP') IS NULL
    CREATE DATABASE [Online_SMSystem4SP];
GO

USE [Online_SMSystem4SP];
GO

/* ============================================================
   0. 兼容修复：如果旧表存在但缺少关键列，则删除旧表（避免 500）
   ============================================================ */

-- 0.1 OperationLogs：若存在但没有 LogId 列，则删除后重建（不迁移旧数据）
IF OBJECT_ID(N'dbo.OperationLogs', N'U') IS NOT NULL
AND COL_LENGTH(N'dbo.OperationLogs', N'LogId') IS NULL
BEGIN
    DROP TABLE dbo.OperationLogs;
END
GO

-- 0.2 RolePermissions：若存在但列名不一致，则删除后重建
IF OBJECT_ID(N'dbo.RolePermissions', N'U') IS NOT NULL
AND (COL_LENGTH(N'dbo.RolePermissions', N'RoleCode') IS NULL OR COL_LENGTH(N'dbo.RolePermissions', N'PermissionKey') IS NULL)
BEGIN
    DROP TABLE dbo.RolePermissions;
END
GO

/* ============================================================
   1. 角色表 Roles（7 种角色）
   ============================================================ */
IF OBJECT_ID(N'dbo.Roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Roles (
        RoleId      INT IDENTITY(1,1) PRIMARY KEY,
        RoleCode    NVARCHAR(50)  NOT NULL UNIQUE,  -- SUPER_ADMIN / SYSTEM_ADMIN / AUTHOR / REVIEWER / EDITOR_IN_CHIEF / EDITOR / EO_ADMIN
        RoleName    NVARCHAR(100) NOT NULL,
        Description NVARCHAR(200) NULL
    );

    INSERT dbo.Roles(RoleCode, RoleName, Description) VALUES
    (N'SUPER_ADMIN',      N'超级管理员',       N'系统内置超级管理员，拥有最高权限'),
    (N'SYSTEM_ADMIN',     N'系统管理员',       N'系统维护、用户管理、权限分配、日志查看'),
    (N'AUTHOR',           N'作者',             N'投稿与跟踪稿件状态'),
    (N'REVIEWER',         N'审稿人',           N'接收邀请并提交审稿意见'),
    (N'EDITOR_IN_CHIEF',  N'主编',             N'学术决策：初审、指派编辑、终审决策、审稿人库、撤稿/归档'),
    (N'EDITOR',           N'编辑',             N'处理分配稿件、邀请审稿人、给出推荐意见'),
    (N'EO_ADMIN',         N'编辑部管理员',     N'形式审查、格式检查、公告/新闻管理');
END;
GO

/* ============================================================
   2. 用户表 Users
   - 代码中注册用户默认 Status=PENDING，需管理员激活（SystemAdminServlet）
   ============================================================ */
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserId         INT IDENTITY(1,1) PRIMARY KEY,
        Username       NVARCHAR(50)  NOT NULL UNIQUE,
        PasswordHash   NVARCHAR(255) NOT NULL,          -- 课程设计阶段：明文（后续可换哈希）
        Email          NVARCHAR(100) NULL,
        FullName       NVARCHAR(100) NULL,
        Affiliation    NVARCHAR(200) NULL,
        ResearchArea   NVARCHAR(200) NULL,
        RoleId         INT NOT NULL,
        RegisterTime   DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        Status         NVARCHAR(20)  NOT NULL DEFAULT N'ACTIVE',  -- ACTIVE / DISABLED / LOCKED / PENDING
        CONSTRAINT FK_Users_Roles FOREIGN KEY(RoleId) REFERENCES dbo.Roles(RoleId),
        CONSTRAINT CK_Users_Status CHECK (Status IN (N'ACTIVE', N'DISABLED', N'LOCKED', N'PENDING'))
    );

    /* 默认账号（可按需改密码）
       - admin / 123        ：超级管理员
       - sysadmin / password123 ：系统管理员
       - eoadmin / password123  ：编辑部管理员
       - eic / password123      ：主编
       - editor1 / password123  ：编辑
       - reviewer1 / password123：审稿人
       - author1 / password123  ：作者
    */
    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'admin', N'123', N'admin@example.com', N'超级管理员', N'系统内置', NULL, RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'SUPER_ADMIN';

    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'sysadmin', N'password123', N'sysadmin@example.com', N'系统管理员', N'信息学院', NULL, RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'SYSTEM_ADMIN';

    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'eoadmin', N'password123', N'eoadmin@example.com', N'编辑部管理员', N'信息学院', NULL, RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'EO_ADMIN';

    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'eic', N'password123', N'eic@example.com', N'主编', N'信息学院', NULL, RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'EDITOR_IN_CHIEF';

    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'editor1', N'password123', N'editor1@example.com', N'编辑1', N'信息学院', NULL, RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'EDITOR';

    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'reviewer1', N'password123', N'reviewer1@example.com', N'审稿人1', N'某高校', N'机器学习', RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'REVIEWER';

    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'author1', N'password123', N'author1@example.com', N'作者1', N'北京林业大学', N'人工智能', RoleId, N'ACTIVE'
      FROM dbo.Roles WHERE RoleCode = N'AUTHOR';
END;
GO

/* ============================================================
   3. 权限映射表 RolePermissions（给后台模块做 URL 级授权）
   - 代码读取：dbo.RolePermissions(RoleCode, PermissionKey)
   - SUPER_ADMIN 在代码中直接放行，不需要写入此表也行
   ============================================================ */
IF OBJECT_ID(N'dbo.RolePermissions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RolePermissions(
        RoleCode       NVARCHAR(50)  NOT NULL,
        PermissionKey  NVARCHAR(100) NOT NULL,
        CONSTRAINT PK_RolePermissions PRIMARY KEY(RoleCode, PermissionKey),
        CONSTRAINT FK_RolePermissions_RoleCode FOREIGN KEY(RoleCode) REFERENCES dbo.Roles(RoleCode)
    );
END;
GO

-- 初始化默认权限：SYSTEM_ADMIN 拥有全部后台权限；EO_ADMIN 仅新闻管理（可按需增减）
IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions)
BEGIN
    INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES
    (N'SYSTEM_ADMIN', N'ADMIN_USERS'),
    (N'SYSTEM_ADMIN', N'ADMIN_PERMISSIONS'),
    (N'SYSTEM_ADMIN', N'ADMIN_LOGS'),
    (N'SYSTEM_ADMIN', N'ADMIN_SYSTEM'),
    (N'SYSTEM_ADMIN', N'ADMIN_JOURNALS'),
    (N'SYSTEM_ADMIN', N'ADMIN_EDITORIAL'),
    (N'SYSTEM_ADMIN', N'ADMIN_NEWS'),

    (N'EO_ADMIN', N'ADMIN_NEWS');
END
GO

/* ============================================================
   Patch: 为新稿件流程相关角色追加默认权限（保持原有数据不变，只在缺失时补齐）
   ============================================================ */
IF OBJECT_ID(N'dbo.RolePermissions', N'U') IS NOT NULL
BEGIN
    -- 作者：提交新稿件
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'AUTHOR' AND PermissionKey = N'MANUSCRIPT_SUBMIT_NEW')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'AUTHOR', N'MANUSCRIPT_SUBMIT_NEW');

    -- 审稿人：填写审稿意见
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'REVIEWER' AND PermissionKey = N'REVIEW_WRITE_OPINION')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'REVIEWER', N'REVIEW_WRITE_OPINION');

    -- 编辑：邀请/指派审稿人 + 查看审稿人身份
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EDITOR' AND PermissionKey = N'MANUSCRIPT_INVITE_ASSIGN')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EDITOR', N'MANUSCRIPT_INVITE_ASSIGN');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EDITOR' AND PermissionKey = N'MANUSCRIPT_VIEW_REVIEWER_ID')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EDITOR', N'MANUSCRIPT_VIEW_REVIEWER_ID');

    -- 主编（EIC）：查看所有稿件 + 邀请/指派 + 查看审稿人身份 + 做出录用/拒稿决定
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EDITOR_IN_CHIEF' AND PermissionKey = N'MANUSCRIPT_VIEW_ALL')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EDITOR_IN_CHIEF', N'MANUSCRIPT_VIEW_ALL');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EDITOR_IN_CHIEF' AND PermissionKey = N'MANUSCRIPT_INVITE_ASSIGN')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EDITOR_IN_CHIEF', N'MANUSCRIPT_INVITE_ASSIGN');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EDITOR_IN_CHIEF' AND PermissionKey = N'MANUSCRIPT_VIEW_REVIEWER_ID')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EDITOR_IN_CHIEF', N'MANUSCRIPT_VIEW_REVIEWER_ID');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EDITOR_IN_CHIEF' AND PermissionKey = N'DECISION_MAKE_ACCEPT_REJECT')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EDITOR_IN_CHIEF', N'DECISION_MAKE_ACCEPT_REJECT');

    -- 编务部管理员：查看所有稿件 + 邀请/指派 + 查看审稿人身份 + 修改系统配置
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EO_ADMIN' AND PermissionKey = N'MANUSCRIPT_VIEW_ALL')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EO_ADMIN', N'MANUSCRIPT_VIEW_ALL');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EO_ADMIN' AND PermissionKey = N'MANUSCRIPT_INVITE_ASSIGN')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EO_ADMIN', N'MANUSCRIPT_INVITE_ASSIGN');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EO_ADMIN' AND PermissionKey = N'MANUSCRIPT_VIEW_REVIEWER_ID')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EO_ADMIN', N'MANUSCRIPT_VIEW_REVIEWER_ID');
    IF NOT EXISTS (SELECT 1 FROM dbo.RolePermissions WHERE RoleCode = N'EO_ADMIN' AND PermissionKey = N'SYSTEM_EDIT_CONFIG')
        INSERT dbo.RolePermissions(RoleCode, PermissionKey) VALUES (N'EO_ADMIN', N'SYSTEM_EDIT_CONFIG');
END
GO

/* ============================================================
   4. 期刊表 Journals
   ============================================================ */
IF OBJECT_ID(N'dbo.Journals', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Journals (
        JournalId     INT IDENTITY(1,1) PRIMARY KEY,
        Name          NVARCHAR(200) NOT NULL,
        Description   NVARCHAR(MAX) NULL,
        ImpactFactor  DECIMAL(6,3) NULL,
        Timeline      NVARCHAR(200) NULL,
        ISSN          NVARCHAR(30)  NULL,
        CreatedAt     DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CreatedBy     INT NULL,
        CONSTRAINT FK_Journals_CreatedBy FOREIGN KEY(CreatedBy) REFERENCES dbo.Users(UserId)
    );

    -- 任务书示例期刊
    INSERT dbo.Journals(Name, Description, ImpactFactor, Timeline, ISSN, CreatedBy)
    SELECT N'International Artificial Intelligence Research',
           N'课程设计示例期刊：国际人工智能研究',
           5.123, N'First decision ~ 4 weeks', N'1234-5678',
           (SELECT TOP 1 UserId FROM dbo.Users WHERE Username = N'admin');
END;
GO

/* ============================================================
   5. 稿件表 Manuscripts（含状态机字段）
   ============================================================ */
IF OBJECT_ID(N'dbo.Manuscripts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Manuscripts (
        ManuscriptId       INT IDENTITY(1,1) PRIMARY KEY,
        JournalId          INT NULL,
        SubmitterId        INT NOT NULL,
        CurrentEditorId    INT NULL,
        Title              NVARCHAR(500) NOT NULL,
        Abstract           NVARCHAR(MAX)  NULL,
        Keywords           NVARCHAR(500)  NULL,
        SubjectArea        NVARCHAR(100)  NULL,
        FundingInfo        NVARCHAR(500)  NULL,
        AuthorList         NVARCHAR(500)  NULL,
        Status             NVARCHAR(30)   NOT NULL DEFAULT N'DRAFT',
        Decision           NVARCHAR(30)   NULL,          -- ACCEPT / REJECT / REVISION
        CurrentRound       INT NOT NULL DEFAULT 1,
        SubmitTime         DATETIME2(0) NULL,
        LastStatusTime     DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        FinalDecisionTime  DATETIME2(0) NULL,
        IsArchived         BIT NOT NULL DEFAULT 0,
        IsWithdrawn        BIT NOT NULL DEFAULT 0,

        CONSTRAINT FK_Manuscripts_Journal        FOREIGN KEY(JournalId)       REFERENCES dbo.Journals(JournalId),
        CONSTRAINT FK_Manuscripts_Submitter     FOREIGN KEY(SubmitterId)     REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Manuscripts_CurrentEditor FOREIGN KEY(CurrentEditorId) REFERENCES dbo.Users(UserId),

        CONSTRAINT CK_Manuscripts_Status CHECK (Status IN (
            N'DRAFT',
            N'SUBMITTED',
            N'FORMAL_CHECK',
            N'RETURNED',
            N'DESK_REVIEW_INITIAL',
            N'TO_ASSIGN',
            N'WITH_EDITOR',
            N'UNDER_REVIEW',
            N'EDITOR_RECOMMENDATION',
            N'FINAL_DECISION_PENDING',
            N'REVISION',
            N'ACCEPTED',
            N'REJECTED',
            N'ARCHIVED'
        ))
    );
END;
GO

/* ============================================================
   6. 稿件版本表 ManuscriptVersions
   ============================================================ */
IF OBJECT_ID(N'dbo.ManuscriptVersions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptVersions (
        VersionId           INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId        INT NOT NULL,
        VersionNumber       INT NOT NULL,
        IsCurrent           BIT NOT NULL DEFAULT 1,
        FileAnonymousPath   NVARCHAR(260) NULL,
        FileOriginalPath    NVARCHAR(260) NULL,
        CoverLetterPath     NVARCHAR(260) NULL,
        CoverLetterHtml     NVARCHAR(MAX) NULL,
        ResponseLetterPath  NVARCHAR(260) NULL,
        CreatedAt           DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CreatedBy           INT NOT NULL,
        Remark              NVARCHAR(200) NULL,

        CONSTRAINT UQ_ManuscriptVersions UNIQUE(ManuscriptId, VersionNumber),
        CONSTRAINT FK_ManuscriptVersions_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_ManuscriptVersions_CreatedBy  FOREIGN KEY(CreatedBy)    REFERENCES dbo.Users(UserId)
    );
END;
GO

/* -- Patch: add CoverLetterHtml column for ManuscriptVersions (if missing) -- */
IF COL_LENGTH(N'dbo.ManuscriptVersions', N'CoverLetterHtml') IS NULL
BEGIN
    ALTER TABLE dbo.ManuscriptVersions ADD CoverLetterHtml NVARCHAR(MAX) NULL;
END;
GO

/* ============================================================
   6.x 稿件编辑指派记录表 ManuscriptAssignments
   ============================================================ */
IF OBJECT_ID(N'dbo.ManuscriptAssignments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptAssignments (
        AssignmentId       INT IDENTITY(1,1) PRIMARY KEY,  -- 主键
        ManuscriptId       INT NOT NULL,                   -- 对应稿件
        EditorId           INT NOT NULL,                   -- 被指派的编辑
        AssignedByChiefId  INT NOT NULL,                   -- 指派的主编
        ChiefComment       NVARCHAR(1000) NULL,            -- 主编给编辑的文字建议
        AssignedTime       DATETIME2(0) NOT NULL 
                          DEFAULT SYSUTCDATETIME(),        -- 指派时间（UTC）

        CONSTRAINT FK_MA_Manuscript 
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_MA_Editor 
            FOREIGN KEY(EditorId) REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_MA_AssignedByChief 
            FOREIGN KEY(AssignedByChiefId) REFERENCES dbo.Users(UserId)
    );
END;
GO

/* ============================================================
   7. 稿件作者表 ManuscriptAuthors
   ============================================================ */
IF OBJECT_ID(N'dbo.ManuscriptAuthors', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptAuthors (
        AuthorId          INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId      INT NOT NULL,
        AuthorOrder       INT NOT NULL,
        FullName          NVARCHAR(100) NOT NULL,
        Affiliation       NVARCHAR(200) NULL,
        Degree            NVARCHAR(50)  NULL,
        Title             NVARCHAR(50)  NULL,
        Position          NVARCHAR(50)  NULL,
        Email             NVARCHAR(100) NULL,
        IsCorresponding   BIT NOT NULL DEFAULT 0,
        CONSTRAINT FK_ManuscriptAuthors_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId)
    );
END;
GO

/* ============================================================
   8. 推荐审稿人表 ManuscriptRecommendedReviewers
   ============================================================ */
IF OBJECT_ID(N'dbo.ManuscriptRecommendedReviewers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptRecommendedReviewers (
        Id            INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId  INT NOT NULL,
        FullName      NVARCHAR(100) NOT NULL,
        Email         NVARCHAR(100) NOT NULL,
        Reason        NVARCHAR(500) NULL,
        CONSTRAINT FK_MRecommendedReviewers_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId)
    );
END;
GO

/* ============================================================
   9. 审稿表 Reviews
   ============================================================ */
IF OBJECT_ID(N'dbo.Reviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reviews (
        ReviewId       INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId   INT NOT NULL,
        VersionId      INT NULL,
        ReviewerId     INT NOT NULL,
        Content        NVARCHAR(MAX) NULL,
        ConfidentialToEditor NVARCHAR(MAX) NULL,
        KeyEvaluation NVARCHAR(1000) NULL,
        ScoreOriginality DECIMAL(4,2) NULL,
        ScoreSignificance DECIMAL(4,2) NULL,
        ScoreMethodology DECIMAL(4,2) NULL,
        ScorePresentation DECIMAL(4,2) NULL,
        Score          DECIMAL(4,2) NULL,
        Recommendation NVARCHAR(50) NULL,
        Status         NVARCHAR(30) NOT NULL DEFAULT N'INVITED', -- INVITED/ACCEPTED/DECLINED/SUBMITTED/EXPIRED
        InvitedAt      DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        AcceptedAt     DATETIME2(0) NULL,
        SubmittedAt    DATETIME2(0) NULL,
        DueAt          DATETIME2(0) NULL,
        RemindCount    INT NOT NULL DEFAULT 0,
        LastRemindedAt DATETIME2(0) NULL,

        CONSTRAINT FK_Reviews_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_Reviews_Version    FOREIGN KEY(VersionId)    REFERENCES dbo.ManuscriptVersions(VersionId),
        CONSTRAINT FK_Reviews_Reviewer   FOREIGN KEY(ReviewerId)   REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_Reviews_Status CHECK (Status IN (N'INVITED', N'ACCEPTED', N'DECLINED', N'SUBMITTED', N'EXPIRED'))
    );
END;
GO

/* ============================================================
   ★ 升级补丁：审稿人评审结构图字段（V2）
   - 解决 reviewer 提交评审时报错：列名 'ConfidentialToEditor' 无效
   - 可安全重复执行（按列是否存在判断）
   ============================================================ */
IF OBJECT_ID(N'dbo.Reviews', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.Reviews','ConfidentialToEditor') IS NULL
        ALTER TABLE dbo.Reviews ADD ConfidentialToEditor NVARCHAR(MAX) NULL;

    IF COL_LENGTH('dbo.Reviews','KeyEvaluation') IS NULL
        ALTER TABLE dbo.Reviews ADD KeyEvaluation NVARCHAR(1000) NULL;

    IF COL_LENGTH('dbo.Reviews','ScoreOriginality') IS NULL
        ALTER TABLE dbo.Reviews ADD ScoreOriginality DECIMAL(4,2) NULL;

    IF COL_LENGTH('dbo.Reviews','ScoreSignificance') IS NULL
        ALTER TABLE dbo.Reviews ADD ScoreSignificance DECIMAL(4,2) NULL;

    IF COL_LENGTH('dbo.Reviews','ScoreMethodology') IS NULL
        ALTER TABLE dbo.Reviews ADD ScoreMethodology DECIMAL(4,2) NULL;

    IF COL_LENGTH('dbo.Reviews','ScorePresentation') IS NULL
        ALTER TABLE dbo.Reviews ADD ScorePresentation DECIMAL(4,2) NULL;
END;
GO

/* ============================================================
   10. 编委会表 EditorialBoard
   ============================================================ */
IF OBJECT_ID(N'dbo.EditorialBoard', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.EditorialBoard (
        BoardMemberId INT IDENTITY(1,1) PRIMARY KEY,
        UserId        INT NOT NULL,
        JournalId     INT NOT NULL,
        Position      NVARCHAR(50) NOT NULL,
        Section       NVARCHAR(100) NULL,
        Bio           NVARCHAR(MAX)  NULL,
        CONSTRAINT FK_EditorialBoard_User    FOREIGN KEY(UserId)    REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_EditorialBoard_Journal FOREIGN KEY(JournalId) REFERENCES dbo.Journals(JournalId)
    );
END;
GO

/* ============================================================
   11. 新闻公告表 News
   ============================================================ */
IF OBJECT_ID(N'dbo.News', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.News (
        NewsId      INT IDENTITY(1,1) PRIMARY KEY,
        Title       NVARCHAR(200) NOT NULL,
        Content     NVARCHAR(MAX) NOT NULL,
        PublishedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        AuthorId    INT NOT NULL,
        IsPublished BIT NOT NULL DEFAULT 1,
        CONSTRAINT FK_News_Author FOREIGN KEY(AuthorId) REFERENCES dbo.Users(UserId)
    );
END;
GO

/* ============================================================
   12. 操作日志表 OperationLogs（★与代码一致）
   - 访问路径：/admin/logs/list
   - 关键字段：LogId / CreatedAt（排序用）
   ============================================================ */
IF OBJECT_ID(N'dbo.OperationLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OperationLogs(
        LogId         INT IDENTITY(1,1) PRIMARY KEY,
        ActorUserId   INT NULL,
        ActorUsername NVARCHAR(100) NULL,
        Module        NVARCHAR(100) NOT NULL,
        Action        NVARCHAR(200) NOT NULL,
        Detail        NVARCHAR(MAX) NULL,
        Ip            NVARCHAR(64)  NULL,
        CreatedAt     DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME()
    );

    -- 可选索引：提升日志列表查询性能
    CREATE INDEX IX_OperationLogs_CreatedAt ON dbo.OperationLogs(CreatedAt DESC, LogId DESC);
    CREATE INDEX IX_OperationLogs_ActorUsername ON dbo.OperationLogs(ActorUsername);
END;
GO

/* ============================================================
   13. 其他可选表：若你还在沿用旧脚本的 Files / 状态历史表，可继续保留
   - 当前 src.zip 代码未强依赖 dbo.Files / dbo.ManuscriptStatusHistory
   ============================================================ */
IF OBJECT_ID(N'dbo.Files', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Files (
        FileId       INT IDENTITY(1,1) PRIMARY KEY,
        FileName     NVARCHAR(260) NOT NULL,
        FilePath     NVARCHAR(260) NOT NULL,
        FileType     NVARCHAR(50)  NULL,
        FileSize     BIGINT        NULL,
        UploadTime   DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        UploaderId   INT NOT NULL,
        ManuscriptId INT NULL,
        VersionId    INT NULL,

        CONSTRAINT FK_Files_Uploader   FOREIGN KEY(UploaderId)   REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Files_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_Files_Version    FOREIGN KEY(VersionId)    REFERENCES dbo.ManuscriptVersions(VersionId)
    );
END;
GO

IF OBJECT_ID(N'dbo.ManuscriptStatusHistory', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptStatusHistory (
        HistoryId    BIGINT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId INT NOT NULL,
        FromStatus   NVARCHAR(30)  NULL,
        ToStatus     NVARCHAR(30)  NOT NULL,
        Event        NVARCHAR(50)  NOT NULL,
        ChangedBy    INT NOT NULL,
        ChangeTime   DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        Remark       NVARCHAR(500) NULL,

        CONSTRAINT FK_MSH_Manuscript FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_MSH_ChangedBy  FOREIGN KEY(ChangedBy)    REFERENCES dbo.Users(UserId)
    );

    CREATE INDEX IX_MSH_ManuscriptId ON dbo.ManuscriptStatusHistory(ManuscriptId, ChangeTime DESC);
END;
GO

/* ============================================================
   14. 常用索引（提升列表查询）
   ============================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Users_Username' AND object_id = OBJECT_ID(N'dbo.Users'))
    CREATE INDEX IX_Users_Username ON dbo.Users(Username);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Manuscripts_Status' AND object_id = OBJECT_ID(N'dbo.Manuscripts'))
    CREATE INDEX IX_Manuscripts_Status ON dbo.Manuscripts(Status);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Manuscripts_SubmitterId' AND object_id = OBJECT_ID(N'dbo.Manuscripts'))
    CREATE INDEX IX_Manuscripts_SubmitterId ON dbo.Manuscripts(SubmitterId, ManuscriptId DESC);
GO

PRINT '✅ Online_SMSystem4SP schema initialized (v2025-12-21).';
GO

/* ============================================================
   99. About Journal - Pages & Seed (Aims / Policies / News seed)
   说明：
   - 关于期刊页面的 Aims and scope、Policies and Guidelines 通过 dbo.JournalPages 配置；
   - 若已存在记录则更新（MERGE），便于重复执行；
   - 如 News 表为空，插入 3 条已发布新闻（便于前台展示）。
   ============================================================ */

USE [Online_SMSystem4SP];
GO


/* ======== MERGED: JournalPages seed from journalpages_seed_currentdb.sql (2026-01-05) ======== */

SET NOCOUNT ON;
SET XACT_ABORT ON;

-- 开启常见 SQL Server 行为开关（兼容索引/计算列/过滤索引等）
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
GO

/* ============================================================
   JournalPages Seed (Current DB)
   解决：/guide /publish /about/aims /about/policies 提示
         “JournalPages 中没有对应记录”
   特点：
   - 不硬编码 USE 数据库：对【当前连接的数据库】生效
   - 自动识别 JournalPages 是否含 CoverImagePath / AttachmentPath 列
   - 记录存在则更新，不存在则插入（MERGE），可重复执行
   ============================================================ */

PRINT N'当前数据库：' + DB_NAME();
GO

IF OBJECT_ID(N'dbo.Journals', N'U') IS NULL
BEGIN
    RAISERROR(N'未找到 dbo.Journals 表，请先初始化期刊基础表。', 16, 1);
    RETURN;
END
GO

DECLARE @jid INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId ASC);

IF @jid IS NULL
BEGIN
    RAISERROR(N'dbo.Journals 中没有任何期刊记录，请先插入至少 1 条期刊（建议 JournalId=1）。', 16, 1);
    RETURN;
END

IF OBJECT_ID(N'dbo.JournalPages', N'U') IS NULL
BEGIN
    -- 如果你的项目里 JournalPages 早已存在，这段不会执行
    CREATE TABLE dbo.JournalPages (
        PageId     INT IDENTITY(1,1) PRIMARY KEY,
        JournalId  INT NOT NULL,
        PageKey    NVARCHAR(50) NOT NULL,
        Title      NVARCHAR(200) NOT NULL,
        Content    NVARCHAR(MAX) NOT NULL,
        UpdatedAt  DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_JournalPages_Journal FOREIGN KEY(JournalId) REFERENCES dbo.Journals(JournalId)
    );
    CREATE UNIQUE INDEX UX_JournalPages_Journal_PageKey
        ON dbo.JournalPages(JournalId, PageKey);
END
GO

DECLARE @hasCover BIT = CASE WHEN COL_LENGTH('dbo.JournalPages', 'CoverImagePath') IS NOT NULL THEN 1 ELSE 0 END;
DECLARE @hasAttach BIT = CASE WHEN COL_LENGTH('dbo.JournalPages', 'AttachmentPath') IS NOT NULL THEN 1 ELSE 0 END;

PRINT N'JournalPages 扩展列：CoverImagePath=' + CAST(@hasCover AS NVARCHAR(10)) + N', AttachmentPath=' + CAST(@hasAttach AS NVARCHAR(10));
GO

DECLARE @sql NVARCHAR(MAX) = N'';

-- 为了兼容有/无 CoverImagePath、AttachmentPath 的两种表结构，这里用动态 SQL 拼接 MERGE
SET @sql = N'
DECLARE @jid2 INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId ASC);

;WITH Seed AS (
    SELECT * FROM (VALUES
        (N''publish'',  N''论文发表（Publish）'', N''' + REPLACE(N'
<p>本页介绍稿件从投稿到发表的全流程，帮助作者了解各环节的时间节点、需要提交的材料以及可能的处理结果。</p>
<h3>流程概览</h3>
<ol>
  <li><b>在线投稿</b>：作者提交稿件、作者信息、关键词、附件（正文/图表/补充材料）。</li>
  <li><b>形式审查</b>：编辑部核对格式、完整性与合规性（可进行查重/相似性检测）。</li>
  <li><b>编辑分配</b>：主编/编辑指派责任编辑，确定外审策略与审稿人名单。</li>
  <li><b>同行评审</b>：外审专家提交意见；作者根据意见修回，必要时多轮评审。</li>
  <li><b>终审决策</b>：主编根据评审意见与稿件质量作出最终决定。</li>
  <li><b>出版准备</b>：版面编辑、校对、版权/许可确认、最终稿归档。</li>
  <li><b>上线发布</b>：文章进入已发表列表，生成可引用信息（卷期/页码/DOI 可在后续版本扩展）。</li>
</ol>
<h3>处理结果与常见状态</h3>
<ul>
  <li><b>需修改（Minor/Major Revision）</b>：作者需在截止日期前提交修回稿与回复信。</li>
  <li><b>录用（Accepted）</b>：进入出版流程，等待排期与发布。</li>
  <li><b>退稿（Rejected）</b>：不再进入后续评审流程，系统保留记录以便追溯。</li>
</ul>
<p class="muted">提示：系统中可在“我的稿件 / 审稿进度”查看每个阶段的状态与历史记录。</p>
', "''") + N'''),

        (N''guide'',    N''用户指南（Guide for Authors）'', N''' + REPLACE(N'
<p>本指南汇总投稿准备、写作结构、格式要求与提交清单，帮助作者高效完成投稿。</p>
<h3>投稿准备</h3>
<ul>
  <li>确认研究主题符合期刊范围（Aims &amp; Scope）。</li>
  <li>准备作者信息、单位、基金与通讯作者邮箱。</li>
  <li>整理正文、图表、补充材料与数据/代码链接（如有）。</li>
</ul>
<h3>写作与格式</h3>
<ul>
  <li>摘要包含背景/方法/结果/结论四要素；关键词 3–6 个。</li>
  <li>图表清晰，图题与注释完整；参考文献格式统一。</li>
</ul>
<p class="muted">提示：后台“期刊管理 → 关于期刊页面”可配置本页内容。</p>
', "''") + N'''),

        (N''aims'',     N''论文主旨与投稿范围（Aims and Scope）'', N''' + REPLACE(N'
<p>本期刊聚焦人工智能与数据科学领域的理论创新与工程应用，欢迎具有明确贡献与可复现性的研究工作投稿。</p>
<ul>
  <li>机器学习 / 深度学习</li>
  <li>自然语言处理与大模型</li>
  <li>计算机视觉与多媒体</li>
  <li>数据挖掘与知识图谱</li>
  <li>系统与工程实践（部署/评测/MLOps）</li>
</ul>
', "''") + N'''),

        (N''policies'', N''政策与指南（Policies and Guidelines）'', N''' + REPLACE(N'
<p>以下政策与指南适用于本期刊的投稿、审稿与出版流程。</p>
<ul>
  <li>同行评审：形式审查 → 编辑处理 → 外审 → 终审。</li>
  <li>出版伦理：严禁一稿多投、抄袭、数据伪造/篡改等学术不端。</li>
  <li>查重与相似性检测：编辑部可进行相似性检测，异常可要求解释或退稿。</li>
  <li>数据与代码：鼓励公开数据/代码以提升可复现性。</li>
</ul>
', "''") + N''')
    ) AS V(PageKey, Title, Content)
)
MERGE dbo.JournalPages AS T
USING (SELECT @jid2 AS JournalId, PageKey, Title, Content FROM Seed) AS S
ON (T.JournalId = S.JournalId AND T.PageKey = S.PageKey)
WHEN MATCHED THEN
    UPDATE SET
        T.Title = S.Title,
        T.Content = S.Content,
        T.UpdatedAt = SYSUTCDATETIME()
WHEN NOT MATCHED THEN
    INSERT (JournalId, PageKey, Title, Content' + CASE WHEN 1=1 THEN N'' ELSE N'' END + N')
    VALUES (S.JournalId, S.PageKey, S.Title, S.Content' + CASE WHEN 1=1 THEN N'' ELSE N'' END + N');
';

-- 如果表里有 CoverImagePath/AttachmentPath，并且它们是 NOT NULL 且无默认值，
-- 上面的 INSERT 可能失败；因此这里在拼接时，把这两列也塞进去（赋 NULL），最大化兼容。
IF COL_LENGTH('dbo.JournalPages', 'CoverImagePath') IS NOT NULL
BEGIN
    SET @sql = REPLACE(@sql,
        N'INSERT (JournalId, PageKey, Title, Content)',
        N'INSERT (JournalId, PageKey, Title, Content, CoverImagePath)');
    SET @sql = REPLACE(@sql,
        N'VALUES (S.JournalId, S.PageKey, S.Title, S.Content)',
        N'VALUES (S.JournalId, S.PageKey, S.Title, S.Content, NULL)');
END

IF COL_LENGTH('dbo.JournalPages', 'AttachmentPath') IS NOT NULL
BEGIN
    SET @sql = REPLACE(@sql,
        N'INSERT (JournalId, PageKey, Title, Content, CoverImagePath)',
        N'INSERT (JournalId, PageKey, Title, Content, CoverImagePath, AttachmentPath)');
    SET @sql = REPLACE(@sql,
        N'VALUES (S.JournalId, S.PageKey, S.Title, S.Content, NULL)',
        N'VALUES (S.JournalId, S.PageKey, S.Title, S.Content, NULL, NULL)');

    -- 如果没有 CoverImagePath，只有 AttachmentPath
    SET @sql = REPLACE(@sql,
        N'INSERT (JournalId, PageKey, Title, Content)',
        N'INSERT (JournalId, PageKey, Title, Content, AttachmentPath)');
    SET @sql = REPLACE(@sql,
        N'VALUES (S.JournalId, S.PageKey, S.Title, S.Content)',
        N'VALUES (S.JournalId, S.PageKey, S.Title, S.Content, NULL)');
END

EXEC sp_executesql @sql;
GO

PRINT N'✅ 已写入/更新 JournalPages：publish / guide / aims / policies';
PRINT N'   你可以用下面语句验证：';
PRINT N'   SELECT JournalId, PageKey, Title, UpdatedAt FROM dbo.JournalPages ORDER BY UpdatedAt DESC;';
GO

/* ======== END MERGED JournalPages seed ======== */

/* 99.3 初始化 News（若 News 表为空） */
IF OBJECT_ID(N'dbo.News', N'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.News)
    BEGIN
        DECLARE @authorId INT =
            COALESCE(
                (SELECT TOP 1 UserId FROM dbo.Users WHERE Username = N'eoadmin'),
                (SELECT TOP 1 UserId FROM dbo.Users WHERE Username = N'admin'),
                (SELECT TOP 1 UserId FROM dbo.Users ORDER BY UserId ASC)
            );

        IF @authorId IS NOT NULL
        BEGIN
            INSERT dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished)
            VALUES
              (N'期刊系统上线公告',
               N'本期刊在线投稿与审稿系统已上线，欢迎作者注册并提交稿件。',
               SYSUTCDATETIME(), @authorId, 1),
              (N'征稿通知：AI 与可复现研究专题',
               N'本期刊开设专题：AI 与可复现研究（Special Issue），欢迎相关工作投稿。',
               DATEADD(DAY, -3, SYSUTCDATETIME()), @authorId, 1),
              (N'审稿人招募',
               N'期刊长期招募审稿人，欢迎具有相关研究背景的学者加入审稿人库。',
               DATEADD(DAY, -10, SYSUTCDATETIME()), @authorId, 1);
        END
    END
END
GO

PRINT N'? Online_SMSystem4SP full schema + AboutJournal seed initialized (v2025-12-21).';
GO

/* ============================================================
   ADD-ON (2025-12-22): Issues + CallForPapers + minimal seeds
   This section is appended to your original sqlserver.sql.
   It does NOT modify or delete your original data.
   It only:
     - Creates dbo.Issues, dbo.CallForPapers if missing
     - Inserts sample rows only if missing
     - Inserts aims/policies pages ONLY IF they don't exist (no overwrite)
   ============================================================ */

USE [Online_SMSystem4SP];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* 0) Get JournalId (your original script inserts at least one journal). */
DECLARE @JournalId INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);
IF @JournalId IS NULL
BEGIN
    -- Extremely defensive fallback: only if original seed was removed.
    INSERT INTO dbo.Journals(Name, Description, ImpactFactor, Timeline, ISSN, CreatedBy)
    VALUES (N'Default Journal', N'Auto-created for Issues/Calls.', NULL, NULL, NULL, NULL);
    SET @JournalId = SCOPE_IDENTITY();
END
GO

/* 1) Issues */
IF OBJECT_ID(N'dbo.Issues', N'U') IS NULL
BEGIN
    PRINT N'Creating dbo.Issues...';

    CREATE TABLE dbo.Issues (
        IssueId      INT            IDENTITY(1,1) NOT NULL PRIMARY KEY,
        JournalId    INT            NOT NULL,
        IssueType    NVARCHAR(20)   NOT NULL, -- LATEST / SPECIAL
        Title        NVARCHAR(300)  NOT NULL,
        Volume       INT            NULL,
        Number       INT            NULL,
        [Year]       INT            NULL,
        Description  NVARCHAR(MAX)  NULL,
        IsPublished  BIT            NOT NULL CONSTRAINT DF_Issues_IsPublished DEFAULT(0),
        PublishDate  DATE           NULL,
        CreatedAt    DATETIME2(0)   NOT NULL CONSTRAINT DF_Issues_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Issues_Journals FOREIGN KEY (JournalId) REFERENCES dbo.Journals(JournalId),
        CONSTRAINT CK_Issues_IssueType CHECK (IssueType IN (N'LATEST', N'SPECIAL'))
    );

    CREATE INDEX IX_Issues_Journal_Type_Published
        ON dbo.Issues(JournalId, IssueType, IsPublished, PublishDate);
END
GO

/* Seed issues: only insert if there are no issues for this journal. */
DECLARE @jid1 INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);
IF NOT EXISTS (SELECT 1 FROM dbo.Issues WHERE JournalId = @jid1)
BEGIN
    PRINT N'Seeding sample Issues...';
    INSERT INTO dbo.Issues(JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate)
    VALUES
      (@jid1, N'LATEST',  N'Latest Issues - Vol.1 No.1', 1, 1, YEAR(GETDATE()), N'（示例）最新一期', 1, CONVERT(date, DATEADD(day,-21,GETDATE()))),
      (@jid1, N'SPECIAL', N'Special Issue: AI in Publishing', NULL, NULL, YEAR(GETDATE()), N'（示例）专刊：AI 与学术出版', 1, CONVERT(date, DATEADD(day,-45,GETDATE())));
END
GO

/* 2) CallForPapers */
IF OBJECT_ID(N'dbo.CallForPapers', N'U') IS NULL
BEGIN
    PRINT N'Creating dbo.CallForPapers...';

    CREATE TABLE dbo.CallForPapers (
        CallId      INT           IDENTITY(1,1) NOT NULL PRIMARY KEY,
        JournalId   INT           NOT NULL,
        Title       NVARCHAR(300) NOT NULL,
        Content     NVARCHAR(MAX) NOT NULL, -- HTML allowed
        StartDate   DATE          NULL,
        Deadline    DATE          NULL,
        EndDate     DATE          NULL,
        IsPublished BIT           NOT NULL CONSTRAINT DF_CallForPapers_IsPublished DEFAULT(1),
        CreatedAt   DATETIME2(0)  NOT NULL CONSTRAINT DF_CallForPapers_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_CallForPapers_Journals FOREIGN KEY (JournalId) REFERENCES dbo.Journals(JournalId)
    );

    CREATE INDEX IX_CallForPapers_Journal_Published
        ON dbo.CallForPapers(JournalId, IsPublished, EndDate, Deadline);
END
GO

/* Seed call: only if none for this journal. */
DECLARE @jid2 INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);
IF NOT EXISTS (SELECT 1 FROM dbo.CallForPapers WHERE JournalId = @jid2)
BEGIN
    PRINT N'Seeding sample Call for Papers...';
    INSERT INTO dbo.CallForPapers(JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished)
    VALUES
    (@jid2,
     N'Call for Papers: Special Issue on AI',
     N'<p>（示例）欢迎投稿 AI 相关专题。请按作者指南准备材料，并通过系统提交稿件。</p>',
     CONVERT(date, DATEADD(day,-30,GETDATE())),
     CONVERT(date, DATEADD(day, 60,GETDATE())),
     CONVERT(date, DATEADD(day, 90,GETDATE())),
     1);
END
GO

/* 3) JournalPages: Insert aims/policies only if missing (no overwrite). */
IF OBJECT_ID(N'dbo.JournalPages', N'U') IS NOT NULL
BEGIN
    DECLARE @jid3 INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);

    IF NOT EXISTS (SELECT 1 FROM dbo.JournalPages WHERE JournalId=@jid3 AND PageKey=N'aims')
    BEGIN
        INSERT INTO dbo.JournalPages(JournalId, PageKey, Title, Content)
        VALUES (@jid3, N'aims', N'About the Journal',
                N'<h3>About the Journal</h3><p>（示例内容）请在后台维护该页面内容。</p>');
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.JournalPages WHERE JournalId=@jid3 AND PageKey=N'policies')
    BEGIN
        INSERT INTO dbo.JournalPages(JournalId, PageKey, Title, Content)
        VALUES (@jid3, N'policies', N'Ethics and Policies',
                N'<h3>Ethics and Policies</h3><p>（示例内容）请在后台维护该页面内容。</p>');
    END
END
GO

-- news_feature_patch.sql
-- 说明：在原有 Online_SMSystem4SP 数据库基础上，为“发布新闻 / 更新期刊公告”增加：
-- 1）支持定时发布（PublishedAt 允许为空，由业务逻辑控制具体发布时间）
-- 2）支持上传附件（AttachmentPath 字段存储附件相对路径）

IF OBJECT_ID(N'dbo.News', N'U') IS NOT NULL
BEGIN
    PRINT '>> Patching dbo.News for scheduled publish & attachment support...';

    -- 1. 确保 PublishedAt 允许为 NULL（用于草稿或尚未到发布时间的记录）
    BEGIN TRY
        ALTER TABLE dbo.News ALTER COLUMN PublishedAt DATETIME2(0) NULL;
        PRINT ' - Column PublishedAt altered to DATETIME2(0) NULL.';
    END TRY
    BEGIN CATCH
        PRINT ' - Skip altering PublishedAt (可能已为 NULL 或存在依赖)。';
    END CATCH;

    -- 2. 如不存在 AttachmentPath 字段，则新增
    IF COL_LENGTH('dbo.News', 'AttachmentPath') IS NULL
    BEGIN
        ALTER TABLE dbo.News ADD AttachmentPath NVARCHAR(500) NULL;
        PRINT ' - Column AttachmentPath(NVARCHAR(500) NULL) added.';
    END
    ELSE
    BEGIN
        PRINT ' - Column AttachmentPath already exists, skip.';
    END
END
ELSE
BEGIN
    PRINT '!! dbo.News 不存在，请先执行原始 sqlserver.sql 初始化数据库。';
END;

/* ============================================================
   EXTRA PATCH APPENDED
   Purpose: Keep original database schema/data exactly the same as sqlserver.sql,
            then add journal-management board enhancements (cover/attachment columns, guest editors, etc.)
   Generated: 2025-12-23 13:08:58
   ============================================================ */

-- Ensure DB exists, then switch context
IF DB_ID(N'Online_SMSystem4SP') IS NULL
BEGIN
    THROW 50000, 'Database Online_SMSystem4SP not found. Run the base script section first.', 1;
END
GO
USE [Online_SMSystem4SP];
GO
-- Patch: Journal management board columns (cover/attachment + guest editors)
-- Safe to run multiple times.
-- Target DB: SQL Server, schema dbo.

PRINT '== Patch start: journal management board columns ==';

-- JournalPages: add resource columns
IF COL_LENGTH('dbo.JournalPages', 'CoverImagePath') IS NULL
BEGIN
    ALTER TABLE dbo.JournalPages ADD CoverImagePath NVARCHAR(255) NULL;
    PRINT 'Added dbo.JournalPages.CoverImagePath';
END
ELSE PRINT 'dbo.JournalPages.CoverImagePath already exists';

IF COL_LENGTH('dbo.JournalPages', 'AttachmentPath') IS NULL
BEGIN
    ALTER TABLE dbo.JournalPages ADD AttachmentPath NVARCHAR(255) NULL;
    PRINT 'Added dbo.JournalPages.AttachmentPath';
END
ELSE PRINT 'dbo.JournalPages.AttachmentPath already exists';

-- Issues: add GuestEditors + resource columns
IF COL_LENGTH('dbo.Issues', 'GuestEditors') IS NULL
BEGIN
    ALTER TABLE dbo.Issues ADD GuestEditors NVARCHAR(255) NULL;
    PRINT 'Added dbo.Issues.GuestEditors';
END
ELSE PRINT 'dbo.Issues.GuestEditors already exists';

IF COL_LENGTH('dbo.Issues', 'CoverImagePath') IS NULL
BEGIN
    ALTER TABLE dbo.Issues ADD CoverImagePath NVARCHAR(255) NULL;
    PRINT 'Added dbo.Issues.CoverImagePath';
END
ELSE PRINT 'dbo.Issues.CoverImagePath already exists';

IF COL_LENGTH('dbo.Issues', 'AttachmentPath') IS NULL
BEGIN
    ALTER TABLE dbo.Issues ADD AttachmentPath NVARCHAR(255) NULL;
    PRINT 'Added dbo.Issues.AttachmentPath';
END
ELSE PRINT 'dbo.Issues.AttachmentPath already exists';

-- CallForPapers: add resource columns
IF COL_LENGTH('dbo.CallForPapers', 'CoverImagePath') IS NULL
BEGIN
    ALTER TABLE dbo.CallForPapers ADD CoverImagePath NVARCHAR(255) NULL;
    PRINT 'Added dbo.CallForPapers.CoverImagePath';
END
ELSE PRINT 'dbo.CallForPapers.CoverImagePath already exists';

IF COL_LENGTH('dbo.CallForPapers', 'AttachmentPath') IS NULL
BEGIN
    ALTER TABLE dbo.CallForPapers ADD AttachmentPath NVARCHAR(255) NULL;
    PRINT 'Added dbo.CallForPapers.AttachmentPath';
END
ELSE PRINT 'dbo.CallForPapers.AttachmentPath already exists';

PRINT '== Patch end: journal management board columns ==';

/*
 * =========================
 * Patch: Notifications (In-App)
 * =========================
 */
IF OBJECT_ID('dbo.Notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications(
        NotificationId INT IDENTITY(1,1) PRIMARY KEY,
        RecipientUserId INT NOT NULL,
        CreatedByUserId INT NULL,
        Type NVARCHAR(20) NOT NULL DEFAULT N'SYSTEM',
        Category NVARCHAR(50) NULL,
        Title NVARCHAR(200) NOT NULL,
        Content NVARCHAR(MAX) NULL,
        RelatedManuscriptId INT NULL,
        IsRead BIT NOT NULL DEFAULT 0,
        ReadAt DATETIME2(0) NULL,
        CreatedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Notifications_Recipient FOREIGN KEY(RecipientUserId) REFERENCES dbo.Users(UserId)
    );

    CREATE INDEX IX_Notifications_Recipient_Read ON dbo.Notifications(RecipientUserId, IsRead, CreatedAt DESC, NotificationId DESC);
    PRINT 'Created dbo.Notifications';
END
ELSE PRINT 'dbo.Notifications already exists';

PRINT '== Patch end: notifications ==';
PRINT '== Patch begin: unique email constraint on dbo.Users.Email ==';

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_Users_Email'
      AND object_id = OBJECT_ID(N'dbo.Users')
)
BEGIN
    -- 为 Email 创建唯一索引（仅对非 NULL 值生效），保证同一邮箱只能注册一个账号
    CREATE UNIQUE NONCLUSTERED INDEX UX_Users_Email
        ON dbo.Users(Email)
        WHERE Email IS NOT NULL;
    PRINT 'Created unique index UX_Users_Email on dbo.Users(Email).';
END
ELSE
    PRINT 'Index UX_Users_Email already exists.';

PRINT '== Patch end: unique email constraint on dbo.Users.Email ==';
GO

/* ============================================================
   稿件阶段时间戳表 ManuscriptStageTimestamps
   用于记录每份稿件在各审稿阶段的完成时间
   Created: 2025-12-26
   ============================================================ */

PRINT '== Patch begin: ManuscriptStageTimestamps ==';

IF OBJECT_ID(N'dbo.ManuscriptStageTimestamps', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptStageTimestamps (
        ManuscriptId                    INT PRIMARY KEY,
        DraftCompletedAt                DATETIME2(0) NULL,      -- 草稿编辑完成时间
        SubmittedAt                     DATETIME2(0) NULL,      -- 已提交待处理完成时间
        FormalCheckCompletedAt          DATETIME2(0) NULL,      -- 形式审查完成时间
        DeskReviewInitialCompletedAt    DATETIME2(0) NULL,      -- 案头初筛完成时间
        ToAssignCompletedAt             DATETIME2(0) NULL,      -- 待分配编辑完成时间
        WithEditorCompletedAt           DATETIME2(0) NULL,      -- 编辑处理完成时间
        UnderReviewCompletedAt          DATETIME2(0) NULL,      -- 外审完成时间
        EditorRecommendationCompletedAt DATETIME2(0) NULL,      -- 编辑推荐意见完成时间
        FinalDecisionPendingCompletedAt DATETIME2(0) NULL,      -- 待主编终审完成时间
        
        CONSTRAINT FK_MST_Manuscript 
            FOREIGN KEY(ManuscriptId) 
            REFERENCES dbo.Manuscripts(ManuscriptId)
    );
    
    PRINT 'Created dbo.ManuscriptStageTimestamps';
END
ELSE
    PRINT 'dbo.ManuscriptStageTimestamps already exists';

PRINT '== Patch end: ManuscriptStageTimestamps ==';
GO

PRINT '== Patch: EditorSuggestions (编辑建议 + 总结报告) ==';

IF OBJECT_ID(N'dbo.EditorSuggestions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.EditorSuggestions (
        ManuscriptId INT NOT NULL PRIMARY KEY,
        EditorId INT NOT NULL,
        Suggestion NVARCHAR(50) NOT NULL,
        Summary NVARCHAR(MAX) NULL,
        SubmittedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),

        CONSTRAINT FK_EditorSuggestions_Manuscript FOREIGN KEY(ManuscriptId)
            REFERENCES dbo.Manuscripts(ManuscriptId),

        CONSTRAINT FK_EditorSuggestions_Editor FOREIGN KEY(EditorId)
            REFERENCES dbo.Users(UserId)
    );

    CREATE INDEX IX_EditorSuggestions_EditorId ON dbo.EditorSuggestions(EditorId);

    PRINT 'Created dbo.EditorSuggestions';
END
ELSE
    PRINT 'dbo.EditorSuggestions already exists';

PRINT '== Patch end: EditorSuggestions ==';
GO

/* ============================================================
   Patch: ArticleMetrics + FormalCheckResults（补齐缺失表）
   Created: 2026-01-02
   说明：
   - 解决 PublicArticleServlet / ManuscriptDAO 依赖 dbo.ArticleMetrics 时报 “对象名无效”
   - 合并形式审查功能所需 dbo.FormalCheckResults（使用 CheckResultId 主键）
   ============================================================ */

PRINT '== Patch begin: ArticleMetrics ==';

IF OBJECT_ID(N'dbo.ArticleMetrics', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ArticleMetrics (
        ManuscriptId     INT NOT NULL PRIMARY KEY,
        ViewCount        INT NOT NULL DEFAULT 0,
        DownloadCount    INT NOT NULL DEFAULT 0,
        CitationCount    INT NOT NULL DEFAULT 0,
        PopularityScore  FLOAT NOT NULL DEFAULT 0,
        UpdatedAt        DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),

        CONSTRAINT FK_ArticleMetrics_Manuscripts
            FOREIGN KEY (ManuscriptId)
            REFERENCES dbo.Manuscripts(ManuscriptId)
            ON DELETE CASCADE
    );

    CREATE INDEX IX_ArticleMetrics_UpdatedAt ON dbo.ArticleMetrics(UpdatedAt DESC);

    PRINT 'Created dbo.ArticleMetrics';
END
ELSE
    PRINT 'dbo.ArticleMetrics already exists';

PRINT '== Patch end: ArticleMetrics ==';
GO

PRINT '== Patch begin: FormalCheckResults ==';

-- 兼容：若旧库的 Manuscripts 缺少 LastStatusTime，则补上（新建库脚本一般已包含）
IF OBJECT_ID(N'dbo.Manuscripts', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.Manuscripts', 'LastStatusTime') IS NULL
    BEGIN
        ALTER TABLE dbo.Manuscripts
            ADD LastStatusTime DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME();

        PRINT 'Added LastStatusTime to dbo.Manuscripts';
    END
END
GO

IF OBJECT_ID(N'dbo.FormalCheckResults', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.FormalCheckResults (
        CheckResultId           INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId            INT NOT NULL,
        ReviewerId              INT NOT NULL,
        CheckTime               DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CheckResult             NVARCHAR(10) NULL,
        AuthorInfoValid         BIT NULL,
        AbstractWordCountValid  BIT NULL,
        BodyWordCountValid      BIT NULL,
        KeywordsValid           BIT NULL,
        FootnoteNumberingValid  BIT NULL,
        FigureTableFormatValid  BIT NULL,
        ReferenceFormatValid    BIT NULL,
        SimilarityScore         DECIMAL(5,2) NULL,
        HighSimilarity          BIT NULL,
        PlagiarismReportUrl     NVARCHAR(500) NULL,
        Feedback                NVARCHAR(MAX) NULL,

        CONSTRAINT FK_FormalCheckResults_Manuscripts
            FOREIGN KEY(ManuscriptId)
            REFERENCES dbo.Manuscripts(ManuscriptId),

        CONSTRAINT FK_FormalCheckResults_Users
            FOREIGN KEY(ReviewerId)
            REFERENCES dbo.Users(UserId),

        CONSTRAINT CK_FormalCheckResults_CheckResult
            CHECK (CheckResult IN (N'PASS', N'FAIL'))
    );

    CREATE INDEX IX_FormalCheckResults_ManuscriptId ON dbo.FormalCheckResults(ManuscriptId, CheckTime DESC);
    CREATE INDEX IX_FormalCheckResults_ReviewerId ON dbo.FormalCheckResults(ReviewerId, CheckTime DESC);

    PRINT 'Created dbo.FormalCheckResults';
END
ELSE
    PRINT 'dbo.FormalCheckResults already exists';

PRINT '== Patch end: FormalCheckResults ==';
GO

-- ============================================================
-- 以下内容来自：sync_formal_check_database.sql
-- ============================================================
GO

-- ============================================================
-- 形式审查功能数据库同步脚本
-- 执行日期: 2025-12-26
-- 说明: 此脚本用于同步形式审查功能相关的数据库变更
-- ============================================================

USE Online_SMSystem4SP;
GO

PRINT '============================================================';
PRINT '开始执行形式审查功能数据库同步脚本...';
PRINT '============================================================';
GO

/* ============================================================
   1. 检查并添加 LastStatusTime 字段到 Manuscripts 表
   ============================================================ */
PRINT '检查 Manuscripts 表的 LastStatusTime 字段...';
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Manuscripts') AND name = 'LastStatusTime')
BEGIN
    ALTER TABLE dbo.Manuscripts ADD LastStatusTime DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME();
    PRINT '✅ 已添加字段 LastStatusTime 到 Manuscripts 表';
END
ELSE
BEGIN
    PRINT 'ℹ️  字段 LastStatusTime 已存在，跳过添加';
END
GO

/* ============================================================
   2. 创建 FormalCheckResults 表（形式审查结果表）
   ============================================================ */
PRINT '检查 FormalCheckResults 表是否存在...';
IF NOT EXISTS (SELECT * FROM sys.tables WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults'))
BEGIN
    CREATE TABLE dbo.FormalCheckResults (
        CheckResultId           INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId            INT NOT NULL,
        ReviewerId              INT NOT NULL,
        CheckTime               DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CheckResult             NVARCHAR(10) NULL,
        AuthorInfoValid         BIT NULL,
        AbstractWordCountValid  BIT NULL,
        BodyWordCountValid      BIT NULL,
        KeywordsValid           BIT NULL,
        FootnoteNumberingValid  BIT NULL,
        FigureTableFormatValid  BIT NULL,
        ReferenceFormatValid    BIT NULL,
        SimilarityScore         DECIMAL(5,2) NULL,
        HighSimilarity          BIT NULL,
        PlagiarismReportUrl     NVARCHAR(500) NULL,
        Feedback                NVARCHAR(MAX) NULL,

        CONSTRAINT FK_FormalCheckResults_Manuscripts FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_FormalCheckResults_Users FOREIGN KEY(ReviewerId) REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_FormalCheckResults_CheckResult CHECK (CheckResult IN (N'PASS', N'FAIL'))
    );

    CREATE INDEX IX_FormalCheckResults_ManuscriptId ON dbo.FormalCheckResults(ManuscriptId, CheckTime DESC);
    CREATE INDEX IX_FormalCheckResults_ReviewerId ON dbo.FormalCheckResults(ReviewerId, CheckTime DESC);

    PRINT '✅ 已创建表 FormalCheckResults';
END
ELSE
BEGIN
    PRINT 'ℹ️  表 FormalCheckResults 已存在，跳过创建';
END
GO

/* ============================================================
   3. 添加查重相关字段到 FormalCheckResults 表
   ============================================================ */
PRINT '检查并添加查重相关字段...';

-- 添加查重率字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults') AND name = 'SimilarityScore')
BEGIN
    ALTER TABLE dbo.FormalCheckResults ADD SimilarityScore DECIMAL(5,2) NULL;
    PRINT '✅ 已添加字段 SimilarityScore';
END
ELSE
BEGIN
    PRINT 'ℹ️  字段 SimilarityScore 已存在，跳过添加';
END
GO

-- 添加高相似度标记字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults') AND name = 'HighSimilarity')
BEGIN
    ALTER TABLE dbo.FormalCheckResults ADD HighSimilarity BIT NULL;
    PRINT '✅ 已添加字段 HighSimilarity';
END
ELSE
BEGIN
    PRINT 'ℹ️  字段 HighSimilarity 已存在，跳过添加';
END
GO

-- 添加查重报告URL字段
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults') AND name = 'PlagiarismReportUrl')
BEGIN
    ALTER TABLE dbo.FormalCheckResults ADD PlagiarismReportUrl NVARCHAR(500) NULL;
    PRINT '✅ 已添加字段 PlagiarismReportUrl';
END
ELSE
BEGIN
    PRINT 'ℹ️  字段 PlagiarismReportUrl 已存在，跳过添加';
END
GO

/* ============================================================
   4. 更新 Manuscripts 表的状态约束
   ============================================================ */
PRINT '检查并更新 Manuscripts 表的状态约束...';

-- 首先删除旧的状态约束（如果存在）
IF EXISTS (SELECT * FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Manuscripts') AND name = N'CK_Manuscripts_Status')
BEGIN
    ALTER TABLE dbo.Manuscripts DROP CONSTRAINT CK_Manuscripts_Status;
    PRINT 'ℹ️  已删除旧的状态约束 CK_Manuscripts_Status';
END
GO

-- 添加新的状态约束（包含 FORMAL_CHECK 和 RETURNED，移除 Incomplete Submission）
ALTER TABLE dbo.Manuscripts
ADD CONSTRAINT CK_Manuscripts_Status CHECK (Status IN (
    N'DRAFT',
    N'SUBMITTED',
    N'FORMAL_CHECK',        -- 新增：形式审查中
    N'RETURNED',            -- 新增：已退回
    N'DESK_REVIEW_INITIAL',
    N'TO_ASSIGN',
    N'WITH_EDITOR',
    N'UNDER_REVIEW',
    N'EDITOR_RECOMMENDATION',
    N'FINAL_DECISION_PENDING',
    N'REVISION',
    N'ACCEPTED',
    N'REJECTED',
    N'ARCHIVED'
));
PRINT '✅ 已更新状态约束 CK_Manuscripts_Status';
GO

/* ============================================================
   5. 更新状态为"Incomplete Submission"的稿件为"RETURNED"
   ============================================================ */
PRINT '检查并更新状态为"Incomplete Submission"的稿件...';
DECLARE @updateCount INT;

UPDATE dbo.Manuscripts
SET Status = 'RETURNED',
    LastStatusTime = SYSUTCDATETIME()
WHERE Status = 'Incomplete Submission';

SET @updateCount = @@ROWCOUNT;

IF @updateCount > 0
BEGIN
    PRINT '✅ 已将 ' + CAST(@updateCount AS NVARCHAR(10)) + ' 条记录的状态从"Incomplete Submission"更新为"RETURNED"';
END
ELSE
BEGIN
    PRINT 'ℹ️  没有找到状态为"Incomplete Submission"的记录，跳过更新';
END
GO

/* ============================================================
   6. 验证数据库结构
   ============================================================ */
PRINT '============================================================';
PRINT '验证数据库结构...';
PRINT '============================================================';

-- 验证 FormalCheckResults 表
IF EXISTS (SELECT * FROM sys.tables WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults'))
BEGIN
    PRINT '✅ 表 FormalCheckResults 存在';

    -- 验证字段
    IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults') AND name = 'SimilarityScore')
        PRINT '✅ 字段 SimilarityScore 存在';
    ELSE
        PRINT '❌ 字段 SimilarityScore 不存在';

    IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults') AND name = 'HighSimilarity')
        PRINT '✅ 字段 HighSimilarity 存在';
    ELSE
        PRINT '❌ 字段 HighSimilarity 不存在';

    IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.FormalCheckResults') AND name = 'PlagiarismReportUrl')
        PRINT '✅ 字段 PlagiarismReportUrl 存在';
    ELSE
        PRINT '❌ 字段 PlagiarismReportUrl 不存在';
END
ELSE
BEGIN
    PRINT '❌ 表 FormalCheckResults 不存在';
END

-- 验证 Manuscripts 表的 LastStatusTime 字段
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Manuscripts') AND name = 'LastStatusTime')
    PRINT '✅ Manuscripts 表的 LastStatusTime 字段存在';
ELSE
    PRINT '❌ Manuscripts 表的 LastStatusTime 字段不存在';

-- 验证 Manuscripts 表的状态约束
IF EXISTS (SELECT * FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Manuscripts') AND name = N'CK_Manuscripts_Status')
    PRINT '✅ Manuscripts 表的状态约束 CK_Manuscripts_Status 存在';
ELSE
    PRINT '❌ Manuscripts 表的状态约束 CK_Manuscripts_Status 不存在';

GO

/* ============================================================
   7. 显示当前数据库状态
   ============================================================ */
PRINT '============================================================';
PRINT '当前数据库状态统计';
PRINT '============================================================';

-- 统计稿件数量
SELECT
    Status AS '稿件状态',
    COUNT(*) AS '数量'
FROM dbo.Manuscripts
GROUP BY Status
ORDER BY Status;
GO

-- 统计形式审查结果数量
SELECT
    COUNT(*) AS '形式审查结果总数'
FROM dbo.FormalCheckResults;
GO

PRINT '============================================================';
PRINT '形式审查功能数据库同步脚本执行完成！';
PRINT '============================================================';
GO

/* =========================================================
   用户菜单入口权限表（可选增强）
   兼容旧库：如果表已存在但缺少 Granted 列，则补齐。
   ========================================================= */
IF OBJECT_ID('dbo.UserMenuPermissions','U') IS NULL
BEGIN
    CREATE TABLE dbo.UserMenuPermissions(
        UserId INT NOT NULL,
        PermissionKey NVARCHAR(100) NOT NULL,
        Granted BIT NOT NULL CONSTRAINT DF_UserMenuPermissions_Granted DEFAULT(1),
        CONSTRAINT PK_UserMenuPermissions PRIMARY KEY(UserId, PermissionKey)
    );
END
ELSE
BEGIN
    IF COL_LENGTH('dbo.UserMenuPermissions','Granted') IS NULL
    BEGIN
        ALTER TABLE dbo.UserMenuPermissions
        ADD Granted BIT NOT NULL CONSTRAINT DF_UserMenuPermissions_Granted DEFAULT(1);
    END
END
GO


/* ============================================================
   DEMO CONTENT ENHANCEMENT (Homepage + TopNav)
   Purpose:
     - Make homepage content non-empty (Journal / Editorial board / News / Calls / Issues / Articles)
     - Provide more "mature" demo dataset for project showcasing
   Safe to run multiple times:
     - Uses NOT EXISTS checks
     - Updates journal description only when it looks like a placeholder
   ============================================================ */
GO
USE [Online_SMSystem4SP];
GO

/* 1) Enrich Journal basic info (only if current looks like placeholder) */
DECLARE @DemoJournalId INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);
IF @DemoJournalId IS NOT NULL
BEGIN
    UPDATE dbo.Journals
       SET Name = CASE WHEN Name IS NULL OR LTRIM(RTRIM(Name)) = N'' THEN N'International Artificial Intelligence Research' ELSE Name END,
           Timeline = CASE WHEN Timeline IS NULL OR LTRIM(RTRIM(Timeline)) = N'' THEN N'First decision: ~4 weeks · Accept to online: ~2 weeks · Review model: single-blind / double-blind optional' ELSE Timeline END,
           ISSN = CASE WHEN ISSN IS NULL OR LTRIM(RTRIM(ISSN)) = N'' THEN N'1234-5678' ELSE ISSN END,
           ImpactFactor = COALESCE(ImpactFactor, 5.123),
           Description = CASE
                            WHEN Description IS NULL
                              OR Description LIKE N'%课程设计示例期刊%'
                              OR LEN(LTRIM(RTRIM(Description))) < 60
                            THEN
N'International Artificial Intelligence Research（IAIR）聚焦人工智能与数据科学领域的原创研究与工程实践，覆盖机器学习、自然语言处理、计算机视觉、知识图谱、可解释与可信 AI、数据治理与可复现研究等方向。
本刊倡导严格同行评审与研究透明性，鼓励作者提供数据与代码以提升可复现性。我们为作者提供清晰的投稿指南、出版伦理与利益冲突声明规范，并支持在线投稿、审稿、修回与终审流程的全链路管理。'
                            ELSE Description
                         END
     WHERE JournalId = @DemoJournalId;
END
GO

/* 2) Add extra demo users (editorial board candidates) */
DECLARE @Role_EDITOR INT = (SELECT TOP 1 RoleId FROM dbo.Roles WHERE RoleCode=N'EDITOR');
DECLARE @Role_REVIEWER INT = (SELECT TOP 1 RoleId FROM dbo.Roles WHERE RoleCode=N'REVIEWER');

IF @Role_EDITOR IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username=N'editor2')
        INSERT dbo.Users(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
        VALUES (N'editor2', N'password123', N'editor2@example.com', N'编辑2', N'清华大学 · 自动化系', N'自然语言处理', @Role_EDITOR, N'ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username=N'editor3')
        INSERT dbo.Users(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
        VALUES (N'editor3', N'password123', N'editor3@example.com', N'编辑3', N'北京大学 · 计算机学院', N'计算机视觉', @Role_EDITOR, N'ACTIVE');
END
GO

IF @Role_REVIEWER IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username=N'reviewer2')
        INSERT dbo.Users(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
        VALUES (N'reviewer2', N'password123', N'reviewer2@example.com', N'审稿人2', N'香港科技大学', N'可解释 AI', @Role_REVIEWER, N'ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username=N'reviewer3')
        INSERT dbo.Users(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
        VALUES (N'reviewer3', N'password123', N'reviewer3@example.com', N'审稿人3', N'上海交通大学', N'知识图谱', @Role_REVIEWER, N'ACTIVE');

    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Username=N'reviewer4')
        INSERT dbo.Users(Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
        VALUES (N'reviewer4', N'password123', N'reviewer4@example.com', N'审稿人4', N'浙江大学', N'数据治理与隐私计算', @Role_REVIEWER, N'ACTIVE');
END
GO

/* 3) Seed EditorialBoard (only insert missing rows) */
IF OBJECT_ID(N'dbo.EditorialBoard', N'U') IS NOT NULL
BEGIN
    DECLARE @jidEB INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);

    DECLARE @u_eic INT = (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'eic');
    DECLARE @u_eoadmin INT = (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'eoadmin');
    DECLARE @u_editor1 INT = (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'editor1');
    DECLARE @u_editor2 INT = (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'editor2');
    DECLARE @u_editor3 INT = (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'editor3');

    IF @jidEB IS NOT NULL
    BEGIN
        IF @u_eic IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.EditorialBoard WHERE JournalId=@jidEB AND UserId=@u_eic)
            INSERT dbo.EditorialBoard(UserId, JournalId, Position, Section, Bio)
            VALUES (@u_eic, @jidEB, N'Editor-in-Chief', N'Overall',
                    N'研究方向：可信 AI、机器学习系统与学术出版。曾任多项国际会议程序委员会成员，关注审稿质量与研究透明性。');

        IF @u_eoadmin IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.EditorialBoard WHERE JournalId=@jidEB AND UserId=@u_eoadmin)
            INSERT dbo.EditorialBoard(UserId, JournalId, Position, Section, Bio)
            VALUES (@u_eoadmin, @jidEB, N'Managing Editor', N'Editorial Office',
                    N'负责稿件形式审查、出版流程与作者沟通。熟悉出版伦理、版权与开放科学实践。');

        IF @u_editor1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.EditorialBoard WHERE JournalId=@jidEB AND UserId=@u_editor1)
            INSERT dbo.EditorialBoard(UserId, JournalId, Position, Section, Bio)
            VALUES (@u_editor1, @jidEB, N'Associate Editor', N'Machine Learning',
                    N'研究方向：表示学习与模型评测。关注可复现性与严谨实验设计。');

        IF @u_editor2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.EditorialBoard WHERE JournalId=@jidEB AND UserId=@u_editor2)
            INSERT dbo.EditorialBoard(UserId, JournalId, Position, Section, Bio)
            VALUES (@u_editor2, @jidEB, N'Section Editor', N'Natural Language Processing',
                    N'研究方向：大模型、检索增强生成（RAG）与对齐技术。长期担任审稿人并参与专题策划。');

        IF @u_editor3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.EditorialBoard WHERE JournalId=@jidEB AND UserId=@u_editor3)
            INSERT dbo.EditorialBoard(UserId, JournalId, Position, Section, Bio)
            VALUES (@u_editor3, @jidEB, N'Section Editor', N'Computer Vision',
                    N'研究方向：视觉识别、医学影像与可信评测。关注数据治理与公平性。');
    END
END
GO

/* 4) Seed richer News (avoid duplicates by Title) */
IF OBJECT_ID(N'dbo.News', N'U') IS NOT NULL
BEGIN
    DECLARE @newsAuthor INT =
        COALESCE(
            (SELECT TOP 1 UserId FROM dbo.Users WHERE Username = N'eoadmin'),
            (SELECT TOP 1 UserId FROM dbo.Users WHERE Username = N'admin'),
            (SELECT TOP 1 UserId FROM dbo.Users ORDER BY UserId ASC)
        );

    IF @newsAuthor IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title=N'投稿指南更新：新增模板与格式检查要点')
            INSERT dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished)
            VALUES (N'投稿指南更新：新增模板与格式检查要点',
                    N'为提升审稿效率与排版一致性，我们更新了作者指南：新增 Word/LaTeX 模板、参考文献格式示例与常见格式问题清单。建议投稿前先完成自检。',
                    DATEADD(DAY, -2, SYSUTCDATETIME()), @newsAuthor, 1);

        IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title=N'出版伦理声明：利益冲突与数据可用性')
            INSERT dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished)
            VALUES (N'出版伦理声明：利益冲突与数据可用性',
                    N'请作者在稿件中明确声明利益冲突，并在可行情况下提供数据与代码可用性说明（Data & Code Availability）。本刊对学术不端采取零容忍政策。',
                    DATEADD(DAY, -7, SYSUTCDATETIME()), @newsAuthor, 1);

        IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title=N'审稿人培训：如何给出高质量审稿意见')
            INSERT dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished)
            VALUES (N'审稿人培训：如何给出高质量审稿意见',
                    N'我们发布了审稿建议清单，涵盖创新性、方法严谨性、实验可复现性与写作表达等维度，帮助审稿人提供可操作的改进建议。',
                    DATEADD(DAY, -14, SYSUTCDATETIME()), @newsAuthor, 1);

        IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title=N'系统功能升级：新增通知中心与消息提醒')
            INSERT dbo.News(Title, Content, PublishedAt, AuthorId, IsPublished)
            VALUES (N'系统功能升级：新增通知中心与消息提醒',
                    N'系统新增站内通知中心：稿件退修、审稿邀请、终审结果等关键节点会以站内信形式推送，帮助作者与审稿人及时跟进。',
                    DATEADD(DAY, -20, SYSUTCDATETIME()), @newsAuthor, 1);
    END
END
GO

/* 5) Seed more Issues (Latest & Special) */
IF OBJECT_ID(N'dbo.Issues', N'U') IS NOT NULL
BEGIN
    DECLARE @jidIssue INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);

    IF @jidIssue IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.Issues WHERE JournalId=@jidIssue AND Title=N'Latest Issues - Vol.1 No.2')
            INSERT dbo.Issues(JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate, GuestEditors)
            VALUES (@jidIssue, N'LATEST', N'Latest Issues - Vol.1 No.2', 1, 2, YEAR(GETDATE()),
                    N'最新一期：覆盖大模型评测、RAG、隐私计算与可复现研究的代表性工作。', 1,
                    CONVERT(date, DATEADD(day,-7,GETDATE())), NULL);

        IF NOT EXISTS (SELECT 1 FROM dbo.Issues WHERE JournalId=@jidIssue AND Title=N'Latest Issues - Vol.2 No.1')
            INSERT dbo.Issues(JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate, GuestEditors)
            VALUES (@jidIssue, N'LATEST', N'Latest Issues - Vol.2 No.1', 2, 1, YEAR(GETDATE()),
                    N'新卷首期：强调开放科学实践与可复现性，鼓励提交数据与代码。', 1,
                    CONVERT(date, DATEADD(day,-60,GETDATE())), NULL);

        IF NOT EXISTS (SELECT 1 FROM dbo.Issues WHERE JournalId=@jidIssue AND Title=N'Special Issue: Trustworthy AI & Safety')
            INSERT dbo.Issues(JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate, GuestEditors)
            VALUES (@jidIssue, N'SPECIAL', N'Trustworthy AI & Safety', NULL, NULL, YEAR(GETDATE()),
                    N'专题聚焦可信与安全 AI：对齐、幻觉、评测基准、风险治理与工具链。', 1,
                    CONVERT(date, DATEADD(day,-90,GETDATE())),
                    N'Guest Editors: Prof. Zhang (PKU); Dr. Chen (HKUST)');

        IF NOT EXISTS (SELECT 1 FROM dbo.Issues WHERE JournalId=@jidIssue AND Title=N'Special Issue: Reproducibility in ML')
            INSERT dbo.Issues(JournalId, IssueType, Title, Volume, Number, [Year], Description, IsPublished, PublishDate, GuestEditors)
            VALUES (@jidIssue, N'SPECIAL', N'Reproducibility in ML', NULL, NULL, YEAR(GETDATE()),
                    N'专题聚焦机器学习可复现：数据集版本、实验报告、复现实验与开源基准。', 1,
                    CONVERT(date, DATEADD(day,-120,GETDATE())),
                    N'Guest Editors: Dr. Li (SJTU); Dr. Wang (ZJU)');
    END
END
GO

/* 6) Seed more Call for Papers (published) */
IF OBJECT_ID(N'dbo.CallForPapers', N'U') IS NOT NULL
BEGIN
    DECLARE @jidCall INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);

    IF @jidCall IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.CallForPapers WHERE JournalId=@jidCall AND Title=N'Call for Papers: Trustworthy AI & Safety')
            INSERT dbo.CallForPapers(JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished)
            VALUES (@jidCall,
                    N'Call for Papers: Trustworthy AI & Safety',
                    N'<p>我们邀请投稿可信与安全 AI 相关研究，包括对齐、幻觉缓解、鲁棒性、公平性、风险评估与治理等方向。</p><ul><li>截稿日期：见下方 Deadline</li><li>建议提供代码与数据链接</li><li>支持匿名审稿</li></ul>',
                    CONVERT(date, DATEADD(day,-15,GETDATE())),
                    CONVERT(date, DATEADD(day, 45,GETDATE())),
                    CONVERT(date, DATEADD(day, 60,GETDATE())),
                    1);

        IF NOT EXISTS (SELECT 1 FROM dbo.CallForPapers WHERE JournalId=@jidCall AND Title=N'Call for Papers: Reproducible AI Systems')
            INSERT dbo.CallForPapers(JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished)
            VALUES (@jidCall,
                    N'Call for Papers: Reproducible AI Systems',
                    N'<p>专题欢迎投稿可复现 AI 系统与工具链：实验追踪、数据版本管理、评测基准、可解释与审计。</p><p>鼓励提交补充材料（Appendix）与可复现说明。</p>',
                    CONVERT(date, DATEADD(day,-40,GETDATE())),
                    CONVERT(date, DATEADD(day, 20,GETDATE())),
                    CONVERT(date, DATEADD(day, 35,GETDATE())),
                    1);

        IF NOT EXISTS (SELECT 1 FROM dbo.CallForPapers WHERE JournalId=@jidCall AND Title=N'Call for Papers: Large Language Models in Practice')
            INSERT dbo.CallForPapers(JournalId, Title, Content, StartDate, Deadline, EndDate, IsPublished)
            VALUES (@jidCall,
                    N'Call for Papers: Large Language Models in Practice',
                    N'<p>欢迎投稿大语言模型在真实场景中的应用与评测：RAG、工具调用、Agent、部署优化、数据治理与安全合规。</p>',
                    CONVERT(date, DATEADD(day,-25,GETDATE())),
                    CONVERT(date, DATEADD(day, 75,GETDATE())),
                    CONVERT(date, DATEADD(day, 90,GETDATE())),
                    1);
    END
END
GO

/* 7) Seed a few ACCEPTED manuscripts + metrics for public "Articles" page */
IF OBJECT_ID(N'dbo.Manuscripts', N'U') IS NOT NULL
BEGIN
    DECLARE @jidM INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId);
    DECLARE @authorId INT = (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'author1');
    DECLARE @editorId INT =
        COALESCE((SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'editor1'),
                 (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'editor2'),
                 (SELECT TOP 1 UserId FROM dbo.Users WHERE Username=N'eic'));

    IF @authorId IS NOT NULL AND @jidM IS NOT NULL
    BEGIN
        DECLARE @seed TABLE(
            SeedKey NVARCHAR(50) NOT NULL,
            Title NVARCHAR(500) NOT NULL,
            Abstract NVARCHAR(MAX) NULL,
            Keywords NVARCHAR(500) NULL,
            SubjectArea NVARCHAR(100) NULL,
            AuthorList NVARCHAR(500) NULL,
            DaysAgo INT NOT NULL,
            Views INT NOT NULL,
            Downloads INT NOT NULL,
            Citations INT NOT NULL
        );

        INSERT INTO @seed(SeedKey, Title, Abstract, Keywords, SubjectArea, AuthorList, DaysAgo, Views, Downloads, Citations)
        VALUES
          (N'A1', N'Retrieval-Augmented Generation for Domain QA: A Reproducible Benchmark',
           N'<p>本文提出一个面向领域问答的 RAG 评测基准与可复现实验协议，系统分析检索质量、提示策略与答案一致性。</p>',
           N'RAG; Benchmark; Reproducibility; QA', N'NLP',
           N'Author One; Author Two; Author Three', 12, 842, 210, 18),

          (N'A2', N'Trustworthy Evaluation of Large Language Models: Hallucination, Robustness, and Safety',
           N'<p>我们构建一套可信评测框架，覆盖幻觉检测、鲁棒性测试与安全对齐指标，并给出可复现的评测流水线。</p>',
           N'LLM; Safety; Evaluation; Hallucination', N'AI Safety',
           N'Author A; Author B', 35, 1260, 388, 42),

          (N'A3', N'Privacy-Preserving Federated Learning with Practical Deployment Considerations',
           N'<p>本文从系统与隐私角度讨论联邦学习的部署挑战，提出一种兼顾效率与隐私保护的训练策略。</p>',
           N'Federated Learning; Privacy; Systems', N'Systems',
           N'Author X; Author Y; Author Z', 58, 630, 155, 11),

          (N'A4', N'Graph Neural Networks for Scientific Discovery: Methods and Open Datasets',
           N'<p>综述图神经网络在科学发现中的关键方法，并整理公开数据集与评测协议，促进领域研究可复现。</p>',
           N'GNN; Scientific Discovery; Survey', N'Graph Learning',
           N'Author M; Author N', 80, 520, 120, 9),

          (N'A5', N'An Empirical Study of Prompting Strategies in Multimodal Models',
           N'<p>我们系统比较多模态模型的提示策略，给出可复现的实验配置与误差分析，为工程实践提供参考。</p>',
           N'Multimodal; Prompting; Empirical Study', N'Computer Vision',
           N'Author P; Author Q', 100, 410, 98, 6);

        DECLARE @new TABLE(ManuscriptId INT NOT NULL, SeedKey NVARCHAR(50) NOT NULL);

        INSERT INTO dbo.Manuscripts(
            JournalId, SubmitterId, CurrentEditorId,
            Title, Abstract, Keywords, SubjectArea, AuthorList,
            Status, Decision, CurrentRound,
            SubmitTime, LastStatusTime, FinalDecisionTime,
            IsArchived, IsWithdrawn
        )
        OUTPUT inserted.ManuscriptId, s.SeedKey INTO @new(ManuscriptId, SeedKey)
        SELECT
            @jidM, @authorId, @editorId,
            s.Title, s.Abstract, s.Keywords, s.SubjectArea, s.AuthorList,
            N'ACCEPTED', N'ACCEPT', 1,
            DATEADD(DAY, -s.DaysAgo, SYSUTCDATETIME()),
            DATEADD(DAY, -s.DaysAgo, SYSUTCDATETIME()),
            DATEADD(DAY, -s.DaysAgo + 3, SYSUTCDATETIME()),
            0, 0
        FROM @seed s
        WHERE NOT EXISTS (SELECT 1 FROM dbo.Manuscripts m WHERE m.Title = s.Title);

        /* Ensure each seeded manuscript has a current Version row (file paths left NULL for demo) */
        IF OBJECT_ID(N'dbo.ManuscriptVersions', N'U') IS NOT NULL
        BEGIN
            INSERT INTO dbo.ManuscriptVersions(ManuscriptId, VersionNumber, IsCurrent, FileAnonymousPath, FileOriginalPath, CoverLetterPath, CoverLetterHtml, ResponseLetterPath, CreatedBy, Remark)
            SELECT n.ManuscriptId, 1, 1, NULL, NULL, NULL, NULL, NULL, @authorId, N'Demo seed'
            FROM @new n
            WHERE NOT EXISTS (SELECT 1 FROM dbo.ManuscriptVersions v WHERE v.ManuscriptId=n.ManuscriptId AND v.VersionNumber=1);
        END

        /* Seed ArticleMetrics for nicer public list sorting */
        IF OBJECT_ID(N'dbo.ArticleMetrics', N'U') IS NOT NULL
        BEGIN
            INSERT INTO dbo.ArticleMetrics(ManuscriptId, ViewCount, DownloadCount, CitationCount, PopularityScore)
            SELECT
                n.ManuscriptId,
                s.Views, s.Downloads, s.Citations,
                CAST((s.Views*0.2 + s.Downloads*0.6 + s.Citations*1.5) AS FLOAT)
            FROM @new n
            JOIN @seed s ON s.SeedKey = n.SeedKey
            WHERE NOT EXISTS (SELECT 1 FROM dbo.ArticleMetrics am WHERE am.ManuscriptId = n.ManuscriptId);
        END
    END
END
GO
