# Web workbench

Shared React + TypeScript shell for Issue #3. Business routes are explicit
placeholders until their feature slices are connected.

```powershell
npm ci
npm test
npm run build
npx playwright install chromium
npm run test:e2e
npm run dev
```

`AppShell` accepts the selected navigation entry, navigation callback, page
title, optional action bar and page children. It resets mounted page state
when the demonstration identity changes, so pending edits cannot cross subjects.
Keep durable server data outside that reset boundary where appropriate.

`ShellStatusOutlet` accepts content, local loading/error/read-only state,
an optional permission reason and a retry callback. Existing children remain
mounted through refresh or failure; an initial load without content has skeletons.
Feature pages own their actual requests and retry lifecycle.

Wrap the shell in `RoleProvider`; `useWorkbenchRole()` supplies the current
demo subject, capabilities and `requestIdentityHeaders`. Attach those headers
to business requests. UI capabilities are descriptive; backend authorization
must enforce the operation, subject and self-approval restrictions independently.
Identity selection is stored in sessionStorage for the current browser tab.

Hash navigation supports direct visits, refresh and browser back/forward
without server rewrite rules. Current routes are line-overview, approval-queue
and improvement-experiment. Diagnosis detail belongs to line-overview when its
feature page is introduced.

See [verification evidence](../docs/plans/issue-3-tdd.md).
