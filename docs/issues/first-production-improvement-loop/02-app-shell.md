# I02：交付工作台框架与演示角色入口

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/3)。

## 类型与状态

类型：AFK（设计输入齐全后）。拟发布标签：`ready-for-human`。

## PRD 绑定

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖的用户故事：—（壳层）。

## 构建内容

交付 React 工作台的公共框架、主导航、角色上下文与窄屏菜单。为后续功能页提供内容出口；演示角色入口仅管理身份上下文，不据此声称生产认证已实现。

## UI 模式

spec-driven。

## 页面绑定

platform-id：`web-workbench`。page-id：`app-shell`。
壳层关系：本页即整体框架页，不承载业务主任务。
布局唯一权威来源：PRD「页面清单」中 `app-shell` 的完整 UI 设计描述。

## PRD 必读

1. 父 PRD「页面清单」中 `app-shell` 条目全文。
2. 父 PRD「状态策略」章节。
3. 父 PRD「用户故事」：N/A（壳层支撑全端导航）。
4. N/A（本页即 app-shell）。
5. `docs/design/DESIGN.md`：§5 导航、布局、角色菜单，以及 §6 宜忌；当前缺失，待 [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 建立。
6. N/A（单端，不要求 platforms.md）。
7. N/A（spec-driven，以 PRD 文字实现）。

## 业务依据补充

实现决策：PRD 实现决策 › 首期模块边界；PRD 实现决策 › 演示角色与后端授权。
测试决策：PRD 测试决策 › Spring Boot API 与业务审批测试边界。
切片包含本页面主路径所需的持久化、业务服务与浏览器集成，不另开横向数据库或 API 工单。

## States 矩阵

| state | PRD 来源 | 可观察预期 |
| --- | --- | --- |
| default | PRD 页面清单 §app-shell「UI 设计描述」 | 三项导航按 PRD 顺序显示，内容出口与当前入口明确。 |
| role-switch | PRD「状态策略」+ PRD 页面清单 §app-shell | 切换独立演示身份，当前角色可见，清除前一身份未提交的操作上下文。 |
| read-only | PRD「状态策略」+ PRD 页面清单 §app-shell | 角色无写权限时保留导航与只读内容出口，操作权限由功能页和后端校验。 |
| narrow | PRD「状态策略」+ PRD 页面清单 §app-shell | 侧栏收进菜单，内容单列；菜单可关闭且导航可访问。 |
| loading | PRD「状态策略」+ PRD 页面清单 §app-shell | 壳层保持稳定，内容出口容纳局部骨架，不另开加载页面。 |
| error | PRD「状态策略」+ PRD 页面清单 §app-shell | 内容失败时公共导航仍可用，不伪造生产数据。 |

## UI 输入

- [ ] [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 的 DESIGN.md 已就绪，§5 原语与 §6 宜忌可定位。
- [x] 父 PRD 已发布，本文已链接至实际父 Issue。
- [ ] 依赖项已完成；不得仅因标题为 AFK 就跳过前置条件。

## 验收标准

### 功能

- [ ] 导航、角色菜单、内容区满足 PRD app-shell 规格，功能页可以继承同一壳层。
- [ ] 默认和窄屏导航可通过键盘操作；切换后当前身份可被业务请求明确携带。

### UI 行为

- [ ] States 矩阵逐行验证；局部失败不抹掉可用数据，业务未知不显示为零或正常。
- [ ] 刷新、返回、重复点击和权限拒绝得到 PRD 约定结果；只读或禁用界面不能替代后端校验。

### 设计 QA

- [ ] 对照 PRD 页面清单 §app-shell 和 DESIGN.md §5/§6 检查布局、层级及交互，不复制或擅改全局视觉规范。
- [ ] 桌面与窄屏截图验证文字、图表、按钮无不合理重叠；宽表格在自身区域滚动。

## 依赖

- [D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2)

## 阻塞项

当前缺少 DESIGN.md，按技能规则初始为 ready-for-human；[D01](https://github.com/qwerqazo21l9-eng/IIoT/issues/2) 合并且依赖完成后复核为 ready-for-agent，不需要重新拆分本页状态。
