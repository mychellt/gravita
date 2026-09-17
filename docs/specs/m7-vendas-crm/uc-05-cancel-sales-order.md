# UC-M7-05 — Cancel Sales Order (`CancelSalesOrderUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Pedido de venda status: ... → Cancelled. Reserva de estoque: ... released on cancellation.

## Description

Cancels an order that hasn't been invoiced yet. If the order was previously `APPROVED` or `IN_SEPARATION`, its stock reservation is released via `inventory`'s `ReleaseStockReservationUseCase`. Invoiced orders cannot be cancelled through this use case — they go through the return flow instead ([UC-07](uc-07-return-sales-order.md)).

## Port signature

```java
public interface CancelSalesOrderUseCase {
    void execute(CancelSalesOrderCommand command);
}
```

`CancelSalesOrderCommand`: `orderId`, `reason`.

## Outbound ports required

- `SalesOrderRepositoryPort`
- `ReleaseStockReservationPort` (into `inventory`)

## REST endpoint

`POST /api/sales/orders/{id}/cancel`

## Domain entities touched

- `SalesOrder`

## Acceptance criteria

- [ ] Order status transitions to `CANCELLED` from `DRAFT`, `APPROVED` or `IN_SEPARATION`.
- [ ] If the order was `APPROVED` or `IN_SEPARATION`, its stock reservation is released.
- [ ] Cancelling an `INVOICED` order is rejected with a message pointing to the return flow.
- [ ] `reason` is stored for audit.

## Dependencies

- **Depends on:** [UC-03 Convert Quote to Order](uc-03-convert-quote-to-order.md).
- **Blocks:** —

## Notes

- Triggers M5's `ReleaseStockReservationUseCase` — see [m5-estoque.md](../m5-estoque.md).
