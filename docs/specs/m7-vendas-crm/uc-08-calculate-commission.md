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
- `CommissionRateRepositoryPort` (reads the configured rate for a salesperson/product pair — added; see Notes)
- `CommissionRepositoryPort` (persists the computed `Commission` entries)

## REST endpoint

`GET /api/sales/commissions?salesperson=&period=`

## Domain entities touched

- `Commission`
- `CommissionRate` (read-only; added, see Notes)
- `SalesOrder` (read-only)

## Acceptance criteria

- [x] Only `INVOICED` orders within the queried period are considered.
- [x] Commission is computed per (salesperson, product) pair using the configured rate.
- [x] Results are aggregable to a monthly total per salesperson.
- [x] Omitting `salesperson` returns commissions for all salespeople in the period.

## Dependencies

- **Depends on:** [UC-06 Invoice Sales Order](uc-06-invoice-sales-order.md).
- **Blocks:** M9's `GetCommissionReportUseCase` (reporting projection).

## Notes

Two gaps surfaced implementing this ticket, neither covered by the earlier use cases it depends on:

- **No salesperson on `Quote`/`SalesOrder`.** Nothing upstream of this ticket ever recorded who sold an order. Resolved by adding a required `salespersonId` to `Quote` (set at creation, `CreateQuoteCommand`) and to `SalesOrder` (carried over on `ConvertQuoteToOrderUseCase`), plus a migration backfilling existing rows. This touches UC-01 (`CreateQuoteUseCase`) and UC-03 (`ConvertQuoteToOrderUseCase`).
- **No period on `SalesOrder`.** Filtering "invoiced orders for the period" needs a date the order was invoiced on. Resolved by adding `invoicedAt`, set by `SalesOrder.invoice()` alongside the `INVOICED` transition, mirroring how `approve()` already records `approvedBy`/`alcadaId`.
- **No rate configuration entity/port.** The description assumes a "configured rate per salesperson/product pair" but no such entity or port existed and none was listed as required. Resolved by adding a minimal `CommissionRate` domain concept and a read-only `CommissionRateRepositoryPort`; there is no management use case yet; rows are seeded directly. A pair with no configured rate makes the calculation fail with `CommissionRateNotFoundException` (404) rather than silently producing a zero-value commission.
- `period` is modeled as a `YearMonth` (matching "monthly commission report"). The use case computes and persists a `Commission` per invoiced line item on every call; it is not idempotent — recalculating the same period creates additional rows rather than replacing prior ones, since no such requirement was stated.
