# UC-M6-08 — Confirm Purchase Receipt (`ConfirmPurchaseReceiptUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Contas a pagar: auto-generated on confirming receipt, using the NF's installment terms. Recebimento: ... physical conference before confirming the entry in the system.

## Description

Finalizes a `PurchaseReceipt` after physical conference (UC-M6-06) and, where applicable, XML import/reconciliation (UC-M6-07). Confirming triggers the two side effects that make the receipt count in the rest of the system: a stock entry in `inventory` and accounts-payable titles in `finance`, generated from the supplier NF's installment terms. The `PurchaseOrder` moves to `CLOSED` once all its items are fully received and confirmed, or stays `PARTIALLY_RECEIVED` otherwise.

## Port signature

```java
public interface ConfirmPurchaseReceiptUseCase {
    void execute(ConfirmPurchaseReceiptCommand command);
}
```

`ConfirmPurchaseReceiptCommand`: `receiptId: PurchaseReceiptId`.

## Outbound ports required

- `PurchaseReceiptRepositoryPort`
- `PurchaseOrderRepositoryPort`
- `RegisterStockEntryPort` (into `inventory`)
- `GeneratePayableFromReceiptPort` (into `finance`)

## REST endpoint

`POST /api/purchasing/receipts/{id}/confirm`

## Domain entities touched

- `PurchaseReceipt` (confirmed)
- `PurchaseOrder` (status `PARTIALLY_RECEIVED` → `CLOSED` when fully received)
- `inventory.StockBalance` (via `RegisterStockEntryPort`)
- `finance.Payable` (via `GeneratePayableFromReceiptPort`)

## Acceptance criteria

- [ ] Confirmation is only possible on a receipt that has completed physical conference.
- [ ] Confirming generates exactly one stock entry per received item.
- [ ] Confirming generates accounts-payable titles matching the NF's installment terms (or the order's terms, when no NF was imported).
- [ ] The order closes only when the sum of all its confirmed receipts covers every item's ordered quantity.
- [ ] Confirming an already-confirmed receipt is a no-op or rejected (idempotency), not a duplicate stock/payable generation.

## Dependencies

- **Depends on:** UC-M6-06 (Receive Purchase Order); UC-M6-07 (Import Supplier NFe at Receiving) when XML import is used; M5's `RegisterStockEntryUseCase` ([m5-estoque.md](../m5-estoque.md)); M8's payable-generation use case ([m8-financeiro.md](../m8-financeiro.md)).
- **Blocks:** —
