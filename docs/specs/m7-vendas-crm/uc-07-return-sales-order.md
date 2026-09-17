# UC-M7-07 — Return Sales Order (`ReturnSalesOrderUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Devolução de venda: a return NF-e is issued in the same flow; stock and financial positions are reverted.

## Description

Registers a full or partial return against an `INVOICED` order. In the same flow, a return NF-e is issued through `tax` (M2), the returned quantities are reverted in `inventory` (M5), and the corresponding financial position is reverted in `finance` (M8) — e.g. the receivable is reduced or cancelled.

## Port signature

```java
public interface ReturnSalesOrderUseCase {
    SalesReturnView execute(ReturnSalesOrderCommand command);
}
```

`ReturnSalesOrderCommand`: `orderId`, `items: [{orderItemId, quantity}]`. Returns the created `SalesReturn` (items, `returnNfeRef`).

## Outbound ports required

- `SalesOrderRepositoryPort`
- `IssueFiscalDocumentPort` (into `tax`, for the return NF-e)
- `RegisterStockEntryPort` (into `inventory`, customer-return entry)
- `AdjustReceivableForReturnPort` (into `finance`)

## REST endpoint

`POST /api/sales/orders/{id}/return`

## Domain entities touched

- `SalesOrder`
- `SalesReturn`

## Acceptance criteria

- [ ] Only `INVOICED` orders can be returned, and only up to the originally invoiced quantities per item.
- [ ] A return NF-e is issued and referenced on the `SalesReturn` (`returnNfeRef`).
- [ ] Stock position is reverted for the returned quantities.
- [ ] The order's associated receivable is adjusted or cancelled to reflect the returned amount.
- [ ] A partial return leaves the order queryable for further (non-overlapping) partial returns.

## Dependencies

- **Depends on:** [UC-06 Invoice Sales Order](uc-06-invoice-sales-order.md), M5's `RegisterStockEntryUseCase` ([m5-estoque/uc-02](../m5-estoque/uc-02-register-stock-entry.md)), M8's `AdjustReceivableForReturnUseCase` ([m8-financeiro/uc-21](../m8-financeiro/uc-21-adjust-receivable-for-return.md)).
- **Blocks:** —

## Notes

- Triggers M2's return NF-e issuance, M5's stock re-entry (via `RegisterStockEntryPort`, the same customer-return path M5 already documents) and M8's receivable adjustment (via `AdjustReceivableForReturnPort`) — see [m2-fiscal-nfe.md](../m2-fiscal-nfe.md), [m5-estoque.md](../m5-estoque.md), [m8-financeiro.md](../m8-financeiro.md).
