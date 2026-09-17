# UC-M5-03 — Register Stock Exit (`RegisterStockExitUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Tipos de movimento: Saída (venda, devolução a fornecedor)... Estoque negativo: Bloqueio configurável — permitir ou bloquear saída sem saldo disponível." (§6.1, §6.2)

## Description

Triggered by a confirmed NFe/NFCe sale (M2/M3) or a supplier return. Decreases `StockBalance.onHand` for the product/warehouse and appends an immutable `EXIT` `StockMovement`. Consumes reserved quantity first when the exit fulfills a reservation (see [UC-M5-06](uc-06-reserve-stock.md)). Must never allocate a lot past its `expiryDate`, and must respect the per-product/warehouse negative-stock block setting.

## Port signature

```java
public interface RegisterStockExitUseCase {
    StockMovement execute(RegisterStockExitCommand command);
}
```

`RegisterStockExitCommand`: `productId`, `warehouseId`, `quantity`, `lot`/`serial` (optional, for traceable products), `originReference` (sale/return document ref), `user`. Returns the created `StockMovement`.

## Outbound ports required

- `StockBalanceRepositoryPort`
- `StockMovementRepositoryPort`
- `LotRepositoryPort`
- `SerialUnitRepositoryPort`

## REST endpoint

`POST /api/inventory/movements/exit`

## Domain entities touched

- `StockBalance` (mutated: `onHand`, and `reserved` if fulfilling a reservation)
- `StockMovement` (created)
- `Lot` / `SerialUnit` (quantity/status updated)

## Acceptance criteria

- [ ] Rejects allocation from a `Lot` whose `expiryDate` has passed, per the `Lot` invariant.
- [ ] Blocks the exit when `available < quantity` and the product/warehouse has negative stock disallowed; allows it (going negative) when explicitly permitted.
- [ ] The resulting `StockMovement` is append-only.
- [ ] When the exit fulfills a `StockReservation`, the reservation is consumed rather than left open.

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md).
- **Blocks:** M2/M3's confirmed-sale flow, M6's supplier-return flow — both require this use case before they can finalize a sale/return.
