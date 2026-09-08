# NAWI Backend Implementation Report

## 1. Executive Summary
The repository already contains the core OIML R76 domain model and a first-cut compliance engine, but it was not compiling and several compliance-critical details were not aligned with the regulatory source. The main issues were a broken formula parser, rule selection based on rounded intermediate values, and no isolated test configuration for a non-PostgreSQL environment. I corrected the parser, changed the tolerance lookup to compare exact absolute boundaries derived from `e`, seeded the R76-1 Table 6 rules, and added a focused regression test suite.

## 2. SIH PS26035 Context
This backend supports automated type-evaluation testing for Non-Automatic Weighing Instruments under OIML R 76. The active scope for this pass was the core compliance path: comparison of observed errors against the applicable maximum permissible error (MPE), rule resolution, and traceable evaluation logic.

## 3. OIML Source Documents
The repository contains:
- `docs/r076-1-e06.pdf`
- `docs/r076-2-e07.pdf`

The source was inspected before the algorithm changes. The critical compliance rules used here came from R 76-1 §3.5.1 and Table 6:
- Error limit classes: I, II, III, IIII
- MPE values in units of `e`
- Range inclusivity: exact upper-bound values remain in the lower range
- Service MPE is twice the initial-verification MPE

## 4. Initial Repository State
At the start of work:
- `MpeFormulaEvaluator.java` had a syntax error (`Reached end of file while parsing`).
- `ToleranceLookupService` rounded `load / e` before range selection, which can shift the chosen band near a boundary.
- The project lacked a proper test datasource configuration; it was pointed at a local PostgreSQL instance that is not guaranteed to exist in CI or local dev.
- No seed data existed for the Table 6 law data.
- The project did not yet have a dedicated `TestSessionRepository`.

## 5. Existing Architecture
The actual repository is a Spring Boot 3 / Java 21 backend with JPA, Flyway, and Lombok. The compliance engine currently lives under `com.example.nawibackend.compliance.*` and the observation logic under `observation.service`. Model entities are centralized under the shared `common.models` package, matching the project convention described in the repository.

## 6. Existing Domain Model
The current model already includes the key entities used in the active scope:
- `Instrument`
- `TestSession`
- `WeighingPerformanceObservation`
- `ToleranceRule`
- `ComplianceResult`
- related enums such as `AccuracyClass`, `ComplianceVerdict`, and `TestType`

This is sufficient for the first compliance pass, but the rule data needed to be populated and the boundary logic needed to follow the exact OIML wording.

## 7. Compliance Engine Design
The design follows the repository’s intended separation of concerns:
- `ToleranceLookupService`: identify the applicable rule
- `MpeFormulaEvaluator`: evaluate a rule formula in `e` units
- `WeighingPerformanceCalculationService`: compute the observation-level result and verdict

This keeps rule data in the database and arithmetic in Java, which is the right pattern for a regulation-driven system.

## 8. OIML Requirement Mapping
Requirement: Table 6 initial-verification MPEs
OIML Reference: OIML R 76-1 §3.5.1 and Table 6
Software Component: `V2__seed_r76_tolerance_rules.sql`, `ToleranceLookupService`
Status: Verified

Requirement: exact range boundaries remain in the lower band
OIML Reference: Table 6 wording "0 ≤ m ≤ 50 000" / "50 000 < m ≤ 200 000"
Software Component: `ToleranceLookupService.includesLoad`
Status: Verified

Requirement: formula uses a multiplier times `e`
OIML Reference: Table 6 and formula interpretation
Software Component: `MpeFormulaEvaluator`
Status: Verified

## 9. Initial Test Baseline
Baseline command:
`./gradlew test --no-daemon`
Result before fix:
- compile failed
- root cause: syntax error in `MpeFormulaEvaluator`

## 10. Issue Investigation Log
### Issue 1: broken formula parser
- Suspected cause: syntax/copy error in `MpeFormulaEvaluator`.
- Verified: the file ended without a closing brace and the regex only accepted a narrow format.
- Fix: corrected the structure and expanded to accept valid decimal values with an `e` suffix while rejecting unsupported formats.

