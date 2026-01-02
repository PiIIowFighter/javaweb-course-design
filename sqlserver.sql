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

/* 99.1 表：dbo.JournalPages */
IF OBJECT_ID(N'dbo.JournalPages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.JournalPages (
        PageId     INT IDENTITY(1,1) PRIMARY KEY,
        JournalId  INT NOT NULL,
        PageKey    NVARCHAR(50) NOT NULL,     -- aims / policies
        Title      NVARCHAR(200) NOT NULL,
        Content    NVARCHAR(MAX) NOT NULL,    -- 存 HTML（<p><ul> 等）
        UpdatedAt  DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_JournalPages_Journal FOREIGN KEY(JournalId) REFERENCES dbo.Journals(JournalId)
    );

    CREATE UNIQUE INDEX UX_JournalPages_Journal_PageKey
        ON dbo.JournalPages(JournalId, PageKey);
END
GO

/* 99.2 初始化 Aims / Policies（若已存在则更新） */
DECLARE @jid INT = (SELECT TOP 1 JournalId FROM dbo.Journals ORDER BY JournalId ASC);

IF @jid IS NOT NULL
BEGIN
    /* Aims and scope */
    MERGE dbo.JournalPages AS T
    USING (SELECT @jid AS JournalId, N'aims' AS PageKey) AS S
    ON (T.JournalId = S.JournalId AND T.PageKey = S.PageKey)
    WHEN MATCHED THEN
        UPDATE SET
            Title = N'论文主旨与投稿范围（Aims and scope）',
            Content = N'
<p>本期刊聚焦人工智能与数据科学领域的理论创新与工程应用，欢迎具有明确贡献与可复现性的研究工作投稿。</p>
<h3>主要研究方向</h3>
<ul>
  <li>机器学习 / 深度学习（监督、无监督、强化学习）</li>
  <li>计算机视觉与模式识别（检测、分割、生成模型）</li>
  <li>自然语言处理与大模型（检索增强、对齐、推理）</li>
  <li>数据挖掘与知识图谱（图学习、信息抽取）</li>
  <li>智能系统与工程应用（部署、评测、系统优化）</li>
</ul>
<h3>稿件类型</h3>
<ul>
  <li>研究论文（Research Article）</li>
  <li>综述论文（Review）</li>
  <li>简报/短文（Short Communication）</li>
