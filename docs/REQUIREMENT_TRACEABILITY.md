# Requirement Traceability

## Scope and evidence

This map is an inspection record, not a claim that the listed requirements are implemented. Regulatory references below were read from `docs/r076-1-e06.pdf` (OIML R 76-1:2006) and `docs/r076-2-e07.pdf` (OIML R 76-2:2007). Repository status is based on the source tree and migrations inspected on 2026-09-08.

### Technical Explanation

R 76-1 supplies the requirements and procedures; R 76-2 supplies the standard type-evaluation report forms. SIH PS26035 supplies the product objective. A report form is evidence of data that must be captured, not by itself an implementation of the regulatory test.

### Plain-Language Explanation

The standard tells the software both what must be tested and what information a formal report must show. This document keeps those two things separate so later work does not mistake an empty database field for a completed legal test.

## SIH Requirements

| Requirement | Source | Backend area | Current status |
|---|---|---|---|
| Instrument information and technical specifications | SIH PS26035 | `common.models.Instrument` | Partial: identity, class, scale, environment/power-related fields exist; no application service/API. |
| Laboratory/environmental conditions | SIH PS26035 | `TestSession`, `EnvironmentalConditions` | Partial data model only. |
| Test-observation entry | SIH PS26035 | observation entities | Partial: entities exist for six test types; no entry services/controllers. |
| Automatic permissible-error calculation and validation | SIH PS26035 | compliance engine | Partial, weighing-performance path only; regulatory migration is not exercised by tests. |
| Automatic pass/fail | SIH PS26035 | `WeighingPerformanceCalculationService` | Partial: row result is not persisted and session verdict persistence is caller-dependent. |
| Standardized reports/repository/search/history/dashboard | SIH PS26035 | report/repository_search/dashboard | Missing: packages and implementations absent. |
| Role-based access | SIH PS26035 | `User`, `Role`, `SecurityConfig` | Data model exists; security implementation absent. |
| Future OIML updates | SIH PS26035 | `ToleranceRule.oimlEdition` | Partial design only; no edition/context selection or provenance. |

## OIML Calculation Requirements

| OIML reference | Requirement | Inputs / output / acceptance condition | Likely component | Current status |
|---|---|---|---|---|
| Definitions T.3.2.2, T.3.2.3, T.3.2.5 | `d` is actual scale interval; `e` is verification scale interval; `n = Max/e`. | Scale values; classification/verification inputs. | `ScaleIntervalSet`, Instrument validation | Partial storage; no validation/calculation. |
| 3.1.2; 3.2; Table 3 | `e`, `n`, and Min must meet class-dependent constraints. | Class, e, Max, Min; compliance outcome. | Instrument validation service | Missing. |
| 3.3 | Multi-interval ranges use load-applicable `e`; multiple ranges are individually treated as one-range instruments. | Range boundaries, e/Max/Min; applicable range. | Range-selection and validation service | Missing; `additionalRanges` lacks range semantics/Min. |
| 3.4.2–3.4.3 | For auxiliary indication, `d < e ≤ 10d`; Min table uses `d`; exception stated for class I below 1 mg. | d, e, class, Min. | Instrument validation | Missing. |
| 3.5.1, Table 6 | Initial-verification MPE for increasing/decreasing load is class and load-in-`e` band dependent. | class, load `m`, applicable e; absolute MPE. | `ToleranceRule`, lookup/evaluator | Partial. See Table 6 map below. |
| 3.5.2 | Service MPE is twice initial-verification MPE. | Initial MPE; service MPE. | tolerance rule context/service | Missing: no verification/service context. |
| 3.5.3.1–3.5.3.4 | Determine errors under normal conditions; remove digital rounding error when `d > 0.2e`; net MPE applies for tare except preset tare; tare-weighing MPE equals instrument MPE for same load. | Conditions, d/e, tare type, gross/net data. | test orchestration and tare module | Mostly missing. |
| 3.6, 3.6.1 | Every individual result must meet MPE; repeated same-load result spread must not exceed absolute MPE. | repeated errors and MPE; pass/fail. | repeatability service | Missing. |
| 3.6.2; A.4.7 | Eccentric-location indications must meet MPE; prescribed loads depend on receptor/support/rolling-load case. | load geometry, location/direction, error/E0/MPE. | eccentricity service | Entity partial; service missing. |
| 3.8; A.4.8 | Discrimination procedure differs by indication type. Digital procedure uses **d** and checks added `1.4d` produces at least `d` indication change. | d, I1/I2, removed/additional loads; pass/fail. | discrimination service | Partial raw fields; calculation missing. |
| 3.9.1; A.5.1 | Class II/III/IIII tilt influence has limits at no load and loaded condition; procedure uses limiting tilt. | class, tilt direction/limit, reference/tilted indications/errors. | tilting service | Partial fields; required tilt/reference data missing. |
| 3.9.2.3; A.5.3.2 | Zero/no-load indication variation: no more than e per 5 °C for II/III/IIII or per 1 °C for I. | temperature sequence, I, ΔL, e; zero change rate. | temperature-effect service | Partial fields; service missing. |
| 3.9.3; A.5.4 | Voltage variations must retain indications within MPE. | supply category/voltages and performance readings. | voltage module | Missing. |
| 3.9.4.1–3.9.4.3; A.4.11, A.6 | Creep, zero return, and endurance have separate procedures and outcomes. | timed readings/load cycles/errors. | dedicated modules | Missing. |
| 4.4.2; A.4.12 | Stability of equilibrium is tested, including zero-setting/tare-balancing case. | release time, indication evolution, device state. | stability module | Missing. |
| 5.3.3, 5.3.5; 5.4; B.2–B.4 | Electronic-instrument span stability, warm-up, influence factors/disturbances, damp heat and span stability are separately tested. | environmental/electrical sequences and readings. | environmental/electrical modules | Missing. |
| 4.1–4.18; A.2; R76-2 checklist | Construction requirements require examination and recorded checklist results. | requirement, applicability, observation, remarks. | checklist/construction module | Partial generic checklist entities only. |

