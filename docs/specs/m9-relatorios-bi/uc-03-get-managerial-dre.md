# UC-M9-03 — Get Managerial DRE (`GetManagerialDreUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — DRE Gerencial: gross revenue, deductions, CMV, expenses by cost center, net result.

## Description

Triggered when a user requests the managerial DRE for a period, optionally filtered by cost center. The use case composes revenue and deductions from `sales`/`tax`, CMV from `inventory`, and expenses by cost center from `finance`, then computes the net result. Read-only.

## Port signature

```java
public interface GetManagerialDreUseCase {
    ManagerialDre execute(DreQuery query);
}
```

`DreQuery` fields: `period`, `costCenter` (optional filter). Returns `ManagerialDre` with `grossRevenue`, `deductions`, `cmv`, `expensesByCostCenter`, `netResult`.

## Outbound ports required

- `SalesReadModelPort` / `TaxReadModelPort` — gross revenue and deductions
- `InventoryReadModelPort` — CMV
- `FinanceReadModelPort` — expenses by cost center

## REST endpoint

`GET /api/reports/dre?period=&costCenter=`

## Domain entities touched

- `ManagerialDre`

## Acceptance criteria

- [ ] Returns gross revenue, deductions, CMV, expenses by cost center and net result for the requested period.
- [ ] Supports an optional cost-center filter.
- [ ] Net result reconciles as `grossRevenue - deductions - cmv - expenses`.

## Dependencies

- **Depends on:** `sales`, `tax`, `inventory`, `finance` read-model ports.
- **Blocks:** —
