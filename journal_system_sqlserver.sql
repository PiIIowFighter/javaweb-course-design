/* =========================================================
   科研论文在线投稿及管理系统
   ========================================================= */

IF DB_ID(N'journal_system') IS NULL
BEGIN
    CREATE DATABASE journal_system;
END
GO
USE journal_system;
GO

/* ---------------------------
   0) 清理旧表（按依赖顺序）
--------------------------- */
IF OBJECT_ID('dbo.cms_post_file','U') IS NOT NULL DROP TABLE dbo.cms_post_file;
IF OBJECT_ID('dbo.cms_post','U')      IS NOT NULL DROP TABLE dbo.cms_post;
IF OBJECT_ID('dbo.sys_message','U')   IS NOT NULL DROP TABLE dbo.sys_message;

IF OBJECT_ID('dbo.format_check','U')     IS NOT NULL DROP TABLE dbo.format_check;
IF OBJECT_ID('dbo.reviewer_profile','U') IS NOT NULL DROP TABLE dbo.reviewer_profile;

IF OBJECT_ID('dbo.manuscript_flow','U')         IS NOT NULL DROP TABLE dbo.manuscript_flow;
IF OBJECT_ID('dbo.manuscript_decision','U')     IS NOT NULL DROP TABLE dbo.manuscript_decision;
IF OBJECT_ID('dbo.editor_recommendation','U')   IS NOT NULL DROP TABLE dbo.editor_recommendation;
IF OBJECT_ID('dbo.review_reminder','U')         IS NOT NULL DROP TABLE dbo.review_reminder;
IF OBJECT_ID('dbo.review_report','U')           IS NOT NULL DROP TABLE dbo.review_report;
IF OBJECT_ID('dbo.review_invitation','U')       IS NOT NULL DROP TABLE dbo.review_invitation;

IF OBJECT_ID('dbo.manuscript_assignment','U') IS NOT NULL DROP TABLE dbo.manuscript_assignment;
IF OBJECT_ID('dbo.manuscript_file','U')       IS NOT NULL DROP TABLE dbo.manuscript_file;
IF OBJECT_ID('dbo.manuscript_author','U')     IS NOT NULL DROP TABLE dbo.manuscript_author;
IF OBJECT_ID('dbo.manuscript_version','U')    IS NOT NULL DROP TABLE dbo.manuscript_version;
IF OBJECT_ID('dbo.manuscript','U')            IS NOT NULL DROP TABLE dbo.manuscript;

IF OBJECT_ID('dbo.file_object','U') IS NOT NULL DROP TABLE dbo.file_object;

IF OBJECT_ID('dbo.wf_transition','U') IS NOT NULL DROP TABLE dbo.wf_transition;
IF OBJECT_ID('dbo.wf_status','U')     IS NOT NULL DROP TABLE dbo.wf_status;

IF OBJECT_ID('dbo.sys_login_log','U') IS NOT NULL DROP TABLE dbo.sys_login_log;
IF OBJECT_ID('dbo.sys_op_log','U')    IS NOT NULL DROP TABLE dbo.sys_op_log;

IF OBJECT_ID('dbo.sys_user_role','U')  IS NOT NULL DROP TABLE dbo.sys_user_role;
IF OBJECT_ID('dbo.sys_role_perm','U')  IS NOT NULL DROP TABLE dbo.sys_role_perm;
IF OBJECT_ID('dbo.sys_menu','U')       IS NOT NULL DROP TABLE dbo.sys_menu;
IF OBJECT_ID('dbo.sys_permission','U') IS NOT NULL DROP TABLE dbo.sys_permission;
IF OBJECT_ID('dbo.sys_role','U')       IS NOT NULL DROP TABLE dbo.sys_role;
IF OBJECT_ID('dbo.sys_user','U')       IS NOT NULL DROP TABLE dbo.sys_user;
GO

