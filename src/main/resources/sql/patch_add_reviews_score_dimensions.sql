-- Patch: Add 5 missing review score dimensions for v3 review form
-- Target: SQL Server, table dbo.Reviews
-- Safe to run multiple times (idempotent).

IF COL_LENGTH('dbo.Reviews', 'ScoreExperimentation') IS NULL
    ALTER TABLE dbo.Reviews ADD ScoreExperimentation DECIMAL(4,2) NULL;

IF COL_LENGTH('dbo.Reviews', 'ScoreLiteratureReview') IS NULL
    ALTER TABLE dbo.Reviews ADD ScoreLiteratureReview DECIMAL(4,2) NULL;

IF COL_LENGTH('dbo.Reviews', 'ScoreConclusions') IS NULL
    ALTER TABLE dbo.Reviews ADD ScoreConclusions DECIMAL(4,2) NULL;

IF COL_LENGTH('dbo.Reviews', 'ScoreAcademicIntegrity') IS NULL
    ALTER TABLE dbo.Reviews ADD ScoreAcademicIntegrity DECIMAL(4,2) NULL;

IF COL_LENGTH('dbo.Reviews', 'ScorePracticality') IS NULL
    ALTER TABLE dbo.Reviews ADD ScorePracticality DECIMAL(4,2) NULL;
