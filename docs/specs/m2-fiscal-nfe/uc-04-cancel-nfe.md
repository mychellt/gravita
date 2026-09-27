# UC-M2-04 — Cancel NFe (`CancelNfeUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.2: "Cancellation: within the legal deadline (up to 24h, or the state limit); justification required."

## Description

Triggered by a user cancelling an authorized NFe. Precondition: the document is `AUTHORIZED` and still within its legal cancellation window (24h, or a longer state-specific limit). Submits the cancellation to SEFAZ with the supplied justification; on confirmation, moves the document to `CANCELLED` while keeping its record intact (no physical deletion — doc §1.1 "robusto desde o primeiro dia").

## Port signature

```java
public interface CancelNfeUseCase {
    NfeDocument execute(CancelNfeCommand command);
}
```

`CancelNfeCommand` fields: `nfeDocumentId`, `justification`. Returns the updated `NfeDocument`.

## Outbound ports required

- `NfeRepositoryPort`
- `SubmitToSefazPort`

## REST endpoint

`POST /api/nfe/{id}/cancel`

## Domain entities touched

- `NfeDocument` (`AUTHORIZED` → `CANCELLED`)

## Acceptance criteria

- [x] Rejects cancellation of a document not in `AUTHORIZED` status.
- [x] Rejects cancellation once the legal deadline (24h or the applicable state limit) has passed.
- [x] `justification` is mandatory and persisted with the cancellation record.
- [x] The cancelled document's data remains queryable — cancellation never physically deletes the record.

## Dependencies

- **Depends on:** [UC-M2-03](uc-03-transmit-nfe.md) (document must be `AUTHORIZED`).
- **Blocks:** —
