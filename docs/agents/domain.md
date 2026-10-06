# 领域文档
探索前读取根目录 CONTEXT.md 和 docs/adr/ 中相关架构决策。
使用 CONTEXT.md 定义的术语；缺失概念交由 grill-with-docs 澄清。
与现有 ADR 冲突时明确指出冲突及重新讨论的理由。
UI 模式按 PRD 末尾摘要、docs/design/DESIGN.md 文首的顺序判断：
headless 跳过设计门禁；spec-driven 读取 DESIGN.md 和 PRD 页面绑定；
mockup-driven 还须读取默认态 references；多端读取 platforms.md。
页面规格以 PRD 为准，issue 使用 PRD 绑定和 States 矩阵。
上述文档不存在时静默继续，由生产者 skill 按需创建。
