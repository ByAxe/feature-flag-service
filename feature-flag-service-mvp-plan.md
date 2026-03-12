# Plan: Feature Flag Service MVP

**Generated**: 2026-03-12

## Overview
Build a greenfield Spring Boot 3.x service on Java 21 with Gradle that supports:
- Feature flag CRUD over REST
- Deterministic per-user evaluation with priority order from the PRD
- Usage logging and aggregate stats endpoints
- H2 for local development and PostgreSQL for production
- GitHub Actions CI for build, test, and artifact retention

The repo is currently empty, so this plan assumes a from-scratch implementation. It keeps Docker packaging optional, as the PRD marks containerization as optional and does not define a deployment platform.

## Assumptions
- Deployment target is not specified; CI/CD means GitHub Actions CI plus a deployable runnable service artifact.
- Persistence for usage analytics will come from an `evaluation_log` table because the PRD requires both per-request logging and aggregate usage stats.
- Observability will combine structured JSON application logs and Spring Boot Actuator health/metrics endpoints.
- The service remains single-environment per deployment, matching the PRD out-of-scope note.

## Prerequisites
- Java 21
- Gradle Wrapper
- Spring Boot 3.x
- Spring Web, Spring Data JPA, Validation, Actuator
- H2 for local/dev and PostgreSQL driver for prod
- GitHub Actions

