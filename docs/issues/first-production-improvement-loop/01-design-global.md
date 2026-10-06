# D01：建立工作台全局视觉规范

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/2)。

## 类型与状态

类型：design-input。拟发布标签：design-input、ready-for-agent。

## 模式与层级

从零建立；D-global。UI 模式：spec-driven。当前不存在 DESIGN.md。

## PRD 输入

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖页面：app-shell、line-overview、diagnosis-detail、approval-queue、improvement-experiment。
覆盖故事：支撑 US-1 至 US-11，全端设计输入。
必读：PRD「UI 与设计要求」全文及末尾 UI 摘要。

## 构建内容与产出物

建立 docs/design/DESIGN.md，按 to-issues 引用的 grill-with-docs/DESIGN-TEMPLATE.md 组织 §1–§6：创意北极星、色彩与表面、字体、层级、通用 UI 原语、宜忌。采用面向工程师和管理者的紧凑工业工作台，尊重已确认 PRD 的页面和状态行为。UI 模式已确认；视觉参数由执行 Agent 基于该工作台定位选定并形成可评审文档，无需另开访谈。

## 验收标准

- [ ] DESIGN.md 文首声明 spec-driven，§1–§6 完整、相互一致，系统字体及中文回退明确。
- [ ] §5 具有可定位的导航、布局、角色菜单、筛选、指标、拓扑状态、表格、时间线、证据列表、表单侧栏、审批意见、冲突提示、数值输入、进度、结果对比、骨架、空态与局部错误规范。
- [ ] §6 明确工业工作台的信息密度、语义状态、无框区块、响应式和可访问性要求；不将功能区整体卡片化。
- [ ] PRD 标注的待定义原语全部闭合或显式列为阻塞；不更改业务字段、页面清单和状态语义。
- [ ] 单端不引入平台映射文件；页面布局仍引用父 PRD，不复制成第二套功能规格。

## 依赖

无；可与 I03 并行。

## 阻塞项

无 — 可开始。

## 范围外

业务路由、API、数据库和功能页面代码。
