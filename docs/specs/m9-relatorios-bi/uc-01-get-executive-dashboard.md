# UC-M9-01 — Get Executive Dashboard (`GetExecutiveDashboardUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.1 — Dashboard Executivo: faturamento (day/week/month totals + comparison to prior period, line chart), CMV e margem, inadimplência (open-overdue total + simplified aging: ≤30 / 31–60 / +60 days), estoque crítico (clickable list of products below minimum or near expiry), top-10 produtos by quantity and by value, and metas de vendas (progress bar per salesperson and for the company, current month).

## Description

Triggered when an authorized user opens the dashboard screen. The use case composes six independent read models from other contexts into a single `ExecutiveDashboardView` for the requested period. It has no mutable state — it's a fan-out query over `sales`, `inventory`, `finance` and `tax` read ports, aggregated and returned in one call. Postcondition: the view is returned (or served from a cache/materialized view) within the module's 3-second load budget.

## Port signature

```java
public interface GetExecutiveDashboardUseCase {
    ExecutiveDashboardView execute(DashboardQuery query);
}
```

`DashboardQuery` fields: `period` (day/week/month), `companyId`. Returns `ExecutiveDashboardView` composing: revenue (current + prior-period comparison), CMV/margin, delinquency total + aging buckets, critical-stock list, top-10 products (by qty and by value), and salesperson/company target progress.

## Outbound ports required

- `SalesReadModelPort` — revenue, top products, target progress
- `InventoryReadModelPort` — CMV/margin, critical stock
- `FinanceReadModelPort` — delinquency/aging
- `TaxReadModelPort` — revenue reconciliation (invoiced totals)
- `PermissionCheckPort` — dashboard visibility per profile

## REST endpoint

`GET /api/reports/dashboard`

## Domain entities touched

- `ExecutiveDashboardView`

## Acceptance criteria

- [ ] Returns faturamento for day/week/month with a comparison to the prior period.
- [ ] Returns CMV and gross margin % for the selected period.
- [ ] Returns total open-overdue plus the ≤30/31–60/+60 aging buckets.
- [ ] Returns a clickable list of products below minimum stock or near expiry.
- [ ] Returns the top-10 products by quantity and by value for the period.
- [ ] Returns target progress per salesperson and for the company for the current month.
- [ ] End-to-end response time stays under 3 seconds under realistic data volume (doc §10 performance budget).

## Dependencies

- **Depends on:** `sales`, `inventory`, `finance`, `tax` read-model ports; `system`'s `PermissionCheckPort`.
- **Blocks:** —
