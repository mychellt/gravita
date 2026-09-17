# UC-M8-19 — Record Internal Cash Movement (`RecordInternalCashMovementUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.3 Fluxo de Caixa e Conciliação — "Caixa interno: caixa físico do escritório separado do PDV; transferências entre caixa e banco."

## Description

Records a transfer into or out of the back office's internal cash box (`InternalCashBox`), separate from the PDV's `PosSession` (M3). Used for cash-to-bank and bank-to-cash transfers, not sales.

## Port signature

```java
public interface RecordInternalCashMovementUseCase {
    CashMovement execute(RecordInternalCashMovementCommand command);
}
```

`RecordInternalCashMovementCommand`: `direction: {TO_BANK, FROM_BANK}`, `amount: Money`, `justification`. Returns the created `CashMovement`.

## Outbound ports required

- `InternalCashBoxRepositoryPort`

## REST endpoint

`POST /api/finance/internal-cash/movements`

## Domain entities touched

- `InternalCashBox`, `CashMovement`

## Acceptance criteria

- [ ] Every movement records direction, amount and justification.
- [ ] The internal cash box balance is independent of any PDV register balance (M3).

## Dependencies

- **Depends on:** None.
- **Blocks:** UC-M8-20.
