# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Document hierarchy — read this first

The ranking of this file, `docs/SPEC.md`, `docs/ARCHITECTURE.md` and `docs/GLOSSARY.md`, and the rules for resolving conflicts between them, are defined in `docs/SPEC.md` §0. Read it before acting; it is not restated here.

This file sits at rank 0 and owns working conventions only: coding style, build and run commands, repo status. `docs/SPEC.md` remains the source of truth for product rules, domain logic, API contracts and scope.

## Project context

Informatics student portfolio project for agriculture-tech job applications. The student must be able to explain every part of the codebase in an interview. Optimize for clarity and teachability, not cleverness — see `.cursorrules` for the full rule set. In short:

- Prefer simple, readable, whiteboard-explainable code over clever abstractions.
- Match existing package style (`com.mariolagger.*`) and constructor injection.
- Don't add layers for "enterprise best practice" (extra interfaces, factories, event buses, DI beyond Spring's defaults).
- Don't add new languages, frameworks, brokers, or databases beyond what's already in use
- Make the smallest change that completes the requested task; don't implement anything listed under SPEC.md §8 Future work.
- If any requirement, edge case, or naming choice isn't fully pinned down by SPEC/ARCHITECTURE/GLOSSARY, ask multiple-choice clarifying questions before writing code — prefer more questions over assumptions.

## Repo status: skeleton mid-rewrite

This repo is being rewritten from an earlier "farm energy optimizer" portfolio project into the AgriFleet Analytics system described in `docs/SPEC.md`/`ARCHITECTURE.md`/`GLOSSARY.md`. The module layout, package names, Maven artifacts, and infra naming have already been renamed to fit the new domain, but **no SPEC business logic has been implemented yet** — the simulator's telemetry generation, the counters, the flags and ranking, the database schema, and every REST endpoint body are all still to be built from scratch. Don't assume any of that logic exists; check the actual module contents before referencing behavior.

## Coding style (established, follow it)

Derived from the surviving scaffolding — match it for any new code:

- **Plain Java, no Lombok, no SLF4J.** DTOs and JPA entities are hand-written POJOs with explicit getters/setters and a no-arg constructor; logging is `System.out.println` / `System.err.println`, not a logging framework.
- **Constructor injection only** — no field injection, no `@Autowired` on fields.
- **Package-by-layer within each module**: `controller/`, `dto/`, `entity/`, `repository/`, `service/business/` (domain logic), `service/messaging/` (RabbitMQ producers/consumers), `scheduler/` (`@Scheduled` jobs). Keep new code in the matching package.
- **JPA entities**: explicit `@Entity`/`@Table(name = ...)`/`@Column(name = ...)` mappings; `spring.jpa.hibernate.ddl-auto=none` — schema is managed by hand-written SQL, not Hibernate auto-DDL.
- **REST error handling**: throw `ResponseStatusException` with an `HttpStatus` and message directly from the service layer; no `@ControllerAdvice`.
- **Verbose, self-explanatory names** over abbreviations, even for locals (e.g. `totalWorkingHours`, not `workHrs`) — the point is a reviewer/interviewer can read the code without extra context. Field and counter names come from `docs/GLOSSARY.md`; don't invent variants.
- **RabbitMQ wiring**: each module declares the exchange, queue and binding beans its own role needs (see `ARCHITECTURE.md` §3) in its `*Application.java`. Idempotent re-declaration by several modules is expected, not a bug to fix.
- **Timezone**: farm-local logic is pinned to `Europe/Vienna` via a `private static final ZoneId FARM_ZONE = ZoneId.of("Europe/Vienna");` on the class that needs it. Keep new time-based logic consistent with that.
- **Tests**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `ArgumentCaptor`), one test class per service/consumer, plain `assertEquals`/`assertTrue`. Every module also keeps a trivial `@SpringBootTest` `contextLoads()` smoke test.
- **Comments are rare**; when present they cite the SPEC section they encode (e.g. `// SPEC §4.2`) rather than explaining what the code does.

## Architecture

Module layout, ports, messaging, and persistence are documented in `docs/ARCHITECTURE.md` §2–§5 — that is the single source of truth for structure; don't restate it here.

Notes not covered there:

- The old `energy_amount`/`battery_state`/`battery_command`/`controller_command` queues from the previous domain are gone — no battery concept exists in the new SPEC.
- **The existing RabbitMQ wiring is stale.** `SimulatorApplication` and `FleetDataServiceApplication` each still declare a plain `Queue("telemetry", true)` bean, which predates the fanout design in `ARCHITECTURE.md` §3 and would make the two consumers compete for messages. Replace both with the exchange/queue/binding beans that document requires; `FleetRuleEngine` has no messaging wiring at all yet.
- `docker/database-script.sql` does not exist (the old solar-domain schema was deleted). Both tables in `ARCHITECTURE.md` §4 still need to be written, and `docker/docker-compose.yml` currently mounts no init script — the file has to be mounted into `/docker-entrypoint-initdb.d` before it is ever applied.
- **`OperatorApi` has no controllers yet.** Its `controller/` package is empty and none of the four SPEC.md §6 endpoints exist.
- **Mind the `/api` prefix.** No module sets a `/api` context path, and `docker/nginx-conf/nginx.conf` proxies `/api/` through without stripping it — so controllers must map the full `/api/...` path (or a context path must be added) or every endpoint 404s through the frontend.
- Frontend (`Frontend/`) is intentionally left untouched for now — it still contains the old solar-energy GUI (`energy-flow-gui.*`, `app.js`) and will be rebuilt separately against the `/api/*` endpoints, using the component names in `GLOSSARY.md` §8.

## Common commands

Infra (Postgres + RabbitMQ + nginx/frontend), from `docker/`:
```
docker compose up -d
docker compose down
```

Each Java module is built/run independently. From a module directory (e.g. `OperatorApi/`):
```
./mvnw spring-boot:run                    # run
./mvnw spring-boot:run -Pdev               # run with low-RAM dev JVM args + "dev" profile
./mvnw test                                # run this module's tests
./mvnw test -Dtest=ClassName               # run a single test class
./mvnw test -Dtest=ClassName#methodName    # run a single test method
```
On Windows use `mvnw.cmd` instead of `./mvnw`. From the repo root, `mvn compile`/`mvn test` builds the whole reactor (all four modules).

Modules with a datasource (`FleetDataService`, `FleetRuleEngine`, `OperatorApi`) need Postgres reachable at `localhost:5432` for their `@SpringBootTest` context-load tests to pass — start the Docker infra first, or that test will fail with a Hibernate dialect error (this is expected, not a bug).

`application-dev.properties` (used by the `-Pdev` Maven profile) sets lazy init and a small Tomcat thread pool for low-RAM local runs — identical boilerplate across all four modules.