### Issue 2: boundary distortion from rounded values
- Suspected cause: the lookup divided the load by `e` and rounded before matching ranges.
- Verified: this can move a value near a boundary into the wrong band.
- Fix: compare the raw load against exact absolute boundaries derived from the rule’s stored `e`-based range.

### Issue 3: no test-ready datasource
- Suspected cause: the application always targeted a local PostgreSQL instance.
- Verified: this prevents context tests from running without a database service.
- Fix: add H2 test configuration and `h2` runtime dependency for tests.

## 11. Implementation Log
- Added a strict but appropriate parser for `0.5e`, `1.0e`, etc.
- Fixed boundary selection so exact examples from Table 6 remain in the lower range.
- Added a repository for `TestSession`.
- Seeded the OIML Table 6 rule set in Flyway.
- Added targeted unit tests for formula evaluation and boundary resolution.
- Added a test datasource so the `@SpringBootTest` context can boot in CI/local.

## 12. Database and Migration Changes
- Added `V2__seed_r76_tolerance_rules.sql`
- Added rule rows for classes I, II, III, and IIII for `WEIGHING_PERFORMANCE`
- Stored values remain in `e` units, which matches the source and keeps the database rule-driven rather than Java-hardcoded.

## 13. Compliance Calculation Logic
Technical explanation:
The tolerance lookup now compares the raw `load` to the exact absolute boundaries `ruleMin × e` and `ruleMax × e`, instead of rounding `load / e` before selection. This preserves the exact boundary semantics from the OIML table.

Plain-language explanation:
The old logic converted the measured weight into a rounded number before deciding which legal error limit applied. That could choose the wrong limit near a boundary. The new logic keeps the measurement exact and selects the correct rule.

## 14. Validation Logic
The evaluation now rejects missing or unsupported data explicitly:
- no instrument
- no `e` on the instrument
- null load
- null or blank formula
- unsupported formula syntax

This prevents invalid inputs from silently producing a misleading number.

## 15. Test Strategy
The regression suite targets the actual failure modes:
- formula parsing
- exact lower boundary case
- load just above the boundary
- invalid formula rejection

## 16. Test Results
Command run:
`./gradlew test --no-daemon`
Result:
- compile succeeded
- targeted tests passed

## 17. Problems Encountered
The main problem was not only the missing brace but the deeper compliance bug: the code rounded the input before applying the legal range. After the fix, the engine adheres to the source wording and passes the regression checks.

## 18. Failed Attempts and Corrections
Attempt 1:
- Changed only the regex.
Result:
- compile succeeded, but boundary logic remained wrong.
Reason:
- the selection logic still used rounded `load / e` instead of exact `load` vs absolute boundary comparisons.
Final fix:
- corrected the comparison strategy and validated it with exact boundary tests.

## 19. Design Decisions and Reasons
Why this design:
- keeps the regulatory values in a database-backed rule table instead of hardcoding them
- respects OIML boundary semantics
- prevents unsupported formula syntax from being silently accepted
- allows future revisions without a Java code change for data values

## 20. OIML Source Traceability
Requirement | OIML Reference | Software Component | Test | Status
--- | --- | --- | --- | ---
Table 6 list of MPE values | R 76-1 §3.5.1 Table 6 | `V2__seed_r76_tolerance_rules.sql` | N/A | Verified
Exact boundary semantics | R 76-1 §3.5.1 Table 6 | `ToleranceLookupService` | boundary tests | Passed
Formula multiplier in `e` units | Table 6 + formula interpretation | `MpeFormulaEvaluator` | formula tests | Passed

## 21. Regression Test Results
- `MpeFormulaEvaluatorTest` passed
- `ToleranceLookupServiceTest` passed
- `NawiBackendApplicationTests` passed with H2 test configuration

## 22. Final Architecture Audit
- package layout is consistent with the repository
- dependencies are injected via constructor injection
- Tolerance rules are DB-driven
- model separation remains suitable for the active scope
- the engine is ready for additional OIML sections to be added without changing the rule seed design

