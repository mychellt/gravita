# UC-M2-11 — Generate SPED Fiscal (`GenerateSpedFiscalUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

§3.4: "SPED Fiscal: EFD ICMS/IPI TXT file generation; mandatory record validation."

## Description

Triggered by a user (typically the accountant or an admin) requesting the EFD ICMS/IPI file for a given company and period. Reads every `NfeDocument` and `InboundNfe` in the period, validates that all mandatory SPED records can be populated, and generates the TXT file in the EFD layout.

## Port signature

```java
public interface GenerateSpedFiscalUseCase {
    SpedFiscalFile execute(GenerateSpedFiscalCommand command);
}
```

`GenerateSpedFiscalCommand` fields: `companyId`, `period` (month/year or date range). Returns a `SpedFiscalFile` (TXT content + validation report).

## Outbound ports required

- `NfeRepositoryPort`, `InboundNfeRepositoryPort` — source records for the period.
- `GenerateSpedFilePort` — EFD TXT-layout generation, distinct from `GenerateDanfePort`.

## REST endpoint

`POST /api/sped/fiscal`

## Domain entities touched

- Reads `NfeDocument`, `InboundNfe`, `VoidedNumberRange` (for numbering gaps) — writes no new aggregate, only the generated file.

## Acceptance criteria

- [ ] Covers every authorized and cancelled `NfeDocument` plus every confirmed `InboundNfe` in the requested period.
- [ ] Validates mandatory EFD records before producing output; generation fails with a clear error listing missing/invalid records rather than emitting an incomplete file.
- [ ] Output matches the EFD ICMS/IPI TXT layout.

## Dependencies

- **Depends on:** A full period of M2 issuance/receipt data (Phase 2); M9's reporting infrastructure being in place.
- **Blocks:** —

## Notes

Although SPED is documented under M2 §3.4 (alongside the rest of the module's fiscal obligations), the roadmap (doc §14) places SPED export in **Phase 7 — Visibilidade**, together with M9's dashboard and reports — not in Phase 2's fiscal core. Don't schedule this ticket before Phase 7 even though it lives in the M2 spec.

## Implementation notes

- **`GenerateSpedFilePort` already exists.** It shipped with [UC-M2-12](uc-12-generate-sped-contribuicoes.md) (`core.ports.outbound.tax.GenerateSpedFilePort`, adapter `SpedFileAdapter`). It writes the shape both EFDs share - `|REG|field|` lines, CR LF, ISO-8859-1, the `X001`/`X990` pair of every block and Block 9 - from the registers the use case supplies (`SpedLayout` of `SpedBlock`s of `SpedRecord`s). This ticket should lay out the EFD ICMS/IPI registers (`0000`, `E100`, `C100`, ...) and hand them to that port, not add a second writer.
