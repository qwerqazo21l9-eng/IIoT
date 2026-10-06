# I07：从获批提议到配对实验收益报告

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/8)。

## 类型与状态

类型：AFK（设计输入齐全后）。拟发布标签：`ready-for-human`。

## PRD 绑定

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖的用户故事：US-9、US-10、US-11。

## 构建内容

将获批参数送入模拟器执行两组受控运行，持久化实验、尝试和结果，通过同一实验页配置、跟踪与比较良品 JPH。包含启动时授权复核、随机过程配对、有效性校验及版本化结果。

## UI 模式

spec-driven。

## 页面绑定

platform-id：`web-workbench`。page-id：`improvement-experiment`。
壳层关系：继承 web-workbench 的 app-shell，选中“改善实验”。
布局唯一权威来源：PRD「页面清单」中 `improvement-experiment` 的完整 UI 设计描述。

## PRD 必读

1. 父 PRD「页面清单」中 `improvement-experiment` 条目全文。
2. 父 PRD「状态策略」章节。
3. 父 PRD「用户故事」：US-9、US-10、US-11。
4. 父 PRD「页面清单」中 web-workbench 的 app-shell 条目及壳层变体规则。
5. `docs/design/DESIGN.md`：§5 实验列表、数值输入、运行进度、结果对比、骨架、空态、局部错误，以及 §6 宜忌；当前缺失，待 [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 建立。
6. N/A（单端，不要求 platforms.md）。
7. N/A（spec-driven，以 PRD 文字实现）。

## 业务依据补充

实现决策：PRD 实现决策 › 配对实验协调；PRD 实现决策 › 实验报告有效性；PRD 实现决策 › 演示角色与后端授权；PRD 实现决策 › 重复投递与业务幂等；PRD 实现决策 › 工作台服务接口。
测试决策：PRD 测试决策 › 模拟产线事件输入测试边界；PRD 测试决策 › Spring Boot API 与业务审批测试边界；PRD 测试决策 › 改善实验结果报告测试边界。
切片包含本页面主路径所需的持久化、业务服务与浏览器集成，不另开横向数据库或 API 工单。

## States 矩阵

| state | PRD 来源 | 可观察预期 |
| --- | --- | --- |
| default | PRD 页面清单 §improvement-experiment「UI 设计描述」 | 列表选中实验，展示绑定提议、配置和运行结果。 |
| loading | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 局部骨架；进度查询刷新保留已有结果。 |
| empty | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 暂无实验可从获批提议配置；未获批说明不能启动。 |
| invalid | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 非法时长、重复次数或不匹配参数在字段处显示。 |
| disabled | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 未批准、参数版本不一致或角色不合法不能启动，后端同步拒绝。 |
| running | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 基线与改善组各自进度可见，重复请求不再执行一次。 |
| failed | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 失败阶段明确，可追溯重试尝试，保留旧记录。 |
| complete | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 显示两组 JPH、绝对和相对变化、质量、期末在制品、配对差值和版本。 |
| unevaluable | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 完整性不足、组未完成或配置不匹配显示无法评价及原因。 |
| zero-or-single | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 基线零值相对收益不可计算；单组不声称波动或显著性。 |
| error | PRD「状态策略」+ PRD 页面清单 §improvement-experiment | 查询错误不伪造完成，保留配置和可用数据。 |

## UI 输入

- [ ] [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 的 DESIGN.md 已就绪，§5 原语与 §6 宜忌可定位。
- [x] 父 PRD 已发布，本文已链接至实际父 Issue。
- [ ] 依赖项已完成；不得仅因标题为 AFK 就跳过前置条件。

## 验收标准

### 功能

- [ ] 仅具体获批版本允许工程师启动，正常过程、工件质量、初始状态与统计条件配对，只有批准因素改变（US-9）。
- [ ] 两组运行与重试尝试可查询；重复启动不重复执行，失败记录不混入成功汇总（US-10）。
- [ ] 至少一组真实配对报告可独立核算，结果允许无改善；零基线、单组和无效数据按 PRD 表达，版本完整（US-11）。

### UI 行为

- [ ] States 矩阵逐行验证；局部失败不抹掉可用数据，业务未知不显示为零或正常。
- [ ] 刷新、返回、重复点击和权限拒绝得到 PRD 约定结果；只读或禁用界面不能替代后端校验。

### 设计 QA

- [ ] 对照 PRD 页面清单 §improvement-experiment 和 DESIGN.md §5/§6 检查布局、层级及交互，不复制或擅改全局视觉规范。
- [ ] 桌面与窄屏截图验证文字、图表、按钮无不合理重叠；宽表格在自身区域滚动。

## 依赖

- [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2)
- [I02](https://github.com/qwerqazo21l9-eng/IIoT/issues/3)
- [I06](https://github.com/qwerqazo21l9-eng/IIoT/issues/7)

## 阻塞项

当前缺少 DESIGN.md，按技能规则初始为 ready-for-human；[D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 合并且依赖完成后复核为 ready-for-agent，不需要重新拆分本页状态。
