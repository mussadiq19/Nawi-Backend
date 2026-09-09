# DECISIONS.md

## WeighingPerformanceObservationRepository
**What it does:** Provides Spring Data JPA access to `WeighingPerformanceObservation` rows, with a custom finder to retrieve all observations belonging to a given `TestSession` by session ID.

**Why built this way:** Standard `JpaRepository` extension with a single derived query method `findBySessionId` — the simplest way to express "all observations for this session" without writing a JPQL string. Constructor injection is unnecessary for repository interfaces since Spring manages them directly.

**Depends on:** `WeighingPerformanceObservation` entity, `TestSession` entity (via the `session` FK column).

**Watch out for:** This repository is observation-type-specific. Other test types (eccentricity, tilting, etc.) will need their own repositories since each has a distinct entity class, even though they share the same `session_id` foreign key pattern.

## ToleranceRuleRepository
**What it does:** Provides Spring Data JPA access to `ToleranceRule` rows, with a custom finder that retrieves all tolerance rules matching a given `AccuracyClass` and `TestType` — returning the full set of load-band rows needed to resolve an MPE for any load value.

**Why built this way:** A single derived query method `findByAccuracyClassAndTestType` keeps the interface minimal. The calling service (`ToleranceLookupService`) handles load-band matching logic, so the repository doesn't need to know about load ranges or OIML editions — it just returns candidates.

**Depends on:** `ToleranceRule` entity, `AccuracyClass` enum, `TestType` enum.

**Watch out for:** Multiple `ToleranceRule` rows will be returned per accuracy-class + test-type combination (one per load band). The caller must iterate through them to find the matching band — a single-row result is not expected.

## TestSessionRepository
**What it does:** Provides standard Spring Data JPA CRUD access to `TestSession` entities. No custom query methods are needed yet — basic `findById`, `save`, `findAll` etc. cover current requirements.

**Why built this way:** A bare `JpaRepository` extension is sufficient. Custom finders (e.g., sessions by instrument) will be added as the API layer demands them. Premature query methods add maintenance cost without immediate benefit.

**Depends on:** `TestSession` entity.

**Watch out for:** The `TestSession` entity has a `@ManyToOne` relationship to `Instrument` — fetching sessions will eagerly or lazily load the instrument depending on the fetch type configured. This matters for performance if sessions are listed in bulk.

## MpeFormulaEvaluator
**What it does:** Evaluates an MPE formula string matching the `<decimal>e` pattern (e.g. `"0.5e"`, `"1.0e"`, `"1.5e"`, `"0.75e"`) against a given scale interval `e`, returning the MPE value as a `BigDecimal`.

**Why built this way:** A regex matcher (`[0-9]+\\.[0-9]+e`, whole-string match) accepts ANY decimal multiplier in the `<decimal>e` pattern rather than a fixed whitelist. This keeps tolerance formulas DB-driven and extensible for future OIML revisions that may introduce new multipliers — a hardcoded whitelist of the three current Table 6 values would break that. An explicit decimal point is required (`"1e"` is rejected, not silently rewritten), and only lowercase `e` matches (OIML writes the scale interval as lowercase), so malformed formulas fail fast with a clear `IllegalArgumentException` naming the offending input — in a compliance system, a loud failure is always preferable to a silent wrong answer.

**Depends on:** Nothing (pure function, no Spring dependencies beyond `@Component` annotation).

**Watch out for:** Only the `<decimal>e` pattern is supported. Percentage formulas (e.g. `0.05%`), fixed-value formulas, and compound expressions (e.g. `1.0e + 0.5e`) used by other OIML test types are explicitly out of scope and will cause an `IllegalArgumentException` if passed. This is intentional — those formula forms belong to test types not yet built.

## ToleranceLookupService
**What it does:** Resolves the maximum permissible error (mpe) for a given instrument, test type, and load value by fetching the matching tolerance rules from the database and evaluating the matched rule's formula against the instrument's scale interval.

