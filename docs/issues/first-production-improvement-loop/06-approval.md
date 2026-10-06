# I06：从待审批提议到管理者授权

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/7)。

## 类型与状态

类型：AFK（设计输入齐全后）。拟发布标签：`ready-for-human`。

## PRD 绑定

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖的用户故事：US-7、US-8。

## 构建内容

从真实待审批提议进入管理者审阅、批准或驳回，保存具体参数与证据版本授权及审计，并让工程师查看审批结果。状态冲突、权限与自审批由后端保证。

## UI 模式

spec-driven。

## 页面绑定

platform-id：`web-workbench`。page-id：`approval-queue`。
壳层关系：继承 web-workbench 的 app-shell，选中“改善审批”。
布局唯一权威来源：PRD「页面清单」中 `approval-queue` 的完整 UI 设计描述。

## PRD 必读

1. 父 PRD「页面清单」中 `approval-queue` 条目全文。
2. 父 PRD「状态策略」章节。
3. 父 PRD「用户故事」：US-7、US-8。
4. 父 PRD「页面清单」中 web-workbench 的 app-shell 条目及壳层变体规则。
5. `docs/design/DESIGN.md`：§5 审批表格、详情侧栏、审批意见、冲突提示、骨架、空态、局部错误，以及 §6 宜忌；当前缺失，待 [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 建立。
6. N/A（单端，不要求 platforms.md）。
7. N/A（spec-driven，以 PRD 文字实现）。

## 业务依据补充

实现决策：PRD 实现决策 › 改善提议与审批状态机；PRD 实现决策 › 演示角色与后端授权；PRD 实现决策 › 重复投递与业务幂等；PRD 实现决策 › 工作台服务接口。
测试决策：PRD 测试决策 › Spring Boot API 与业务审批测试边界。
切片包含本页面主路径所需的持久化、业务服务与浏览器集成，不另开横向数据库或 API 工单。

## States 矩阵

| state | PRD 来源 | 可观察预期 |
| --- | --- | --- |
| default | PRD 页面清单 §approval-queue「UI 设计描述」 | 提议列表可筛选，侧栏显示依据、参数、验证条件及诊断入口。 |
| loading | PRD「状态策略」+ PRD 页面清单 §approval-queue | 列表局部骨架，刷新保留位置与旧内容。 |
| empty | PRD「状态策略」+ PRD 页面清单 §approval-queue | 无待审提议与筛选无结果分别提示。 |
| review | PRD「状态策略」+ PRD 页面清单 §approval-queue | 批准前确认参数，驳回须填理由。 |
| submitting | PRD「状态策略」+ PRD 页面清单 §approval-queue | 提交中防重；失败保留意见。 |
| read-only | PRD「状态策略」+ PRD 页面清单 §approval-queue | 已处理记录展示人、时间和意见；工程师只读。 |
| conflict | PRD「状态策略」+ PRD 页面清单 §approval-queue | 并发已处理提示并刷新，不覆盖另一决定。 |
| error | PRD「状态策略」+ PRD 页面清单 §approval-queue | 局部错误可重试，权限拒绝不改变状态。 |

## UI 输入

- [ ] [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 的 DESIGN.md 已就绪，§5 原语与 §6 宜忌可定位。
- [x] 父 PRD 已发布，本文已链接至实际父 Issue。
- [ ] 依赖项已完成；不得仅因标题为 AFK 就跳过前置条件。

## 验收标准

### 功能

- [ ] 管理者能批准具体版本或附理由驳回，审计记录操作者、时间、前后状态及意见（US-7）。
- [ ] 工程师能查看审批结果但不能审批，未知主体和自审批被后端拒绝（US-8）。
- [ ] 并发审批仅一个有效决定；重复提交不重复审计或覆盖结果，返回列表保留位置（US-7、US-8）。

### UI 行为

- [ ] States 矩阵逐行验证；局部失败不抹掉可用数据，业务未知不显示为零或正常。
- [ ] 刷新、返回、重复点击和权限拒绝得到 PRD 约定结果；只读或禁用界面不能替代后端校验。

### 设计 QA

- [ ] 对照 PRD 页面清单 §approval-queue 和 DESIGN.md §5/§6 检查布局、层级及交互，不复制或擅改全局视觉规范。
- [ ] 桌面与窄屏截图验证文字、图表、按钮无不合理重叠；宽表格在自身区域滚动。

## 依赖

- [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2)
- [I02](https://github.com/qwerqazo21l9-eng/IIoT/issues/3)
- [I05](https://github.com/qwerqazo21l9-eng/IIoT/issues/6)

## 阻塞项

当前缺少 DESIGN.md，按技能规则初始为 ready-for-human；[D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 合并且依赖完成后复核为 ready-for-agent，不需要重新拆分本页状态。
