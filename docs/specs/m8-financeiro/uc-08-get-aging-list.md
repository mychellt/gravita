# UC-M8-08 — Get Aging List (`GetAgingListUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Aging list: relatório de inadimplência por faixa de atraso (0–30, 31–60, 61–90, +90 dias)."

## Description

Read-only report bucketing open/overdue `Receivable`s by days-overdue range. Feeds the M9 dashboard's simplified aging widget and M1's `SetCustomerCreditStatusUseCase` delinquency signal.

## Port signature

```java
public interface GetAgingListUseCase {
    AgingReport execute(GetAgingListQuery query);
}
```

`GetAgingListQuery`: optional filters (`customer`, `costCenter`, `asOfDate`). Returns an `AgingReport` with totals per bucket (0–30, 31–60, 61–90, +90 days).

## Outbound ports required

- `ReceivableRepositoryPort`

## REST endpoint

`GET /api/finance/receivables/aging`

## Domain entities touched

- `Receivable` (read-only)

## Acceptance criteria

- [ ] Open receivables are bucketed correctly by days overdue as of the query date.
- [ ] Buckets match the doc's ranges exactly: 0–30, 31–60, 61–90, +90 days.
- [ ] Renegotiated/settled titles are excluded from the aging buckets.

## Dependencies

- **Depends on:** UC-M8-01, UC-M8-02, UC-M8-05, UC-M8-06 (needs receivables and settlements to report on).
- **Blocks:** —

## Implementation notes

- `GetAgingListQuery(customerId, costCenterId, asOfDate)`: every component is optional; `asOfDate` defaults to today. Days overdue = `asOfDate` − due date; a title due on `asOfDate` is 0 days overdue (bucket 0–30), a title not due yet is not part of the report.
- Ranges are inclusive: 0–30, 31–60, 61–90, 91+ (`AgingRange`). `AgingReport` always lists the four buckets in that order, each with its title count and total, plus the overall total and count.
- Only `OPEN`/`PARTIALLY_SETTLED` titles count, each for what is still owed after its settlements (`Receivable.remainingBalance`); `SETTLED`, `RENEGOTIATED` and `CANCELLED` titles are excluded. Hence `SettlementRepositoryPort` is also read, beside `ReceivableRepositoryPort` (new query `findOutstandingByCustomerDueUntil`).
- Cost center: receivables are not charged to a cost center, so a report restricted to one is empty (same rule as the cash flow).
- REST: `GET /api/finance/receivables/aging?customerId=&costCenterId=&asOfDate=`.
