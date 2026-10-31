# docs\RUNNING.md

This document owns how to start, test and stop the system on a local machine.

---

## 1. Prerequisites

- **JDK 25.**
- **Docker Desktop**, running.

Maven does not need to be installed: each module ships its own Maven wrapper (`mvnw.cmd` on
Windows, `mvnw` on macOS/Linux).

## 2. Start the infrastructure

PostgreSQL, RabbitMQ and the nginx web server (which serves the dashboard) run in Docker.
From the `docker/` directory:

```
docker compose up -d
```

## 3. Start the three modules

Open one terminal per module and start them in this order:

1. `telemetryprocessor`
2. `operatorapi`
3. `simulator`

In each terminal, change into the module's directory and run:

Windows:

```
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```
./mvnw spring-boot:run
```

On a machine with little RAM, add the `dev` profile, which starts the module with lighter
JVM and Spring settings:

Windows:

```
.\mvnw.cmd spring-boot:run -Pdev
```

macOS/Linux:

```
./mvnw spring-boot:run -Pdev
```

## 4. Open the dashboard

- Dashboard: http://localhost:8080
- RabbitMQ management UI: http://localhost:15672 (user `admin`, password `admin`)

## 5. Run the tests

From a module directory, run all of that module's tests:

Windows:

```
.\mvnw.cmd test
```

macOS/Linux:

```
./mvnw test
```

Run a single test class:

Windows:

```
.\mvnw.cmd test -Dtest=ClassName
```

macOS/Linux:

```
./mvnw test -Dtest=ClassName
```

The `*ApplicationTests` classes start the full Spring context and need the Docker
infrastructure (step 2) running. The other test classes are plain unit tests and run
without it.

## 6. Stop the system

1. Press `Ctrl+C` in each module terminal.
2. From the `docker/` directory:

```
docker compose down
```

To also delete the database volume:

```
docker compose down -v
```

This is required after any change to `docker/database-script.sql`: PostgreSQL applies that
script only when it creates a new, empty database, so the old schema stays in place until
the volume is deleted.

## 7. Demo knob: the startup burst

At startup the Simulator publishes a burst of past telemetry so that the dashboard shows
meaningful values right away (REQ-SIM-BURST). Its length in hours is set by
`simulator.backfill.hours` in `simulator/src/main/resources/application.properties`.
Setting it to `0` disables the burst.