</ul>
<p><b>投稿要求：</b>稿件需包含清晰的问题定义、方法描述、实验设置与结论分析；鼓励提供数据/代码链接以提升可复现性。</p>
',
            UpdatedAt = SYSUTCDATETIME()
    WHEN NOT MATCHED THEN
        INSERT (JournalId, PageKey, Title, Content)
        VALUES (@jid, N'aims', N'论文主旨与投稿范围（Aims and scope）',
N'
<p>本期刊聚焦人工智能与数据科学领域的理论创新与工程应用，欢迎具有明确贡献与可复现性的研究工作投稿。</p>
<h3>主要研究方向</h3>
<ul>
  <li>机器学习 / 深度学习（监督、无监督、强化学习）</li>
  <li>计算机视觉与模式识别（检测、分割、生成模型）</li>
  <li>自然语言处理与大模型（检索增强、对齐、推理）</li>
  <li>数据挖掘与知识图谱（图学习、信息抽取）</li>
  <li>智能系统与工程应用（部署、评测、系统优化）</li>
</ul>
<h3>稿件类型</h3>
<ul>
  <li>研究论文（Research Article）</li>
  <li>综述论文（Review）</li>
  <li>简报/短文（Short Communication）</li>
</ul>
<p><b>投稿要求：</b>稿件需包含清晰的问题定义、方法描述、实验设置与结论分析；鼓励提供数据/代码链接以提升可复现性。</p>
');
    ;

    /* Policies and Guidelines */
    MERGE dbo.JournalPages AS T
    USING (SELECT @jid AS JournalId, N'policies' AS PageKey) AS S
    ON (T.JournalId = S.JournalId AND T.PageKey = S.PageKey)
    WHEN MATCHED THEN
        UPDATE SET
            Title = N'政策与指南（Policies and Guidelines）',
            Content = N'
<p>以下政策与指南适用于本期刊的投稿、审稿与出版流程。若与系统功能存在差异，以系统实际流程为准。</p>

<h3>1. 投稿与格式要求</h3>
<ul>
  <li>稿件需包含：标题、摘要、关键词、正文、参考文献（按期刊格式）。</li>
  <li>图表需提供清晰标题与编号；引用数据需注明来源。</li>
</ul>

<h3>2. 同行评审政策</h3>
<ul>
  <li>采用同行评审流程（系统中对应：初审 → 指派编辑 → 外审 → 终审）。</li>
  <li>审稿意见与修回记录将被系统保存以便追踪。</li>
</ul>

<h3>3. 出版伦理与学术规范</h3>
<ul>
  <li>严禁一稿多投、抄袭、伪造数据等学术不端行为。</li>
  <li>作者署名与贡献需真实有效；如存在利益冲突需在稿件中声明。</li>
</ul>

<h3>4. 版权与许可</h3>
<ul>
  <li>录用后作者需确认版权/许可协议（可在后续版本中扩展为在线协议确认）。</li>
</ul>

<h3>5. 数据与代码可复现</h3>
<ul>
  <li>鼓励提供数据与代码的公开链接或附加材料，提升研究可复现性。</li>
</ul>
',
            UpdatedAt = SYSUTCDATETIME()
    WHEN NOT MATCHED THEN
        INSERT (JournalId, PageKey, Title, Content)
        VALUES (@jid, N'policies', N'政策与指南（Policies and Guidelines）',
N'
<p>以下政策与指南适用于本期刊的投稿、审稿与出版流程。若与系统功能存在差异，以系统实际流程为准。</p>

<h3>1. 投稿与格式要求</h3>
<ul>
  <li>稿件需包含：标题、摘要、关键词、正文、参考文献（按期刊格式）。</li>
  <li>图表需提供清晰标题与编号；引用数据需注明来源。</li>
</ul>

<h3>2. 同行评审政策</h3>
<ul>
  <li>采用同行评审流程（系统中对应：初审 → 指派编辑 → 外审 → 终审）。</li>
  <li>审稿意见与修回记录将被系统保存以便追踪。</li>
</ul>

<h3>3. 出版伦理与学术规范</h3>
<ul>
  <li>严禁一稿多投、抄袭、伪造数据等学术不端行为。</li>
  <li>作者署名与贡献需真实有效；如存在利益冲突需在稿件中声明。</li>
</ul>

<h3>4. 版权与许可</h3>
<ul>
  <li>录用后作者需确认版权/许可协议（可在后续版本中扩展为在线协议确认）。</li>
</ul>

<h3>5. 数据与代码可复现</h3>
<ul>
  <li>鼓励提供数据与代码的公开链接或附加材料，提升研究可复现性。</li>
</ul>
');
END
GO

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
     - Creates dbo.Issues, dbo.IssueManuscripts, dbo.CallForPapers if missing
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

IF OBJECT_ID(N'dbo.IssueManuscripts', N'U') IS NULL
BEGIN
    PRINT N'Creating dbo.IssueManuscripts...';

    IF OBJECT_ID(N'dbo.Manuscripts', N'U') IS NULL
    BEGIN
        THROW 50001, 'Missing dbo.Manuscripts table (required for IssueManuscripts FK).', 1;
    END

    CREATE TABLE dbo.IssueManuscripts (
        IssueId      INT NOT NULL,
        ManuscriptId INT NOT NULL,
        OrderNo      INT NOT NULL CONSTRAINT DF_IssueManuscripts_OrderNo DEFAULT(0),
        AddedAt      DATETIME2(0) NOT NULL CONSTRAINT DF_IssueManuscripts_AddedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_IssueManuscripts PRIMARY KEY (IssueId, ManuscriptId),
        CONSTRAINT FK_IssueManuscripts_Issues FOREIGN KEY (IssueId) REFERENCES dbo.Issues(IssueId),
        CONSTRAINT FK_IssueManuscripts_Manuscripts FOREIGN KEY (ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId)
    );

    CREATE INDEX IX_IssueManuscripts_IssueId ON dbo.IssueManuscripts(IssueId);
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
