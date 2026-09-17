# UC-M9-09 — Export Report (`ExportReportUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10 intro: all reports are filterable, exportable to PDF and Excel, and accessible per permission profile.

## Description

Triggered when a user exports any of the other reporting use cases' output (dashboard, ABC curve, DRE, turnover, commissions, supplier summary, fiscal books, assessed taxes) as a file. The use case re-runs (or reuses the cached result of) the target report, then renders it to the requested format. Generic across report types — implemented once, not duplicated per report.

## Port signature

```java
public interface ExportReportUseCase {
    ExportedFile execute(ExportReportQuery query);
}
```

`ExportReportQuery` fields: `reportId` (identifies which report — dashboard, abc-curve, dre, stock-turnover, commissions, purchases-by-supplier, fiscal-books, assessed-taxes), `format` (`PDF` or `XLSX`), plus that report's own query parameters (period, filters). Returns an `ExportedFile` (binary content + filename + content type).

## Outbound ports required

- `RenderPdfPort` — PDF rendering
- `RenderExcelPort` — Excel rendering
- `PermissionCheckPort` — export permission per profile (`EXPORT` action, doc §11.1)
- The read-model port(s) needed by the underlying report being exported (delegates to the corresponding `Get*UseCase`)

## REST endpoint

`GET /api/reports/{reportId}/export?format=pdf|xlsx`

## Domain entities touched

- None of its own — renders whichever read model the target `Get*UseCase` returns.

## Acceptance criteria

- [ ] Supports exporting every report listed in the module spec's use case table, in both PDF and Excel.
- [ ] Exported content matches what the equivalent on-screen report shows for the same query parameters.
- [ ] Export is denied for users without the `EXPORT` permission on that report/screen.

## Dependencies

- **Depends on:** `system`'s `PermissionCheckPort`; all other UC-M9 use cases (01–08) as the data source being exported.
- **Blocks:** —
