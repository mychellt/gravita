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

`CreateQuoteCommand`: `customerId`, `salespersonId`, `items: [{productOrServiceId, quantity, unitPrice, discount}]`, `validUntil`. Returns the created `Quote` (id, status `DRAFT`, echoed items and validity).

## Outbound ports required

- `QuoteRepositoryPort`

## REST endpoint

`POST /api/sales/quotes`

## Domain entities touched

- `Quote`

## Acceptance criteria

- [x] Quote is persisted with status `DRAFT` and the given `validUntil`.
- [x] Items, unit prices and discounts are stored exactly as submitted (no re-pricing at creation time).
- [x] A quote with no items is rejected.
- [x] `validUntil` must be in the future at creation time.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-02 Send Quote](uc-02-send-quote.md), [UC-03 Convert Quote to Order](uc-03-convert-quote-to-order.md).

## Notes

- Per-line `discount` is an absolute amount taken off the line subtotal (`quantity × unitPrice`), matching `SaleItem.itemDiscount` in `tax`; it defaults to zero, and it can never be negative or exceed the subtotal. The order-level `discountPercent` on `SalesOrder` is a separate concept.
- `CreateQuoteCommand` also carries `salespersonId` (required), recording who the quote — and, later, the order it converts to — belongs to. Added by [UC-08 Calculate Commission](uc-08-calculate-commission.md), which needed a salesperson to attribute commissions to; see its Notes.
- `validUntil` is a calendar date, and "in the future" means strictly after today: a quote that expires on its creation day is rejected.
- Amounts are stored as `NUMERIC(14, 4)`. The REST layer rejects values with more than four decimal places, so nothing is silently rounded on the way in.