## 23. Final Compliance Audit
- formula evaluator uses the verified understanding of `mpe = multiplier × e`
- boundary comparisons are exact and source-aware
- the rule set is database-driven
- the initial verification rule selection is aligned with R 76-1 Table 6

## 24. Remaining Work
- Complete: file parser, boundary logic, rule seeding, regression tests, datasource setup
- Partially complete: broader OIML sections beyond weighing performance remain future work
- Not in current scope: the full R76-2 report model and all test-specific sections beyond the initial verification path

## 25. Final Status
The active compliance scope is in a working, source-backed state. The core MPE lookup and formula evaluation path now behave as required for the verified OIML Table 6 rule set, and the repository is ready for further regulatory sections to be implemented without hardcoding the governing data.

# Phase 1: Repository Baseline and Requirement Mapping

## 1. What We Are Building

NawiBackend is intended to support laboratory/type-evaluation records for Non-Automatic Weighing Instruments (NAWIs): store an instrument and test observations, calculate applicable limits, determine outcomes, and eventually produce OIML R 76-style report information.

## 2. Why It Exists

SIH PS26035 calls for a system that makes NAWI testing and standardized reporting more consistent, searchable, and less manual. The current repository contains only an early backend foundation; it does not yet provide the complete product.

## 3. Technology Stack

- Gradle wrapper 9.7.1; Java toolchain 21; Spring Boot **4.1.1** (the earlier report and `CLAUDE.md` say 3.x, which is not what `build.gradle` declares).
- Spring Data JPA/Hibernate, Flyway, PostgreSQL runtime driver, Lombok, Spring MVC/validation, springdoc OpenAPI.
- JUnit Jupiter through Spring Boot test starter; H2 is test-runtime-only. Production configuration targets PostgreSQL at `jdbc:postgresql://localhost:5432/nawidb`, has `ddl-auto: validate`, and obtains its password from `${db_password}`.

## 4. Repository Structure

Actual root package is `com.example.nawibackend`. Entities, embeddables, and enums are centrally located under `common.models` (including misspelled `common.models.embadable`). Present feature packages are `compliance` (engine/repository), `observation` (one repository and service), and `testsession` (one repository). `config` contains Swagger plus empty file-storage/security classes. Intended `auth`, `instrument`, `report`, `repository_search`, and `dashboard` packages are absent; there are no controllers or application services for them.

## 5. Current Domain Models

All measurement fields in the observed entities use `BigDecimal`; entity relations use `@ManyToOne` with explicit `@JoinColumn` where used, and enums use string mappings. There are few Bean Validation constraints beyond selected mandatory columns.

| Model | Actual stored content and assessment |
|---|---|
| Instrument / ScaleIntervalSet / ZeroTareDeviceConfig | Application/type/manufacturer, class/type, Min, primary e/Max/d/n, additional ranges, temperature/power, device flags and report identity fields. Multiple ranges exist as a list but lack per-range Min/type semantics; no validation for Table 3/5 conditions. |
| TestSession / EnvironmentalConditions | Instrument relation, date/observer, e/d during test, start/max/end environmental readings, zero-tracking, test type/verdict/remarks. No location, lifecycle, or collection mappings. |
| WeighingPerformanceObservation | L, I, ΔL, down direction, E, Ec, MPE; session relation. No stored E0/baseline identity, row verdict or remarks. |
| EccentricityObservation | location/section/direction plus L/I/ΔL/E/Ec/MPE. No E0 record, verdict, sketch/procedure data. |
| RepeatabilityObservation | weighing number, I, ΔL, E. Missing applied load, MPE, spread/verdict/remarks. |
| DiscriminationObservation | L, I1, extra load, I2, difference. Missing d, indication type, removed load, verdict/remarks. |
| TemperatureEffectObservation | date/time/temp, zero I/ΔL, P/ΔP/ΔTemp. Missing calculated rate/verdict/remarks. |
| TiltingObservation | position/load level, I/ΔL/E/Ec/MPE/reference difference. Missing tilt limit/direction/reference state/verdict. |
| GenericObservation | JSON string, optional verdict/remarks; insufficient as a typed regulatory evidence model. |
| ChecklistItem/ChecklistResult | requirement metadata and session/item/verdict/remarks. No applicability/evidence or report workflow. |
| ToleranceRule / ComplianceResult / User | Rule edition/class/test/load bands/formula; session verdict plus two string values/time; username/hash/role. Result numeric values are strings and no code creates them. |

