# UC-M6-03 — Register Quotation Response (`RegisterQuotationResponseUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Cotação: the item list is sent to multiple suppliers, with side-by-side price/deadline comparison.

## Description

Records one supplier's reply to a `Quotation`: their price per item and delivery deadline. Each supplier can have at most one response per quotation. The buyer compares responses side by side (a query, not a separate use case) to pick a winner before creating the `PurchaseOrder` (UC-M6-04).

## Port signature

```java
public interface RegisterQuotationResponseUseCase {
    void execute(RegisterQuotationResponseCommand command);
}
```

`RegisterQuotationResponseCommand`: `quotationId: QuotationId`, `supplier: SupplierRef`, `itemPrices: [{product, unitPrice}]`, `deadline`.

## Outbound ports required

- `QuotationRepositoryPort`

## REST endpoint

`POST /api/purchasing/quotations/{id}/responses`

## Domain entities touched

- `Quotation` (appends a `QuotationResponse`)

## Acceptance criteria

- [ ] The responding supplier must be one of the suppliers the quotation was originally sent to.
- [ ] A supplier cannot register more than one response per quotation (re-submission replaces the prior response).
- [ ] `itemPrices` must cover every item in the quotation.
- [ ] Responses from different suppliers on the same quotation are independently retrievable for comparison.

## Dependencies

- **Depends on:** UC-M6-02 (Send Quotation).
- **Blocks:** UC-M6-04 (Create Purchase Order) — the chosen response's price/supplier feed the order.
