/* =========================================
   可选：如需重建数据库，请先手动执行这段（谨慎！会删除原库）
-------------------------------------------
IF DB_ID(N'Online_SMSystem4SP') IS NOT NULL
    DROP DATABASE [Online_SMSystem4SP];
GO
========================================= */

-- 如果数据库不存在则创建
IF DB_ID(N'Online_SMSystem4SP') IS NULL
    CREATE DATABASE [Online_SMSystem4SP];
GO

USE [Online_SMSystem4SP];
GO


/* =========================================================
   1. 角色表 Roles  —— 对应七种角色
   ========================================================= */
IF OBJECT_ID(N'dbo.Roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Roles (
        RoleId      INT IDENTITY(1,1) PRIMARY KEY,
        RoleCode    NVARCHAR(50)  NOT NULL UNIQUE,  -- 机器可读：SUPER_ADMIN / AUTHOR ...
        RoleName    NVARCHAR(100) NOT NULL,         -- 中文名称：超级管理员 / 作者 ...
        Description NVARCHAR(200) NULL
    );

    -- 初始化七种角色（和任务书一致）
    INSERT dbo.Roles(RoleCode, RoleName, Description) VALUES
    (N'SUPER_ADMIN',      N'超级管理员',       N'内置超级管理员，拥有最高权限'),
    (N'SYSTEM_ADMIN',     N'系统管理员',       N'系统维护、配置、日志查看'),
    (N'AUTHOR',           N'作者',             N'外部作者，负责投稿和修回'),
    (N'REVIEWER',         N'审稿人',           N'外部审稿专家'),
    (N'EDITOR_IN_CHIEF',  N'主编',             N'主编/副主编，学术决策、终审'),
    (N'EDITOR',           N'编辑',             N'责任编辑，处理分配稿件'),
    (N'EO_ADMIN',         N'编辑部管理员',     N'形式审查、格式检查、新闻公告管理');
END;
GO


/* =========================================================
   2. 用户表 Users —— 对应任务书中的 User 表
   ========================================================= */
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserId         INT IDENTITY(1,1) PRIMARY KEY,
        Username       NVARCHAR(50)  NOT NULL UNIQUE,   -- 登录名
        PasswordHash   NVARCHAR(255) NOT NULL,          -- 密码（可以先明文，后续改为加密）
        Email          NVARCHAR(100) NULL,
        FullName       NVARCHAR(100) NULL,              -- 全名
        Affiliation    NVARCHAR(200) NULL,              -- 单位
        ResearchArea   NVARCHAR(200) NULL,              -- 研究方向
        RoleId         INT NOT NULL,                    -- 角色外键 -> Roles
        RegisterTime   DATETIME2(0) NOT NULL 
                         DEFAULT SYSUTCDATETIME(),      -- 注册时间
        Status         NVARCHAR(20) NOT NULL 
                         DEFAULT N'ACTIVE',             -- ACTIVE / DISABLED / LOCKED 等
        CONSTRAINT FK_Users_Roles
            FOREIGN KEY(RoleId) REFERENCES dbo.Roles(RoleId)
    );

    -- 内置超级管理员：用户名 admin，密码 123
    INSERT dbo.Users (Username, PasswordHash, Email, FullName, Affiliation, ResearchArea, RoleId, Status)
    SELECT N'admin', N'123', N'admin@example.com', N'超级管理员', N'系统内置', NULL, RoleId, N'ACTIVE'
    FROM dbo.Roles WHERE RoleCode = N'SUPER_ADMIN';
END;
GO


/* =========================================================
   3. 期刊表 Journals —— 对应 Journal 表
   ========================================================= */
