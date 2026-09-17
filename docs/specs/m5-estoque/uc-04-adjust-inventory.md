# UC-M5-04 — Adjust Inventory (`AdjustInventoryUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Ajuste de inventário: Positivo ou negativo com justificativa obrigatória; gera lançamento contábil automático." (§6.1)

## Description

A user manually corrects a product's stock balance (positive or negative) outside the entry/exit/transfer flows — e.g. breakage, loss, or a standalone correction. Requires a mandatory justification. Appends an `ADJUSTMENT` `StockMovement` and posts an automatic accounting entry via `finance`. This is also the mechanism [UC-M5-09](uc-09-approve-physical-count.md) uses internally to reconcile counted vs. system quantities.

## Port signature

```java
public interface AdjustInventoryUseCase {
    StockMovement execute(AdjustInventoryCommand command);
}
```

`AdjustInventoryCommand`: `productId`, `warehouseId`, `quantityDelta` (positive or negative), `justification` (required), `user`. Returns the created `StockMovement`.

## Outbound ports required

- `StockBalanceRepositoryPort`
- `StockMovementRepositoryPort`
- `PostAdjustmentAccountingEntryPort`

## REST endpoint

`POST /api/inventory/adjustments`

## Domain entities touched

- `StockBalance` (mutated: `onHand`)
- `StockMovement` (created, `type: ADJUSTMENT`)

## Acceptance criteria

- [ ] Rejects the command when `justification` is blank.
- [ ] `StockBalance.onHand` reflects `quantityDelta` exactly; `available` is recalculated accordingly.
- [ ] Every adjustment posts exactly one accounting entry via `PostAdjustmentAccountingEntryPort`.
- [ ] The resulting `StockMovement` is append-only and carries the justification.

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md); `finance`'s accounting-entry intake (`PostAdjustmentAccountingEntryPort`'s implementation) for the automatic posting.
- **Blocks:** [UC-M5-09](uc-09-approve-physical-count.md) (physical-count approval reuses this use case to generate divergence adjustments).
