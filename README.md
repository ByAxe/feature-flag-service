# Feature Flag Service (Demo MVP)

This repository contains a **demo Spring Boot service** for feature flag management built from the PRD:
[`PRD: Feature Flag Service MVP`](https://www.notion.so/PRD-Feature-Flag-Service-MVP-320546180eea801ca30ec8dd21b1f5cc).

The primary purpose is to show a minimal production-style implementation of feature-flag APIs that can:

- create and update flags,
- evaluate flags in request-time,
- route users through deterministic rollout logic,
- expose usage stats,
- persist data with migration-backed schema,
- and run under a CI/CD pipeline.

The implementation intentionally focuses on the MVP surface from the PRD and leaves out broader platform concerns (full RBAC/admin UI/SDKs/A/B variants) for future iterations.

## What this service does

- Manage feature flags via REST:
  - create/list/read/update/delete flags
  - define rollout percentage and targeted users
  - toggle features quickly with API-driven configuration
- Evaluate features for one user or many in one call:
  - single evaluate endpoint
  - evaluate-all endpoint
  - deterministic rollout (`hash(flagKey + userId)`) for stable results
  - reason returned for each decision (`DISABLED`, `TARGET_LIST`, `FULL_ROLLOUT`, `NO_ROLLOUT`, `PERCENTAGE_ROLLOUT`)
- Track evaluation activity and aggregate usage stats.
- Provide service health/metrics via Spring Actuator.
- Run validation and return structured error payloads for bad requests.

## Scope highlights (from PRD)

- ✅ **In scope**
  - API-only management (no UI/admin panel)
  - Single-instance behavior suitable for one environment
  - Logging of evaluate inputs/outputs for observability
  - CI/CD and tests in GitHub Actions
- 🚧 **Out of scope (MVP intentionally excludes)**
  - audit log history UI
  - A/B and multivariate experiments
  - advanced authN/authZ
  - SDK/client libraries
  - multi-environment strategy

## Quick start

Prerequisites:
- Java 21
- Gradle wrapper included (`./gradlew`)
- PostgreSQL for production profile (H2 is used in dev profile)

### Run in development

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The service starts on port `8080` by default, with in-memory H2 when `dev` profile is active.

### Run tests

```bash
./gradlew test           # unit tests
./gradlew integrationTest # integration tests
./gradlew check           # all checks
```

## API

Base path:
- `/api/flags` — feature flag CRUD + evaluation
- `/api/stats` — global and per-flag metrics
- `/actuator/health` and `/actuator/metrics` — operational endpoints
- `/v3/api-docs` — OpenAPI JSON
- `/swagger-ui/index.html` — Swagger UI

### Examples

Create a flag:

```bash
curl -X POST http://localhost:8080/api/flags \
  -H "Content-Type: application/json" \
  -d '{
    "key":"checkout-beta",
    "description":"Checkout beta rollout",
    "enabled":true,
    "rolloutPercentage":10,
    "targetUserIds":["user-1","user-2"]
  }'
```

Evaluate one flag:

```bash
curl -X POST http://localhost:8080/api/flags/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "flagKey":"checkout-beta",
    "userId":"user-1"
  }'
```

Evaluate all flags for a user:

```bash
curl -X POST http://localhost:8080/api/flags/evaluate-all \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"user-1"
  }'
```

## Deployment

This demo includes GitHub Actions workflow for:

- build
- unit tests
- integration tests
- publishing test reports as workflow artifacts

## Notes

- This repository is intentionally a **demo** and can be used as a base for a more complete internal feature-flag platform.
- Data model and endpoint behavior are designed for quick extension: add auth, UI, and richer targeting rules when moving beyond MVP.
