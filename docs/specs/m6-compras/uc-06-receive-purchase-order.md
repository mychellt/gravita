# UC-M6-06 — Receive Purchase Order (`ReceivePurchaseOrderUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Recebimento: total or partial, with physical conference before confirming the entry in the system.

## Description

Opens a `PurchaseReceipt` against an open `PurchaseOrder`, recording the physically received quantities per item (total or partial). This is the physical-conference step; it does not yet update stock or generate a payable — that happens once `ConfirmPurchaseReceiptUseCase` (UC-M6-08) is called, after any divergences are checked (typically together with UC-M6-07's XML import).

## Port signature

```java
public interface ReceivePurchaseOrderUseCase {
    PurchaseReceiptId execute(ReceivePurchaseOrderCommand command);
}
```

`ReceivePurchaseOrderCommand`: `orderId: PurchaseOrderId`, `receivedItems: [{product, receivedQty}]`. Returns the created `PurchaseReceipt`'s id.

## Outbound ports required

- `PurchaseOrderRepositoryPort`
- `PurchaseReceiptRepositoryPort`

## REST endpoint

`POST /api/purchasing/orders/{id}/receipts`

## Domain entities touched

- `PurchaseReceipt` (created)
- `PurchaseOrder` (status `OPEN`/`PARTIALLY_RECEIVED` updated based on whether all items are now fully received)

## Acceptance criteria

- [ ] A receipt can only be opened against an order in status `OPEN` or `PARTIALLY_RECEIVED`, and only after approval if the order required it (UC-M6-05).
- [ ] `receivedQty` per item can be less than `orderedQty` (partial receipt).
- [ ] If every item reaches its full ordered quantity across all receipts, the order becomes eligible to close on confirmation; otherwise it becomes `PARTIALLY_RECEIVED`.
- [ ] The receipt's `total`/`partial` flag reflects whether all items were fully received in this receipt.

## Dependencies

- **Depends on:** UC-M6-04 (Create Purchase Order); UC-M6-05 (Approve Purchase Order) when required.
- **Blocks:** UC-M6-07 (Import Supplier NFe at Receiving), UC-M6-08 (Confirm Purchase Receipt).
