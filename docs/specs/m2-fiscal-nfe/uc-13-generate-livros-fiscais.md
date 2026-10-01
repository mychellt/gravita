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

## Implementation notes

- **Endpoint.** `GET /api/livros-fiscais?companyId=<uuid>&period=yyyy-MM`. `200` with the three books and the tax summary as structured data, plus `pdf` and `txt` (base64) rendered from those same books. Errors: `400` missing/malformed `companyId` or `period`; `404` unknown company. Like the other `tax` endpoints it is not gated by a screen permission.
- **Period.** A calendar month, in the server's time zone. An NFe is booked on the day SEFAZ authorized it (`authorizedAt`), a received NFe on the day its supplier issued it (`issuedAt`).
- **Entry book (AC1).** Every `InboundNfe` of the company issued in the period, plus any NFe the company issued itself under an *entry* CFOP (first digit 1, 2 or 3). **Exit book:** NFe the company issued under an exit CFOP (5, 6, 7). Only `AUTHORIZED` NFe are booked; rejected and cancelled ones are not. NFC-e is out of scope here: it carries no company or tax breakdown, and this ticket's sources are `NfeDocument` and `InboundNfe` (M9-07's on-screen books do list it).
- **Numbering gaps (AC2).** The company's `VoidedNumberRange` entries voided in the period (every series, `NFE` only) ride on the exit book as a "Numeração inutilizada" section: series, first/last number, quantity, date, SEFAZ protocol and justification, with a count of voided numbers. Ranges are placed by their `voidedAt`, per the repository query shape M2-06 shipped. Numbers missing from the exit book that are neither voided nor authorized (e.g. a cancelled NFe) are not reported as gaps.
- **ICMS Assessment book.** Every booked document that stated ICMS, exits first as debits and entries as credits; `balance = debit - credit` (positive is ICMS to pay, negative a credit to carry forward).
- **Tax summary (AC3).** ICMS, IPI, PIS and COFINS, each as `onExits` (owed), `onEntries` (creditable) and `balance = onExits - onEntries`. Issued NFe contribute their `taxTotals`; received NFe the totals stated on the supplier's document.
- **Outbound ports.** `NfeRepositoryPort.findAuthorizedByCompanyBetween`, `InboundNfeRepositoryPort.findIssuedByCompanyBetween` and `VoidedNumberRangeRepositoryPort.findByCompanyIdAndVoidedAtBetween` are new company-scoped period queries (no migration). The company is looked up through `CompanyRepositoryPort` so an unknown one is a `404` and the books can print its CNPJ and state registration.
- **Rendering and M9-07 reuse.** `GenerateFiscalBookPort` (`FiscalBookAdapter`) lays the books out once as a `RenderPdfPort.PdfReport` and prints that report both as PDF and as fixed-width TXT, so the two files carry the same cells and totals. The PDF goes through the shared `RenderPdfPort` (`PdfBoxReportAdapter`) that M9-07 introduced, so there is a single PDF renderer for book content. M9-07 still builds its own layout from `tax`'s read model; converging the two layouts (this one adds the voided ranges and the four-tax summary) is a follow-up, not part of this ticket.
- **Not statutory format.** The TXT is a readable fixed-width print of the books, not the SPED EFD layout.
