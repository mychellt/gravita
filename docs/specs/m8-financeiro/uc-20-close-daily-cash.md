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

## Implementation notes

- `CloseDailyCashCommand.account` is the `InternalCashBoxId` to close. The request's `account` is optional and defaults to the single back-office box (`InternalCashBoxId.MAIN`).
- Read-only summary: nothing is persisted. `DailyClosing` is derived from the day's `CashMovement`s: entries = sum of `FROM_BANK`, exits = sum of `TO_BANK`, closing = opening + entries − exits. It lists those movements too.
- Opening balance = the box balance when the day started (current balance minus every movement from that day on), which is the prior day's closing balance by construction. Days are cut in the server's time zone.
- A date after today is rejected (`BusinessRuleException`, HTTP 400); an unknown box is a 404. The box row is locked during the call so balance and movements are one consistent snapshot.
- REST: `POST /api/finance/internal-cash/close` with `{"account": "<uuid, optional>", "date": "yyyy-MM-dd"}` → `200` with `account`, `date`, `openingBalance`, `entries`, `exits`, `closingBalance`, `movements`.
