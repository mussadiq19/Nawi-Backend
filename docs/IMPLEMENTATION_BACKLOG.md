# Implementation Backlog

This is a dependency-ordered plan derived from the inspected repository and supplied OIML documents. Status is updated through Phase 2; later phases remain planned work.

## Phase 2 — Tolerance Rule and MPE System

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `ToleranceRule`, `V3__add_tolerance_rule_context_and_traceability.sql`, `Instrument` | **Complete on migrated H2; PostgreSQL unverified.** Added context, source reference, endpoint flags and seed-sequence repair; removed invalid embedded-column override. | R76-1 3.5.1, 3.5.2, Table 6; Phase 1 schema findings. | Flyway V1–V3/JPA-validate integration tests, seed traceability and boundary tests. |
| `ToleranceRuleRepository`, `ToleranceLookupService`, `ToleranceRuleConfigurationValidator` | **Complete on migrated H2.** Ordered edition/context/class/test candidate query; explicit matching and overlap rejection. | Phase 2 entity/schema. | Database query/boundary/open-range/overlap integration tests. |

## Phase 3 — Formula Evaluation and Rule Lookup

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `ToleranceLookupService`, `MpeFormulaEvaluator` | Apply exact Table 6 lower-exclusive/upper-inclusive semantics and context; select applicable scale/range, reject invalid configuration. | R76-1 3.3, 3.5.1–3.5.2; Phase 2. | Each class/boundary, service-double, range and invalid-input tests. |
| `instrument/service/InstrumentMetrologicalValidationService.java` (new) | Validate e/d/n/Min/class constraints before regulatory calculation. | R76-1 3.1.2, 3.2–3.4. | Table 3/5 and multi/multiple-range cases. |

## Phase 4 — Weighing Performance

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `WeighingPerformanceCalculationService`, observation entity/migration | Model a traceable E0 baseline and row direction; calculate E and Ec, resolve MPE, persist row/session result. | R76-1 3.5.3; R76-2 §1/A.4.4/A.5.3.1. | Positive/negative/error-boundary, zero baseline, missing data, e-vs-d, transaction tests. |

## Phase 5 — Test Session Orchestration and Persistence

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `testsession/service/TestSessionService.java` (new), `TestSessionRepository`, `ComplianceResult` | Create/load/authorize/save sessions, run appropriate evaluator transactionally, aggregate verdicts and persist `ComplianceResult`. | SIH; all previous phases. | Repository integration, rollback and detached-session cases; persisted result audit. |

## Phase 6 — Eccentricity

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `observation/service/EccentricityCalculationService.java` (new), `EccentricityObservation`, new migration | Implement weights and rolling-load procedures, required load selection, per-location E0/MPE and results. | R76-1 3.6.2; A.4.7; R76-2 §3. | Support-point, rolling, location and failure tests; report-ready rows. |

## Phase 7 — Repeatability

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `observation/service/RepeatabilityCalculationService.java` (new), `RepeatabilityObservation`, new migration | Capture load and readings; calculate individual error and Emax−Emin; enforce both conditions. | R76-1 3.6, 3.6.1; A.4.10; R76-2 §5. | Ten/twenty reading pass/fail and individual-MPE tests. |

## Phase 8 — Discrimination

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| `observation/service/DiscriminationCalculationService.java` and `SensitivityCalculationService.java` (new), `DiscriminationObservation`, new sensitivity entity/migration | Separate digital, analog and non-self-indicating procedures; use d only for digital criterion. | R76-1 3.8; A.4.8/A.4.9; R76-2 §4. | Digital `1.4d`/`I2-I1≥d`, analog and sensitivity threshold tests. |

## Phase 9 — Remaining Applicable Test Modules

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| New exact services `TemperatureEffectCalculationService.java`, `ZeroReturnService.java`, `CreepService.java`, `StabilityOfEquilibriumService.java`, `TiltingCalculationService.java`, `TareService.java`, `WarmUpTimeService.java`, `VoltageVariationService.java`, `ElectricalDisturbanceService.java`, `DampHeatService.java`, `SpanStabilityService.java`, `EnduranceService.java`, `ConstructionExaminationService.java`, and corresponding typed entities/migrations | Implement temperature/no-load, zero return, creep, stability, tilting, tare, warm-up, voltage, disturbances, damp heat, span stability, endurance, construction/checklist. Replace `GenericObservation` where a regulated table needs typed fields. | R76-1 3.9, 4.1–4.18, 5.3–5.4, A/B annexes; R76-2 §§2, 6–17. | Procedure-specific acceptance/boundary tests and retained raw evidence. |

## Phase 10 — Integration, Regression, Audit, Documentation

| File/class | Work and reason | Source / dependencies | Tests and completion evidence |
|---|---|---|---|
| New `report/service/R76ReportService.java`, `report/model/R76Report.java`, `repository_search/service/ReportSearchService.java`, and integration-test fixtures | Generate standardized R76-2 report data, audit/version records, API integration and regression suite. Correct production/test Flyway parity and database portability. | R76-2 report structure; SIH requirements. | End-to-end report fixtures, PostgreSQL Flyway validation, security/audit tests, traceability update. |
