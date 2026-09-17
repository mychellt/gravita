# UC-M2-09 — Enter Inbound NFe Manually (`EnterInboundNfeManuallyUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.3: "Manual entry: type the access key or enter data manually for suppliers without XML."

## Description

Triggered by a user entering a purchase NFe from a supplier who didn't provide an XML file. Either the access key is typed (and the document is fetched from SEFAZ if available) or the full data is entered manually. Produces the same `InboundNfe` shape as [UC-M2-08](uc-08-import-supplier-nfe-xml.md), so downstream conference and confirmation work identically regardless of entry method.

## Port signature

```java
public interface EnterInboundNfeManuallyUseCase {
    InboundNfe execute(EnterInboundNfeManuallyCommand command);
}
```

`EnterInboundNfeManuallyCommand` fields: `companyId`, `accessKey` (optional), `manualData` (supplier, items, quantities, values — required when `accessKey` isn't resolvable). Returns the created `InboundNfe`.

## Outbound ports required

- `InboundNfeRepositoryPort`

## REST endpoint

`POST /api/nfe/inbound`

## Domain entities touched

- `InboundNfe` (created)

## Acceptance criteria

- [ ] Accepts either an access key or fully manual data — at least one path must succeed without the other.
- [ ] Produces an `InboundNfe` in the same shape/state as XML import, so [UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md) doesn't need to branch by entry method.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md); M6's `ReceivePurchaseOrderUseCase`.
