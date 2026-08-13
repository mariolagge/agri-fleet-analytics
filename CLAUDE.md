# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Document hierarchy — read this first

The ranking of this file, `docs/SPEC.md`, `docs/ARCHITECTURE.md` and `docs/GLOSSARY.md`, and the rules for resolving conflicts between them, are defined in `docs/SPEC.md` §0. Read it before acting; it is not restated here.

This file sits at rank 0 and owns working conventions only: coding style, build and run commands, repo status. `docs/SPEC.md` remains the source of truth for product rules, domain logic, API contracts and scope.

## Project context

Informatics student portfolio project for agriculture-tech job applications. The student must be able to explain every part of the codebase in an interview. Optimize for clarity and teachability, not cleverness — see `.cursorrules` for the full rule set. In short:

- Prefer simple, readable, whiteboard-explainable code over clever abstractions.
- Match existing package style (`com.mariolagger.*`, cased per `GLOSSARY.md` §1) and constructor injection.
- Don't add layers for "enterprise best practice" (extra interfaces, factories, event buses, DI beyond Spring's defaults).
- Don't add new languages, frameworks, brokers, or databases beyond what's already in use
- Make the smallest change that completes the requested task; don't implement anything listed under SPEC.md §8 Future work.
- If any requirement, edge case, or naming choice isn't fully pinned down by SPEC/ARCHITECTURE/GLOSSARY, ask multiple-choice clarifying questions before writing code — prefer more questions over assumptions.

## Repo status: backend complete, frontend not started

The rewrite from the earlier "farm energy optimizer" portfolio project into the AgriFleet Analytics system is done on the backend. All three modules implement their `docs/SPEC.md` behaviour end to end: the simulator generates telemetry, `TelemetryProcessor` accumulates the counters and hourly buckets and evaluates flags, status, live condition and ranking, the SQL schema exists and is applied by docker, and all four REST endpoints are implemented. The system runs: start the infra, then the three modules, and the API answers with a ranked fleet.

What is *not* done, and is the obvious next work:

- **Frontend.** `Frontend/` still contains the previous project's solar-energy GUI (`energy-flow-gui.*`, `app.js`) calling the old `/energy/*` endpoints. It is untouched on purpose and will be rebuilt against `/api/*` using the component names in `GLOSSARY.md` §8. Leave it alone unless the task is that rebuild.
- **Per-service unit tests.** The coding-style section below mandates JUnit 5 + Mockito classes per service and consumer; only the three `contextLoads()` smoke tests exist so far. Keep those green.

The system is three modules, not four. `FleetDataService` and `FleetRuleEngine` were merged into a single consumer, `TelemetryProcessor`, because `SPEC.md` §4.3 derives the rolling window from the same hourly buckets as the historical record — see `ARCHITECTURE.md` §2. Both old names are gone; don't reintroduce them.

One wrinkle to know about before moving files: the working tree shows the module directories lowercased, while git, the build files and `docker-compose.yml` all use the `PascalCase` form that `GLOSSARY.md` §1 defines. Windows resolves either, so it goes unnoticed; a Linux checkout would not.

## Coding style (established, follow it)

Derived from the surviving scaffolding — match it for any new code:

