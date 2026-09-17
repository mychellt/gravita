# UC-M2-01 — Issue NFe (`IssueNfeUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.1 Emissão: issuance origin is manual (direct entry) or automatic from an approved sales order; free CFOP registration with automatic mapping by operation type; recipient search by CPF/CNPJ/name with IE validation; item search by barcode/code/description with variant/kit support; automatic tax calculation with justified manual override; discount respecting the price table's max-discount rule; freight/insurance/other expenses impacting the tax base; transport data (CIF/FOB, carrier, weights, RNTRC); reference to a prior NF for returns/complementary notes; free-text additional info.

## Description

Triggered by a user filling the issuance screen, or automatically by M7's `InvoiceSalesOrderUseCase` when a sales order is invoiced. Precondition: the issuing company has a valid, non-expired digital certificate and a configured document series (M1). Builds a `NfeDocument` in `DRAFT`, resolves item-level taxes via `CalculateTaxUseCase`, allocates the next document number, and enqueues it for transmission (`QUEUED`).

## Port signature

```java
public interface IssueNfeUseCase {
    NfeDocument execute(IssueNfeCommand command);
}
```

`IssueNfeCommand` fields: `issuerCompanyId`, `originSalesOrderId` (optional), `naturezaOperacao`, `cfop`, `recipient` (`PersonRef` or ad-hoc PF/PJ data), `items: [NfeItemInput]`, `freight`, `insurance`, `otherExpenses`, `transport{modality, carrier, volume, grossWeight, netWeight, rntrc}`, `referencedAccessKey` (optional), `additionalInfo`. Returns the created `NfeDocument` (id + access key + status).

## Outbound ports required

- `NfeRepositoryPort`
- `AllocateDocumentNumberUseCase` (from `masterdata`)
- `CalculateTaxUseCase` (collaborator — see [UC-M2-02](uc-02-calculate-tax.md))
- `TransmissionQueuePort`

## REST endpoint

`POST /api/nfe`

## Domain entities touched

- `NfeDocument` (created, `DRAFT` → `QUEUED`)
- `NfeItem`
- `TransmissionQueueEntry` (created)

## Acceptance criteria

- [ ] Accepts both manual entry and an `originSalesOrderId`-driven automatic call with the order's data preserved.
- [ ] CFOP is resolved from the free CFOP registry by operation type, not hardcoded.
- [ ] Recipient CPF/CNPJ passes check-digit and IE-taxpayer validation before the document is created.
- [ ] Item-level tax totals come from `CalculateTaxUseCase`; a manual override is only accepted with a justification string.
- [ ] Discount is rejected if it exceeds the linked price table's max-discount rule (per M1) unless override is explicitly justified.
- [ ] A `referencedAccessKey` is required when `naturezaOperacao` is a return or complementary note.
- [ ] On success, the document is `QUEUED` and a `TransmissionQueueEntry` exists for it.

## Dependencies

- **Depends on:** M1 `AllocateDocumentNumberUseCase` and company/certificate/series setup; [UC-M2-02](uc-02-calculate-tax.md) `CalculateTaxUseCase`.
- **Blocks:** [UC-M2-03](uc-03-transmit-nfe.md) `TransmitNfeUseCase`; M7's `InvoiceSalesOrderUseCase` (automatic-origin flow).
