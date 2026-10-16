# UC-M2-10 — Confirm Inbound NFe Receipt (`ConfirmInboundNfeReceiptUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.3: conference between what was ordered, what physically arrived and what's on the NF; automatic stock entry generated immediately after receipt is confirmed; accounts-payable titles auto-generated from the purchase NF's payment terms; ICMS/PIS/COFINS credit calculated per the applicable tax regime.

## Description

Triggered once a user has reconciled an `InboundNfe` (from [UC-M2-08](uc-08-import-supplier-nfe-xml.md) or [UC-M2-09](uc-09-enter-inbound-nfe-manually.md)) against a purchase order and physical count. Confirms the conference result, immediately generates the corresponding stock entry in `inventory`, generates accounts-payable titles in `finance` from the NF's payment terms, and computes the ICMS/PIS/COFINS credit for the tax regime in effect.

GRA-246: before any of that, the supplier must resolve by CNPJ to a registered `Supplier` and every item must already carry a [`SupplierProductCatalogLink`](../m1-cadastros-base/uc-16-supplier-product-catalog-link.md) for `(supplierId, item.supplierProductCode)` — both checked dynamically via [UC-M2-15](uc-15-resolve-inbound-nfe-item.md), independent of the pre-existing item-count check. On success, each item's link has its `lastPurchaseUnitValue`/`lastPurchaseAt` refreshed to this NFe's own values (never an average).

## Port signature

```java
public interface ConfirmInboundNfeReceiptUseCase {
    InboundNfe execute(ConfirmInboundNfeReceiptCommand command);
}
```

`ConfirmInboundNfeReceiptCommand` fields: `inboundNfeId`, `conferenceResult: [{itemRef, orderedQty, receivedQty}]`. Returns the updated `InboundNfe`.

## Outbound ports required

- `InboundNfeRepositoryPort`
- `SupplierRepositoryPort`, `SupplierProductCatalogLinkRepositoryPort`, `UpsertSupplierProductCatalogLinkUseCase` (GRA-246 resolution checks and price update)
- `NotifyStockEntryPort` (into `inventory`)
- `NotifyPayableGeneratedPort` (into `finance`)
- `CalculateTaxUseCase` ([UC-M2-02](uc-02-calculate-tax.md)) — for the ICMS/PIS/COFINS credit computation

## REST endpoint

`POST /api/nfe/inbound/{id}/confirm-receipt`

## Domain entities touched

- `InboundNfe` (conference result recorded, marked confirmed)

## Acceptance criteria

- [ ] Records the three-way comparison (ordered vs. physically received vs. on the NF) before allowing confirmation.
- [ ] Stock entry is created immediately on confirmation — not on a delayed batch job.
- [ ] Accounts-payable titles are generated with installments matching the NF's payment terms, not re-entered manually.
- [ ] ICMS/PIS/COFINS credit is computed according to the company's tax regime at confirmation time.
- [ ] GRA-246 AC2: rejects confirmation with a clear message when the NFe's supplier is unresolved, independent of the item-count check.
- [ ] GRA-246 AC3: rejects confirmation with a clear message when any item has no resolved `SupplierProductCatalogLink`.
- [ ] GRA-246 AC8/BR7: on success, each item's link price/date is updated to this NFe's own values.
- [ ] GRA-246 AC10/BR8: confirming a receipt never mutates the originally imported `InboundNfeItem` values.

## Dependencies

- **Depends on:** [UC-M2-08](uc-08-import-supplier-nfe-xml.md) or [UC-M2-09](uc-09-enter-inbound-nfe-manually.md) (an `InboundNfe` must exist); [UC-M2-02](uc-02-calculate-tax.md); [UC-M2-15](uc-15-resolve-inbound-nfe-item.md) (GRA-246 supplier/item resolution).
- **Blocks:** M5's stock-entry registration; M8's payable generation; M6's `ConfirmPurchaseReceiptUseCase`.
