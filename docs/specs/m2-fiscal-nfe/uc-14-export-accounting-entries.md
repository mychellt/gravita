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