IF OBJECT_ID(N'dbo.Journals', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Journals (
        JournalId   INT IDENTITY(1,1) PRIMARY KEY,
        Name        NVARCHAR(200) NOT NULL,     -- 期刊名称
        Description NVARCHAR(MAX) NULL,         -- 期刊介绍
        ImpactFactor DECIMAL(6,3) NULL,         -- 影响因子
        Timeline    NVARCHAR(200) NULL,         -- 发表时间线描述
        ISSN        NVARCHAR(30)  NULL,
        CreatedAt   DATETIME2(0) NOT NULL 
                      DEFAULT SYSUTCDATETIME(),
        CreatedBy   INT NULL,
        CONSTRAINT FK_Journals_CreatedBy
            FOREIGN KEY(CreatedBy) REFERENCES dbo.Users(UserId)
    );
END;
GO


/* =========================================================
   4. 稿件表 Manuscripts —— 对应 Manuscript 表 + 状态机
   ========================================================= */
IF OBJECT_ID(N'dbo.Manuscripts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Manuscripts (
        ManuscriptId     INT IDENTITY(1,1) PRIMARY KEY, -- 稿件 ID
        JournalId        INT NULL,                      -- 所投期刊
        SubmitterId      INT NOT NULL,                  -- 提交作者 ID（作者）
        CurrentEditorId  INT NULL,                      -- 当前责任编辑（编辑）
        Title            NVARCHAR(500) NOT NULL,        -- 标题
        Abstract         NVARCHAR(MAX)  NULL,           -- 摘要
        Keywords         NVARCHAR(500) NULL,            -- 关键词（逗号分隔）
        SubjectArea      NVARCHAR(100) NULL,            -- 研究主题
        FundingInfo      NVARCHAR(500) NULL,            -- 项目资助情况
        AuthorList       NVARCHAR(500) NULL,            -- 作者列表简要字符串（可作冗余显示）
        Status           NVARCHAR(30)  NOT NULL 
                           DEFAULT N'DRAFT',            -- 当前状态（状态机）
        Decision         NVARCHAR(30)  NULL,            -- 最终决策：ACCEPT / REJECT / REVISION 等
        CurrentRound     INT NOT NULL DEFAULT 1,        -- 当前轮次（初稿=1，修回=2，...）
        SubmitTime       DATETIME2(0) NULL,             -- 初次提交时间
        LastStatusTime   DATETIME2(0) NOT NULL 
                           DEFAULT SYSUTCDATETIME(),    -- 最近状态变更时间
        FinalDecisionTime DATETIME2(0) NULL,            -- 最终决策时间
        IsArchived       BIT NOT NULL DEFAULT 0,        -- 是否归档
        IsWithdrawn      BIT NOT NULL DEFAULT 0,        -- 是否撤稿

        CONSTRAINT FK_Manuscripts_Journal
            FOREIGN KEY(JournalId) REFERENCES dbo.Journals(JournalId),
        CONSTRAINT FK_Manuscripts_Submitter
            FOREIGN KEY(SubmitterId) REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Manuscripts_CurrentEditor
            FOREIGN KEY(CurrentEditorId) REFERENCES dbo.Users(UserId),

        -- 状态机对应的所有状态（machine.pdf 中的状态）
        CONSTRAINT CK_Manuscripts_Status CHECK (Status IN (
            N'DRAFT',                 -- 草稿（作者）
            N'SUBMITTED',             -- 已提交（编辑部管理员看到）
            N'FORMAL_CHECK',          -- 形式审查（编辑部管理员）
            N'RETURNED',              -- 退回给作者修改（作者）
            N'DESK_REVIEW_INITIAL',   -- 主编初审
            N'TO_ASSIGN',             -- 待分配给编辑
            N'WITH_EDITOR',           -- 编辑处理中
            N'UNDER_REVIEW',          -- 审稿中
            N'EDITOR_RECOMMENDATION', -- 编辑建议
            N'FINAL_DECISION_PENDING',-- 主编终审待决
            N'REVISION',              -- 需要修回（作者）
            N'ACCEPTED',              -- 已接收（主编）
            N'REJECTED',              -- 已拒稿（主编）
            N'ARCHIVED'               -- 已归档
        ))
    );
END;
GO


/* =========================================================
   5. 稿件版本表 ManuscriptVersions —— 对应 Versions 表
   ========================================================= */
IF OBJECT_ID(N'dbo.ManuscriptVersions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptVersions (
        VersionId           INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId        INT NOT NULL,
        VersionNumber       INT NOT NULL,             -- 1,2,3...
        IsCurrent           BIT NOT NULL DEFAULT 1,   -- 是否当前版本
        FileAnonymousPath   NVARCHAR(260) NULL,       -- 匿名版 PDF 路径
        FileOriginalPath    NVARCHAR(260) NULL,       -- 原始版 PDF 路径
        CoverLetterPath     NVARCHAR(260) NULL,       -- Cover Letter 路径
        ResponseLetterPath  NVARCHAR(260) NULL,       -- Reply/Response Letter 路径
        CreatedAt           DATETIME2(0) NOT NULL 
                              DEFAULT SYSUTCDATETIME(),
        CreatedBy           INT NOT NULL,             -- 版本创建者（一般为作者）

        Remark              NVARCHAR(200) NULL,

        CONSTRAINT UQ_ManuscriptVersions UNIQUE(ManuscriptId, VersionNumber),

        CONSTRAINT FK_ManuscriptVersions_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_ManuscriptVersions_CreatedBy
            FOREIGN KEY(CreatedBy)    REFERENCES dbo.Users(UserId)
    );
END;
GO


/* =========================================================
   6. 稿件作者表 ManuscriptAuthors —— 细化“作者列表”
   ========================================================= */
IF OBJECT_ID(N'dbo.ManuscriptAuthors', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptAuthors (
        AuthorId      INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId  INT NOT NULL,
        AuthorOrder   INT NOT NULL,                  -- 作者顺序
        FullName      NVARCHAR(100) NOT NULL,
        Affiliation   NVARCHAR(200) NULL,            -- 单位
        Degree        NVARCHAR(50)  NULL,            -- 学历
        Title         NVARCHAR(50)  NULL,            -- 职称
        Position      NVARCHAR(50)  NULL,            -- 职位
        Email         NVARCHAR(100) NULL,
        IsCorresponding BIT NOT NULL DEFAULT 0,      -- 是否通讯作者

        CONSTRAINT FK_ManuscriptAuthors_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId)
    );
END;
GO


/* =========================================================
   7. 推荐审稿人表 ManuscriptRecommendedReviewers
      —— 对应“推荐审稿人”元数据
   ========================================================= */
IF OBJECT_ID(N'dbo.ManuscriptRecommendedReviewers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptRecommendedReviewers (
        Id            INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId  INT NOT NULL,
        FullName      NVARCHAR(100) NOT NULL,
        Email         NVARCHAR(100) NOT NULL,
        Reason        NVARCHAR(500) NULL,

        CONSTRAINT FK_MRecommendedReviewers_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId)
    );
