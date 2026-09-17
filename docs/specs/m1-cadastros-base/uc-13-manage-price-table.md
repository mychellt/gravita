# UC-M1-13 — Manage Price Table (`ManagePriceTableUseCase`)

**Module:** M1 — Cadastros Base ([module spec](../m1-cadastros-base.md))
**Context package:** `br.gravita.masterdata`
**Roadmap phase:** Phase 1 — Fundação (doc §14)

## Functional requirement

§2.5 Tabelas de Preço: unlimited tables, one per customer or sales channel; price formation (fixed / % over cost / % over base); validity window with automatic transition; configurable max-discount limit with block-or-alert behavior.

## Description

A user creates or edits a price table and its entries (per product or product class). Precondition: referenced products exist (UC-11) if entries are added. Postcondition: the table is available for linking to customers (UC-05/UC-06) and for price resolution in `sales` (M7).

## Port signature

```java
public interface ManagePriceTableUseCase {
    PriceTableId execute(UpsertPriceTableCommand command);
}
```

`UpsertPriceTableCommand`: `priceTableId` (optional, for update), `formation: {FIXED, PERCENT_OVER_COST, PERCENT_OVER_BASE}`, `validFrom`, `validTo`, `maxDiscountPercent`, `maxDiscountBehavior: {BLOCK, ALERT}`, `entries: [{productOrClassRef, value}]`.

## Outbound ports required

- `PriceTableRepositoryPort`

## REST endpoint

`POST /api/price-tables` (create), `PATCH /api/price-tables/{id}` (edit).

## Domain entities touched

- `PriceTable`
- `PriceTableEntry`

## Acceptance criteria

- [ ] There is no upper limit on the number of price tables (doc: "unlimited").
- [ ] `PERCENT_OVER_COST` and `PERCENT_OVER_BASE` formations compute from `Product.averageCost`/`basePrice` at resolution time, not at table-save time, so later product-price changes propagate.
- [ ] A table with `validTo` in the past is automatically excluded from active price resolution — no manual deactivation step needed.
- [ ] `maxDiscountBehavior = BLOCK` prevents a `sales` order line from exceeding the limit; `ALERT` allows it with a warning surfaced to the salesperson.

## Dependencies

- **Depends on:** UC-11 (Register Product), if entries reference specific products.
- **Blocks:** UC-05/UC-06 (linking a table to a customer) and `sales` (M7) price resolution.
