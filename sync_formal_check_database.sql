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
