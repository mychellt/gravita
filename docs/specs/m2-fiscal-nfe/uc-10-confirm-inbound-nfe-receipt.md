# UC-M2-10 — Confirm Inbound NFe Receipt (`ConfirmInboundNfeReceiptUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.3: conference between what was ordered, what physically arrived and what's on the NF; automatic stock entry generated immediately after receipt is confirmed; accounts-payable titles auto-generated from the purchase NF's payment terms; ICMS/PIS/COFINS credit calculated per the applicable tax regime.

## Description

Triggered once a user has reconciled an `InboundNfe` (from [UC-M2-08](uc-08-import-supplier-nfe-xml.md) or [UC-M2-09](uc-09-enter-inbound-nfe-manually.md)) against a purchase order and physical count. Confirms the conference result, immediately generates the corresponding stock entry in `inventory`, generates accounts-payable titles in `finance` from the NF's payment terms, and computes the ICMS/PIS/COFINS credit for the tax regime in effect.

## Port signature

```java
public interface ConfirmInboundNfeReceiptUseCase {
    InboundNfe execute(ConfirmInboundNfeReceiptCommand command);
}
```

`ConfirmInboundNfeReceiptCommand` fields: `inboundNfeId`, `conferenceResult: [{itemRef, orderedQty, receivedQty}]`. Returns the updated `InboundNfe`.

## Outbound ports required

- `InboundNfeRepositoryPort`
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

## Dependencies

- **Depends on:** [UC-M2-08](uc-08-import-supplier-nfe-xml.md) or [UC-M2-09](uc-09-enter-inbound-nfe-manually.md) (an `InboundNfe` must exist); [UC-M2-02](uc-02-calculate-tax.md).
- **Blocks:** M5's stock-entry registration; M8's payable generation; M6's `ConfirmPurchaseReceiptUseCase`.
