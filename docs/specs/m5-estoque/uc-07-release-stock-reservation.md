# UC-M5-07 — Release Stock Reservation (`ReleaseStockReservationUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Reserva: Quantidade reservada para pedidos confirmados; não disponível para nova venda." (§6.1) — the release side of the same rule.

## Description

Triggered by M7's `CancelSalesOrderUseCase` when an approved order is cancelled before fulfillment. Releases the previously reserved quantity back into the product's `available` balance by decreasing `StockBalance.reserved`, without touching `onHand`.

## Port signature

```java
public interface ReleaseStockReservationUseCase {
    void execute(ReleaseStockReservationCommand command);
}
```

`ReleaseStockReservationCommand`: `reservationId` (or `orderRef`). No return value beyond success/failure.

## Outbound ports required

- `StockBalanceRepositoryPort`

## REST endpoint

`DELETE /api/inventory/reservations/{id}`

## Domain entities touched

- `StockBalance` (mutated: `reserved` decreases, `available` recalculated)
- `StockReservation` (removed/closed)

## Acceptance criteria

- [ ] Releasing a reservation restores exactly the reserved quantity to `available`.
- [ ] `onHand` is unchanged.
- [ ] Releasing an already-released or already-fulfilled reservation is rejected, not silently ignored.

## Dependencies

- **Depends on:** [UC-M5-06](uc-06-reserve-stock.md) (a reservation must exist to release).
- **Blocks:** M7's `CancelSalesOrderUseCase` — order cancellation cannot complete without releasing the associated reservation.
