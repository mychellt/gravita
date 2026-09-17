# UC-M2-06 — Void Document Number Range (`VoidDocumentNumberRangeUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§3.2: "Inutilização: unused numbering voided with justification; the record is immutable."

## Description

Triggered by a user (or the end-of-day job, for the M3 PDV variant — see M3's `VoidUntransmittedNumberingUseCase`) to formally void a range of document numbers that were allocated but never used. Submits the void request to SEFAZ with a mandatory justification, producing an immutable record so the numbering gap is explained for audit and SPED purposes.

## Port signature

```java
public interface VoidDocumentNumberRangeUseCase {
    VoidedNumberRange execute(VoidNumberRangeCommand command);
}
```

`VoidNumberRangeCommand` fields: `companyId`, `series`, `startNumber`, `endNumber`, `justification`. Returns the created `VoidedNumberRange`.

## Outbound ports required

- `SubmitToSefazPort` — voiding a number range is a SEFAZ webservice call in the real NFe protocol.
- `VoidedNumberRangeRepositoryPort` — dedicated persistence for the immutable `VoidedNumberRange` record.

## REST endpoint

`POST /api/nfe/void-range`

## Domain entities touched

- `VoidedNumberRange` (created)

## Acceptance criteria

- [ ] `justification` is mandatory; the request is rejected without one.
- [ ] The resulting record is immutable — no update or delete operation exists for a `VoidedNumberRange`.
- [ ] The voided range is reflected in SPED/Livros Fiscais generation so numbering gaps are explained, not silently missing.

## Dependencies

- **Depends on:** M1's document series configuration (the range being voided must belong to a real series).
- **Blocks:** [UC-M2-13](uc-13-generate-livros-fiscais.md) (books must account for voided ranges).
