# Implementation Plan: 稿件阶段时间追踪

## Overview

本实现计划将稿件阶段时间追踪功能分解为数据库、Model、DAO、业务集成和界面展示五个部分，按顺序实现。

## Tasks

- [x] 1. 创建数据库表
  - [x] 1.1 在 sqlserver.sql 中添加 ManuscriptStageTimestamps 表创建脚本
    - 创建表结构，包含 ManuscriptId 主键和9个可空时间戳字段
    - 添加外键约束关联 Manuscripts 表
    - 使用 IF OBJECT_ID 检查避免重复创建
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. 创建 Model 类
  - [x] 2.1 创建 ManuscriptStageTimestamps.java 实体类
    - 在 edu.bjfu.onlinesm.model 包下创建
    - 包含所有字段的 getter/setter 方法
    - 添加根据状态码获取对应时间戳的辅助方法
    - _Requirements: 4.1_

- [x] 3. 创建 DAO 类
  - [x] 3.1 创建 ManuscriptStageTimestampsDAO.java
    - 实现 findByManuscriptId() 方法
    - 实现 exists() 方法
    - 实现 create() 方法
    - 实现 updateStageCompletedAt() 方法
    - 实现 ensureAndUpdateStage() 组合方法
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ]* 3.2 编写 DAO 层单元测试
    - 测试各方法的基本功能
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 4. 集成状态变更逻辑
  - [x] 4.1 修改 ManuscriptDAO 集成时间戳记录
    - 在 updateStatusWithHistory() 方法中添加时间戳记录调用
    - 在 assignEditorWithHistory() 方法中添加时间戳记录调用
    - 在其他状态变更方法中添加时间戳记录调用
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10_

- [ ]* 4.2 编写属性测试：状态变更时间戳记录一致性
    - **Property 1: 状态变更时间戳记录一致性**
    - **Validates: Requirements 2.1-2.9**

- [x] 5. 修改界面展示
  - [x] 5.1 修改 ManuscriptServlet 获取时间戳数据
    - 在 track 方法中查询 ManuscriptStageTimestamps
    - 将时间戳数据传递给 JSP 页面
    - _Requirements: 3.1_

  - [x] 5.2 修改 manuscript_track.jsp 显示完成时间
    - 在时间线每个已完成阶段下显示完成时间
    - 使用 yyyy-MM-dd HH:mm 格式
    - 仅当时间戳非空时显示
    - _Requirements: 3.2, 3.3, 3.4_

- [ ]* 5.3 编写属性测试：时间戳显示格式正确性
    - **Property 2: 时间戳显示格式正确性**
    - **Validates: Requirements 3.2, 3.4**

- [x] 6. Checkpoint - 验证功能完整性
  - 确保所有测试通过
  - 验证界面正确显示时间信息
  - 如有问题请告知

## Notes

- 任务标记 `*` 为可选测试任务，可跳过以加快 MVP 开发
- 每个任务引用了具体的需求条目以便追溯
- 建议按顺序执行，确保依赖关系正确
