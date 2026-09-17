# UC-M7-08 — Calculate Commission (`CalculateCommissionUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Comissão: calculated per salesperson and per product; monthly commission report.

## Description

Computes `Commission` entries from invoiced orders for a given salesperson and period, applying the configured rate per salesperson/product pair (owned by `sales`, per the module spec's Notes). Feeds M9's `GetCommissionReportUseCase` for the payroll-ready monthly report.

## Port signature

```java
public interface CalculateCommissionUseCase {
    List<CommissionView> execute(CalculateCommissionQuery query);
}
```

`CalculateCommissionQuery`: `salesperson` (optional), `period`. Returns `Commission` entries (salesperson, product, order, rate, amount).

## Outbound ports required

- `SalesOrderRepositoryPort` (reads invoiced orders for the period)
- `CommissionRepositoryPort` (persists the computed `Commission` entries)

## REST endpoint

`GET /api/sales/commissions?salesperson=&period=`

## Domain entities touched

- `Commission`
- `SalesOrder` (read-only)

## Acceptance criteria

- [ ] Only `INVOICED` orders within the queried period are considered.
- [ ] Commission is computed per (salesperson, product) pair using the configured rate.
- [ ] Results are aggregable to a monthly total per salesperson.
- [ ] Omitting `salesperson` returns commissions for all salespeople in the period.

## Dependencies

- **Depends on:** [UC-06 Invoice Sales Order](uc-06-invoice-sales-order.md).
- **Blocks:** M9's `GetCommissionReportUseCase` (reporting projection).
