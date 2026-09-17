# UC-M5-01 — Get Stock Balance (`GetStockBalanceUseCase`)

**Module:** M5 — Estoque ([module spec](../m5-estoque.md))
**Context package:** `br.gravita.inventory`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

"Controle de estoque em tempo real... O usuário vê o saldo atual de qualquer produto em qualquer tela sem precisar abrir o módulo de estoque." (§6, module intro)

## Description

Any screen in the system (product registration, PDV, sales order, purchasing) can query the current balance of a product without opening the inventory module. Given a product and optionally a warehouse/branch, returns on-hand, reserved, in-transit and available quantities. Read-only — this is the query other modules embed directly in their own UIs.

## Port signature

```java
public interface GetStockBalanceUseCase {
    StockBalanceView execute(GetStockBalanceQuery query);
}
```

`GetStockBalanceQuery`: `productId`, `warehouseId` (optional — omit for an aggregate across all warehouses). Returns a `StockBalanceView` projecting `StockBalance`'s `onHand`, `reserved`, `inTransit`, `available`, `averageCost`.

## Outbound ports required

- `StockBalanceRepositoryPort`

## REST endpoint

`GET /api/inventory/products/{id}/balance`

## Domain entities touched

- `StockBalance` (read-only)

## Acceptance criteria

- [ ] Returns `available = onHand - reserved` consistent with the `StockBalance` invariant.
- [ ] Omitting `warehouseId` aggregates balances across all warehouses/branches for the product.
- [ ] Response time supports embedding in other modules' screens without a noticeable delay (no dedicated caching mandated by the source doc, but the query must stay cheap enough for that use).
- [ ] Unknown `productId` returns a not-found result rather than a zeroed balance.

## Dependencies

- **Depends on:** None (first use case to implement; every other M5 use case writes data this one reads).
- **Blocks:** Every module that displays stock balance on its own screens (`masterdata` product screen, `sales`, `purchasing`, `reporting` dashboard).