- **Plain Java, no Lombok, no SLF4J.** DTOs and JPA entities are hand-written POJOs with explicit getters/setters and a no-arg constructor; logging is `System.out.println` / `System.err.println`, not a logging framework.
- **Constructor injection only** — no field injection, no `@Autowired` on fields.
- **Package-by-layer within each module**: `controller/`, `dto/`, `entity/`, `repository/`, `service/business/` (domain logic), `service/messaging/` (RabbitMQ producers/consumers), `scheduler/` (`@Scheduled` jobs). Keep new code in the matching package.
- **JPA entities**: explicit `@Entity`/`@Table(name = ...)`/`@Column(name = ...)` mappings, written to match a schema the application never generates (`ARCHITECTURE.md` §4 owns where that schema comes from). A new column means editing the SQL by hand as well as the entity.
- **REST error handling**: throw `ResponseStatusException` with an `HttpStatus` and message directly from the service layer; no `@ControllerAdvice`.
- **Verbose, self-explanatory names** over abbreviations, even for locals (e.g. `totalWorkingHours`, not `workHrs`) — the point is a reviewer/interviewer can read the code without extra context. Field and counter names come from `docs/GLOSSARY.md`; don't invent variants.
- **RabbitMQ wiring**: each module declares the beans its own role needs (see `ARCHITECTURE.md` §3) in its `*Application.java`. Idempotent re-declaration by both modules is expected, not a bug to fix.
- **Timezone**: farm-local logic is pinned to `Europe/Vienna` via a `private static final ZoneId FARM_ZONE = ZoneId.of("Europe/Vienna");` on the class that needs it. Keep new time-based logic consistent with that.
- **Tests**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `ArgumentCaptor`), one test class per service/consumer, plain `assertEquals`/`assertTrue`. Every module also keeps a trivial `@SpringBootTest` `contextLoads()` smoke test.
- **Comments are rare**; when present they cite the SPEC section they encode (e.g. `// SPEC §4.2`) rather than explaining what the code does.

## Architecture

Module layout, ports, messaging, and persistence are documented in `docs/ARCHITECTURE.md` §2–§5 — that is the single source of truth for structure; don't restate it here.

Notes not covered there:

- The old `energy_amount`/`battery_state`/`battery_command`/`controller_command` queues from the previous domain are gone — no battery concept exists in the new SPEC.
- The queue wiring matches `ARCHITECTURE.md` §3: `SimulatorApplication` and `TelemetryProcessorApplication` each declare the same `Queue("telemetry", true)` bean, and nothing else. There is no exchange and no binding, because there is only one consumer.
- **Mind the `/api` prefix.** `docker/nginx-conf/nginx.conf` proxies `/api/` through without stripping it, and per `ARCHITECTURE.md` §5 no module sets a servlet context path — so controllers map the full `/api/...` paths from SPEC.md §6 verbatim. Adding a context path instead would turn `@RequestMapping("/api/assets")` into `/api/api/assets`.

## Common commands

Infra (Postgres + RabbitMQ + nginx/frontend), from `docker/`:
```
docker compose up -d
docker compose down
docker compose down -v      # also drops the database-data volume
```

**The schema only loads into an empty volume.** Postgres applies the init script (`ARCHITECTURE.md` §4) only when it initialises a fresh data directory, and the `database-data` volume outlives a plain `docker compose down`. So any change to the schema file — and the first run on a machine whose volume predates it — needs `docker compose down -v` before `up -d`, or the old schema silently stays in place.

Each Java module is built/run independently. From a module directory (e.g. `OperatorApi/`):
```
./mvnw spring-boot:run                    # run
./mvnw spring-boot:run -Pdev               # run with low-RAM dev JVM args + "dev" profile
./mvnw test                                # run this module's tests
./mvnw test -Dtest=ClassName               # run a single test class
./mvnw test -Dtest=ClassName#methodName    # run a single test method
```
On Windows use `mvnw.cmd` instead of `./mvnw`. From the repo root, `mvn compile`/`mvn test` builds the whole reactor (all three modules).

Modules with a datasource (`TelemetryProcessor`, `OperatorApi`) need Postgres reachable at `localhost:5432` for their `@SpringBootTest` context-load tests to pass — start the Docker infra first, or that test will fail with a Hibernate dialect error (this is expected, not a bug). `TelemetryProcessor` additionally needs RabbitMQ up, since its listener container starts with the context.

The length of the simulator's startup burst (`SPEC.md` §3.2) is `simulator.backfill.hours` in `Simulator`'s `application.properties`; `0` disables the burst entirely, which is what the module's own smoke test does. Shortening it is the quickest way to watch flags appear during a session rather than arriving already set.

`application-dev.properties` (used by the `-Pdev` Maven profile) sets lazy init for low-RAM local runs. Only `OperatorApi` also caps the Tomcat thread pool — `Simulator` and `TelemetryProcessor` run with `spring.main.web-application-type=none` and have no web server to tune.
