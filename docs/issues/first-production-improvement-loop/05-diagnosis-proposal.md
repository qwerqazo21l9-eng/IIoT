# I05：从诊断证据到待审批改善提议

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/6)。

## 类型与状态

类型：AFK（设计输入齐全后）。拟发布标签：`ready-for-human`。

## PRD 绑定

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖的用户故事：US-4、US-5、US-6。

## 构建内容

贯通诊断证据查询、跨工位时间线、工程师提议表单与待审批记录保存。包含白名单参数校验、版本绑定、幂等提交及审计起点；不在本切片实施批准或执行改善。

## UI 模式

spec-driven。

## 页面绑定

platform-id：`web-workbench`。page-id：`diagnosis-detail`。
壳层关系：继承 web-workbench 的 app-shell，选中“产线概览”。
布局唯一权威来源：PRD「页面清单」中 `diagnosis-detail` 的完整 UI 设计描述。

## PRD 必读

1. 父 PRD「页面清单」中 `diagnosis-detail` 条目全文。
2. 父 PRD「状态策略」章节。
3. 父 PRD「用户故事」：US-4、US-5、US-6。
4. 父 PRD「页面清单」中 web-workbench 的 app-shell 条目及壳层变体规则。
5. `docs/design/DESIGN.md`：§5 时间线、证据列表、表单侧栏、骨架、空态、局部错误，以及 §6 宜忌；当前缺失，待 [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 建立。
6. N/A（单端，不要求 platforms.md）。
7. N/A（spec-driven，以 PRD 文字实现）。

## 业务依据补充

实现决策：PRD 实现决策 › 工位状态与规则诊断；PRD 实现决策 › 改善提议与审批状态机；PRD 实现决策 › 重复投递与业务幂等；PRD 实现决策 › 演示角色与后端授权；PRD 实现决策 › 工作台服务接口。
测试决策：PRD 测试决策 › Kafka 与 Flink 计算输出测试边界；PRD 测试决策 › Spring Boot API 与业务审批测试边界。
切片包含本页面主路径所需的持久化、业务服务与浏览器集成，不另开横向数据库或 API 工单。

## States 矩阵

| state | PRD 来源 | 可观察预期 |
| --- | --- | --- |
| default | PRD 页面清单 §diagnosis-detail「UI 设计描述」 | 诊断标识、时间区间、候选源、影响工位、时间线与证据可核对。 |
| loading | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 时间线和证据局部加载，保留返回入口。 |
| empty | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 没有关联提议时显示可创建入口；无证据说明原因。 |
| undetermined | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 显示无法判定、排除理由和时间可信度，不转成确认故障。 |
| form-open | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 侧栏包含 PRD 规定字段，主页面上下文仍可见。 |
| invalid | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 参数或必填依据错误在字段处显示，输入保留。 |
| submitting | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 防重复提交；失败保留表单，成功产生一个待审批记录。 |
| read-only | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 已提交提议只读；非工程师无提交权限。 |
| error | PRD「状态策略」+ PRD 页面清单 §diagnosis-detail | 局部失败可重试；对象不存在区别于服务故障。 |

## UI 输入

- [ ] [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 的 DESIGN.md 已就绪，§5 原语与 §6 宜忌可定位。
- [x] 父 PRD 已发布，本文已链接至实际父 Issue。
- [ ] 依赖项已完成；不得仅因标题为 AFK 就跳过前置条件。

## 验收标准

### 功能

- [ ] 缩放和选择时间线片段可查工件、原始时间、持续时间、质量与配置版本，证据不依赖真值（US-4、US-5）。
- [ ] 有效提议绑定证据版本与白名单动作，工程师提交后持久化待审批；重复请求仅有一条业务记录（US-6）。
- [ ] 错误参数或非法身份不能改变状态；已提交提议不可原地修改批准依据；返回概览保留筛选（US-5、US-6）。

### UI 行为

- [ ] States 矩阵逐行验证；局部失败不抹掉可用数据，业务未知不显示为零或正常。
- [ ] 刷新、返回、重复点击和权限拒绝得到 PRD 约定结果；只读或禁用界面不能替代后端校验。

### 设计 QA

- [ ] 对照 PRD 页面清单 §diagnosis-detail 和 DESIGN.md §5/§6 检查布局、层级及交互，不复制或擅改全局视觉规范。
- [ ] 桌面与窄屏截图验证文字、图表、按钮无不合理重叠；宽表格在自身区域滚动。

## 依赖

- [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2)
- [I02](https://github.com/qwerqazo21l9-eng/IIoT/issues/3)
- [I04](https://github.com/qwerqazo21l9-eng/IIoT/issues/5)

## 阻塞项

当前缺少 DESIGN.md，按技能规则初始为 ready-for-human；[D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 合并且依赖完成后复核为 ready-for-agent，不需要重新拆分本页状态。
