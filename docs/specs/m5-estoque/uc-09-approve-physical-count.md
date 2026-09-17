# UC-M5-09 — Approve Physical Count (`ApprovePhysicalCountUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Inventário: Contagem física parcial (por grupo) ou total; ajuste automático após aprovação." (§6.2)

## Description

Once counted quantities have been submitted for a `PhysicalCount` (moving it to `PENDING_APPROVAL`), an authorized user approves it. Approval compares counted vs. snapshotted system quantities per product and generates an `ADJUSTMENT` `StockMovement` for every divergence, via [UC-M5-04](uc-04-adjust-inventory.md). The count moves to `APPROVED`.

## Port signature

```java
public interface ApprovePhysicalCountUseCase {
    PhysicalCount execute(ApprovePhysicalCountCommand command);
}
```

`ApprovePhysicalCountCommand`: `physicalCountId`, `approvedBy`. Returns the `PhysicalCount` in `APPROVED` status, with the list of generated adjustments.

## Outbound ports required

- `PhysicalCountRepositoryPort`
- `AdjustInventoryUseCase` (internal collaborator, not a separate outbound port — reused directly to generate the divergence adjustments)

## REST endpoint

`POST /api/inventory/counts/{id}/approve`

## Domain entities touched

- `PhysicalCount` (mutated: `status: APPROVED`)
- `StockMovement` (created, one `ADJUSTMENT` per divergent product, via UC-M5-04)
- `StockBalance` (mutated, via UC-M5-04)

## Acceptance criteria

- [ ] Only a count in `PENDING_APPROVAL` can be approved; approving an `IN_PROGRESS` or already-`APPROVED` count is rejected.
- [ ] Exactly one adjustment is generated per product where counted quantity differs from the snapshotted system quantity; no adjustment is generated for matching products.
- [ ] Each generated adjustment's justification references the physical count it came from.
- [ ] Approval is atomic: either all divergence adjustments are posted and the count reaches `APPROVED`, or none are and the count stays `PENDING_APPROVAL`.

## Dependencies

- **Depends on:** [UC-M5-08](uc-08-start-physical-count.md), [UC-M5-04](uc-04-adjust-inventory.md).
- **Blocks:** None outside M5.
