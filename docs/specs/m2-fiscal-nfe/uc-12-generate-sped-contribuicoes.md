# UC-M2-12 — Generate SPED Contribuições (`GenerateSpedContribuicoesUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

§3.4: "SPED Contribuições: EFD PIS/COFINS file generation; assessed per period."

## Description

Triggered by a user (typically the accountant or an admin) requesting the EFD PIS/COFINS file for a given company and period. Reads every `NfeDocument` and `InboundNfe` in the period, assesses PIS/COFINS per the company's tax regime, and generates the TXT file in the EFD Contribuições layout.

## Port signature

```java
public interface GenerateSpedContribuicoesUseCase {
    SpedContribuicoesFile execute(GenerateSpedContribuicoesCommand command);
}
```

`GenerateSpedContribuicoesCommand` fields: `companyId`, `period`. Returns a `SpedContribuicoesFile` (TXT content + assessment summary).

## Outbound ports required

- `NfeRepositoryPort`, `InboundNfeRepositoryPort` — source records for the period.
- `GenerateSpedFilePort` — shared with [UC-M2-11](uc-11-generate-sped-fiscal.md).

## REST endpoint

`POST /api/sped/contribuicoes`

## Domain entities touched

- Reads `NfeDocument`, `InboundNfe` — writes no new aggregate, only the generated file.

## Acceptance criteria

- [ ] Assesses PIS/COFINS per period, using the company's tax regime.
- [ ] Covers every authorized `NfeDocument` and confirmed `InboundNfe` in the period.
- [ ] Output matches the EFD Contribuições TXT layout.

## Dependencies

- **Depends on:** A full period of M2 issuance/receipt data (Phase 2); M9's reporting infrastructure being in place.
- **Blocks:** —

## Notes

Although SPED is documented under M2 §3.4, the roadmap (doc §14) places SPED export in **Phase 7 — Visibilidade**, together with M9's dashboard and reports — not in Phase 2's fiscal core. Don't schedule this ticket before Phase 7 even though it lives in the M2 spec.