/* ---------------------------
   1) RBAC
--------------------------- */
CREATE TABLE dbo.sys_user(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  username NVARCHAR(64) NOT NULL UNIQUE,
  password_hash NVARCHAR(255) NOT NULL,
  real_name NVARCHAR(64) NULL,
  email NVARCHAR(128) NULL,
  phone NVARCHAR(32) NULL,
  status TINYINT NOT NULL DEFAULT 0,  -- 0待审核 1启用 2禁用
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  last_login_at DATETIME2 NULL
);
GO

CREATE TABLE dbo.sys_role(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  code NVARCHAR(64) NOT NULL UNIQUE,
  name NVARCHAR(64) NOT NULL
);
GO

CREATE TABLE dbo.sys_permission(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  code NVARCHAR(128) NOT NULL UNIQUE,
  name NVARCHAR(128) NOT NULL,
  type NVARCHAR(32) NOT NULL DEFAULT N'API'
);
GO

CREATE TABLE dbo.sys_user_role(
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  CONSTRAINT pk_user_role PRIMARY KEY(user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY(user_id) REFERENCES dbo.sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_ur_role FOREIGN KEY(role_id) REFERENCES dbo.sys_role(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.sys_role_perm(
  role_id BIGINT NOT NULL,
  perm_code NVARCHAR(128) NOT NULL,
  CONSTRAINT pk_role_perm PRIMARY KEY(role_id, perm_code),
  CONSTRAINT fk_rp_role FOREIGN KEY(role_id) REFERENCES dbo.sys_role(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.sys_menu(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  parent_id BIGINT NULL,
  name NVARCHAR(64) NOT NULL,
  path NVARCHAR(128) NOT NULL,
  icon NVARCHAR(64) NULL,
  perm_code NVARCHAR(128) NULL,
  sort_no INT NOT NULL DEFAULT 0
);
GO

/* ---------------------------
   2) 文件
--------------------------- */
CREATE TABLE dbo.file_object(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  biz_tag NVARCHAR(32) NOT NULL DEFAULT N'MANUSCRIPT',
  original_name NVARCHAR(255) NOT NULL,
  stored_name NVARCHAR(255) NOT NULL,
  content_type NVARCHAR(128) NULL,
  storage_type NVARCHAR(16) NOT NULL DEFAULT N'LOCAL',
  url NVARCHAR(512) NULL,
  local_path NVARCHAR(512) NULL,
  size_bytes BIGINT NULL,
  sha1 NVARCHAR(40) NULL,
  uploader_id BIGINT NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_file_uploader FOREIGN KEY(uploader_id) REFERENCES dbo.sys_user(id) ON DELETE SET NULL
);
GO

/* ---------------------------
   3) 稿件/版本/作者/附件/指派
--------------------------- */
CREATE TABLE dbo.manuscript(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_no NVARCHAR(32) NOT NULL UNIQUE,
  title NVARCHAR(255) NOT NULL,
  abstract_text NVARCHAR(MAX) NULL,
  keywords NVARCHAR(255) NULL,
  discipline NVARCHAR(128) NULL,
  submitter_id BIGINT NOT NULL,
  corresponding_author_id BIGINT NULL,
  status_code NVARCHAR(32) NOT NULL DEFAULT N'DRAFT',
  assigned_editor_id BIGINT NULL,
  latest_version_id BIGINT NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  submitted_at DATETIME2 NULL,
  updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_manu_submitter FOREIGN KEY(submitter_id) REFERENCES dbo.sys_user(id),
  CONSTRAINT fk_manu_corr FOREIGN KEY(corresponding_author_id) REFERENCES dbo.sys_user(id),
  CONSTRAINT fk_manu_editor FOREIGN KEY(assigned_editor_id) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.manuscript_version(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  version_type NVARCHAR(16) NOT NULL DEFAULT N'ORIGINAL',
  cover_letter NVARCHAR(MAX) NULL,
  changes_summary NVARCHAR(MAX) NULL,
  submitted_at DATETIME2 NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT uk_manu_ver UNIQUE(manuscript_id, version_no),
  CONSTRAINT fk_ver_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.manuscript_author(
  manuscript_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  author_order INT NOT NULL DEFAULT 1,
  affiliation NVARCHAR(255) NULL,
  is_corresponding TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT pk_manu_author PRIMARY KEY(manuscript_id, user_id),
  CONSTRAINT fk_ma_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_ma_user FOREIGN KEY(user_id) REFERENCES dbo.sys_user(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.manuscript_file(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  version_id BIGINT NULL,
  file_id BIGINT NOT NULL,
  file_type NVARCHAR(32) NOT NULL,
  is_blind TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  -- ✅ 修复多重级联路径：version_id FK 不使用 SET NULL/CASCADE（默认 NO ACTION）
  CONSTRAINT fk_mf_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_mf_ver  FOREIGN KEY(version_id)   REFERENCES dbo.manuscript_version(id),
  CONSTRAINT fk_mf_file FOREIGN KEY(file_id)      REFERENCES dbo.file_object(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.manuscript_assignment(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  editor_id BIGINT NOT NULL,
  note NVARCHAR(255) NULL,
  assigned_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_assign_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_assign_by FOREIGN KEY(assigned_by) REFERENCES dbo.sys_user(id),
  CONSTRAINT fk_assign_editor FOREIGN KEY(editor_id) REFERENCES dbo.sys_user(id)
);
GO

/* ---------------------------
   4) 邀审/审稿/催审
--------------------------- */
CREATE TABLE dbo.review_invitation(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  editor_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  status NVARCHAR(16) NOT NULL DEFAULT N'PENDING',
  invited_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  response_at DATETIME2 NULL,
  due_date DATETIME2 NULL,
  decline_reason NVARCHAR(255) NULL,
  reminder_count INT NOT NULL DEFAULT 0,
  last_reminder_at DATETIME2 NULL,
  CONSTRAINT uk_invite UNIQUE(manuscript_id, reviewer_id),
  CONSTRAINT fk_inv_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_inv_editor FOREIGN KEY(editor_id) REFERENCES dbo.sys_user(id),
  CONSTRAINT fk_inv_reviewer FOREIGN KEY(reviewer_id) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.review_report(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  invitation_id BIGINT NOT NULL UNIQUE,
  manuscript_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  confidential_comment NVARCHAR(MAX) NULL,
  public_comment NVARCHAR(MAX) NULL,
  score_originality INT NULL,
  score_quality INT NULL,
  score_clarity INT NULL,
  score_significance INT NULL,
  recommendation NVARCHAR(16) NOT NULL,
  submitted_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  -- ✅ 修复多重级联路径：只保留 invitation_id 的级联删除
  CONSTRAINT fk_rr_inv FOREIGN KEY(invitation_id) REFERENCES dbo.review_invitation(id) ON DELETE CASCADE,
  CONSTRAINT fk_rr_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id),
  CONSTRAINT fk_rr_reviewer FOREIGN KEY(reviewer_id) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.review_reminder(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  invitation_id BIGINT NOT NULL,
  editor_id BIGINT NOT NULL,
  message NVARCHAR(255) NULL,
  sent_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_rem_inv FOREIGN KEY(invitation_id) REFERENCES dbo.review_invitation(id) ON DELETE CASCADE,
  CONSTRAINT fk_rem_editor FOREIGN KEY(editor_id) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.editor_recommendation(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  editor_id BIGINT NOT NULL,
  recommendation NVARCHAR(16) NOT NULL,
  summary NVARCHAR(MAX) NULL,
  submitted_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT uk_editor_rec UNIQUE(manuscript_id, editor_id),
  CONSTRAINT fk_er_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_er_editor FOREIGN KEY(editor_id) REFERENCES dbo.sys_user(id)
);
GO

/* ---------------------------
   5) 决策/流转/审稿人库/形式审查/消息
--------------------------- */
CREATE TABLE dbo.manuscript_decision(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  decided_by BIGINT NOT NULL,
  decision NVARCHAR(24) NOT NULL,
  reason NVARCHAR(MAX) NULL,
  decided_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_dec_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_dec_by FOREIGN KEY(decided_by) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.manuscript_flow(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  actor_id BIGINT NULL,
  actor_role NVARCHAR(64) NULL,
  action_code NVARCHAR(64) NOT NULL,
  from_status NVARCHAR(32) NULL,
  to_status NVARCHAR(32) NULL,
  note NVARCHAR(255) NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_flow_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_flow_actor FOREIGN KEY(actor_id) REFERENCES dbo.sys_user(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.reviewer_profile(
  user_id BIGINT PRIMARY KEY,
  affiliation NVARCHAR(255) NULL,
  title NVARCHAR(128) NULL,
  fields NVARCHAR(255) NULL,
  keywords NVARCHAR(255) NULL,
  verified TINYINT NOT NULL DEFAULT 0,
  rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  total_reviews INT NOT NULL DEFAULT 0,
  last_review_at DATETIME2 NULL,
  CONSTRAINT fk_rp_user FOREIGN KEY(user_id) REFERENCES dbo.sys_user(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.format_check(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  manuscript_id BIGINT NOT NULL,
  checker_id BIGINT NOT NULL,
  result NVARCHAR(8) NOT NULL,
  comment NVARCHAR(MAX) NULL,
  checked_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_fc_manu FOREIGN KEY(manuscript_id) REFERENCES dbo.manuscript(id) ON DELETE CASCADE,
  CONSTRAINT fk_fc_checker FOREIGN KEY(checker_id) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.sys_message(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  to_user_id BIGINT NOT NULL,
  from_user_id BIGINT NULL,
  msg_type NVARCHAR(32) NOT NULL DEFAULT N'SYSTEM',
  title NVARCHAR(255) NULL,
  content NVARCHAR(MAX) NULL,
  related_type NVARCHAR(32) NULL,
  related_id BIGINT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  sent_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  -- ✅ 修复多重级联路径：两个 FK 不能同时 CASCADE/SET NULL
  -- 这里保留“收件人删除则消息删除”，发送人不做级联（需要时先把 from_user_id 置空再删用户）
  CONSTRAINT fk_msg_to   FOREIGN KEY(to_user_id)   REFERENCES dbo.sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_msg_from FOREIGN KEY(from_user_id) REFERENCES dbo.sys_user(id)
);
GO

/* ---------------------------
   6) CMS
--------------------------- */
CREATE TABLE dbo.cms_post(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  post_type NVARCHAR(16) NOT NULL,
  title NVARCHAR(255) NOT NULL,
  content NVARCHAR(MAX) NOT NULL,
  status NVARCHAR(16) NOT NULL DEFAULT N'DRAFT',
  author_id BIGINT NOT NULL,
  publish_at DATETIME2 NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_post_author FOREIGN KEY(author_id) REFERENCES dbo.sys_user(id)
);
GO

CREATE TABLE dbo.cms_post_file(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  post_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT uk_post_file UNIQUE(post_id, file_id),
  CONSTRAINT fk_pf_post FOREIGN KEY(post_id) REFERENCES dbo.cms_post(id) ON DELETE CASCADE,
  CONSTRAINT fk_pf_file FOREIGN KEY(file_id) REFERENCES dbo.file_object(id) ON DELETE CASCADE
);
GO

/* ---------------------------
   7) 日志
--------------------------- */
CREATE TABLE dbo.sys_login_log(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NULL,
  username NVARCHAR(64) NULL,
  ip NVARCHAR(64) NULL,
  ua NVARCHAR(255) NULL,
  success TINYINT NOT NULL DEFAULT 1,
  msg NVARCHAR(255) NULL,
  login_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_ll_user FOREIGN KEY(user_id) REFERENCES dbo.sys_user(id) ON DELETE SET NULL
);
GO

CREATE TABLE dbo.sys_op_log(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NULL,
  username NVARCHAR(64) NULL,
  op_type NVARCHAR(32) NULL,
  module NVARCHAR(64) NULL,
  action NVARCHAR(128) NULL,
  method NVARCHAR(16) NULL,
  path NVARCHAR(255) NULL,
  req_params NVARCHAR(MAX) NULL,
  success TINYINT NOT NULL DEFAULT 1,
  error_msg NVARCHAR(255) NULL,
  cost_ms BIGINT NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT fk_ol_user FOREIGN KEY(user_id) REFERENCES dbo.sys_user(id) ON DELETE SET NULL
);
GO

/* ---------------------------
   8) 状态机字典
--------------------------- */
CREATE TABLE dbo.wf_status(
  code NVARCHAR(32) PRIMARY KEY,
  name NVARCHAR(64) NOT NULL,
  visible_roles NVARCHAR(255) NULL,
  sort_no INT NOT NULL DEFAULT 0
);
GO

CREATE TABLE dbo.wf_transition(
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  from_code NVARCHAR(32) NOT NULL,
  action_code NVARCHAR(64) NOT NULL,
  role_code NVARCHAR(64) NOT NULL,
  to_code NVARCHAR(32) NOT NULL,
  CONSTRAINT uk_wf UNIQUE(from_code, action_code, role_code, to_code),
  -- ✅ 修复多重级联路径：两条 FK 都不做级联（状态字典一般不删除）
  CONSTRAINT fk_wft_from FOREIGN KEY(from_code) REFERENCES dbo.wf_status(code),
  CONSTRAINT fk_wft_to   FOREIGN KEY(to_code)   REFERENCES dbo.wf_status(code)
);
GO

/* =========================================================
   9) 初始化数据（账号/角色/权限/状态机/示例数据）
========================================================= */
SET NOCOUNT ON;
GO

-- 角色
SET IDENTITY_INSERT dbo.sys_role ON;
INSERT INTO dbo.sys_role(id, code, name) VALUES
(1,N'ADMIN',N'系统管理员'),
(2,N'EIC',N'主编'),
(3,N'EDITOR',N'编辑'),
(4,N'REVIEWER',N'审稿人'),
(5,N'AUTHOR',N'作者'),
(6,N'CMS',N'前台内容管理员');
SET IDENTITY_INSERT dbo.sys_role OFF;
GO

-- 权限
INSERT INTO dbo.sys_permission(code,name,type) VALUES
(N'SYS:USER:VIEW',N'查看用户',N'API'),
(N'SYS:USER:EDIT',N'编辑用户',N'API'),
(N'SYS:ROLE:VIEW',N'查看角色/权限',N'API'),
(N'SYS:ROLE:EDIT',N'编辑角色/权限',N'API'),
(N'SYS:LOG:VIEW',N'查看日志/审计',N'API'),
(N'MANU:SUBMIT:CREATE',N'作者投稿',N'API'),
(N'MANU:DETAIL:VIEW',N'查看稿件详情',N'API'),
(N'MANU:REVISION:UPLOAD',N'作者修回提交',N'API'),
(N'EIC:DESK:DECIDE',N'主编初审决策',N'API'),
(N'EIC:FINAL:DECIDE',N'主编终审决策',N'API'),
(N'EIC:REVIEWER_POOL:EDIT',N'审稿人库管理',N'API'),
(N'EDITOR:INVITE:SEND',N'编辑邀审',N'API'),
(N'EDITOR:REMIND:SEND',N'编辑催审',N'API'),
(N'EDITOR:RECOMMEND:SUBMIT',N'编辑建议提交',N'API'),
(N'REVIEW:INVITE:RESPOND',N'审稿邀请响应',N'API'),
(N'REVIEW:REPORT:SUBMIT',N'提交审稿意见',N'API'),
(N'CMS:POST:EDIT',N'内容发布管理',N'API'),
(N'CMS:FORMAT_CHECK:DO',N'形式审查处理',N'API');
GO

-- 角色-权限
INSERT INTO dbo.sys_role_perm(role_id, perm_code) VALUES
(1,N'SYS:USER:VIEW'),(1,N'SYS:USER:EDIT'),(1,N'SYS:ROLE:VIEW'),(1,N'SYS:ROLE:EDIT'),(1,N'SYS:LOG:VIEW'),
(2,N'EIC:DESK:DECIDE'),(2,N'EIC:FINAL:DECIDE'),(2,N'EIC:REVIEWER_POOL:EDIT'),(2,N'MANU:DETAIL:VIEW'),
(3,N'EDITOR:INVITE:SEND'),(3,N'EDITOR:REMIND:SEND'),(3,N'EDITOR:RECOMMEND:SUBMIT'),(3,N'MANU:DETAIL:VIEW'),
(4,N'REVIEW:INVITE:RESPOND'),(4,N'REVIEW:REPORT:SUBMIT'),(4,N'MANU:DETAIL:VIEW'),
(5,N'MANU:SUBMIT:CREATE'),(5,N'MANU:DETAIL:VIEW'),(5,N'MANU:REVISION:UPLOAD'),
(6,N'CMS:POST:EDIT'),(6,N'CMS:FORMAT_CHECK:DO'),(6,N'MANU:DETAIL:VIEW');
GO

-- 账号（统一密码 123456；这里存 bcrypt hash）
SET IDENTITY_INSERT dbo.sys_user ON;
INSERT INTO dbo.sys_user(id, username, password_hash, real_name, status) VALUES
(1,N'admin',   N'$2a$10$sZRBgvwR2wps4X6Rf2URNOpJMbJFOEgGBqtS6h83H1Gu0.afTozjS',N'管理员',1),
(2,N'eic1',    N'$2a$10$sZRBgvwR2wps4X6Rf2URNOpJMbJFOEgGBqtS6h83H1Gu0.afTozjS',N'主编1',1),
(3,N'editor1', N'$2a$10$sZRBgvwR2wps4X6Rf2URNOpJMbJFOEgGBqtS6h83H1Gu0.afTozjS',N'编辑1',1),
(4,N'reviewer1',N'$2a$10$sZRBgvwR2wps4X6Rf2URNOpJMbJFOEgGBqtS6h83H1Gu0.afTozjS',N'审稿人1',1),
(5,N'author1', N'$2a$10$sZRBgvwR2wps4X6Rf2URNOpJMbJFOEgGBqtS6h83H1Gu0.afTozjS',N'作者1',1),
(6,N'cms1',    N'$2a$10$sZRBgvwR2wps4X6Rf2URNOpJMbJFOEgGBqtS6h83H1Gu0.afTozjS',N'内容管理员1',1);
SET IDENTITY_INSERT dbo.sys_user OFF;
GO

-- 用户-角色
INSERT INTO dbo.sys_user_role(user_id, role_id) VALUES (1,1),(2,2),(3,3),(4,4),(5,5),(6,6);
GO

-- 审稿人档案
INSERT INTO dbo.reviewer_profile(user_id, affiliation, title, fields, keywords, verified, rating, total_reviews)
VALUES (4,N'北京林业大学',N'副教授',N'计算机科学/人工智能',N'NLP,信息检索,深度学习',1,4.80,12);
GO

-- 状态机
INSERT INTO dbo.wf_status(code,name,visible_roles,sort_no) VALUES
(N'DRAFT',N'草稿',N'AUTHOR',1),
(N'SUBMITTED',N'已提交',N'AUTHOR,EDITOR,EIC,CMS',2),
(N'FORMAL_CHECK',N'形式审查',N'CMS,EIC,EDITOR,AUTHOR',3),
(N'UNDER_REVIEW',N'外审中',N'AUTHOR,EDITOR,EIC,REVIEWER',4),
(N'REVISION',N'修回中',N'AUTHOR,EDITOR,EIC',5),
(N'ACCEPTED',N'录用',N'AUTHOR,EDITOR,EIC',6),
(N'REJECTED',N'拒稿',N'AUTHOR,EDITOR,EIC',7),
(N'ARCHIVED',N'归档',N'ADMIN,EIC,EDITOR',8);
GO

INSERT INTO dbo.wf_transition(from_code, action_code, role_code, to_code) VALUES
(N'DRAFT',N'SUBMIT',N'AUTHOR',N'SUBMITTED'),
(N'SUBMITTED',N'FORMAL_CHECK_PASS',N'CMS',N'UNDER_REVIEW'),
(N'SUBMITTED',N'FORMAL_CHECK_FAIL',N'CMS',N'FORMAL_CHECK'),
(N'FORMAL_CHECK',N'RESUBMIT',N'AUTHOR',N'SUBMITTED'),
(N'UNDER_REVIEW',N'EIC_DECIDE_REVISION',N'EIC',N'REVISION'),
(N'UNDER_REVIEW',N'EIC_DECIDE_REJECT',N'EIC',N'REJECTED'),
(N'UNDER_REVIEW',N'EIC_DECIDE_ACCEPT',N'EIC',N'ACCEPTED'),
(N'REVISION',N'SUBMIT_REVISION',N'AUTHOR',N'UNDER_REVIEW'),
(N'ACCEPTED',N'ARCHIVE',N'EIC',N'ARCHIVED');
GO

/* ---------------------------
   示例数据：一条稿件全流程（保持在同一批次，避免 GO 导致变量失效）
--------------------------- */
DECLARE @MANU_ID BIGINT, @VER1 BIGINT, @INV_ID BIGINT;

INSERT INTO dbo.manuscript(manuscript_no,title,abstract_text,keywords,discipline,submitter_id,corresponding_author_id,status_code,assigned_editor_id,submitted_at)
VALUES (N'MS20251216-0001',N'基于Transformer的森林病虫害识别研究',N'这是一个示例摘要',N'Transformer;CV;Forest',N'计算机',5,5,N'SUBMITTED',3,SYSDATETIME());
SET @MANU_ID = SCOPE_IDENTITY();

INSERT INTO dbo.manuscript_version(manuscript_id,version_no,version_type,cover_letter,submitted_at)
VALUES (@MANU_ID,1,N'ORIGINAL',N'Cover letter 示例',SYSDATETIME());
SET @VER1 = SCOPE_IDENTITY();

UPDATE dbo.manuscript SET latest_version_id=@VER1 WHERE id=@MANU_ID;

INSERT INTO dbo.manuscript_author(manuscript_id,user_id,author_order,affiliation,is_corresponding)
VALUES (@MANU_ID,5,1,N'北京林业大学',1);

INSERT INTO dbo.manuscript_flow(manuscript_id,actor_id,actor_role,action_code,from_status,to_status,note) VALUES
(@MANU_ID,5,N'AUTHOR',N'SUBMIT',N'DRAFT',N'SUBMITTED',N'作者提交稿件'),
(@MANU_ID,3,N'EDITOR',N'ASSIGN_EDITOR',N'SUBMITTED',N'SUBMITTED',N'主编/系统指派编辑 editor1');

INSERT INTO dbo.manuscript_assignment(manuscript_id,assigned_by,editor_id,note)
VALUES (@MANU_ID,2,3,N'指派编辑处理');

INSERT INTO dbo.format_check(manuscript_id,checker_id,result,comment)
VALUES (@MANU_ID,6,N'PASS',N'格式与附件齐全');

UPDATE dbo.manuscript SET status_code=N'UNDER_REVIEW' WHERE id=@MANU_ID;
INSERT INTO dbo.manuscript_flow(manuscript_id,actor_id,actor_role,action_code,from_status,to_status,note)
VALUES (@MANU_ID,6,N'CMS',N'FORMAL_CHECK_PASS',N'SUBMITTED',N'UNDER_REVIEW',N'形式审查通过，进入外审');

INSERT INTO dbo.review_invitation(manuscript_id,editor_id,reviewer_id,status,due_date)
VALUES (@MANU_ID,3,4,N'PENDING',DATEADD(DAY,14,SYSDATETIME()));
SET @INV_ID = SCOPE_IDENTITY();

INSERT INTO dbo.manuscript_flow(manuscript_id,actor_id,actor_role,action_code,from_status,to_status,note)
VALUES (@MANU_ID,3,N'EDITOR',N'INVITE_REVIEWER',N'UNDER_REVIEW',N'UNDER_REVIEW',N'邀请 reviewer1 审稿');

UPDATE dbo.review_invitation SET status=N'ACCEPTED', response_at=SYSDATETIME() WHERE id=@INV_ID;

INSERT INTO dbo.review_report(invitation_id,manuscript_id,reviewer_id,confidential_comment,public_comment,
                              score_originality,score_quality,score_clarity,score_significance,recommendation)
VALUES (@INV_ID,@MANU_ID,4,N'给编辑：创新性尚可',N'给作者：建议补充实验对比',4,4,3,4,N'MINOR');

UPDATE dbo.review_invitation SET status=N'SUBMITTED' WHERE id=@INV_ID;

INSERT INTO dbo.manuscript_flow(manuscript_id,actor_id,actor_role,action_code,from_status,to_status,note)
VALUES (@MANU_ID,4,N'REVIEWER',N'SUBMIT_REVIEW',N'UNDER_REVIEW',N'UNDER_REVIEW',N'提交审稿意见');

INSERT INTO dbo.editor_recommendation(manuscript_id,editor_id,recommendation,summary)
VALUES (@MANU_ID,3,N'MINOR',N'建议小修，补充对比实验后可接收');

INSERT INTO dbo.manuscript_flow(manuscript_id,actor_id,actor_role,action_code,from_status,to_status,note)
VALUES (@MANU_ID,3,N'EDITOR',N'EDITOR_RECOMMEND',N'UNDER_REVIEW',N'UNDER_REVIEW',N'提交编辑建议');

INSERT INTO dbo.manuscript_decision(manuscript_id,decided_by,decision,reason)
VALUES (@MANU_ID,2,N'REVISION',N'请作者补充对比实验并完善图表说明');

UPDATE dbo.manuscript SET status_code=N'REVISION' WHERE id=@MANU_ID;
INSERT INTO dbo.manuscript_flow(manuscript_id,actor_id,actor_role,action_code,from_status,to_status,note)
VALUES (@MANU_ID,2,N'EIC',N'EIC_DECIDE_REVISION',N'UNDER_REVIEW',N'REVISION',N'主编给出小修决定');

INSERT INTO dbo.sys_message(to_user_id,from_user_id,msg_type,title,content,related_type,related_id) VALUES
(5,2,N'WORKFLOW',N'稿件需要修回',N'您的稿件 MS20251216-0001 需要小修，请在截止日前提交修回版本。',N'MANUSCRIPT',@MANU_ID),
(3,2,N'WORKFLOW',N'已下发修回决定',N'请跟进作者修回并准备复审/终审建议。',N'MANUSCRIPT',@MANU_ID);

INSERT INTO dbo.cms_post(post_type,title,content,status,author_id,publish_at)
VALUES (N'NOTICE',N'征稿通知：2026春季专刊',N'欢迎投稿，主题：智慧林业与AI',N'PUBLISHED',6,SYSDATETIME());
GO