END;
GO


/* =========================================================
   8. 审稿任务表 Reviews —— 对应 Review 表
   ========================================================= */
IF OBJECT_ID(N'dbo.Reviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reviews (
        ReviewId       INT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId   INT NOT NULL,
        VersionId      INT NULL,                     -- 对应具体版本（可选）
        ReviewerId     INT NOT NULL,                 -- 审稿人 ID
        Content        NVARCHAR(MAX) NULL,           -- 审稿意见
        Score          DECIMAL(4,2) NULL,            -- 打分
        Recommendation NVARCHAR(50)  NULL,           -- 建议：ACCEPT / MINOR / MAJOR / REJECT 等
        Status         NVARCHAR(30)  NOT NULL 
                         DEFAULT N'INVITED',         -- INVITED/ACCEPTED/DECLINED/SUBMITTED/EXPIRED
        InvitedAt      DATETIME2(0) NOT NULL 
                         DEFAULT SYSUTCDATETIME(),   -- 邀请时间
        AcceptedAt     DATETIME2(0) NULL,            -- 接受时间
        SubmittedAt    DATETIME2(0) NULL,            -- 提交时间
        DueAt          DATETIME2(0) NULL,            -- 截止日期
        RemindCount    INT NOT NULL DEFAULT 0,
        LastRemindedAt DATETIME2(0) NULL,

        CONSTRAINT FK_Reviews_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_Reviews_Version
            FOREIGN KEY(VersionId)    REFERENCES dbo.ManuscriptVersions(VersionId),
        CONSTRAINT FK_Reviews_Reviewer
            FOREIGN KEY(ReviewerId)   REFERENCES dbo.Users(UserId)
    );
END;
GO


/* =========================================================
   9. 编委表 EditorialBoard —— 对应 Editorial_Board 表
   ========================================================= */
