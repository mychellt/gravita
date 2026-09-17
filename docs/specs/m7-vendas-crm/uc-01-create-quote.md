# UC-M7-01 — Create Quote (`CreateQuoteUseCase`)

**Module:** M7 — Vendas & CRM ([module spec](../m7-vendas-crm.md))
**Context package:** `br.gravita.sales`
**Roadmap phase:** Phase 4 — Comercial (doc §14)

## Functional requirement

§8.1 — Orçamento (quote): created quickly; configurable validity; printed as PDF or sent via WhatsApp. (This ticket covers creation; delivery is [UC-02](uc-02-send-quote.md).)

## Description

A salesperson creates a quote for a customer from a set of product/service lines. The quote starts in `DRAFT`/`SENT`-ready state with a configurable validity date (`validUntil`); it carries no stock reservation and no fiscal effect until converted to an order.

## Port signature

```java
public interface CreateQuoteUseCase {
    QuoteView execute(CreateQuoteCommand command);
}
```

`CreateQuoteCommand`: `customerId`, `items: [{productOrServiceId, quantity, unitPrice, discount}]`, `validUntil`. Returns the created `Quote` (id, status `DRAFT`, echoed items and validity).

## Outbound ports required

- `QuoteRepositoryPort`

## REST endpoint

`POST /api/sales/quotes`

## Domain entities touched

- `Quote`

## Acceptance criteria

- [ ] Quote is persisted with status `DRAFT` and the given `validUntil`.
- [ ] Items, unit prices and discounts are stored exactly as submitted (no re-pricing at creation time).
- [ ] A quote with no items is rejected.
- [ ] `validUntil` must be in the future at creation time.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-02 Send Quote](uc-02-send-quote.md), [UC-03 Convert Quote to Order](uc-03-convert-quote-to-order.md).
