# UC-M9-08 — Get Assessed Taxes (`GetAssessedTaxesUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — Tributos apurados: ICMS, IPI, PIS, COFINS, ISS per period — for accountant review.

## Description

Triggered when a user requests the assessed-tax summary for a period, typically for the accountant to review ahead of SPED submission. The use case pulls authorized fiscal documents from `tax` and sums ICMS, IPI, PIS, COFINS and ISS per period. Read-only.

## Port signature

```java
public interface GetAssessedTaxesUseCase {
    AssessedTaxSummary execute(AssessedTaxesQuery query);
}
```

`AssessedTaxesQuery` fields: `period`. Returns `AssessedTaxSummary` with totals for `icms`, `ipi`, `pis`, `cofins`, `iss`.

## Outbound ports required

- `TaxReadModelPort` — authorized NFe/NFCe/NFSe tax totals for the period

## REST endpoint

`GET /api/reports/assessed-taxes?period=`

## Domain entities touched

- `AssessedTaxSummary`

## Acceptance criteria

- [ ] Returns ICMS, IPI, PIS, COFINS and ISS totals for the requested period.
- [ ] Totals reconcile against `tax`'s authorized fiscal documents for the period.

## Dependencies

- **Depends on:** `tax` read-model port.
- **Blocks:** —