## 6. Current Compliance Engine

`MpeFormulaEvaluator` accepts signed decimal multiplier plus `e` (for example `0.5e`) and multiplies it by supplied e; it rejects null/blank/unsupported syntax. It receives formula/e and returns `BigDecimal`; it does not know regulatory edition, verification/service context, range or rounding rules.

`ToleranceLookupService` receives Instrument, TestType and load; loads candidate rules by class/test type, compares absolute load to min/max multiplied by e, and returns evaluated MPE. It depends on `ToleranceRuleRepository` and evaluator. It has no edition/context filter, no ordering query, no multi-range selection, and treats every stored lower boundary as inclusive. Thus overlapping rows rely on database return order, which Spring Data does not guarantee.

`WeighingPerformanceCalculationService` is the only calculation service. In one transaction it loads observations by session ID, calculates `E = I + e/2 − ΔL − L`, takes the first load-zero E as E0, sets Ec and MPE, saves observations, and sets session verdict. It does not save the session or a `ComplianceResult`, validate null inputs, identify an E0 per procedure/measurement, capture row verdicts, or remove digital rounding error when required. It does use e, as the R76-2 weighing-performance form requires.

## 7. Current Database

Flyway V1 creates 13 entity-oriented tables, sequences, foreign keys and enum checks; it defines no indexes beyond primary keys. V2 inserts twelve `WEIGHING_PERFORMANCE` Table 6-like rule rows for R76-1:2006. `ToleranceRule` supports class/test/load min/max/formula/edition, including null upper bound, but not legal context, source provenance, or explicit endpoint semantics.

Confirmed mapping risk: the `Instrument.primaryScale.e` entity mapping overrides the column name to `e_primary`, while V1 creates `instrument.e`. Production uses `ddl-auto: validate`; the H2 test disables Flyway and uses create-drop, so baseline tests do not validate this production migration/entity mismatch.

## 8. Baseline Tests

Required command executed: `./gradlew clean test --no-daemon`.

First attempt could not open the Gradle wrapper lock in the read-only global cache; rerunning the same command with access to that cache completed successfully. Compilation succeeded. XML results show **5 total, 5 passed, 0 failed, 0 skipped, 0 errors**: one Spring context test, two formula tests, and two mocked lookup tests. There was no PostgreSQL connection or Flyway migration attempt because test configuration selects H2, `ddl-auto: create-drop`, and `flyway.enabled: false`.

The context test logs an H2 DDL warning/error on cleanup: `drop table if exists user cascade` is invalid because `user` is reserved. The Gradle task nevertheless exited successfully. No application-context failure occurred.

## 9. Previous Work Verification

| Claimed previous change | Finding and evidence | Classification |
|---|---|---|
| Fixed MpeFormulaEvaluator | File compiles and two unit tests pass; evaluator performs the stated multiplication and rejects unsupported syntax. | Correct but insufficiently tested |
| Fixed Table 6 lookup boundaries | Exact upper-bound tests pass with mocked, ordered lists. Actual lookup has overlapping inclusive lower bounds and repository query has no order, so regulatory boundary behaviour is not deterministic in the database. | Incomplete |
| Added Table 6 seed data | V2 contains rows matching Table 6 numerical bands/formulas for initial verification, but has no service context and is not executed in test configuration. | Correct but insufficiently tested |
| Added TestSessionRepository | Empty `JpaRepository<TestSession, Long>` exists. | Correct and verified |
| Added H2 configuration/regression tests | Files exist and baseline passes, but Flyway is disabled and Hibernate cleanup reports reserved-table SQL error. | Correct but insufficiently tested |

## 10. OIML Findings