IF OBJECT_ID(N'dbo.EditorialBoard', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.EditorialBoard (
        BoardMemberId INT IDENTITY(1,1) PRIMARY KEY,
        UserId        INT NOT NULL,              -- 对应用户（主编/编辑/编辑部管理员）
        JournalId     INT NOT NULL,              -- 所属期刊
        Position      NVARCHAR(50) NOT NULL,     -- 主编/副主编/编辑/EO 管理员
        Section       NVARCHAR(100) NULL,        -- 所属栏目
        Bio           NVARCHAR(MAX)  NULL,       -- 简介

        CONSTRAINT FK_EditorialBoard_User
            FOREIGN KEY(UserId)    REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_EditorialBoard_Journal
            FOREIGN KEY(JournalId) REFERENCES dbo.Journals(JournalId)
    );
END;
GO


/* =========================================================
   10. 新闻表 News —— 对应 News 表
   ========================================================= */
IF OBJECT_ID(N'dbo.News', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.News (
        NewsId      INT IDENTITY(1,1) PRIMARY KEY,
        Title       NVARCHAR(200) NOT NULL,
        Content     NVARCHAR(MAX) NOT NULL,
        PublishedAt DATETIME2(0) NOT NULL 
                      DEFAULT SYSUTCDATETIME(),
        AuthorId    INT NOT NULL,                 -- 发布人 ID
        IsPublished BIT NOT NULL DEFAULT 1,

        CONSTRAINT FK_News_Author
            FOREIGN KEY(AuthorId) REFERENCES dbo.Users(UserId)
    );
END;
GO


/* =========================================================
   11. 文件表 Files —— 对应 File 表
   ========================================================= */
IF OBJECT_ID(N'dbo.Files', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Files (
        FileId       INT IDENTITY(1,1) PRIMARY KEY,
        FileName     NVARCHAR(260) NOT NULL,
        FilePath     NVARCHAR(260) NOT NULL,
        FileType     NVARCHAR(50)  NULL,         -- MANUSCRIPT / COVER_LETTER / SUPPLEMENT / OTHER
        FileSize     BIGINT        NULL,
        UploadTime   DATETIME2(0) NOT NULL 
                       DEFAULT SYSUTCDATETIME(),
        UploaderId   INT NOT NULL,
        ManuscriptId INT NULL,
        VersionId    INT NULL,

        CONSTRAINT FK_Files_Uploader
            FOREIGN KEY(UploaderId)   REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Files_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_Files_Version
            FOREIGN KEY(VersionId)    REFERENCES dbo.ManuscriptVersions(VersionId)
    );
END;
GO


/* =========================================================
   12. 日志表 Logs —— 对应 Logs 表
   ========================================================= */
IF OBJECT_ID(N'dbo.Logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Logs (
        LogId        BIGINT IDENTITY(1,1) PRIMARY KEY,
        LogTime      DATETIME2(0) NOT NULL 
                       DEFAULT SYSUTCDATETIME(),
        UserId       INT NULL,                  -- 操作人 ID
        ActionType   NVARCHAR(100) NOT NULL,    -- 操作类型：LOGIN / CHANGE_STATUS / SUBMIT / ...
        ActionDesc   NVARCHAR(MAX)  NULL,       -- 操作描述
        ManuscriptId INT NULL,
        IpAddress    NVARCHAR(45)  NULL,

        CONSTRAINT FK_Logs_User
            FOREIGN KEY(UserId)       REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Logs_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId)
    );
END;
GO


/* =========================================================
   13. 稿件状态流转历史表 ManuscriptStatusHistory
       —— 对应 machine.pdf 的状态机（事件 + 状态变化）
   ========================================================= */
IF OBJECT_ID(N'dbo.ManuscriptStatusHistory', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ManuscriptStatusHistory (
        HistoryId    BIGINT IDENTITY(1,1) PRIMARY KEY,
        ManuscriptId INT NOT NULL,
        FromStatus   NVARCHAR(30)  NULL,        -- 原状态，可为 NULL（初次创建 DRAFT）
        ToStatus     NVARCHAR(30)  NOT NULL,    -- 目标状态，如 SUBMITTED
        Event        NVARCHAR(50)  NOT NULL,    -- 触发事件：Submit / Start Check / Return / ...
        ChangedBy    INT NOT NULL,              -- 操作人 ID（作者/编辑/主编/管理员等）
        ChangeTime   DATETIME2(0) NOT NULL 
                       DEFAULT SYSUTCDATETIME(),
        Remark       NVARCHAR(500) NULL,

        CONSTRAINT FK_MSH_Manuscript
            FOREIGN KEY(ManuscriptId) REFERENCES dbo.Manuscripts(ManuscriptId),
        CONSTRAINT FK_MSH_ChangedBy
            FOREIGN KEY(ChangedBy)    REFERENCES dbo.Users(UserId)
    );
END;
GO


/* =========================================================
   14. 一些常用索引（可选，提升查询效率）
   ========================================================= */
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = N'IX_Users_Username' AND object_id = OBJECT_ID(N'dbo.Users')
)
BEGIN
    CREATE INDEX IX_Users_Username ON dbo.Users(Username);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = N'IX_Manuscripts_Status' AND object_id = OBJECT_ID(N'dbo.Manuscripts')
)
BEGIN
    CREATE INDEX IX_Manuscripts_Status ON dbo.Manuscripts(Status);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = N'IX_Manuscripts_Submitter' AND object_id = OBJECT_ID(N'dbo.Manuscripts')
)
BEGIN
    CREATE INDEX IX_Manuscripts_Submitter ON dbo.Manuscripts(SubmitterId);
END;
GO
