# UC-M6-09 — Return to Supplier (`ReturnToSupplierUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Devolução: partial or total return to the supplier; a return NF-e is issued in the same flow.

## Description

Against a confirmed `PurchaseReceipt`, the user selects the items and quantities to return (partial or total) to the supplier. This creates a `PurchaseReturn` and, in the same flow, issues a return NF-e via M2's `IssueNfeUseCase` ([uc-01-issue-nfe.md](../m2-fiscal-nfe/uc-01-issue-nfe.md)). Stock and financial reversal for the returned quantity are the responsibility of the inbound stock/payable adjustment triggered by the return, mirroring UC-M6-08's effects in reverse.

## Port signature

```java
public interface ReturnToSupplierUseCase {
    PurchaseReturnId execute(ReturnToSupplierCommand command);
}
```

`ReturnToSupplierCommand`: `receiptId: PurchaseReceiptId`, `items: [{product, quantity}]`. Returns the created `PurchaseReturn`'s id.

## Outbound ports required

- `PurchaseReceiptRepositoryPort` (via `PurchaseReceiptRepositoryPort`, reused for reads)
- M2's `IssueNfeUseCase` (inbound port of `tax`, invoked as a collaborator).

## REST endpoint

`POST /api/purchasing/receipts/{id}/return`

## Domain entities touched

- `PurchaseReturn` (created)
- M2's `NfeDocument` (created via the delegated use case, `returnNfeRef` linked back)

## Acceptance criteria

- [ ] A return can only be made against a confirmed receipt.
- [ ] Return quantity per item cannot exceed the quantity originally received.
- [ ] A partial return only reduces stock/payable for the returned items, leaving the rest of the receipt intact.
- [ ] The return NF-e is issued through M2 in the same flow, not as a separate manual step.
- [ ] `PurchaseReturn.partial`/`total` reflects whether every received item was returned.

## Dependencies

- **Depends on:** UC-M6-08 (Confirm Purchase Receipt); M2's `IssueNfeUseCase`.
- **Blocks:** —
