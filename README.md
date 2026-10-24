# AgriFleet Analytics

A distributed system that monitors the utilization of a small Austrian hay/dairy farm's
fleet — one tractor and three implements — and turns raw telemetry into transparent
decision support for maintenance prioritisation and short-term operational risk.

Every number the system shows is traceable to a counter, and every counter is traceable to
a telemetry message. There is no hidden scoring, no machine learning and no opaque
weighting: when a machine is flagged, the screen shows the counter, the threshold and the
value that crossed it.

Built as an informatics portfolio project. Java 25, Spring Boot, RabbitMQ, PostgreSQL, and
a frontend of plain HTML/CSS/JavaScript with no framework, bundler or dependencies.

## Where things are documented

This README is a front door only. Each document below owns one band of the
project and is the single source of truth for it — nothing is restated between them.

| Document | Owns |
|----------|------|
| [`CLAUDE.md`](CLAUDE.md) | Working rules for AI coding agents (interview-first coding style) |
| [`docs/SPEC.md`](docs/SPEC.md) | Product rules, domain logic, API contract, scope |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Modules, ports, messaging, persistence, technical boundaries |
| [`docs/GLOSSARY.md`](docs/GLOSSARY.md) | Every name: types, fields, counters, flags, statuses, endpoints, components |
| [`docs/RUNNING.md`](docs/RUNNING.md) | How to start, test and stop the system |
| [`docs/STAKEHOLDERS.md`](docs/STAKEHOLDERS.md) | Stakeholders and their needs |
| [`docs/PROCESS.md`](docs/PROCESS.md) | Business process models (BPMN 2.0): maintenance planning as-is and to-be |

Start with `SPEC.md` §1 for what the system does, `ARCHITECTURE.md` §1 for how it is put
together, and `docs/RUNNING.md` for how to run it.
