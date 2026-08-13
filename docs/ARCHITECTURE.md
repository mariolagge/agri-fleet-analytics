# docs\ARCHITECTURE.md

Governed by `SPEC.md` §0 (document hierarchy, anti-duplication, change protocol). This document owns structure, modules, messaging, persistence, ports and technical boundaries — and nothing else. Product rules live in `SPEC.md`, names in `GLOSSARY.md`.

---

## 1. High-Level Overview

The technical structure implementing the product defined in `SPEC.md` §1:

```
Simulator ──▶ RabbitMQ fanout ──┬──▶ FleetDataService ──▶ PostgreSQL ◀── OperatorApi ──▶ nginx ──▶ browser
                                └──▶ FleetRuleEngine  ──▶ PostgreSQL
```

Telemetry flows one way only. No module calls another over HTTP, and nothing flows back towards the Simulator — the API is read-only (`SPEC.md` §6), so no command path exists.

Infrastructure (PostgreSQL, RabbitMQ, nginx/frontend) is started via `docker compose up` from `docker/`. The four Spring Boot modules are **not** containerized; each is built and run independently (Maven/IDE) against that infra.

Every run starts from the same known state, because each consumer clears the table it owns at startup (§4). Reproducibility therefore does not depend on whether the Postgres volume survives a `docker compose down`.

---

## 2. Modules

Four independent Spring Boot Maven modules under the root aggregator `pom.xml` (artifactId `agri-fleet-analytics`), each with its own `pom.xml` and `application.properties`. There is no shared library module — duplication across modules is expected where the same DTO or entity is needed in more than one.

| Module | Port | Consumes | Writes | Role |
|--------|------|----------|--------|------|
| `Simulator` | 8081 | — | exchange `telemetry` | Generates telemetry per `SPEC.md` §3 and publishes it. Holds its state in memory. |
| `FleetDataService` | 8084 | queue `telemetry.aggregates` | table `hourly_aggregate` | Accumulates hourly buckets and persists each hour once it completes. Owns the historical record. |
| `FleetRuleEngine` | 8087 | queue `telemetry.rules` | table `asset_state` | Accumulates lifetime and rolling-24 h counters in memory, evaluates flags, status, live condition and ranking (`SPEC.md` §4–§5), and upserts the result. Owns the live view. |
| `OperatorApi` | 8083 | — | — | Serves the read-only REST API (`SPEC.md` §6) by querying `asset_state` and `hourly_aggregate`. Consumes no queue and writes no table. |

Ports 8081, 8084 and 8087 exist only because Spring Boot binds one; those three modules expose no application endpoints.

**Deliberate duplication.** `FleetDataService` and `FleetRuleEngine` both implement the `SPEC.md` §4.2 accumulation rule and the §4.3 hourly bucketing, independently and from the same message stream. Given the no-shared-library rule above, that is the intended cost of keeping the two concerns in separate modules; both must stay aligned to §4 whenever it changes.

---

## 3. Messaging

One RabbitMQ fanout exchange named `telemetry`, with two durable queues bound to it:

| Queue | Bound by | Consumed by |
|-------|----------|-------------|
| `telemetry.aggregates` | `FleetDataService` | `FleetDataService` |
| `telemetry.rules` | `FleetRuleEngine` | `FleetRuleEngine` |

A fanout, not a shared queue: both consumers need **every** message. Two consumers on one queue would compete and each receive roughly half, silently halving all counters.

Each module declares the beans it needs in its own `*Application.java` — `Simulator` the exchange, each consumer the exchange plus its own queue and binding. Idempotent re-declaration by several modules is expected, not a bug.

There is no command or control queue. Nothing publishes towards the `Simulator`.

---

## 4. Persistence

PostgreSQL database `agri-fleet-analytics-db` (user/password `admin`/`admin`). Schema is hand-written SQL in `docker/database-script.sql`, mounted into the container's `/docker-entrypoint-initdb.d` and applied when the database is first created; `spring.jpa.hibernate.ddl-auto=none`, no Hibernate auto-DDL.

| Table | Written by | Read by | Contents |
|-------|------------|---------|----------|
| `asset_state` | `FleetRuleEngine` | `OperatorApi` | One row per asset, upserted on every processed message: the eight counters in both scopes, the four flag severities, status, live condition, priority rank, and the most recent telemetry message. |
| `hourly_aggregate` | `FleetDataService` | `OperatorApi` | One row per asset per completed hour: the eight counters' per-hour deltas, keyed by asset and hour start. |

Raw telemetry is not persisted; only the latest message per asset survives, inside `asset_state`.

**Startup reset.** Each module clears the table it owns before consuming anything: `FleetRuleEngine` clears `asset_state` and writes the `SPEC.md` §4.4 baselines, `FleetDataService` clears `hourly_aggregate`. Neither reads the other's table. Lifetime counters and history are therefore never carried across a restart, and a stale Postgres volume cannot leak state into a new run.

Counter columns store hours as `numeric`. The rolling-24 h buckets themselves live only in `FleetRuleEngine`'s memory — they are derived state, rebuilt from the stream, and are never persisted.

---

## 5. Networking

`docker/nginx-conf/nginx.conf` serves the frontend and proxies `/api/` to `OperatorApi` on port 8083. `OperatorApi` is the only module the browser ever reaches; RabbitMQ and PostgreSQL are reachable from the host only for local development and management.

Module ports are listed in §2.

---

## 6. REST API

`OperatorApi` (port 8083) is the only module exposing REST endpoints. The full contract — paths, parameters, response bodies and status codes — is in `SPEC.md` §6.

---

## 7. Technology Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 25 |
| Application framework | Spring Boot 4.0.5 (root `pom.xml` is the authoritative version) |
| Messaging | RabbitMQ |
| Database | PostgreSQL |
| Frontend | HTML + CSS + vanilla JavaScript |
| Orchestration | Docker Compose (infrastructure only) |

No AI runtime is part of the system; see `SPEC.md` §8.
