# UC-M9-07 — Get Fiscal Books (`GetFiscalBooksUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — Livros fiscais: Entries, Exits and ICMS Assessment — generated per period, in PDF and TXT.

## Description

Triggered when a user (typically the accountant or an internal finance role) requests the fiscal books for a period. The use case pulls entry/exit NFe and ICMS assessment data from `tax` and renders it as the three statutory books, in both PDF and TXT formats. Read-only over `tax`, but produces file output via the export ports.

## Port signature

```java
public interface GetFiscalBooksUseCase {
    FiscalBooks execute(FiscalBooksQuery query);
}
```

`FiscalBooksQuery` fields: `period`. Returns `FiscalBooks`, composing `FiscalBookEntry` collections for Entries, Exits and ICMS Assessment, plus rendered PDF and TXT outputs.

## Outbound ports required

- `TaxReadModelPort` — entry/exit NFe and ICMS assessment data
- `RenderPdfPort` — PDF rendering
- (TXT rendering is a plain serialization, no dedicated port required beyond the use case itself)

## REST endpoint

`GET /api/reports/fiscal-books?period=`

## Domain entities touched

- `FiscalBookEntry`

## Acceptance criteria

- [ ] Generates the Entries, Exits and ICMS Assessment books for the requested period.
- [ ] Output is available in both PDF and TXT.
- [ ] Book contents reconcile against `tax`'s authorized NFe/NFCe records for the period.

## Dependencies

- **Depends on:** `tax` read-model port.
- **Blocks:** —
