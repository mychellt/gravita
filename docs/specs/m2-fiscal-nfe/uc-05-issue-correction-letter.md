# UC-M2-05 — Issue Correction Letter (`IssueCorrectionLetterUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.2: "CC-e: Carta de Correção Eletrônica for non-tax data; limit of 20 events."

## Description

Triggered by a user correcting non-tax data on an already-authorized NFe (e.g. address wording, additional info) without reissuing the document. Precondition: the document is `AUTHORIZED` and has fewer than 20 correction-letter events already registered. Submits the correction text to SEFAZ and records the resulting protocol.

## Port signature

```java
public interface IssueCorrectionLetterUseCase {
    CorrectionLetter execute(IssueCorrectionLetterCommand command);
}
```

`IssueCorrectionLetterCommand` fields: `nfeDocumentId`, `text`. Returns the created `CorrectionLetter` (sequence number, protocol, timestamp).

## Outbound ports required

- `NfeRepositoryPort`
- `SubmitToSefazPort`

## REST endpoint

`POST /api/nfe/{id}/correction-letters`

## Domain entities touched

- `NfeDocument` (read, must be `AUTHORIZED`)
- `CorrectionLetter` (created)

## Acceptance criteria

- [ ] Rejects the request once 20 correction-letter events already exist for the document.
- [ ] Requires the document to be `AUTHORIZED`; rejects for `DRAFT`, `CANCELLED` or `VOIDED`.
- [ ] Each event is assigned a sequence number and a SEFAZ protocol, both persisted.

## Dependencies

- **Depends on:** [UC-M2-03](uc-03-transmit-nfe.md) (document must be `AUTHORIZED`).
- **Blocks:** —
