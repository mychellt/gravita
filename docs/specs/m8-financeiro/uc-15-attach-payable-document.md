# UC-M8-15 — Attach Payable Document (`AttachPayableDocumentUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "Anexo: upload de boleto, NF ou comprovante vinculado ao título."

## Description

Uploads a supporting document (boleto, NF, receipt) and links it to a `Payable`.

## Port signature

```java
public interface AttachPayableDocumentUseCase {
    Payable execute(AttachPayableDocumentCommand command);
}
```

`AttachPayableDocumentCommand`: `payableId`, `file`. Returns the updated `Payable` with the new entry in `attachments`.

## Outbound ports required

- `PayableRepositoryPort`
- `DocumentAttachmentStoragePort`

## REST endpoint

`POST /api/finance/payables/{id}/attachments`

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] The uploaded file is stored and its reference appended to `Payable.attachments`.
- [ ] Multiple attachments per payable are supported.

## Dependencies

- **Depends on:** UC-M8-10 or UC-M8-11.
- **Blocks:** —
