# I04：从工位异常到产线概览候选诊断

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/5)。

## 类型与状态

类型：AFK（设计输入齐全后）。拟发布标签：`ready-for-human`。

## PRD 绑定

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖的用户故事：US-1、US-2、US-3。

## 构建内容

在真实链路上增加产品与动作规则、跨工位状态和候选诊断，再将指标、拓扑、证据质量与可筛选列表展示在产线概览。包含本页所需状态结果保存和查询 API；诊断主结果在本切片产生，详情与提议由后续切片完成。

## UI 模式

spec-driven。

## 页面绑定

platform-id：`web-workbench`。page-id：`line-overview`。
壳层关系：继承 web-workbench 的 app-shell，选中“产线概览”。
布局唯一权威来源：PRD「页面清单」中 `line-overview` 的完整 UI 设计描述。

## PRD 必读

1. 父 PRD「页面清单」中 `line-overview` 条目全文。
2. 父 PRD「状态策略」章节。
3. 父 PRD「用户故事」：US-1、US-2、US-3。
4. 父 PRD「页面清单」中 web-workbench 的 app-shell 条目及壳层变体规则。
5. `docs/design/DESIGN.md`：§5 筛选、指标、拓扑状态、表格、骨架、空态、局部错误，以及 §6 宜忌；当前缺失，待 [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 建立。
6. N/A（单端，不要求 platforms.md）。
7. N/A（spec-driven，以 PRD 文字实现）。

## 业务依据补充

实现决策：PRD 实现决策 › 工位状态与规则诊断；PRD 实现决策 › 指标与结果版本；PRD 实现决策 › 工作台服务接口。
测试决策：PRD 测试决策 › Kafka 与 Flink 计算输出测试边界；PRD 测试决策 › Spring Boot API 与业务审批测试边界。
切片包含本页面主路径所需的持久化、业务服务与浏览器集成，不另开横向数据库或 API 工单。

## States 矩阵

| state | PRD 来源 | 可观察预期 |
| --- | --- | --- |
| default | PRD 页面清单 §line-overview「UI 设计描述」 | 批次和区间指标、四工位与缓冲、候选诊断显示真实结果及版本。 |
| loading | PRD「状态策略」+ PRD 页面清单 §line-overview | 首载局部骨架，刷新保留已有数据并显示刷新状态。 |
| empty | PRD「状态策略」+ PRD 页面清单 §line-overview | 区分尚无数据、筛选无结果和当前区间暂无诊断记录。 |
| error | PRD「状态策略」+ PRD 页面清单 §line-overview | 失败区块可重试，旧数据标未更新，其余区块可用。 |
| observation-gap | PRD「状态策略」+ PRD 页面清单 §line-overview | 缺失观测单独标记，不显示为正常运行。 |
| undetermined | PRD「状态策略」+ PRD 页面清单 §line-overview | 证据或时间质量不足显示无法判定，源工位和受影响工位不混淆。 |
| filter | PRD「状态策略」+ PRD 页面清单 §line-overview | 工位与判断结果筛选可组合，刷新和详情返回保留筛选。 |

## UI 输入

- [ ] [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 的 DESIGN.md 已就绪，§5 原语与 §6 宜忌可定位。
- [x] 父 PRD 已发布，本文已链接至实际父 Issue。
- [ ] 依赖项已完成；不得仅因标题为 AFK 就跳过前置条件。

## 验收标准

### 功能

- [ ] 正常、微停、缺料、阻塞、换型和观测缺失可区分；工位与缓冲按生产顺序显示（US-2）。
- [ ] 诊断按产品与动作规则产生，只用已到达事实；源工位与受影响工位分别显示，0.5–10 秒代表性场景记录检出与限制（US-3）。
- [ ] 指标区显示区间、更新时间和结果版本；筛选结果来自真实服务，并能定位诊断详情入口（US-1、US-3）。

### UI 行为

- [ ] States 矩阵逐行验证；局部失败不抹掉可用数据，业务未知不显示为零或正常。
- [ ] 刷新、返回、重复点击和权限拒绝得到 PRD 约定结果；只读或禁用界面不能替代后端校验。

### 设计 QA

- [ ] 对照 PRD 页面清单 §line-overview 和 DESIGN.md §5/§6 检查布局、层级及交互，不复制或擅改全局视觉规范。
- [ ] 桌面与窄屏截图验证文字、图表、按钮无不合理重叠；宽表格在自身区域滚动。

## 依赖

- [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2)
- [I02](https://github.com/qwerqazo21l9-eng/IIoT/issues/3)
- [I03](https://github.com/qwerqazo21l9-eng/IIoT/issues/4)

## 阻塞项

当前缺少 DESIGN.md，按技能规则初始为 ready-for-human；[D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 合并且依赖完成后复核为 ready-for-agent，不需要重新拆分本页状态。
