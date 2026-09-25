# UC-M5-08b — Submit Physical Count (`SubmitPhysicalCountUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Inventário: Contagem física parcial (por grupo) ou total; ajuste automático após aprovação." (§6.2)

## Description

Fills the gap between [UC-M5-08](uc-08-start-physical-count.md) (opens a count, `IN_PROGRESS`) and [UC-M5-09](uc-09-approve-physical-count.md) (approves a count that's already `PENDING_APPROVAL` with counted quantities on its lines). Without it, a `PhysicalCount` can never actually reach `PENDING_APPROVAL` outside of tests or direct DB seeding — this was flagged as a missed use case while implementing UC-M5-09 (GRA-79) and tracked as GRA-87.

A user records the physically counted quantity per product for an `IN_PROGRESS` count. Submitting is cumulative — counting can be split across several calls (e.g. one per warehouse aisle) without losing counts already recorded. Once every line in the count has a counted quantity, the count transitions to `PENDING_APPROVAL` and becomes eligible for UC-M5-09; until then it stays `IN_PROGRESS`.

## Port signature

```java
public interface SubmitPhysicalCountUseCase {
    PhysicalCount execute(SubmitPhysicalCountCommand command);
}
```

`SubmitPhysicalCountCommand`: `physicalCountId`, `countedQuantities: Map<productId, BigDecimal>`, `submittedBy`. Returns the `PhysicalCount` with the submitted lines updated, in `PENDING_APPROVAL` if that completes every line, otherwise still `IN_PROGRESS`.

## Outbound ports required

- `PhysicalCountRepositoryPort`

## REST endpoint

`POST /api/inventory/counts/{id}/submit`

## Domain entities touched

- `PhysicalCount` (mutated: `PhysicalCountLine.countedQuantity` set per submitted product; `status` becomes `PENDING_APPROVAL` once complete)

## Acceptance criteria

- [ ] Only a count in `IN_PROGRESS` accepts submitted counts; submitting against `PENDING_APPROVAL` or `APPROVED` is rejected.
- [ ] A submitted product must already be one of the count's lines (i.e. was in scope when the count was started); submitting a count for a product outside the count is rejected.
- [ ] A submitted counted quantity must not be negative.
- [ ] Submitting is cumulative: a line counted in an earlier call keeps its counted quantity if a later call doesn't mention that product.
- [ ] The count moves to `PENDING_APPROVAL` only once every line has a counted quantity; a partial submission leaves it `IN_PROGRESS`.

## Dependencies

- **Depends on:** [UC-M5-08](uc-08-start-physical-count.md).
- **Blocks:** [UC-M5-09](uc-09-approve-physical-count.md) (a count must be fully submitted, reaching `PENDING_APPROVAL`, before it can be approved).
