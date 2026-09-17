# UC-M9-04 — Get Stock Turnover (`GetStockTurnoverUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — Giro de estoque: turnover per product in the period; identification of stalled items.

## Description

Triggered when a user requests the stock turnover report for a period. The use case pulls movement and balance data from `inventory`, computes a turnover rate per product, and flags items with no meaningful movement in the period as stalled. Read-only.

## Port signature

```java
public interface GetStockTurnoverUseCase {
    List<StockTurnoverEntry> execute(StockTurnoverQuery query);
}
```

`StockTurnoverQuery` fields: `period`. Returns a list of `StockTurnoverEntry`, each with `product`, `turnoverRate`, `stalledFlag`.

## Outbound ports required

- `InventoryReadModelPort` — stock movements and balances for the period

## REST endpoint

`GET /api/reports/stock-turnover?period=`

## Domain entities touched

- `StockTurnoverEntry`

## Acceptance criteria

- [ ] Returns a turnover rate per product for the requested period.
- [ ] Flags stalled items (no meaningful movement in the period).

## Dependencies

- **Depends on:** `inventory` read-model port.
- **Blocks:** —
