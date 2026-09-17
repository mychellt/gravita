# UC-M6-02 — Send Quotation (`SendQuotationUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Cotação: the item list is sent to multiple suppliers, with side-by-side price/deadline comparison.

## Description

Given an `OPEN` `PurchaseRequest`, the buyer selects one or more suppliers and sends them the request's item list for pricing. This creates a `Quotation` linked to the request and moves the request to `QUOTED`. The quotation starts with no responses; suppliers' answers are recorded separately (UC-M6-03).

## Port signature

```java
public interface SendQuotationUseCase {
    QuotationId execute(SendQuotationCommand command);
}
```

`SendQuotationCommand`: `requestId: PurchaseRequestId`, `suppliers: [SupplierRef]`. Returns the created `Quotation`'s id.

## Outbound ports required

- `PurchaseRequestRepositoryPort`
- `QuotationRepositoryPort`

## REST endpoint

`POST /api/purchasing/requests/{id}/quotations`

## Domain entities touched

- `Quotation` (created)
- `PurchaseRequest` (status `OPEN` → `QUOTED`)

## Acceptance criteria

- [ ] A quotation can only be sent for a request in status `OPEN`.
- [ ] At least one supplier must be selected.
- [ ] The quotation carries the full item list from the originating request.
- [ ] Sending a quotation transitions the request to `QUOTED`.

## Dependencies

- **Depends on:** UC-M6-01 (Create Purchase Request).
- **Blocks:** UC-M6-03 (Register Quotation Response).