## Phase 2 Evidence — Tolerance/MPE Foundation

| Source requirement | Code component | Database component | Test evidence | Status |
|---|---|---|---|---|
| R76-1 T.3.2.3; 3.5.1/Table 6: MPE bands are in e | `ToleranceLookupService`, `MpeFormulaEvaluator` | `tolerance_rule.load_range_*`, `mpe_formula` | `ToleranceRuleFlywayIntegrationTest` exact/just-above boundaries and open Class I band | Implemented and verified on migrated H2 |
| R76-1 3.5.1/Table 6: successor bands are lower-exclusive; upper bounds are inclusive | `ToleranceRule` endpoint flags, lookup matcher | V3 columns/backfill | Database-backed Class I–IIII transition tests and Class III second boundary | Implemented and verified on migrated H2 |
| R76-1 3.5.1/Table 6: all rule values and source traceability | `ToleranceRule.oimlEdition/sourceReference` | V2 rows plus V3 `oiml_edition`, `source_reference` | All 12 migrated rows asserted for class/test/bounds/formula/context/edition/source/endpoints | Implemented and verified on migrated H2 |
| R76-1 3.5.2: service MPE is distinct from initial verification | `ToleranceContext` and lookup parameter | V3 `context` constraint | Initial rows asserted; no service rows seeded | Implemented as representation; service evaluation not in scope |
| One applicable rule only | configuration validator and ordered repository query | V3 boundary fields; sequence restart | Transactional overlap test throws configuration error | Implemented and verified on migrated H2 |
| Entity/migration compatibility | `Instrument.primaryScale` embedded mapping | Existing V1 `instrument.e`; V1–V3 | Context starts with Flyway then `ddl-auto: validate` | Verified on H2 only; PostgreSQL unverified |

PostgreSQL/Testcontainers verification is **unverified**: Docker is installed but this environment denied access to its daemon. H2 runs in PostgreSQL compatibility mode and validates the actual Flyway path, but is not evidence of PostgreSQL execution. `ToleranceRulePostgresIntegrationTest` provides an opt-in real-PostgreSQL path using `NAWI_POSTGRES_IT=true` and `NAWI_POSTGRES_URL`, `NAWI_POSTGRES_USERNAME`, and `NAWI_POSTGRES_PASSWORD`; it was intentionally skipped here.

### e versus d: mandatory distinction

| Quantity | Source meaning | Where it is used |
|---|---|---|
| `e` | R 76-1 T.3.2.3: verification scale interval, used for classification and verification. | Table 6 load bands and MPE; R76-2 weighing-performance equation `E = I + 1/2 e − ΔL − L`; temperature, repeatability and many report forms. |
| `d` | R 76-1 T.3.2.2: actual scale interval between marks/indicated values. | R76-1 rounding-elimination trigger (`d > 0.2e`) and R76-2 digital-discrimination criterion (`I2 − I1 ≥ d`, extra load `1.4d`). |

The current weighing-performance service uses `e`, which agrees with the R76-2 form. It must not be changed to `d` merely because a digital instrument has a display increment. Digital discrimination is a separate procedure and specifically uses `d`.

### Weighing-performance calculation

R76-2 section 1 (A.4.4/A.5.3.1) states:

`E = I + 1/2 e − ΔL − L`

`Ec = E − E0`, where `E0` is the error calculated at or near zero; pass when `|Ec| ≤ |mpe|`.

`I` is indication, `e` verification scale interval, `ΔL` additional load used to remove rounding, and `L` applied load. The zero-load (or near-zero) observation supplies `I0`, `ΔL0`, and `L0` to calculate `E0` by the same error procedure. The R76-2 eccentricity form additionally says E0 is determined before each measurement. MPE is selected from R76-1 Table 6 for initial verification (or twice that value for service under 3.5.2), using the applicable verification interval and context.

### Table 6: initial-verification MPE structure

