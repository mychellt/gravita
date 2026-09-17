# UC-M2-08 — Import Supplier NFe XML (`ImportSupplierNfeXmlUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.3: "XML import: upload the supplier's XML file; fields are auto-filled."

## Description

Triggered by a user uploading a supplier-issued NFe XML file (or by M6's `ImportSupplierNfeAtReceivingUseCase` during purchase receiving). Parses the XML and creates an `InboundNfe` with all fields auto-filled — no re-typing — ready for conference against a purchase order.

## Port signature

```java
public interface ImportSupplierNfeXmlUseCase {
    InboundNfe execute(ImportSupplierNfeXmlCommand command);
}
```

`ImportSupplierNfeXmlCommand` fields: `companyId`, `xmlFile` (byte stream). Returns the created `InboundNfe`.

## Outbound ports required

- `InboundNfeRepositoryPort`
- `XmlObjectStoragePort`

## REST endpoint

`POST /api/nfe/inbound/import-xml`

## Domain entities touched

- `InboundNfe` (created)

## Acceptance criteria

- [ ] All fields available in the XML (supplier, items, quantities, values, taxes) are auto-filled with no manual re-entry.
- [ ] The original XML is stored via `XmlObjectStoragePort`, referenced from the `InboundNfe` record.
- [ ] The resulting `InboundNfe` is in a state ready for conference, not yet confirmed into stock/payables.

## Dependencies

- **Depends on:** None.
- **Blocks:** [UC-M2-10](uc-10-confirm-inbound-nfe-receipt.md); M6's `ReceivePurchaseOrderUseCase`.
