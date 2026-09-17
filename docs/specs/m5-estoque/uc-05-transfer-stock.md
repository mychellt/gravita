# UC-M5-05 — Transfer Stock (`TransferStockUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Transferência: Entre depósitos ou filiais; gera estoque em trânsito até confirmação no destino." (§6.1)

## Description

Moves stock between warehouses or branches in two steps: initiating the transfer decreases the source warehouse's `onHand` and increases its `inTransit` counterpart at the destination; confirming the transfer at the destination moves the quantity into the destination's `onHand`. Both steps append an immutable `TRANSFER` `StockMovement`.

## Port signature

```java
public interface TransferStockUseCase {
    StockMovement initiate(InitiateTransferCommand command);
    StockMovement confirm(ConfirmTransferCommand command);
}
```

`InitiateTransferCommand`: `productId`, `sourceWarehouseId`, `destinationWarehouseId`, `quantity`, `lot`/`serial` (optional), `user`. `ConfirmTransferCommand`: `transferMovementId`, `user`. Each returns the corresponding `StockMovement`.

## Outbound ports required

- `StockBalanceRepositoryPort`
- `StockMovementRepositoryPort`
- `LotRepositoryPort` / `SerialUnitRepositoryPort` (when transferring traceable stock)

## REST endpoint

`POST /api/inventory/transfers` (initiate) / `POST /api/inventory/transfers/{id}/confirm` (confirm)

## Domain entities touched

- `StockBalance` (source: `onHand` decreases, `inTransit` reflects the pending leg; destination: `onHand` increases on confirmation)
- `StockMovement` (created, `type: TRANSFER`, one per step)

## Acceptance criteria

- [ ] Initiating a transfer never exceeds the source's `available` quantity (respects the same negative-stock rule as an exit).
- [ ] Stock in transit is visible as `inTransit`, not yet part of the destination's `available`.
- [ ] Confirming a transfer that was never initiated, or confirming it twice, is rejected.
- [ ] Both the initiate and confirm steps produce an append-only `StockMovement`.

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md).
- **Blocks:** None outside M5 — this is a self-contained inventory operation, not triggered by another module.
