# Issue #3: workbench shell verification

Source: [Issue #3](https://github.com/qwerqazo21l9-eng/IIoT/issues/3).
Parent PRD: [Issue #1](https://github.com/qwerqazo21l9-eng/IIoT/issues/1).
UI mode: spec-driven. Platform: web-workbench. Page: app-shell.

## Preflight

- Issue #3 is ready-for-agent. Design dependency #2 is closed and merged by
  [PR #11](https://github.com/qwerqazo21l9-eng/IIoT/pull/11), commit
  `897ab281749d0685e94f0b31bd297718ef8d27b5`.
- Required reading 1: app-shell uses a 56px fixed header, 208px navigation,
  three ordered entries, and a scrollable content area with 24px padding.
- Required reading 2: initial loading is local; refresh and errors preserve
  available content; permissions have visible reasons; unknown values stay unknown.
- Required reading 3: N/A; this shell supports navigation for all web stories.
- Required reading 4: N/A; this page is the shared shell itself.
- Required reading 5: DESIGN sections 5.4, 5.5 and 6 specify navigation, menu
  keyboard behavior, responsive layout, semantic tokens and unframed sections.
- Required reading 6: N/A; single platform.
- Required reading 7: N/A; spec-driven, no reference mockups required.
- PRD implementation decisions keep demonstration subjects separate and require
  independent backend authorization for proposal submission, approval and execution.

## Observed Red-To-Green Cycles

The initial shell and five tests existed before TDD was requested. They are
baseline work, not claimed as test-first implementation. Subsequent cycles
were executed one behavior at a time:

| Behavior | Observed RED | Minimal GREEN |
| --- | --- | --- |
| Direct navigation and refresh | Direct approval URL rendered the overview heading | Read route from hash and restore browser navigation events |
| Repeated identity selection | Selecting the current role cleared pending input | Ignore unchanged role selection |
| Read-only identity | Role selection displayed no permission reason | Automatically render the read-only notice |
| Narrow menu Escape | Menu stayed expanded | Close menu and return focus to its trigger |
| Identity after refresh | Remount reset manager to engineer | Validate and restore tab-local session identity |
| Outside menu dismissal | Menu remained open after clicking content | Dismiss on pointer events outside the role menu |
| Responsive header offset | Content started at 56px below a 57px/69px header | Observe header layout height and share the offset |
| 200% content zoom | Header offset doubled, leaving 112px extra space | Measure CSS layout height instead of scaled visual height |

The baseline run also exposed missing test cleanup, an ambiguous text query,
and deferred menu focus. These were corrected before regression verification.

## States And Regression Coverage

- default: ordered navigation, current entry, reusable content outlet.
- role-switch: independent identity headers, keyboard selection, focus return,
  clearing unsubmitted page state, and preserving drafts on unchanged selection.
- read-only: visible permission reason and accessible navigation/content.
- narrow: menu opening, navigation, Escape, closure and focus; closed navigation
  is hidden from keyboard and assistive technology.
- loading: initial skeleton; refresh retains existing input and content.
- error: failure is local, old content is marked not updated, and retry invokes
  the caller's recovery action without removing available content.
- browser regression: back, refresh, repeated navigation, keyboard menus,
  320px/768px/1440px layouts and 200% CSS content zoom.

Commands: `npm test`, `npm run test:e2e`, `npm run build` in `web-workbench`.
The current suite contains 14 integration scenarios and 6 Chromium scenarios.
Playwright's content zoom test is not a claim of testing browser chrome zoom.

## UI Acceptance

- [x] Required PRD and DESIGN sections opened and read.
- [x] Shared shell and identity context available to feature pages.
- [x] State behavior and keyboard/browser regression checks passed.
- [x] Desktop and narrow screenshots generated and visually inspected by Agent.
- [x] Fixed-header offsets and horizontal overflow checked in Chromium.
- [ ] PR reviewer design QA against PRD app-shell and DESIGN 5.4/5.5/6.

Screenshots are in `docs/evidence/issue-3/`: shell-320, shell-768, shell-1440,
narrow-menu-320, role-menu-320 and zoom-200 PNGs.

## Continued Verification (2026-10-07)

The live Issue #3 and complete parent PRD were reopened. The ready-for-agent
label and merged design dependency still satisfy preflight. Existing cycles
above remain baseline work; this continuation does not claim they were rerun
as new test-first development.

An additional request-identity cycle exercised the connected overview through
the public UI. RED: its fetch contained only the URL, without the active demo
subject or role. GREEN: the request now uses the shared role context headers.
The same scenario verifies that switching to the manager sends demo-manager
and manager rather than the previous identity.

The combined local workspace had 15 Vitest scenarios, including a #5 overview
request test. That page and test are excluded from this independent #3 delivery.
Its 14 shell scenarios cover the shared identity context and all shell states.
The request-header fix stays with the separate #5 working implementation.

Current shell checks: 14 Vitest scenarios; 6 Chromium scenarios across
320px, 768px, 1440px and 200% content zoom; production build passed. Desktop
and narrow screenshots were regenerated and inspected. PR reviewer design QA
and upstream merge remain outstanding. The overview itself belongs to #5;
this delivery contains only the common shell and its identity context.

## Delivery Boundary

This is the common React shell. Business pages show an explicit not-connected
state; production observations, API responses and reports are not fabricated.
The identity context exposes X-Demo-Subject and X-Demo-Role for feature requests.
Backend permission rejection, idempotent writes, and production authentication
are not implemented or validated by this frontend-only shell.
Local loading/error states are exercised through reusable outlet fixtures,
not through developer controls in the product interface.
