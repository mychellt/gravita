# UC-M5-08 — Start Physical Count (`StartPhysicalCountUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Inventário: Contagem física parcial (por grupo) ou total; ajuste automático após aprovação." (§6.2)

## Description

A user opens a physical count, either scoped to a product group (partial) or the whole warehouse (total). Creates a `PhysicalCount` in `IN_PROGRESS` status, snapshotting the system's current quantities for the scoped products so they can later be compared against what's physically counted.

## Port signature

```java
public interface StartPhysicalCountUseCase {
    PhysicalCount execute(StartPhysicalCountCommand command);
}
```

`StartPhysicalCountCommand`: `scope: {PARTIAL_BY_GROUP, TOTAL}`, `productGroupId` (required when scope is `PARTIAL_BY_GROUP`), `warehouseId`, `user`. Returns the created `PhysicalCount`, including the snapshotted system quantities per product.

## Outbound ports required

- `PhysicalCountRepositoryPort`
- `StockBalanceRepositoryPort` (to snapshot current quantities)

## REST endpoint

`POST /api/inventory/counts`

## Domain entities touched

- `PhysicalCount` (created, `status: IN_PROGRESS`)
- `StockBalance` (read-only, snapshotted)

## Acceptance criteria

- [ ] `PARTIAL_BY_GROUP` scope requires a `productGroupId` and only snapshots that group's products.
- [ ] `TOTAL` scope snapshots every product in the warehouse.
- [ ] The snapshot captures system quantities at count-start time, so later stock movements during counting don't retroactively change what's being compared.
- [ ] A count starts in `IN_PROGRESS` and cannot be approved directly — it must first reach `PENDING_APPROVAL` (counted quantities submitted).

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md).
- **Blocks:** [UC-M5-08b](uc-08b-submit-physical-count.md) (a count must be started before counts can be submitted against it).
