# UC-M7-03 — Convert Quote to Order (`ConvertQuoteToOrderUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Conversão: Quote → Order with one click; data fully preserved.

## Description

Converts a quote into a `SalesOrder` in a single action. Customer, items, prices and discounts are carried over unchanged; the new order starts in `DRAFT` and references the originating quote. The quote itself moves to `CONVERTED` and can no longer be sent or re-converted.

## Port signature

```java
public interface ConvertQuoteToOrderUseCase {
    SalesOrderView execute(ConvertQuoteToOrderCommand command);
}
```

`ConvertQuoteToOrderCommand`: `quoteId`. Returns the created `SalesOrder` (id, status `DRAFT`, `originQuote` set).

## Outbound ports required

- `QuoteRepositoryPort`
- `SalesOrderRepositoryPort`

## REST endpoint

`POST /api/sales/quotes/{id}/convert`

## Domain entities touched

- `Quote`
- `SalesOrder`

## Acceptance criteria

- [ ] `SalesOrder.originQuote` references the source quote.
- [ ] Customer, items, unit prices and discounts are identical between quote and resulting order.
- [ ] Resulting order status is `DRAFT`.
- [ ] Quote status transitions to `CONVERTED`; converting an already-`CONVERTED` or expired quote is rejected.

## Dependencies

- **Depends on:** [UC-01 Create Quote](uc-01-create-quote.md).
- **Blocks:** [UC-04 Approve Sales Order](uc-04-approve-sales-order.md).