**Why built this way:** Uses constructor injection via Lombok `@RequiredArgsConstructor` for both dependencies (`ToleranceRuleRepository`, `MpeFormulaEvaluator`), following CLAUDE.md's convention of never using field injection. The half-open interval convention (min < load <= max) is used for load-band matching because it matches how OIML R76-1's cascading load bands work — a load exactly on a boundary belongs to the next tighter band. Each rule's `loadRangeMin`/`loadRangeMax` (expressed in units of `e`) is multiplied by the instrument's scale interval to get absolute boundary values, avoiding floating-point errors that would occur from dividing the load by `e`. Throws `IllegalStateException` when no rule matches rather than returning a default — silent wrong answers are unacceptable in a compliance system.

**Depends on:** `ToleranceRuleRepository`, `MpeFormulaEvaluator`, `Instrument` entity (for `accuracyClass` and `primaryScale.e`).

**Watch out for:** Only the `<decimal>e` formula pattern is supported (via `MpeFormulaEvaluator`). The method signature is intentionally simplified to `(Instrument, TestType, BigDecimal load)` — OIML edition and tolerance context are not parameters, keeping the API surface minimal for the current scope. Instrument validation (auxiliary device checks, n-value verification, etc.) is not performed here; that responsibility belongs to `InstrumentMetrologicalValidationService` if/when pre-lookup validation is needed.

## WeighingPerformanceCalculationService
**What it does:** Orchestrates the full OIML R76 weighing performance evaluation for a test session: computes raw errors, corrects them against a zero-load baseline, looks up the applicable mpe for each load, determines pass/fail, persists all results, and sets the session-level verdict.

**Why built this way:** Annotated with `@Transactional` because the method writes to two separate entity types (`WeighingPerformanceObservation` and `TestSession`) in a single atomic operation. Without the transaction boundary, a failure after saving observations but before saving the session verdict would leave the database inconsistent — observations with computed values but no corresponding session verdict. Constructor injection via Lombok `@RequiredArgsConstructor` provides all three dependencies. The session is explicitly saved via `TestSessionRepository.save()` after setting the verdict — this was a real bug in an earlier draft where the session was modified in memory but never persisted, causing callers to see a stale verdict.

**Depends on:** `WeighingPerformanceObservationRepository`, `ToleranceLookupService`, `TestSessionRepository`, `Instrument` entity (via `TestSession`), `WeighingPerformanceObservation` entity.

**Watch out for:** The zero-load baseline (E0) is found by locating the observation row where `load == 0`. If no such row exists, the method throws an `IllegalStateException` — it will not proceed without a baseline. This is a hard requirement, not a fallback. The method assumes observations are already persisted (it fetches them by session ID), so the caller must create the session and its observations before calling this method.

## Controllers (TestSessionController, InstrumentController)
**What it does:** Expose REST endpoints for creating test sessions, adding observations, triggering evaluation, and querying results. `TestSessionController` handles session CRUD and evaluation; `InstrumentController` lists sessions for a given instrument.

**Why built this way:** Constructor injection via `@RequiredArgsConstructor` for all dependencies. Each controller maps to its feature package (`testsession/controller/`, `instrument/controller/`). The POST `/evaluate` endpoint triggers `WeighingPerformanceCalculationService` and returns the full result (session verdict plus every observation's E, Ec, mpe, and pass/fail) — callers get the complete evaluation in one round trip. A `GlobalExceptionHandler` (`@RestControllerAdvice`) catches `IllegalArgumentException` and `IllegalStateException` from the service layer and returns proper 4xx errors (not generic 500s) with a JSON error message body.

**Depends on:** `TestSessionRepository`, `InstrumentRepository`, `WeighingPerformanceObservationRepository`, `WeighingPerformanceCalculationService`, DTOs (`CreateTestSessionRequest`, `CreateObservationRequest`, `TestSessionResponse`, `EvaluateResponse`, `ObservationResponse`), `GlobalExceptionHandler`.

**Watch out for:** The list-sessions-by-instrument endpoint currently fetches all sessions and filters in memory — this will need a repository-level query method (`findByInstrumentId`) when the dataset grows. No authentication or authorization is enforced (auth is explicitly deferred per CLAUDE.md). The evaluate endpoint only supports `WEIGHING_PERFORMANCE` test type currently — other test types will need their own calculation services.
