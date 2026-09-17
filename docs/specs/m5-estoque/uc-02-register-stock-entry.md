# UC-M5-02 — Register Stock Entry (`RegisterStockEntryUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Tipos de movimento: Entrada (compra, devolução de cliente)... Custo médio (CMV): Atualizado a cada entrada... Rastreabilidade: Lote com data de validade e número de série." (§6.1)

## Description

Triggered by M2's `ConfirmInboundNfeReceiptUseCase` (supplier NF-e receipt) and M6's `ConfirmPurchaseReceiptUseCase` (purchase order receipt), or by a customer-return entry. Increases `StockBalance.onHand` for the product/warehouse, recalculates `averageCost` (CMV), and appends an immutable `ENTRY` `StockMovement`. If the product has lot or serial control active, records the `Lot` (with expiry) or `SerialUnit`.

## Port signature

```java
public interface RegisterStockEntryUseCase {
    StockMovement execute(RegisterStockEntryCommand command);
}
```

`RegisterStockEntryCommand`: `productId`, `warehouseId`, `quantity`, `unitCost`, `lot` (optional: code + expiryDate), `serials` (optional list), `originReference` (purchase/return document ref), `user`. Returns the created `StockMovement`.

## Outbound ports required

- `StockBalanceRepositoryPort`
- `StockMovementRepositoryPort`
- `LotRepositoryPort`
- `SerialUnitRepositoryPort`

## REST endpoint

`POST /api/inventory/movements/entry`

## Domain entities touched

- `StockBalance` (mutated: `onHand`, `averageCost`)
- `StockMovement` (created)
- `Lot` / `SerialUnit` (created, if applicable)

## Acceptance criteria

- [ ] `averageCost` recalculates as a weighted average including the new entry's cost and quantity.
- [ ] The resulting `StockMovement` is append-only — no update/delete path exists for it.
- [ ] When the product has lot control active, `expiryDate` is required and a `Lot` record is created or incremented.
- [ ] When the product has serial control active, each unit is recorded individually rather than as a quantity.
- [ ] Rejects a negative or zero `quantity`.

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md) (balance must exist or be creatable for the product/warehouse pair).
- **Blocks:** M2's confirmed inbound receipt flow, M6's `ConfirmPurchaseReceiptUseCase` — both require this use case to exist before their "confirm receipt" step can update stock.
