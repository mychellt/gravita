# UC-M5-11 — Suggest Reorder (`SuggestReorderUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Ponto de reposição: Sugestão automática de pedido de compra quando saldo atinge o ponto." (§6.2)

## Description

Evaluated whenever a `StockBalance` changes (or on a scheduled sweep): compares `available` against the product's configured reorder point (from `masterdata`). When the balance hits or drops below the reorder point, emits a reorder suggestion that feeds M6's `CreatePurchaseRequestUseCase` with `origin: MIN_STOCK_TRIGGER`.

## Port signature

```java
public interface SuggestReorderUseCase {
    List<ReorderSuggestion> execute(SuggestReorderQuery query);
}
```

`SuggestReorderQuery`: `warehouseId` (optional filter), or none for a full sweep. Returns a list of `ReorderSuggestion` (product, warehouse, current available, reorder point, suggested quantity).

## Outbound ports required

- `StockBalanceRepositoryPort`
- (reads reorder-point configuration from `masterdata`, via that context's product query — not a persistence port owned by M5)

## REST endpoint

Not directly exposed as its own endpoint in the module spec's adapter table; invoked internally and consumed by M6. Exposed for inspection via `GET /api/inventory/alerts/low-stock` (shared with the min-stock dashboard alert).

## Domain entities touched

- `StockBalance` (read-only)

## Acceptance criteria

- [ ] A suggestion is produced exactly when `available <= reorderPoint` for a product/warehouse.
- [ ] Suggested quantity brings the balance back to at least the configured maximum (per the product's stock parameters), not just to the reorder point.
- [ ] The same low-stock condition also alerts the M9 dashboard (doc §6.2 "Estoque mínimo") — this use case and the min-stock alert share the same underlying comparison, not two divergent thresholds.

## Dependencies

- **Depends on:** [UC-M5-01](uc-01-get-stock-balance.md); `masterdata`'s product stock parameters (min/max/reorder point).
- **Blocks:** M6's `CreatePurchaseRequestUseCase` (`origin: MIN_STOCK_TRIGGER`) — purchasing's automatic request creation needs this use case first.
