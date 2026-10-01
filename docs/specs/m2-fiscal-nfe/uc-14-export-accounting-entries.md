# UC-M2-14 — Export Accounting Entries (`ExportAccountingEntriesUseCase`)

**Module:** M2 — Fiscal: NFe ([module spec](../m2-fiscal-nfe.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

§3.4: "Exportação contábil: entries exported in CSV/TXT in the configured accounting system's format."

## Description

Triggered by a user (accountant or admin) requesting an accounting export for a period. Translates the period's `NfeDocument`/`InboundNfe` fiscal entries into the accounting entry format configured for the company (per M1's chart of accounts and cost centers) and exports them as CSV/TXT.

## Port signature

```java
public interface ExportAccountingEntriesUseCase {
    AccountingExportFile execute(ExportAccountingEntriesCommand command);
}
```

`ExportAccountingEntriesCommand` fields: `companyId`, `period`, `format` (`CSV`/`TXT`). Returns an `AccountingExportFile`.

## Outbound ports required

- `NfeRepositoryPort`, `InboundNfeRepositoryPort` — source records.
- `ExportAccountingFilePort` — configured-format (CSV/TXT) export.

## REST endpoint

`POST /api/accounting/export`

## Domain entities touched

- Reads `NfeDocument`, `InboundNfe` — writes no new aggregate, only the generated export file.

## Acceptance criteria

- [ ] Output format matches the accounting system configured for the company (the "configured format" isn't specified further by the source doc — treat the configuration itself as M1/M10 scope, this use case only executes it).
- [ ] Covers every authorized `NfeDocument` and confirmed `InboundNfe` in the period.
- [ ] Supports both CSV and TXT output.

## Dependencies

- **Depends on:** A full period of M2 issuance/receipt data (Phase 2); M1's chart of accounts/cost centers.
- **Blocks:** —

## Notes

Although accounting export is documented under M2 §3.4, the roadmap (doc §14) places it in **Phase 7 — Visibilidade**, together with M9's dashboard and reports — not in Phase 2's fiscal core. Don't schedule this ticket before Phase 7 even though it lives in the M2 spec.

## Implementation notes

- **Endpoint.** `POST /api/accounting/export` with `{"companyId": "<uuid>", "period": "yyyy-MM", "format": "CSV" | "TXT"}`. `200` answers with the file itself as an attachment (`Content-Disposition`, `text/csv` or `text/plain`, UTF-8) named `accounting-entries-<cnpj>-<yyyy-MM>.<ext>`, and the number of entries in `X-Entry-Count`. Errors: `400` missing/malformed `companyId` or `period`, or a `format` other than `CSV`/`TXT`; `404` unknown company. Like the other `tax` endpoints it is not gated by a screen permission.
- **Configured format — gap flagged, not filled (AC1).** M1 and M10 hold no accounting-system configuration today: `IntegrationCredential` (M10-10) names an "accounting export" integration but carries only endpoint/credentials, and M1's chart of accounts (`ChartOfAccountsDomain`) and cost centers (`CostCenterDomain`) hold no mapping from a fiscal document to an account or cost center. Per the ticket, this use case does not invent that configuration: `ExportAccountingFilePort` writes the one default layout described below, and a `format` left out of the request is `CSV`. When M1/M10 define the per-company accounting format and the document-to-account mapping, they plug in behind `ExportAccountingFilePort` (the use case only selects entries) and the entries gain their account and cost-center columns. Until then the file carries fiscal data only: **no debit/credit account codes**.
- **Coverage (AC2).** Every NFe the company issued that SEFAZ authorized in the period (`NfeRepositoryPort.findAuthorizedByCompanyBetween`: cancelled and rejected NFe are not exported) and every received NFe whose receipt was confirmed (`InboundNfeRepositoryPort.findConfirmedByCompanyBetween`: pending-conference ones are not). Both queries already existed; no new repository query, no migration. NFC-e and NFS-e are out of scope: this ticket's sources are `NfeDocument` and `InboundNfe`.
- **Period.** A calendar month, in the server's time zone. An issued NFe falls on the day SEFAZ authorized it, a received NFe on the day its supplier issued it. Entries are ordered by that day, then access key. A period without documents still answers `200`, with a header-only CSV or an empty TXT and `X-Entry-Count: 0`.
- **Entry.** One per document: day, nature (`ENTRADA` for a received NFe or an issued one under CFOP 1/2/3, `SAIDA` otherwise — the rule of the fiscal books, UC-M2-13), series, number, access key, participant name and CNPJ/CPF (the supplier of an entrada, the recipient of a saída), CFOP (a received NFe's distinct CFOPs joined by `/`), total value and ICMS, IPI, PIS, COFINS.
- **Layout (both formats).** UTF-8, CRLF, no BOM, dates `dd/MM/yyyy`, amounts with a comma and two decimals and no thousands separator (`1234,56`). **CSV:** semicolon-separated, a header row, cells quoted when they hold `;`, `"` or a line break; a text cell starting with `=`, `+`, `-` or `@` is prefixed with `'` so a spreadsheet does not run supplier-supplied text as a formula. **TXT:** no header; fixed-width fields separated by one space — date 10, nature 7, series 3, number 9, access key 44, participant 40 (cut to width), CNPJ/CPF 14, CFOP 15, five amounts 15 right-aligned. Only the participant name is ever cut; other fields are padded, never truncated.
