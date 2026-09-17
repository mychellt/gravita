# UC-M3-09 — Void Untransmitted Numbering (`VoidUntransmittedNumberingUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 8 — Robustez (doc §14)

## Functional requirement

§4.2: "Inutilização: numbering that was never transmitted is auto-voided at day end."

## Description

End-of-day scheduled job that finds document numbers allocated to this register/company but never transmitted to SEFAZ (e.g. a sale abandoned before issuance), and voids them via the SEFAZ Inutilização request, keeping the numbering sequence clean and auditable.

## Port signature

```java
public interface VoidUntransmittedNumberingUseCase {
    void execute(VoidUntransmittedNumberingCommand command);
}
```

`VoidUntransmittedNumberingCommand`: `companyId`, `date` (the day being closed).

## Outbound ports required

- `SubmitToSefazPort` (Inutilização request, shared with M2)
- `NfceRepositoryPort`

## REST endpoint

None — end-of-day scheduled job, not a synchronous user-facing endpoint. The module's inbound adapter table doesn't list one; inferred from "auto-voided at day end" (§4.2).

## Domain entities touched

- `NfceSale` (numbering ranges never transmitted), referencing `masterdata`'s `DocumentSeries`.

## Acceptance criteria

- [ ] Runs automatically at day end, with no manual trigger required.
- [ ] Only numbering that was never transmitted is voided — a sale still legitimately in progress is not touched.
- [ ] The voiding is recorded as an immutable record (mirrors M2's `VoidedNumberRange`, per the doc §13 audit-trail decision).

## Dependencies

- **Depends on:** [04 — Issue NFCe](uc-04-issue-nfce.md) and its numbering allocation.
- **Blocks:** —
