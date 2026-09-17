# UC-M6-07 — Import Supplier NFe at Receiving (`ImportSupplierNfeAtReceivingUseCase`)

**Module:** M6 — Compras ([module spec](../m6-compras.md))
**Context package:** `br.gravita.purchasing`
**Roadmap phase:** Phase 3 — Operação (doc §14)

## Functional requirement

Importação de XML: the supplier's NF-e is auto-imported at receiving — zero re-typing.

## Description

At receiving time, the supplier's NF-e XML is uploaded and parsed, then reconciled against the `PurchaseOrder` and the physical `PurchaseReceipt` (UC-M6-06), surfacing quantity/value divergences between ordered, physically received, and invoiced amounts. This use case delegates the actual XML parsing to M2's `ImportSupplierNfeXmlUseCase` ([uc-08-import-supplier-nfe-xml.md](../m2-fiscal-nfe/uc-08-import-supplier-nfe-xml.md)) and adds the order/receipt reconciliation on top.

## Port signature

```java
public interface ImportSupplierNfeAtReceivingUseCase {
    ConferenceResult execute(ImportSupplierNfeAtReceivingCommand command);
}
```

`ImportSupplierNfeAtReceivingCommand`: `orderId: PurchaseOrderId`, `receiptId: PurchaseReceiptId`, `xmlFile`. Returns a `ConferenceResult` listing per-item divergences (ordered vs. received vs. on-NF).

## Outbound ports required

- `PurchaseOrderRepositoryPort`
- `PurchaseReceiptRepositoryPort`
- M2's `ImportSupplierNfeXmlUseCase` (inbound port of `tax`, invoked as a collaborator — not an outbound port of this module).

## REST endpoint

`POST /api/purchasing/orders/{id}/receipts` (XML import happens as part of the receiving request; see [m6-compras.md](../m6-compras.md) adapter table).

## Domain entities touched

- `PurchaseReceipt` (`conferenceResult` populated)
- M2's `InboundNfe` (created via the delegated use case)

## Acceptance criteria

- [ ] Uploading the supplier's XML fills item quantities/values with no manual re-typing.
- [ ] The conference result flags any divergence between ordered, physically received, and NF-invoiced quantities/values.
- [ ] The created `InboundNfe` (M2) is linked to this `PurchaseReceipt`.
- [ ] Import can be skipped for suppliers without XML (manual entry stays on the M2 side per `EnterInboundNfeManuallyUseCase`).

## Dependencies

- **Depends on:** UC-M6-06 (Receive Purchase Order); M2's `ImportSupplierNfeXmlUseCase`.
- **Blocks:** UC-M6-08 (Confirm Purchase Receipt).
