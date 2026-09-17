# UC-M2-13 — Generate Livros Fiscais (`GenerateLivrosFiscaisUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

§3.4: "Livros fiscais: Entry, Exit and ICMS Assessment books generated per period." Also §3.4's "Relatório de tributos": ICMS, IPI, PIS, COFINS summary per period for the accountant — produced alongside the books by this use case.

## Description

Triggered by a user (accountant or admin) requesting the fiscal books for a company and period. Aggregates every `NfeDocument` (Entry and Exit) and `InboundNfe` in the period, factors in `VoidedNumberRange` entries so numbering gaps are explained, and assesses ICMS to produce the three legally-required books plus the tax summary report.

## Port signature

```java
public interface GenerateLivrosFiscaisUseCase {
    LivrosFiscaisReport execute(GenerateLivrosFiscaisCommand command);
}
```

`GenerateLivrosFiscaisCommand` fields: `companyId`, `period`. Returns `LivrosFiscaisReport`: Entry book, Exit book, ICMS Assessment book, and the ICMS/IPI/PIS/COFINS summary.

## Outbound ports required

- `NfeRepositoryPort`, `InboundNfeRepositoryPort` — source records.
- `GenerateFiscalBookPort` — PDF/TXT rendering for the aggregated period books, distinct from `GenerateDanfePort`'s single-document rendering.

## REST endpoint

`GET /api/livros-fiscais`

## Domain entities touched

- Reads `NfeDocument`, `InboundNfe`, `VoidedNumberRange` — writes no new aggregate, only the generated report.

## Acceptance criteria

- [ ] Produces Entry, Exit and ICMS Assessment books for the requested period, in both PDF and TXT.
- [ ] Numbering gaps from `VoidedNumberRange` (see [UC-M2-06](uc-06-void-document-number-range.md)) are accounted for, not silently omitted.
- [ ] The accompanying tax summary covers ICMS, IPI, PIS and COFINS for the period.

## Dependencies

- **Depends on:** A full period of M2 issuance/receipt data (Phase 2); [UC-M2-06](uc-06-void-document-number-range.md); M9's reporting infrastructure being in place.
- **Blocks:** —

## Notes

Although fiscal books are documented under M2 §3.4, the roadmap (doc §14) places this in **Phase 7 — Visibilidade**, together with M9's dashboard and reports — not in Phase 2's fiscal core. Don't schedule this ticket before Phase 7 even though it lives in the M2 spec.
