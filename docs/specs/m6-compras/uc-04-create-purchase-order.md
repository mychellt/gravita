# UC-M6-04 — Create Purchase Order (`CreatePurchaseOrderUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Pedido de compra: status Open / Partially Received / Closed / Cancelled.

## Description

Converts a quoted (or, for cases with no formal quotation, a directly requested) `PurchaseRequest` into a `PurchaseOrder` against one chosen supplier, carrying the agreed unit prices. The order is created in status `OPEN` and its `approvalAlcada` is resolved from the order's total value against `system`'s shared alçada configuration, determining whether UC-M6-05 (Approve) is required before receiving.

## Port signature

```java
public interface CreatePurchaseOrderUseCase {
    PurchaseOrderId execute(CreatePurchaseOrderCommand command);
}
```

`CreatePurchaseOrderCommand`: `requestId: PurchaseRequestId`, `quotationId: QuotationId` (optional), `supplier: SupplierRef`, `items: [{product, quantity, unitPrice}]`. Returns the created `PurchaseOrder`'s id.

## Outbound ports required

- `PurchaseRequestRepositoryPort`
- `QuotationRepositoryPort`
- `PurchaseOrderRepositoryPort`
- `ApprovalAlcadaRepositoryPort` (from `system`, [m10-sistema.md](../m10-sistema.md)) — resolves whether the order needs approval.

## REST endpoint

`POST /api/purchasing/orders`

## Domain entities touched

- `PurchaseOrder` (created)
- `PurchaseRequest` (status `QUOTED` → `CONVERTED`)

## Acceptance criteria

- [ ] An order can only be created from a request in status `OPEN` or `QUOTED`.
- [ ] When created from a quotation, the order's unit prices come from the selected supplier's `QuotationResponse`.
- [ ] Creating the order marks the originating request `CONVERTED`.
- [ ] The order's `approvalAlcada` is resolved from its total value at creation time.
- [ ] A newly created order starts in status `OPEN`.

## Dependencies

- **Depends on:** UC-M6-01 (Create Purchase Request); UC-M6-03 (Register Quotation Response) when a quotation was used.
- **Blocks:** UC-M6-05 (Approve Purchase Order), UC-M6-06 (Receive Purchase Order).
