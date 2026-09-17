# UC-M9-02 — Get ABC Curve (`GetAbcCurveUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — Curva ABC: products and customers classified by revenue representativeness (A/B/C).

## Description

Triggered when a user requests the ABC curve report for products or customers over a period. The use case pulls revenue-by-entity from `sales`/`tax`, ranks it, and classifies each entry into class A, B or C by cumulative revenue share. Read-only; no state is mutated.

## Port signature

```java
public interface GetAbcCurveUseCase {
    List<AbcCurveEntry> execute(AbcCurveQuery query);
}
```

`AbcCurveQuery` fields: `type` (`PRODUCT` or `CUSTOMER`), `period`. Returns a list of `AbcCurveEntry`, each with the entity reference, `revenueShare`, and `class` (A/B/C).

## Outbound ports required

- `SalesReadModelPort` — revenue per product/customer for the period
- `TaxReadModelPort` — invoiced-amount reconciliation

## REST endpoint

`GET /api/reports/abc-curve?type=product|customer&period=`

## Domain entities touched

- `AbcCurveEntry`

## Acceptance criteria

- [ ] Supports classification by product and by customer via the `type` filter.
- [ ] Each entry carries its computed `revenueShare` and resulting A/B/C class.
- [ ] Classification is scoped to the requested period.
- [ ] Results are exportable via UC-M9-09 (`ExportReportUseCase`).

## Dependencies

- **Depends on:** `sales`, `tax` read-model ports.
- **Blocks:** —
