# Issue 跟踪器：GitHub
本仓库的 issue 与 PRD 使用 GitHub Issues，通过 gh CLI 操作。
从 git remote 推断目标仓库；未配置 remote 时，先确定仓库，禁止猜测。
创建：gh issue create --title "..." --body-file <正文文件>
读取：gh issue view <number> --comments
列出：gh issue list --state open --json number,title,body,labels
评论：gh issue comment <number> --body-file <正文文件>
标签：gh issue edit <number> --add-label "..." / --remove-label "..."
关闭：gh issue close <number> --comment "..."
“发布到 issue 跟踪器”表示创建 GitHub issue。
“获取相关工单”表示读取 issue 及其评论。
