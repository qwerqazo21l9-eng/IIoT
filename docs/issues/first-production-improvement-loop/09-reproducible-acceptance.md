# I09：交付一键演示与全闭环可复现验收证据

发布链接：[GitHub Issue](https://github.com/qwerqazo21l9-eng/IIoT/issues/10)。

## 类型与状态

类型：AFK。拟发布标签：`ready-for-agent`。

## PRD 绑定

父 PRD：[首个产线诊断与改善闭环](https://github.com/qwerqazo21l9-eng/IIoT/issues/1)。
覆盖的用户故事：US-1 至 US-15（整体验收，重点 US-15）。

## 构建内容

复用前面切片的启动与测试入口，串联真实协议、查询、诊断、提议、独立管理者审批及配对报告，交付可重复运行的验收脚本、说明和实际证据。每个切片已拥有自身测试，本工单只负责跨切片核对及交付缺口。

## UI 模式

headless（本工单交付服务/API/验收路径；父 PRD 仍为 spec-driven）。

## PRD 必读

1. 父 PRD「实现决策」：**本地演示交付**、**配对实验协调**、**实验报告有效性**、**演示角色与后端授权**。
2. 父 PRD「测试决策」：**模拟产线事件输入测试边界**、**EdgeX 与接入统一事件测试边界**、**Kafka 与 Flink 计算输出测试边界**、**Spring Boot API 与业务审批测试边界**、**改善实验结果报告测试边界**。
3. 父 PRD「用户故事」：US-1 至 US-15（整体验收，重点 US-15）。

Seam：五个已确认测试边界的联合演示入口、实际 HTTP 和浏览器行为。。
接口唯一权威来源：上述实现决策及 ADR；行为依据：上述测试决策及本工单验收标准。

## 验收标准

- [ ] 正常、异常、驳回、批准、配对改善、缺失观测与重复投递均有可复现入口，记录环境、配置、种子、实际结果和限制。（PRD 依据：`PRD 实现决策 › 本地演示交付`；`用户故事 US-15`）
- [ ] 从真实 OPC UA / EdgeX 到报告至少完成一组配对，授权与单次执行可独立核对，未就绪时不能生成伪报告。（PRD 依据：`PRD 测试决策 › EdgeX 与接入统一事件测试边界`；`PRD 测试决策 › 改善实验结果报告测试边界`）
- [ ] 同种子同版本重跑可核验指标，报告保留质量、在制品及适用范围；无改善与无法评价均如实交付。（PRD 依据：`PRD 实现决策 › 实验报告有效性`；`PRD 测试决策 › 改善实验结果报告测试边界`）
- [ ] 五个测试边界结果、US-1 至 US-11 浏览器路径及桌面/窄屏截图齐全；不宣称已达成云边容错或最终八小时误报目标。（PRD 依据：`PRD 测试决策 › Spring Boot API 与业务审批测试边界`；`PRD 测试决策 › Kafka 与 Flink 计算输出测试边界`；`用户故事 US-15`）

## 依赖

- [I03](https://github.com/qwerqazo21l9-eng/IIoT/issues/4)
- [I04](https://github.com/qwerqazo21l9-eng/IIoT/issues/5)
- [I05](https://github.com/qwerqazo21l9-eng/IIoT/issues/6)
- [I06](https://github.com/qwerqazo21l9-eng/IIoT/issues/7)
- [I07](https://github.com/qwerqazo21l9-eng/IIoT/issues/8)
- [I08](https://github.com/qwerqazo21l9-eng/IIoT/issues/9)

## 阻塞项

须先完成上述依赖；不需要新增产品决策。ready-for-agent 表示规格完整，执行顺序仍受依赖约束。
