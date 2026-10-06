# 首个产线诊断与改善闭环：已发布工单

来源：[父 PRD](../../prd/first-production-improvement-loop.md)。日期：2026-10-06。
共九张工单：一张全局设计输入、一张整体框架、四张功能页面垂直切片、三张服务/验收切片。
已发布至 qwerqazo21l9-eng/IIoT：[父 PRD Issue #1](https://github.com/qwerqazo21l9-eng/IIoT/issues/1) 及九张子工单。以下本地正文已同步发布版本；GitHub Issues 是正式跟踪器。

## 拆解与依赖

| 顺序 | 本地标识及正文 | 类型 | 依赖 | 覆盖故事 | 拟发布标签 |
| --- | --- | --- | --- | --- | --- |
| 1 | [D01 全局视觉规范](01-design-global.md) | design-input | 无 | 支撑 US-1 至 US-11 | design-input、ready-for-agent |
| 2 | [I02 工作台框架](02-app-shell.md) | AFK，待设计 | D01 | —（壳层） | ready-for-human |
| 3 | [I03 正常生产到 JPH 查询](03-normal-production-path.md) | AFK | 无，可与 D01 并行 | US-1、US-12、US-13、US-15 | ready-for-agent |
| 4 | [I04 概览与候选诊断](04-line-overview.md) | AFK，待设计 | D01、I02、I03 | US-1 至 US-3 | ready-for-human |
| 5 | [I05 证据与改善提议](05-diagnosis-proposal.md) | AFK，待设计 | D01、I02、I04 | US-4 至 US-6 | ready-for-human |
| 6 | [I06 管理者审批](06-approval.md) | AFK，待设计 | D01、I02、I05 | US-7、US-8 | ready-for-human |
| 7 | [I07 实验与收益报告](07-paired-experiment.md) | AFK，待设计 | D01、I02、I06 | US-9 至 US-11 | ready-for-human |
| 8 | [I08 事件质量与重复验证](08-event-quality.md) | AFK | I04、I05 | US-2、US-5、US-12 至 US-14 | ready-for-agent |
| 9 | [I09 可复现整体验收](09-reproducible-acceptance.md) | AFK | I03 至 I08 | US-1 至 US-15 | ready-for-agent |

I03 是可独立验证的服务路径，而不是单独建数据库或消息平台。I04 至 I07 各自交付一页完整状态及所需服务行为，严格保留父 PRD 页面边界。I08 的核心质量语义在前置切片已实现，本工单负责贯通异常输入、查询结果与契约验证，不将页面状态留待后补。

## 发布与就绪规则

1. 目标仓库已明确；父 PRD 已发布，拆解已按用户提供目标仓库的指示执行。
2. 工单正文中的父 PRD 和依赖已转换为真实 GitHub Issue 链接，本地 D01/I02 等仅作为索引标识。
3. 已按依赖顺序发布，已补齐五个 triage 状态及 design-input 分类标签。
4. 当前 DESIGN.md 缺失，全部 UI 工单初始 ready-for-human；D01 完成并核对依赖后再转 ready-for-agent。
5. 服务工单 ready-for-agent 表示规格可由 Agent 执行，依赖未完成时不能提前实施。无新增 HITL 工单；设计与最终 PR 仍需常规评审。
6. 不关闭或修改父 Issue，不改变既有 PRD 的页面、状态或范围。

## 核验结果

已读取十张远端 Issue，核对正文、父 PRD 链接、依赖、状态矩阵和标签。每张 UI 工单含七项 PRD 必读、完整 States 矩阵和功能/UI 行为/设计 QA 验收；每张服务工单含三个 PRD 必读项和逐条 PRD 依据。设计输入缺口由 D01 明确承接。

## GitHub 发布映射

| 本地标识 | 已发布工单 |
| --- | --- |
| D01 | [Issue #2](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) |
| I02 | [Issue #3](https://github.com/qwerqazo21l9-eng/IIoT/issues/3) |
| I03 | [Issue #4](https://github.com/qwerqazo21l9-eng/IIoT/issues/4) |
| I04 | [Issue #5](https://github.com/qwerqazo21l9-eng/IIoT/issues/5) |
| I05 | [Issue #6](https://github.com/qwerqazo21l9-eng/IIoT/issues/6) |
| I06 | [Issue #7](https://github.com/qwerqazo21l9-eng/IIoT/issues/7) |
| I07 | [Issue #8](https://github.com/qwerqazo21l9-eng/IIoT/issues/8) |
| I08 | [Issue #9](https://github.com/qwerqazo21l9-eng/IIoT/issues/9) |
| I09 | [Issue #10](https://github.com/qwerqazo21l9-eng/IIoT/issues/10) |
