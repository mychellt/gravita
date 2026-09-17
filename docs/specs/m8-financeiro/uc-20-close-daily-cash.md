# UC-M8-20 — Close Daily Cash (`CloseDailyCashUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.3 Fluxo de Caixa e Conciliação — "Fechamento diário: resumo do dia: entradas, saídas, saldo inicial e final por conta."

## Description

End-of-day summary of the internal cash box: entries, exits, opening and closing balance per account. Distinct from M3's PDV Z-report — this closes the back-office cash box, not a sales register.

## Port signature

```java
public interface CloseDailyCashUseCase {
    DailyClosing execute(CloseDailyCashCommand command);
}
```

`CloseDailyCashCommand`: `account`, `date`. Returns a `DailyClosing` (entries, exits, opening/closing balance).

## Outbound ports required

- `InternalCashBoxRepositoryPort`

## REST endpoint

`POST /api/finance/internal-cash/close`

## Domain entities touched

- `InternalCashBox`, `DailyClosing`

## Acceptance criteria

- [ ] The closing summary totals match the sum of the day's `CashMovement`s.
- [ ] Opening balance equals the prior day's closing balance.

## Dependencies

- **Depends on:** UC-M8-19.
- **Blocks:** —
