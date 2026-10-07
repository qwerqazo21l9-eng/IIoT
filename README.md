# IIoT production diagnosis and improvement

Requirements: [GitHub Issue #1](https://github.com/qwerqazo21l9-eng/IIoT/issues/1).
Implementation begins with [Issue #4](https://github.com/qwerqazo21l9-eng/IIoT/issues/4)
and the React workbench shell from
[Issue #3](https://github.com/qwerqazo21l9-eng/IIoT/issues/3).

## Current implementation

The normal production service path uses actual OPC UA, EdgeX device-rest/Core
Data, Kafka, Java Flink, PostgreSQL and Spring Boot HTTP queries. It is a local
finite-order demo with versioned inputs and reproducible metrics. Start it from
the repository root with:

```powershell
./scripts/demo.ps1
```

See [normal production entry, API and acceptance](docs/normal-production-path.md).

The Java simulator SDK generates deterministic observations for a four-station
line with finite buffers, blocking, production batches, changeover and final
quality decisions. Scenario inputs support workpiece-specific pauses at any
station and station-specific observation gaps; pause inputs do not alter normal
timing randomness or workpiece quality. It can map those observations into a versioned diagnostic
event stream and calculate outlet-only good-unit JPH for a half-open statistics
interval. The service path uses persisted Flink contributions for HTTP metrics;
the offline metrics helper is separate.

Diagnostic events carry run identity and independent per-device sequences.
Observation gaps preserve surviving event identities, times and sequence numbers.
The in-memory metrics query currently assumes complete input; data completeness
and confidence handling are still pending.

The `web-workbench` module provides the React + TypeScript app shell for the
spec-driven workbench. It includes the fixed top bar, three primary navigation
entries, demonstration role context, read-only messaging, local loading/error
outlets and narrow-screen menu behavior. The role switch only carries local
demo identity headers; functional pages and backend APIs must still enforce
write permissions.

## Run tests

Requires JDK 17 or newer and Maven 3.9. Dependencies are pinned in the POMs.

```powershell
cd C:\IIoT_Project\IIoT
mvn test
```

Tests exercise public simulator, event-stream and metrics query interfaces
without mocks or private method assertions. See
[TDD evidence and remaining acceptance](docs/plans/issue-4-tdd.md).

Requires Node.js 24 or newer and npm 11 or newer for the workbench shell.

```powershell
cd C:\IIoT_Project\IIoT\web-workbench
npm install
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

For local UI inspection:

```powershell
cd C:\IIoT_Project\IIoT\web-workbench
npm run dev
```

See [workbench verification](docs/plans/issue-3-tdd.md) for observed red-to-green
cycles, state coverage, screenshots and remaining reviewer design QA.

See [domain vocabulary](CONTEXT.md), [architecture decisions](docs/adr/), and
[system plan](docs/plans/system-framework.md) before extending
the simulator or starting another child issue.
