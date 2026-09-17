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
