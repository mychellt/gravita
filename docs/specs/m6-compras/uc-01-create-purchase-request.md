# UC-M6-01 — Create Purchase Request (`CreatePurchaseRequestUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Solicitação de compra: created by a user, by the min-stock trigger, or by demand from a sales order.

## Description

A `PurchaseRequest` is opened by one of three origins: a user manually listing items, an automatic trigger from `inventory` when a product's balance hits its reorder point, or demand raised by an approved `sales` order. On creation the request holds a list of `{product, quantity}` pairs and starts in status `OPEN`, ready to be sent for quotation.

## Port signature

```java
public interface CreatePurchaseRequestUseCase {
    PurchaseRequestId execute(CreatePurchaseRequestCommand command);
}
```

`CreatePurchaseRequestCommand`: `origin: {USER, MIN_STOCK_TRIGGER, SALES_ORDER_DEMAND}`, `items: [{product, quantity}]`, `requestedBy` (null when system-triggered). Returns the created `PurchaseRequest`'s id.

## Outbound ports required

- `PurchaseRequestRepositoryPort`

## REST endpoint

`POST /api/purchasing/requests`

## Domain entities touched

- `PurchaseRequest` (created)

## Acceptance criteria

- [ ] A request can be created manually by a user with an arbitrary item list.
- [ ] A request can be created automatically from `inventory`'s `SuggestReorderUseCase` with `origin = MIN_STOCK_TRIGGER`.
- [ ] A request can be created from a sales-order demand with `origin = SALES_ORDER_DEMAND`.
- [ ] A newly created request always starts in status `OPEN`.
- [ ] `items` cannot be empty.

## Dependencies

- **Depends on:** None (entry point of the module).
- **Blocks:** UC-M6-02 (Send Quotation).

## Notes

- Triggered by M5's `SuggestReorderUseCase` ([m5-estoque.md](../m5-estoque.md)) or by M7 sales demand ([m7-vendas-crm.md](../m7-vendas-crm.md)); both are external callers of this same port, not separate use cases.