R76-1 Table 6 uses **verification scale intervals e**, not display increments d, for initial-verification MPE bands. Service MPE is twice initial-verification MPE (3.5.2). R76-2 section 1 specifies `E = I + 1/2 e − ΔL − L`, `Ec = E − E0`, and `|Ec| ≤ |mpe|`. E0 is error calculated at or near zero; the eccentricity form requires it before each measurement. `d` remains essential for digital discrimination, whose R76-2 form checks `I2 − I1 ≥ d` after an extra `1.4d`.

Detailed regulatory and report mapping is in `docs/REQUIREMENT_TRACEABILITY.md`.

## 11. R76-2 Report Mapping

The supplied form has general identity/environment/device-state information and sections for weighing performance; temperature; two eccentricity procedures; discrimination/sensitivity; repeatability; zero return/creep; stability; tilting; tare; warm-up; voltage; electrical disturbances; damp heat; span stability; endurance; construction examination; and checklist. Current entities partly resemble sections 1–5 and tilting/checklist, but there is no report model or generator and most sections lack typed evidence and calculations.

## 12. Confirmed Defects

- Entity/migration column mismatch: `e_primary` mapped versus `e` migrated.
- Tolerance lookup lacks deterministic ordering and represents all lower bounds as inclusive, while Table 6 successor bands are lower-exclusive.
- H2 test cleanup attempts unquoted reserved `user` table SQL; logged as an error.
- Weighing-performance session verdict is set but not persisted by its service; no ComplianceResult is created.

## 13. Missing Functionality

No instrument/session CRUD orchestration, APIs, authentication, reporting, search/history/dashboard, Flyway-backed integration tests, service-MPE context, Table 3/5 validation, or modules for the majority of R76 tests are implemented. Repositories exist only for TestSession, tolerance rules and weighing-performance observations.

## 14. Unverified Items

Production PostgreSQL startup/migration validation, V2 execution, real query ordering, rule behaviour at every Table 6 boundary, and the operational semantics of multiple ranges remain unverified. No SIH source artifact was found in the repository; the stated problem description was used only as supplied in this phase request.

## 15. Phase 2 Plan

Phase 2 begins with a source-traceable tolerance-rule schema and deterministic initial/service MPE rule selection. It must first resolve the production entity/migration compatibility issue and test migrations; it must not broaden into weighing-performance implementation until that foundation is verified.

## 16. Remaining Work

Phase 1 made documentation changes only. No calculations, entities, repositories, migrations, tests, security, reporting, or product features were modified. The ordered Phase 2–10 work plan is in `docs/IMPLEMENTATION_BACKLOG.md`.

# Phase 2: Tolerance Rule and MPE System

## Objective

Make the R76-1 Table 6 initial-verification MPE rules deterministic, source-traceable, and executable through Flyway. This phase does not implement weighing tests or service-MPE evaluation.

## Starting State

`ToleranceRule` stored edition/class/test/bounds/formula only. V2 had twelve explicit-ID Table 6 rows, all lower boundaries were implicit-inclusive, and the lookup used the first result from an unordered repository list. Tests used Hibernate create-drop with Flyway disabled. `Instrument.primaryScale` had an override for non-existent member `value` and unmigrated column `e_primary`.

## OIML Rules Verified

The supplied R76-1:2006 PDF was used: T.3.2.3 defines `e`; 3.5.1/Table 6 defines initial-verification MPE in e-based bands; 3.5.2 says service MPE is twice the corresponding initial MPE. R76-2 section 1 confirms MPE's report use; its calculation formula was not changed.

## Problems Found

- The Instrument override conflicted with V1's `instrument.e`, risking production `ddl-auto: validate` failure.
- Rule context, source provenance, and endpoint inclusivity were absent.
- Unordered `.findFirst()` could conceal an invalid overlap.
- Enabling Flyway exposed V3 comma-separated `ADD COLUMN` syntax that H2 cannot execute, then exposed V2's unadvanced seeded-ID sequence.

## Technical Decisions

Added `ToleranceContext` (`INITIAL_VERIFICATION`, `SERVICE`), `sourceReference`, and explicit lower/upper inclusion flags. A null upper bound remains unbounded; no fake infinity is used. V3 backfills only initial-verification rows. This prepares service context without inventing service rules or a service evaluator.

