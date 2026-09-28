# UC-M8-17 — Get Cash Flow (`GetCashFlowUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.3 Fluxo de Caixa e Conciliação — "Visão do fluxo: diário, semanal e mensal; entradas e saídas realizadas + projeção de abertos. Filtros: por empresa, filial, conta bancária e centro de custo. Alerta de saldo: notificação automática quando saldo projetado fica negativo."

## Description

Read-only cash-flow view combining realized `Settlement`s (receivable and payable sides) with open `Receivable`/`Payable` titles projected by due date, filterable by company/branch/bank account/cost center. Triggers a balance alert when the projection goes negative.

## Port signature

```java
public interface GetCashFlowUseCase {
    CashFlowProjection execute(GetCashFlowQuery query);
}
```

`GetCashFlowQuery`: `granularity: {DAILY, WEEKLY, MONTHLY}`, `company`, `branch`, `bankAccount`, `costCenter`. Returns a `CashFlowProjection`.

## Outbound ports required

- `ReceivableRepositoryPort`
- `PayableRepositoryPort`
- `SettlementRepositoryPort`
- `NotifyNegativeBalanceProjectionPort`

## REST endpoint

`GET /api/finance/cash-flow`

## Domain entities touched

- `CashFlowProjection` (read model), `Receivable`, `Payable`, `Settlement` (all read-only)

## Acceptance criteria

- [ ] The projection combines realized entries/exits with open titles by due date.
- [ ] All four filters (company, branch, bank account, cost center) are supported.
- [ ] A negative projected balance triggers `NotifyNegativeBalanceProjectionPort` automatically.

## Dependencies

- **Depends on:** UC-M8-01, UC-M8-02, UC-M8-10, UC-M8-11 (needs both receivables and payables to project anything meaningful).
- **Blocks:** —

## Implementation notes

- `GetCashFlowQuery` also takes an optional period (`from`/`to`, default 30 days back to 90 days ahead, at most 3660 days) and an optional `openingBalance` (default 0): the finance model holds no bank balance yet, so the running balance is the opening balance plus the net movement of the buckets.
- Realized: every `Settlement` in the period counts on its day for the cash that moved (principal + interest + fine + surcharge; a discount is not cash). A settlement applies either to a `Receivable` (inflow) or to a `Payable` (outflow).
- Projected: an open receivable (`OPEN`/`PARTIALLY_SETTLED`) counts for its remaining balance and an open payable (`OPEN`/`APPROVED`) for its amount, on the due date; a title already overdue counts today.
- Filters: `Receivable`, `Payable` carry a `LedgerScope` (company, branch, bank account); the cost center comes from a payable's cost-center split. A cost-center filter keeps only payables and counts each one's percentage share; receivables are not charged to a cost center.
- Alert: `NotifyNegativeBalanceProjectionPort` is called when a period that is not over yet closes with a negative balance, carrying the first such period and the lowest balance. A failing notification is logged and does not fail the read. The alert is not de-duplicated across calls.
- REST: `GET /api/finance/cash-flow?granularity=&companyId=&branchId=&bankAccountId=&costCenterId=&from=&to=&openingBalance=`.
