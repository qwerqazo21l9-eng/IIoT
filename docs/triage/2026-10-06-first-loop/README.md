# 首批工单分拣建议

日期：2026-10-06（Asia/Shanghai）。目标仓库：[qwerqazo21l9-eng/IIoT](https://github.com/qwerqazo21l9-eng/IIoT)。
状态：检查与评论草稿已完成；尚未修改 GitHub 标签、正文或评论，等待维护者确认建议。

## 检查证据

- 已读取十张 Issue 全文、评论、标签、报告人与时间；全部 OPEN，均无评论。没有多个状态角色冲突。
- #2 已有 design-input，其余九张缺少类别角色，建议补 enhancement。
- 五张 UI 工单的七项必读、父 PRD 页面/状态策略、布局来源、状态矩阵和三类验收完整；缺 DESIGN.md，保留 ready-for-human。
- #4、#9、#10 的三个必读项均完整，实现及测试定位词能在父 PRD 唯一找到。
- #9 依赖 #5/#6，#10 依赖 #4–#9；均无完成证据，不满足功能工单 Agent 就绪门禁。
- 本地只有文档，没有代码、既有测试或 DESIGN.md；GitHub contents API 明确返回 repository is empty，远端克隆缺少本地 CONTEXT/ADR。
- 未发现范围外知识库或与历史拒绝请求重复的证据；不存在 bug 复现任务。

## 拟应用的分拣结果

| 工单 | 类别 | 建议状态 | 理由 |
| --- | --- | --- | --- |
| [#1 父 PRD](https://github.com/qwerqazo21l9-eng/IIoT/issues/1) | enhancement | 保留 ready-for-agent | 需求汇总规格就绪，不作为一次可领取的实现工单 |
| [#2 全局视觉规范](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) | design-input | 保留 ready-for-agent | 本地无前置依赖，从零建立设计输入 |
| [#3 工作台框架](https://github.com/qwerqazo21l9-eng/IIoT/issues/3) | enhancement | 保留 ready-for-human | 等待 #2 的设计输入 |
| [#4 正常生产链路](https://github.com/qwerqazo21l9-eng/IIoT/issues/4) | enhancement | 保留 ready-for-agent | 三项必读与验收完整，本地可从零实现 |
| [#5 产线概览](https://github.com/qwerqazo21l9-eng/IIoT/issues/5) | enhancement | 保留 ready-for-human | 等待设计、壳层和正常链路 |
| [#6 证据与提议](https://github.com/qwerqazo21l9-eng/IIoT/issues/6) | enhancement | 保留 ready-for-human | 等待设计、壳层和诊断结果 |
| [#7 管理者审批](https://github.com/qwerqazo21l9-eng/IIoT/issues/7) | enhancement | 保留 ready-for-human | 等待设计、壳层和提议路径 |
| [#8 配对实验](https://github.com/qwerqazo21l9-eng/IIoT/issues/8) | enhancement | 保留 ready-for-human | 等待设计、壳层和审批路径 |
| [#9 事件质量验证](https://github.com/qwerqazo21l9-eng/IIoT/issues/9) | enhancement | 改为 needs-triage | #5/#6 未完成，需依赖可用后重新评估 |
| [#10 整体验收](https://github.com/qwerqazo21l9-eng/IIoT/issues/10) | enhancement | 改为 needs-triage | #4–#9 未完成，需依赖可用后重新评估 |

当前仅 #2 和 #4 是可在共享工作区启动的实现任务。若改为远端克隆执行，先同步领域与架构文档，再复核该执行环境的就绪状态。本次不推送或初始化 Git。

## 评论与修改草稿

- [#1 评论草稿](comments/01.md)
- [#2 评论草稿](comments/02.md)
- [#3 评论草稿](comments/03.md)
- [#4 评论草稿](comments/04.md)
- [#5 评论草稿](comments/05.md)
- [#6 评论草稿](comments/06.md)
- [#7 评论草稿](comments/07.md)
- [#8 评论草稿](comments/08.md)
- [#9 评论草稿](comments/09.md)
- [#10 评论草稿](comments/10.md)

- [#9 正文状态修订](bodies/09.md) 与 [#10 正文状态修订](bodies/10.md)：消除正文与建议标签冲突，不改变功能范围。
- [应用清单](actions.json)：含预期标签/更新时间、拟增删标签及评论正文。应用前重新检查远端状态，避免覆盖新讨论。

所有拟发布评论及修订正文均以前置 AI 分拣声明开头。UI 七项与功能三项必读从对应远端正文原样复制，不改写为另一套规范。#9/#10 未给出可执行 Agent 简报，因为当前依赖门禁未通过。

## 确认依据

triage 技能要求在提出类别和状态建议后“等待指示”。确认本方案后，再应用标签、发布评论、修订 #9/#10 正文并核验每张工单恰好一个类别和一个状态角色。当前没有关闭或修改任何远端 Issue。