Loads `m` are expressed in verification scale intervals `e`; exact upper bounds belong to the preceding band.

| Class | `±0.5e` | `±1.0e` | `±1.5e` |
|---|---|---|---|
| I | `0 ≤ m ≤ 50 000` | `50 000 < m ≤ 200 000` | `200 000 < m` |
| II | `0 ≤ m ≤ 5 000` | `5 000 < m ≤ 20 000` | `20 000 < m ≤ 100 000` |
| III | `0 ≤ m ≤ 500` | `500 < m ≤ 2 000` | `2 000 < m ≤ 10 000` |
| IIII | `0 ≤ m ≤ 50` | `50 < m ≤ 200` | `200 < m ≤ 1 000` |

## R76-2 Report Requirements

| Report section | Data required / calculation / result | Existing model | Missing data or behaviour | Status |
|---|---|---|---|---|
| General information | Application/type/manufacturer, instrument parameters, dates, observer/location, e/d and environmental conditions. | Instrument, TestSession, EnvironmentalConditions | Test equipment and location/report metadata. | Partial |
| Test equipment | Identified standards, equipment and relevant metrological properties. | None | Equipment model, calibration/reference evidence. | Missing |
| Summary of type evaluation | Applicable tests, results and overall summary/remarks. | TestSession verdict, ComplianceResult | Summary aggregation and report fields. | Missing |
| 1 Weighing performance | L, I, ΔL, direction, E, E0, Ec, MPE, remarks, pass/fail. | WeighingPerformanceObservation | E0 identity/sequence and directional/report metadata; persisted result/session workflow. | Partial |
| 2 Temperature no-load | date/time/temp/I/ΔL/P/ΔP/ΔTemp and class-rate verdict. | TemperatureEffectObservation | report-page linkage, class-rate calculation/verdict. | Partial |
| 3.1 Eccentricity using weights | Load locations/sketch, L/I/ΔL/E/E0/Ec/MPE, verdict/remarks. | EccentricityObservation | Sketches, E0 timing, procedure selection, verdict/service. | Partial |
| 3.2 Eccentricity using rolling load | Section, location, travel direction, L/I/ΔL/E/E0/Ec/MPE, verdict. | EccentricityObservation | Explicit rolling procedure and result handling. | Partial |
| 4.1 Digital/analog/non-self discrimination | Indication-type-specific readings; d for digital, MPE/visible movement otherwise; verdict. | DiscriminationObservation | d/indication-type procedure, removed load and acceptance calculation. | Partial |
| 4.2 Sensitivity | L, extra load = |mpe|, permanent displacement and class-dependent threshold. | None | Sensitivity model/service/report data. | Missing |
| 5 Repeatability | 10–20 readings, E, max/min spread, MPE/verdict. | RepeatabilityObservation | load, MPE, corrected/error range/verdict calculation. | Partial |
| 6.1 Zero return | Time-indexed readings and zero-return outcome. | GenericObservation | Typed model, timing/calculation/service. | Missing |
| 6.2 Creep | Load sequence, time/readings and creep outcome. | GenericObservation | Typed model, timing/calculation/service. | Missing |
| 7 Stability of equilibrium | Device state, release/event, time/indication observations and outcome. | GenericObservation | Typed model and procedure. | Missing |
| 8 Tilting | Limiting tilt, direction/position, reference/tilted readings/errors/MPE and outcome. | TiltingObservation | Tilt limit/direction/reference state and verdict logic. | Partial |
| 9 Tare | Tare load/type and weighing readings/errors/MPE/outcome. | ZeroTareDeviceConfig, GenericObservation | Tare observation/calculation and report table. | Missing |
| 10 Warm-up time | Applied power/start, time/readings and compliance result. | GenericObservation | Typed warm-up evidence/service. | Missing |
| 11 Voltage variations | Supply category/reference/lower/upper voltages and corrected-error/MPE results. | Instrument electrical fields, GenericObservation | Voltage-sequence observations/service. | Missing |
| 12 Electrical disturbances | Disturbance parameters, indication and significant-fault/detection response. | GenericObservation | Typed disturbance observations/service. | Missing |
| 13 Damp heat, steady state | Initial/high-humidity/final conditions; weighing rows and results. | GenericObservation | Structured stages/environment/readings/service. | Missing |
| 14 Span stability | Repeated corrected values after named conditions, average error and variation plot/outcome. | GenericObservation | Time series, correction/variation calculation and plot data. | Missing |
| 15 Endurance | Initial/final weighing tables, cycle count/load, wear-and-tear difference/MPE. | GenericObservation | Structured cycle/performance data and evaluator. | Missing |
| 16 Examination of construction | Description, component/instrument information and remarks. | Instrument, ChecklistItem | Dedicated examination record/report field. | Missing |
| 17 Checklist | Requirement, applicability/existence, result and remarks; it summarizes examinations. | ChecklistItem, ChecklistResult | Applicability/evidence/report formatting. | Partial |
