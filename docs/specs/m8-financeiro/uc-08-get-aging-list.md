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
