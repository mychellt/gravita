# UC-M5-06 — Reserve Stock (`ReserveStockUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Reserva: Quantidade reservada para pedidos confirmados; não disponível para nova venda." (§6.1)

## Description

Triggered by M7's `ApproveSalesOrderUseCase` when a sales order moves to `APPROVED`. Reserves the ordered quantity against the product's `StockBalance`, increasing `reserved` and reducing `available` (without changing `onHand`), so the same stock can't be sold twice.

## Port signature

```java
public interface ReserveStockUseCase {
    StockReservation execute(ReserveStockCommand command);
}
```

`ReserveStockCommand`: `orderRef` (sales order id), `productId`, `warehouseId`, `quantity`. Returns the created `StockReservation`.

## Outbound ports required

- `StockBalanceRepositoryPort`

## REST endpoint

`POST /api/inventory/reservations`

## Domain entities touched

- `StockBalance` (mutated: `reserved`, derived `available`)
- `StockReservation` (created)

## Acceptance criteria

- [ ] Reservation fails when `available < quantity` (subject to the same negative-stock configuration as an exit).
- [ ] `onHand` is unchanged by a reservation; only `reserved`/`available` move.
- [ ] Each reservation is traceable back to its `orderRef`.
- [ ] A reservation is later either consumed by [UC-M5-03](uc-03-register-stock-exit.md) (fulfillment) or released by [UC-M5-07](uc-07-release-stock-reservation.md) (cancellation) — it never sits both reserved and stale indefinitely without one of these.

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md).
- **Blocks:** M7's `ApproveSalesOrderUseCase` — order approval cannot complete without a successful reservation.