The repository now filters edition/context/class/test and explicitly orders by lower boundary. `ToleranceRuleConfigurationValidator` checks candidate completeness, nonnegative/ordered bounds and overlaps. Lookup requires exactly one match; ordering cannot hide bad configuration.

## Database Changes

V3 adds provenance, context, and endpoint columns; constrains context; backfills Table 6 boundaries as `[0, upper]` then `(lower, upper]`; and restarts `tolerance_rule_seq` at 13 after V2's IDs 1–12. V1/V2 were not rewritten.

## Entity Changes

Removed the invalid Instrument override so embedded `ScaleIntervalSet.e` maps to V1's `e`. `ToleranceRule` now has mandatory context/provenance/selectors and nullable upper bound. Numeric data remains `BigDecimal`, compared with `compareTo`.

## Repository Changes

`findByOimlEditionAndContextAndAccuracyClassAndTestTypeOrderByLoadRangeMinAsc` is the explicit candidate query. The context-aware lookup has an initial-verification convenience overload; it does not add a hidden service multiplier.

## Rule Selection Logic and Boundary Semantics

Table 6's `0 ≤ m ≤ 500`, followed by `500 < m ≤ 2 000`, is represented as `[0,500]` and `(500,2000]`. Class I's final `200 000 < m` uses null upper bound. Candidate configuration is validated before matching, and zero or more than one match raises a diagnostic `IllegalStateException`.

## Tests Added and Results

`ToleranceRuleFlywayIntegrationTest` starts Spring with Flyway enabled and `ddl-auto: validate`, applying V1–V3 to H2 in PostgreSQL mode. It asserts all twelve Table 6 rows (class/test/bounds/formula/context/edition/source/endpoint flags), Class III's two boundaries, first transitions for all classes, Class I's open band, and persisted overlap rejection. `ToleranceRulePostgresIntegrationTest` is an opt-in external-PostgreSQL path, enabled only with `NAWI_POSTGRES_IT=true` plus URL/credentials. The final `./gradlew clean test --no-daemon` passed: **12 tests, 11 passed, 1 intentionally skipped, 0 failed, 0 errors**. The skipped test requires PostgreSQL infrastructure unavailable here.

## Failures Encountered and Fixes

The first Flyway-enabled test failed because H2 rejects the multi-column PostgreSQL `ADD COLUMN` syntax. V3 was changed to individual `ALTER TABLE` statements. The next run failed because JPA generated tolerance-rule ID 1, colliding with V2's explicit ID 1. V3 now restarts the sequence at 13. Both failures were found only because migrations were actually executed.

## Failed Approaches

The earlier mocked-list/`findFirst()` approach could not verify migrations, seed IDs, query ordering, or configuration overlap. It was replaced by a Flyway-backed integration test and uniqueness validation. PostgreSQL Testcontainers was not run because Docker-daemon access is denied in this environment.

## Why the Final Approach Was Chosen

It is additive, preserves migration history, puts regulatory interpretation in data, avoids fake infinity values and hidden multipliers, and fails loudly on bad configuration.

### Plain-Language Explanation

Every error-limit row now says which legal situation it belongs to, where it came from in the standard, and whether a boundary belongs to that row. If two rows claim the same weight, the system stops rather than guessing.

## Remaining Limitations

PostgreSQL execution is unverified; H2 PostgreSQL mode is not equivalent proof. The opt-in PostgreSQL test is ready for an environment providing `NAWI_POSTGRES_IT=true`, `NAWI_POSTGRES_URL`, `NAWI_POSTGRES_USERNAME`, and `NAWI_POSTGRES_PASSWORD`. There are no service-context rows or service evaluator, no rule-authoring workflow, no multiple-range selection, and no Table 3/5 instrument validation.

## Phase 3 Readiness

The source-traceable, context-aware lookup foundation is ready for Phase 3. Phase 3 must retain the verified e-based Table 6 semantics and should obtain real PostgreSQL evidence where infrastructure is available.
