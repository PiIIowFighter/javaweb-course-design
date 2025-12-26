# Requirements Document

## Introduction

本功能为稿件管理系统新增阶段时间追踪能力。通过创建一张专门的数据表，记录每份稿件在各个审稿阶段的完成时间，并在作者的"状态追踪"界面的时间线上显示这些时间信息。

## Glossary

- **Manuscript_Stage_Timestamps**: 稿件阶段时间戳表，存储每份稿件各阶段的完成时间
- **Stage_Completion_Time**: 阶段完成时间，指稿件离开某个阶段进入下一阶段的时间点
- **Timeline_View**: 时间线视图，作者在"我的稿件-状态追踪"界面看到的流程进度展示
- **Manuscript**: 稿件实体，对应 dbo.Manuscripts 表

## Requirements

### Requirement 1: 创建稿件阶段时间戳数据表

**User Story:** As a 系统管理员, I want 系统能存储每份稿件各阶段的完成时间, so that 可以追踪稿件在各阶段的处理效率。

#### Acceptance Criteria

1. THE Manuscript_Stage_Timestamps 表 SHALL 包含 ManuscriptId 字段作为外键关联 Manuscripts 表
2. THE Manuscript_Stage_Timestamps 表 SHALL 包含以下可空的时间戳字段：
   - DraftCompletedAt（草稿编辑完成时间）
   - SubmittedAt（已提交待处理完成时间）
   - FormalCheckCompletedAt（形式审查完成时间）
   - DeskReviewInitialCompletedAt（案头初筛完成时间）
   - ToAssignCompletedAt（待分配编辑完成时间）
   - WithEditorCompletedAt（编辑处理完成时间）
   - UnderReviewCompletedAt（外审完成时间）
   - EditorRecommendationCompletedAt（编辑推荐意见完成时间）
   - FinalDecisionPendingCompletedAt（待主编终审完成时间）
3. WHEN 稿件不存在对应记录时 THEN THE System SHALL 允许所有时间戳字段为 NULL
4. THE Manuscript_Stage_Timestamps 表 SHALL 与 Manuscripts 表建立一对一关系

### Requirement 2: 自动记录阶段完成时间

**User Story:** As a 系统, I want 在稿件状态变更时自动记录阶段完成时间, so that 时间数据能准确反映实际流程。

#### Acceptance Criteria

1. WHEN 稿件状态从 DRAFT 变更为其他状态 THEN THE System SHALL 记录 DraftCompletedAt 为当前时间
2. WHEN 稿件状态从 SUBMITTED 变更为其他状态 THEN THE System SHALL 记录 SubmittedAt 为当前时间
3. WHEN 稿件状态从 FORMAL_CHECK 变更为其他状态 THEN THE System SHALL 记录 FormalCheckCompletedAt 为当前时间
4. WHEN 稿件状态从 DESK_REVIEW_INITIAL 变更为其他状态 THEN THE System SHALL 记录 DeskReviewInitialCompletedAt 为当前时间
5. WHEN 稿件状态从 TO_ASSIGN 变更为其他状态 THEN THE System SHALL 记录 ToAssignCompletedAt 为当前时间
6. WHEN 稿件状态从 WITH_EDITOR 变更为其他状态 THEN THE System SHALL 记录 WithEditorCompletedAt 为当前时间
7. WHEN 稿件状态从 UNDER_REVIEW 变更为其他状态 THEN THE System SHALL 记录 UnderReviewCompletedAt 为当前时间
8. WHEN 稿件状态从 EDITOR_RECOMMENDATION 变更为其他状态 THEN THE System SHALL 记录 EditorRecommendationCompletedAt 为当前时间
9. WHEN 稿件状态从 FINAL_DECISION_PENDING 变更为其他状态 THEN THE System SHALL 记录 FinalDecisionPendingCompletedAt 为当前时间
10. IF 对应稿件的时间戳记录不存在 THEN THE System SHALL 先创建记录再更新时间戳

### Requirement 3: 在时间线界面显示阶段完成时间

**User Story:** As a 作者, I want 在状态追踪界面的时间线上看到每个已完成阶段的完成时间, so that 我能了解稿件的处理进度和效率。

#### Acceptance Criteria

1. WHEN 作者访问稿件状态追踪页面 THEN THE Timeline_View SHALL 从 Manuscript_Stage_Timestamps 表获取时间数据
2. WHEN 某阶段的完成时间不为空 THEN THE Timeline_View SHALL 在该阶段节点下方显示完成时间
3. WHEN 某阶段的完成时间为空 THEN THE Timeline_View SHALL 不显示时间信息
4. THE Timeline_View SHALL 以 "yyyy-MM-dd HH:mm" 格式显示完成时间

### Requirement 4: 数据访问层支持

**User Story:** As a 开发者, I want 有完整的 DAO 层支持时间戳数据的增删改查, so that 业务逻辑可以方便地操作数据。

#### Acceptance Criteria

1. THE ManuscriptStageTimestampsDAO SHALL 提供根据 ManuscriptId 查询时间戳记录的方法
2. THE ManuscriptStageTimestampsDAO SHALL 提供创建新时间戳记录的方法
3. THE ManuscriptStageTimestampsDAO SHALL 提供更新指定阶段完成时间的方法
4. THE ManuscriptStageTimestampsDAO SHALL 提供检查记录是否存在的方法
