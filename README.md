# Game Session Management & Matchmaking Service

Java 21 / Spring Boot backend that registers players, matches them by skill rating, and manages game sessions. Built to practice concurrency, testability, low-latency reads and observability.


## Stack

Java 21 · Spring Boot 3.3 · REST · PostgreSQL · Redis · Multithreading (`ReentrantLock`) · Docker / Docker Compose · JUnit 5 · Mockito · JaCoCo · Micrometer · Prometheus · Grafana · OpenTelemetry (OTLP → Jaeger) · GitHub Actions

## Features

1. Player registration with validation (unique username, rating 0–5000).
2. Skill-based matchmaking: a sorted queue forms matches from adjacent players inside a rating window that **widens the longer a player waits** (capped), so nobody waits forever.
3. Thread-safe concurrent queueing: all queue mutations go through one lock; a stress test fires 2,000 concurrent enqueues and asserts no player lands in two matches.
4. Redis cache-aside for session reads (TTL 10 min). Redis failures degrade to a cache miss, PostgreSQL stays the source of truth.
5. REST API for sessions: create (via matchmaking), read, complete.
6. Unit, controller-slice and end-to-end integration tests.
7. Docker Compose stack with Prometheus, Grafana and Jaeger.

## REST API

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/players` | Register a player `{"username","rating"}` |
| GET | `/api/v1/players/{id}` | Fetch a player |
| POST | `/api/v1/matchmaking/queue` | Join the queue `{"playerId"}` (202) |
| DELETE | `/api/v1/matchmaking/queue/{playerId}` | Leave the queue (204) |
| GET | `/api/v1/matchmaking/status/{playerId}` | `NOT_QUEUED`, `QUEUED` or `MATCHED` + `sessionId` |
| GET | `/api/v1/sessions/{id}` | Fetch a session (Redis first) |
| POST | `/api/v1/sessions/{id}/complete` | End a session |

Errors use RFC 7807 problem responses (400 validation, 404 not found, 409 conflict).

## Run it

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| Prometheus metrics | http://localhost:8080/actuator/prometheus |
| Prometheus UI | http://localhost:9090 |
| Grafana (add dashboards on the provisioned Prometheus source) | http://localhost:3000 |
| Jaeger traces | http://localhost:16686 |

Try it:

```bash
A=$(curl -s -X POST localhost:8080/api/v1/players -H 'Content-Type: application/json' -d '{"username":"alice","rating":1000}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
B=$(curl -s -X POST localhost:8080/api/v1/players -H 'Content-Type: application/json' -d '{"username":"bob","rating":1040}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
curl -s -X POST localhost:8080/api/v1/matchmaking/queue -H 'Content-Type: application/json' -d "{\"playerId\":\"$A\"}"
curl -s -X POST localhost:8080/api/v1/matchmaking/queue -H 'Content-Type: application/json' -d "{\"playerId\":\"$B\"}"
sleep 1
curl -s localhost:8080/api/v1/matchmaking/status/$A    # MATCHED + sessionId
```

## Tests

```bash
mvn verify            # runs all tests and writes target/site/jacoco/index.html
```

| Layer | Tooling | What it covers |
|---|---|---|
| Matchmaking algorithm | JUnit 5 | window matching, widening, cap, duplicates, cancel, 2,000-thread concurrency |
| Services | JUnit 5 + Mockito | cache hit/miss, conflict/not-found paths, re-queue on failure |
| Controllers | `@WebMvcTest` | validation and error mapping |
| End to end | `@SpringBootTest` + H2 | register → queue → match → read → complete, Prometheus metrics |

## Observability

* **Metrics** (Micrometer → Prometheus): `matchmaking_queue_size`, `matchmaking_matches_created_total`, `matchmaking_queue_wait_seconds`, `session_cache_total{result="hit|miss"}`, plus JVM and HTTP server metrics.
* **Tracing**: Micrometer Tracing with the OpenTelemetry bridge exports spans over OTLP to Jaeger; trace and span ids are included in log lines.

## Design notes

* `Matchmaker` is plain Java with an injectable clock, so time-based behaviour is tested without sleeping.
* Matching runs on a scheduler (`matchmaking.interval-ms`, default 500 ms). The in-memory queue lives in one instance; scaling horizontally would move the queue into Redis (sorted sets) and make matching a single-leader task.
* If session creation fails, the matched players are put back in the queue instead of being dropped.

## Project layout

```
src/main/java/com/vyshnavi/gamesession/
  matchmaking/  Matchmaker (core), service, scheduler, controller
  player/       entity, repository, service, controller, DTOs
  session/      entity, repository, service, controller
  cache/        SessionCache, RedisSessionCache, InMemorySessionCache
  common/       exceptions and global error handler
```
