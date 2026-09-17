# UC-M8-18 — Reconcile Bank Statement (`ReconcileBankStatementUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.3 Fluxo de Caixa e Conciliação — "Conciliação bancária: importação de extrato OFX/CSV; casamento automático por valor e data."

## Description

Imports an OFX/CSV bank statement and automatically matches each `BankStatementLine` to an existing `Settlement` or `CashMovement` by value and date, surfacing unmatched lines for manual review. Distinct from UC-M8-05 (CNAB-specific automatic settlement of receivables): this is the general bank-statement reconciliation across both receivable and payable settlements and internal cash movements.

## Port signature

```java
public interface ReconcileBankStatementUseCase {
    ReconciliationResult execute(ReconcileBankStatementCommand command);
}
```

`ReconcileBankStatementCommand`: `bankAccount`, `fileContent` (OFX/CSV payload). Returns a `ReconciliationResult` (matched/unmatched `BankStatementLine`s).

## Outbound ports required

- `ImportBankStatementPort`
- `SettlementRepositoryPort`

## REST endpoint

`POST /api/finance/bank-statements/import`

## Domain entities touched

- `BankStatementLine`, `Settlement`

## Acceptance criteria

- [ ] Statement lines are matched to settlements/cash movements by value and date.
- [ ] Unmatched lines are reported for manual review, not silently discarded.

## Dependencies

- **Depends on:** UC-M8-05, UC-M8-06, UC-M8-14, UC-M8-19 (settlements and cash movements must exist to match against).
- **Blocks:** —
