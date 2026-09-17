# UC-M9-05 — Get Commission Report (`GetCommissionReportUseCase`)

**Module:** M9 — Relatórios & BI ([module spec](../m9-relatorios-bi.md))
**Context package:** `br.gravita.reporting`
**Roadmap phase:** Phase 7 — Visibilidade (doc §14)

## Functional requirement

Doc §10.2 — Comissões: per salesperson, per product, per period — ready for payroll.

## Description

Triggered when a user (typically finance or a sales manager) requests the commission report for a salesperson and/or period, ahead of payroll processing. The use case projects `sales.Commission` records into `CommissionReportEntry` rows. Read-only.

## Port signature

```java
public interface GetCommissionReportUseCase {
    List<CommissionReportEntry> execute(CommissionReportQuery query);
}
```

`CommissionReportQuery` fields: `salesperson` (optional filter), `period`. Returns a list of `CommissionReportEntry`, projected from `sales.Commission` (salesperson, product, order, rate, amount).

## Outbound ports required

- `SalesReadModelPort` — commission records

## REST endpoint

`GET /api/reports/commissions?salesperson=&period=`

## Domain entities touched

- `CommissionReportEntry`

## Acceptance criteria

- [ ] Returns commission entries per salesperson, per product, for the requested period.
- [ ] Supports an optional salesperson filter.
- [ ] Output format is suitable for direct payroll input (per-salesperson totals derivable).

## Dependencies

- **Depends on:** `sales` read-model port.
- **Blocks:** —
