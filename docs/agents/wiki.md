# Repo Wiki 代码导读
供人阅读；workflow skills 默认不读，用户本轮显式引用时除外。
固定目录 docs/repo-wiki/，由 /repo-wiki 生成、刷新或删除。
布局：_meta.yaml、README.md、01-execution-flow.md、
02-core-modules/index.md 及模块页、03-cross-boundaries.md、
04-data-state-flow.md、05-config-boundaries.md、
06-extension-points.md、07-risk-points.md，以及可选 diagrams/。
从 README 和执行链路开始阅读，对照源码锚点验证。
_meta.yaml 记录分支、commit、语言、生成时间和页数。
源码变更后人工刷新；代码现状以源码为准，架构规范以 ADR 为准。
Wiki 不重新定义术语、不复制 PRD；同一仓库只保留一种语言。
## 本仓库配置
- Wiki 启用：是
- 默认语言：zh-CN
