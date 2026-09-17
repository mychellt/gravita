# UC-M7-14 — Get Target Progress (`GetTargetProgressUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.2 — Metas de vendedor: ... with a tracking panel.

## Description

Reports a salesperson's progress toward their `(salesperson, month)` target, computed from invoiced orders in that month: revenue achieved, order count achieved, and percentage complete against each target.

## Port signature

```java
public interface GetTargetProgressUseCase {
    TargetProgressView execute(GetTargetProgressQuery query);
}
```

`GetTargetProgressQuery`: `salesperson`, `month`. Returns `TargetProgressView{valueAchieved, orderCountAchieved, valueTarget, orderCountTarget, percentComplete}`.

## Outbound ports required

- `SalespersonTargetRepositoryPort`
- `SalesOrderRepositoryPort`

## REST endpoint

`GET /api/crm/targets/{salesperson}/{month}`

## Domain entities touched

- `SalespersonTarget`
- `SalesOrder` (read-only, `INVOICED` orders for the month)

## Acceptance criteria

- [ ] `valueAchieved`/`orderCountAchieved` are computed only from `INVOICED` orders within the requested month.
- [ ] `percentComplete` is computed against both value and order-count targets.
- [ ] Querying a `(salesperson, month)` with no target set returns a clear "no target configured" result rather than a division-by-zero error.

## Dependencies

- **Depends on:** [UC-13 Set Salesperson Target](uc-13-set-salesperson-target.md), [UC-06 Invoice Sales Order](uc-06-invoice-sales-order.md).
- **Blocks:** —