## Documentation used
- Spring Boot Reference: [docs.spring.io/spring-boot/reference](https://docs.spring.io/spring-boot/reference/)
- GitHub Actions Gradle CI: [docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle)
- Flyway Java migration docs: [documentation.red-gate.com/flyway/flyway-concepts/migrations/java-based-migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/java-based-migrations)

## Dependency Graph

```text
T1 ──┬── T2 ──┬── T4 ──┬── T5 ───────────┐
     │        │        ├── T6 ──┬── T7 ──┼── T8 ──┐
     │        │        │        │        │        │
     │        │        │        └── T9 ──┘        │
     │        └── T3 ──┴───────────────────────────┤
     └─────────────────────────────────────────────┤
                                                   ├── T11 ── T10
                                                   │
                                                   └── T2
```

## Tasks

### T1: Bootstrap the Spring Boot project
- **depends_on**: []
- **location**: `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `gradlew`, `src/main/java/`, `src/test/java/`
- **description**: Initialize a Java 21 / Spring Boot 3.x Gradle project with the base dependency set: Web, Validation, Data JPA, Actuator, H2, PostgreSQL driver, Flyway, and test dependencies. Establish package naming, application entrypoint, profile structure, and default configuration files so other agents can work against stable paths.
- **validation**: Gradle wrapper exists, project structure is consistent, and the service can be built locally with the standard `build` task once implementation tasks complete.
- **status**: Completed
- **log**: Bootstrapped a Java 21 Gradle Spring Boot service, replaced the generated Boot 4 scaffold with a Boot 3.4.3 project layout, and established the package structure, app entrypoint, profiles, hooks, CI skeleton, and initial test split to unblock dependent tasks.
- **files edited/created**: settings.gradle.kts, build.gradle.kts, gradlew, gradle/, src/main/java/com/demo/featureflagservice/FeatureFlagServiceApplication.java, src/main/resources/application.yml, src/main/resources/application-dev.yml, src/main/resources/application-prod.yml, .githooks/pre-commit, .githooks/pre-push, .github/workflows/ci-cd.yml

### T2: Configure persistence, profiles, and migrations
- **depends_on**: [T1]
- **location**: `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`, `src/main/resources/application-prod.yml`, `src/main/resources/db/migration/`
- **description**: Configure H2 for dev, PostgreSQL for prod, and add Flyway-backed schema migrations for `feature_flag` and `evaluation_log`. Include indexes needed for fast lookup by `key`, `flag_key`, `user_id`, and recent evaluation queries used by stats endpoints. Keep schema types compatible across H2 and PostgreSQL, and enforce canonical key uniqueness at the schema level where practical.
- **validation**: Migrations define both tables and constraints from the PRD, profile-specific datasource settings are present, the schema supports CRUD, evaluation logging, and stats aggregation, and migration choices are safe across both H2 and PostgreSQL.
- **status**: Completed
- **log**: Added profile-based datasource configuration and a Flyway migration that creates feature flag, target user, and evaluation log tables with rollout and lowercase-key constraints plus analytics indexes.
- **files edited/created**: src/main/resources/application.yml, src/main/resources/application-dev.yml, src/main/resources/application-prod.yml, src/main/resources/db/migration/V1__create_feature_flag_tables.sql

### T3: Define API contracts and validation model
- **depends_on**: [T1]
- **location**: `src/main/java/.../api/`, `src/main/java/.../dto/`
- **description**: Define request and response DTOs for CRUD, `evaluate`, `evaluate-all`, and stats endpoints. Encode PRD validation rules: `key` required, lowercase alphanumeric plus hyphen, max 64 chars; `rolloutPercentage` limited to `0..100`; `key` immutable on update.
- **validation**: DTOs cover every PRD request/response shape, update payload excludes mutable key changes, and validation annotations match the PRD constraints.
- **status**: Completed
- **log**: Added dedicated request and response records for CRUD, evaluation, and stats APIs with validation annotations and normalization helpers for target user lists.
- **files edited/created**: src/main/java/com/demo/featureflagservice/dto/FeatureFlagRequest.java, src/main/java/com/demo/featureflagservice/dto/FeatureFlagUpdateRequest.java, src/main/java/com/demo/featureflagservice/dto/FeatureFlagResponse.java, src/main/java/com/demo/featureflagservice/dto/EvaluateRequest.java, src/main/java/com/demo/featureflagservice/dto/EvaluateAllRequest.java, src/main/java/com/demo/featureflagservice/dto/EvaluationResponse.java, src/main/java/com/demo/featureflagservice/dto/GlobalStatsResponse.java, src/main/java/com/demo/featureflagservice/dto/FlagStatsResponse.java

### T4: Implement domain model and repositories
- **depends_on**: [T2, T3]
- **location**: `src/main/java/.../domain/`, `src/main/java/.../repository/`
- **description**: Implement JPA entities and repositories for `FeatureFlag` and `EvaluationLog`, including mapping for `targetUserIds`, autogenerated IDs/timestamps, and repository methods for `key` lookup plus stats aggregation queries. Normalize `targetUserIds` input by trimming and deduplicating values, and keep the persistence model aligned with canonical lowercase keys.
- **validation**: Entity fields match the PRD domain model, repository methods cover CRUD, evaluation writes, and aggregate stats reads without requiring controller logic to understand storage details, and `targetUserIds` persistence is deterministic and sanitized.
- **status**: Completed
- **log**: Added canonical key normalization and deterministic target-user mapping in domain entities (`FeatureFlag`, `EvaluationLog`), added repository-level lowercase-safe lookup wrappers for flag key operations, and implemented normalized stats/retrieval methods for `EvaluationLog` while preserving existing call sites.
- **files edited/created**: src/main/java/com/demo/featureflagservice/domain/FeatureFlag.java, src/main/java/com/demo/featureflagservice/domain/EvaluationLog.java, src/main/java/com/demo/featureflagservice/repository/FeatureFlagRepository.java, src/main/java/com/demo/featureflagservice/repository/EvaluationLogRepository.java

### T5: Implement feature flag CRUD service and REST endpoints
- **depends_on**: [T3, T4]
- **location**: `src/main/java/.../service/`, `src/main/java/.../controller/`
- **description**: Build application services and controllers for `POST /api/flags`, `GET /api/flags`, `GET /api/flags/{key}`, `PUT /api/flags/{key}`, and `DELETE /api/flags/{key}`. Enforce unique-key conflicts, immutable keys, lowercase canonicalization, and expected HTTP semantics (`201`, `204`, `404`, `409`).
- **validation**: CRUD endpoints map exactly to the PRD, duplicate or casing-equivalent keys return conflict, missing resources return not found, and update behavior excludes `key` mutation.
- **status**: Completed
- **log**: Implemented CRUD service/controller behavior for canonicalized flag keys with deterministic 201/204/404/409 semantics, including case-insensitive conflict detection at create/update/read/delete boundaries, path-based canonical lookup, and immutable key enforcement by excluding key from update state changes. Added focused integration coverage for canonicalized create/update/read/delete flows and immutable-key update payload behavior.
- **files edited/created**: src/main/java/com/demo/featureflagservice/dto/FeatureFlagRequest.java, src/main/java/com/demo/featureflagservice/service/FeatureFlagService.java, src/test/java/com/demo/featureflagservice/controller/FeatureFlagControllerIT.java, feature-flag-service-mvp-plan.md

### T6: Implement deterministic evaluation engine
- **depends_on**: [T3, T4]
- **location**: `src/main/java/.../service/evaluation/`, `src/main/java/.../util/`
- **description**: Implement the evaluation algorithm with strict PRD order: disabled flag returns `DISABLED`; targeted user returns `TARGET_LIST`; `100` returns `FULL_ROLLOUT`; `0` returns `NO_ROLLOUT`; otherwise deterministic `hash(flagKey + userId) % 100 < rolloutPercentage` returns `PERCENTAGE_ROLLOUT`. Keep the hashing deterministic and isolated behind a testable component, and define service-level behavior for invalid or blank `userId` and unknown flag keys so controllers can produce stable API semantics.
- **validation**: Evaluation logic is deterministic for the same `(flagKey, userId)`, reasons match the PRD enum values, branch priority cannot be reordered accidentally by callers, and invalid-input / missing-flag outcomes are explicit rather than implicit.
- **status**: Not Completed
- **log**:
- **files edited/created**:

### T7: Implement evaluation logging and statistics aggregation
- **depends_on**: [T2, T4, T6]
- **location**: `src/main/java/.../service/stats/`, `src/main/java/.../repository/`
- **description**: Add the write path for `EvaluationLog` entries and repository/service logic to compute `GET /api/stats` and `GET /api/stats/{flagKey}`. Stats must include total evaluations, unique users, active flags count for global stats, and total evaluations, unique users, true/false ratio, and last evaluated timestamp for per-flag stats. Reuse the same reason/result contract produced by the evaluation engine so logs, responses, and aggregates cannot drift.
- **validation**: Aggregation methods can answer both stats endpoints directly from persisted evaluation data, the result shape matches the PRD, and logged evaluation outcomes are consistent with the evaluation engine contract.
- **status**: Not Completed
- **log**:
- **files edited/created**:

### T8: Implement evaluation and stats endpoints plus structured logging
- **depends_on**: [T3, T5, T6, T7, T9]
- **location**: `src/main/java/.../controller/`, `src/main/java/.../logging/`, `src/main/resources/`
- **description**: Implement `POST /api/flags/evaluate`, `POST /api/flags/evaluate-all`, `GET /api/stats`, and `GET /api/stats/{flagKey}`. Ensure each evaluate request emits structured JSON logs with `flagKey`, `userId`, `result`, `reason`, `latency`, and request correlation data where available, and expose baseline Actuator endpoints needed for health and metrics with an explicit allowlist. Define endpoint semantics for blank inputs, unknown flag keys, and evaluate-all behavior so partial failures are not ambiguous.
- **validation**: Endpoint responses match the PRD examples, evaluate-all behavior is explicitly defined, JSON logs contain the required success and failure fields, health/metrics endpoints are enabled for operational use, and the design includes an acceptance criterion aligned with the `< 50ms p95` evaluation target.
- **status**: Not Completed
- **log**:
- **files edited/created**:

### T9: Add global error handling and not-found semantics
- **depends_on**: [T3, T4, T5, T6]
- **location**: `src/main/java/.../error/`
- **description**: Implement global `@ControllerAdvice`, domain exceptions, and validation/error translation so the API consistently returns `{ "error": "...", "status": 4xx }`. Cover validation failures, duplicate key conflicts, missing flag lookups, malformed JSON, type mismatches, and missing required fields for both CRUD and evaluation endpoints.
- **validation**: All client-visible error responses share one format, controllers do not handcraft error payloads, and `404`/`409`/validation and request-parsing failures are generated centrally.
- **status**: Not Completed
- **log**:
- **files edited/created**:

### T10: Set up GitHub Actions CI and artifact publishing
- **depends_on**: [T2, T11]
- **location**: `.github/workflows/`, optional `Dockerfile`, optional `docker-compose.yml`
- **description**: Add a GitHub Actions workflow that runs on push and pull request, sets up Java 21 and Gradle, executes the build and tests, validates migrations/configuration as part of the pipeline, and uploads concrete review artifacts such as test reports and the built application package. Keep Docker packaging as an optional follow-on deliverable if time remains after the core MVP.
- **validation**: Workflow definition covers push and PR events, uses Gradle-based build/test execution, includes migration/config validation, and publishes the agreed artifact set required by the PRD review flow.
- **status**: Not Completed
- **log**:
- **files edited/created**:

### T11: Add unit and integration test suites
- **depends_on**: [T2, T5, T6, T7, T8, T9]
- **location**: `src/test/java/`
- **description**: Implement unit tests for the evaluation engine and service rules, plus MockMvc-based integration tests for CRUD, evaluate, evaluate-all, stats, validation, and error-handling flows. Include deterministic rollout tests, conflict/not-found coverage, malformed-request coverage, migration-backed database tests, and at least one performance-oriented assertion or benchmark around the evaluate path.
- **validation**: Tests cover all PRD-critical behavior, especially evaluation priority ordering, deterministic hashing, CRUD HTTP semantics, stats calculations, error payload shape, and the main non-functional expectations around evaluation latency and profile-backed persistence.
- **status**: Not Completed
- **log**:
- **files edited/created**:

## Parallel Execution Groups

| Wave | Tasks | Can Start When |
|------|-------|----------------|
| 1 | T1 | Immediately |
| 2 | T2, T3 | T1 complete |
| 3 | T4 | T2 and T3 complete |
| 4 | T5, T6 | T3 and T4 complete |
| 5 | T7, T9 | T7 requires T2, T4, T6; T9 requires T3, T4, T5, T6 |
| 6 | T8 | T3, T5, T6, T7, and T9 complete |
| 7 | T11 | T2, T5, T6, T7, T8, and T9 complete |
| 8 | T10 | T2 and T11 complete |

## Testing Strategy
- Unit-test the evaluation engine independently from persistence and HTTP.
- Use MockMvc integration tests for all API contracts from the PRD.
- Run repository-backed integration tests against H2 for CRUD and stats aggregation.
- Add deterministic rollout assertions to guarantee stable results for the same `(flagKey, userId)`.
- Validate structured logging fields on evaluate paths at least at the service or integration boundary.
- Include migration/profile smoke coverage so H2 and PostgreSQL assumptions do not diverge silently.
- Add one lightweight latency-oriented test or benchmark for evaluate-path regression detection.

## Risks & Mitigations
- **Risk**: `targetUserIds` mapping becomes awkward in JPA and slows evaluation.
  **Mitigation**: Store it as an element collection or join table early and isolate access through the repository model.
- **Risk**: Stats queries become expensive as evaluation volume grows.
  **Mitigation**: Add indexes in the first migration and keep aggregation in dedicated repository queries; defer pre-aggregation until needed.
- **Risk**: p95 `< 50ms` target is missed if every evaluation does heavy DB work.
  **Mitigation**: Keep evaluation logic lightweight, index lookup by key, and avoid cross-table work in the critical path beyond a single async-friendly log write if needed.
- **Risk**: H2 behavior differs from PostgreSQL in schema or query semantics.
  **Mitigation**: Use dialect-safe column choices, validate migrations in CI, and treat PostgreSQL compatibility as a first-class acceptance criterion.
- **Risk**: CI passes locally but prod config drifts from dev.
  **Mitigation**: Separate `dev` and `prod` configs clearly and ensure PostgreSQL settings are first-class, not an afterthought.
- **Risk**: “CI/CD” is interpreted as full deployment automation later.
  **Mitigation**: Call out that this MVP delivers CI plus a runnable artifact and optional container packaging unless a concrete deployment target is later specified.
