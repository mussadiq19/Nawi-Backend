# TrueScale — OIML R76 NAWI Test Report Automation

## Project
Spring Boot 3.x backend automating OIML R76 type-evaluation test reports
for Non-Automatic Weighing Instruments (NAWI). Java 21, PostgreSQL, Flyway,
package: com.example.nawibackend

## Architecture
- Package-by-feature for services/controllers/repositories
  (instrument/, observation/, compliance/, report/, auth/)
- All @Entity, @Embeddable, and enum classes live in one shared
  common.models package tree (model/, model/enums/, model/embadable/)
  — NOT split per-feature, since entities are heavily interlinked
- Compliance tolerance data (mpe formulas, load bands) is DB-driven via
  ToleranceRule entity, NOT hardcoded — this supports future OIML revisions

## Conventions
- Lombok on all entities: @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
- Constructor injection via @RequiredArgsConstructor, never field injection
- Every @ManyToOne needs explicit @JoinColumn(name = "...")
- Every @Enumerated(EnumType.STRING) needs @Column(length = N) sized to the enum
- BigDecimal for all measurement values, never float/double

## Core calculation formulas (OIML R76-2)
- E = I + ½e – ΔL – L
- Ec = E – E0 (E0 = error at zero-load baseline)
- Pass condition: |Ec| ≤ |mpe|
- mpe resolved via ToleranceRule: accuracy class + test type + load band (in units of e)

## Build/test commands
[fill in once you have them — mvn spring-boot:run, mvn test, etc.]

## Do NOT
- Do not build auth yet — explicitly deferred, compliance engine first
- Do not hardcode mpe values in Java — always via ToleranceRule lookup