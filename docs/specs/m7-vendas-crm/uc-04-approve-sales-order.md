# UC-M7-04 — Approve Sales Order (`ApproveSalesOrderUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Pedido de venda status: Draft → Approved. Aprovação: alçada by order value or discount percentage; remote approval via app. Reserva de estoque: automatic on order approval.

## Description

Approves a `DRAFT` order, checking it against the approval alçada (by order value or discount percentage, configured in `system`, M10). On approval the order transitions to `APPROVED` and stock is reserved automatically by calling `inventory`'s `ReserveStockUseCase` for every line item.

## Port signature

```java
public interface ApproveSalesOrderUseCase {
    SalesOrderView execute(ApproveSalesOrderCommand command);
}
```

`ApproveSalesOrderCommand`: `orderId`, `approvedBy`. Returns the updated `SalesOrder` (status `APPROVED`, `approval{alcada, approvedBy}` populated).

## Outbound ports required

- `SalesOrderRepositoryPort`
- `ReserveStockPort` (into `inventory`)

## REST endpoint

`POST /api/sales/orders/{id}/approve`

## Domain entities touched

- `SalesOrder`

## Acceptance criteria

- [ ] Only `DRAFT` orders can be approved.
- [ ] Orders exceeding the configured value/discount alçada require an approver with the elevated profile (per M10's `ApprovalAlcada`); otherwise approval is rejected.
- [ ] Stock is reserved for every line item via `inventory.ReserveStockUseCase` exactly once, atomically with the status change.
- [ ] `approval.approvedBy` and `approval.alcada` are recorded on the order.

## Dependencies

- **Depends on:** [UC-03 Convert Quote to Order](uc-03-convert-quote-to-order.md); M10's `ApprovalAlcada` configuration; M5's `ReserveStockUseCase`.
- **Blocks:** [UC-06 Invoice Sales Order](uc-06-invoice-sales-order.md).

## Notes

- Triggers M5's `ReserveStockUseCase` — see [m5-estoque.md](../m5-estoque.md).
