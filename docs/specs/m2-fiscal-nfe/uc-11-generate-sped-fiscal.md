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

- **Endpoint.** `POST /api/sped/fiscal`, JSON body: `companyId`; the period as `period` (`yyyy-MM`) *or* `startDate` + `endDate`; optional `finality` (`ORIGINAL` default, `SUBSTITUTE`); and the `taxpayer` and `accountant` blocks below. `200` with `fileName`, `txt` (base64 of the ISO-8859-1 file) and `validation` (the report). Errors: `400` malformed or ambiguous request; `404` unknown company; `422` when mandatory records are missing or invalid — the body is `{message, errors[], warnings[]}` and **no file is produced**. Like the other `tax` endpoints it is not gated by a screen permission.
- **Period.** An EFD covers one calendar month, whole or part of it (company start/close), so a range that spans two months is a `422` on `0000`. A document belongs to the day SEFAZ authorized it (a received NFe: the day its supplier issued it), in the server's time zone.
- **Coverage (AC1).** Every `NfeDocument` of the company that is `AUTHORIZED` or `CANCELLED` and was authorized in the period, and every `InboundNfe` of the company that is `CONFIRMED` and was issued in the period. Rejected and unconfirmed documents are left out. A cancelled NFe stays in the file of the month it was issued in. The company's `VoidedNumberRange` entries (NFE only, placed by `voidedAt`, as M2-13 does) each become one `C100` per voided number with `COD_SIT = 05`, so numbering gaps are explained; a number that is both voided and issued, or a document that appears twice, is a validation error. An own NFe under an entry CFOP (1/2/3) is an entry the company issued (`IND_OPER 0`, `IND_EMIT 0`), as in M2-13.
- **Records generated.** `0000`, `0001`, `0005`, `0100`, `0150`; `C100` (regular — full values, `COD_SIT 02` cancelled — only the identification fields the layout keeps, `COD_SIT 05` voided number); `E100`, `E110` (ICMS assessment: debits of the regular exits against credits of the regular entries, a negative balance reported as credit to carry forward); the `1010` indicators (all `N`); and every other block of the layout (`B`, `D`, `G`, `H`, `K`) opened with `IND_MOV = 1` and closed. Block 9 counters and every `X990` line count are computed by the writer, never by the caller.
- **Validation (AC2).** Everything is populated and checked before any text is written; every problem is collected, so one failed request lists all of them. Errors: `0000` (legal name, 7-digit IBGE municipality, numeric IE, profile, activity, single-month period), `0005` (CEP, address), `0100` (accountant name, CRC, valid CPF), `C100` (44-digit access key, series up to 3 characters, 9-digit number, duplicates).
- **What the request must carry.** The company registry (M1) holds neither the legal name, the IBGE municipality, the activity profile nor the accountant, so `taxpayer` (`legalName`, `municipalityCode`, `profile` A/B/C, `activity` INDUSTRIAL/OTHER, optional `tradeName`, `zipCode`, `number`, `neighborhood`) and `accountant` (`name`, `cpf`, `crc`, optional `email`) are part of the command. Moving them onto `Company` is a follow-up.
- **Known gaps — reported as warnings, not errors.** The file is generated, and the report says what it goes out without: `0150` `COD_MUN`/`END`/`NUM`/`BAIRRO` (not held for customers and suppliers); the itemised `C170`/`0200` and the analytic `C190` (no CST, ICMS rate, unit or NCM per item on an issued NFe — the PVA is expected to require `C190` for regular documents, so the file should not be treated as ready to transmit until that data exists); `C100.VL_BC_ICMS` of received NFe (only the amount is stored); `E110.VL_SLD_CREDOR_ANT` (credit carried from the previous period is not tracked, reported as 0,00). IPI assessment (`E500`+), inventory (`H`) and ST are not covered.
- **Layout version.** `COD_VER` is the constant `SpedFiscalRecords.LAYOUT_VERSION` (`020`); confirm it against the Guia Prático of the year being filed and bump it when the layout changes.
- **Outbound ports.** `NfeRepositoryPort.findAuthorizedOrCancelledByCompanyBetween` and `InboundNfeRepositoryPort.findConfirmedByCompanyBetween` are new company-scoped period queries (no migration); `VoidedNumberRangeRepositoryPort.findByCompanyIdAndVoidedAtBetween` is M2-13's. `CompanyRepositoryPort` gives the CNPJ, IE, IM, UF, address, phone and e-mail.
- **`GenerateSpedFilePort` (shared with UC-M2-12).** It shipped with [UC-M2-12](uc-12-generate-sped-contribuicoes.md) (`core.ports.outbound.tax.GenerateSpedFilePort`, adapter `SpedFileAdapter`) and is used here as is: it writes the shape both EFDs share - `|REG|field|` lines, CR LF, ISO-8859-1, `|` and line breaks stripped from values, the `X001`/`X990` pair of every block and Block 9 - from the `SpedLayout` (the `0000` header plus `SpedBlock`s of `SpedRecord`s) this ticket supplies. `SpedFiscalRecords` lays out the EFD ICMS/IPI registers (`0000`, `0005`, `0100`, `0150`, `C100`, `E100`, `E110`, `1010`) with typed values (`BigDecimal` at scale 2, `LocalDate`) and the port formats them; there is no second writer.
