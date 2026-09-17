# UC-M8-07 — Renegotiate Title (`RenegotiateTitleUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Renegociação: título vencido transformado em novo parcelamento com um fluxo simples."

## Description

Converts one or more overdue `Receivable`s into a new installment plan. Precondition: the source title(s) are overdue. Creates a `Renegotiation` linking the original receivable(s) to a new set of `Receivable`s (`status = OPEN`), and marks the originals `RENEGOTIATED`. Triggers `UpdateCustomerCreditStatusPort` so M1's `SetCustomerCreditStatusUseCase` reflects the renegotiation.

## Port signature

```java
public interface RenegotiateTitleUseCase {
    Renegotiation execute(RenegotiateTitleCommand command);
}
```

`RenegotiateTitleCommand`: `originalReceivableIds: [id]`, `newInstallmentPlan: [{dueDate, amount: Money}]`. Returns the created `Renegotiation`.

## Outbound ports required

- `ReceivableRepositoryPort`
- `UpdateCustomerCreditStatusPort`

## REST endpoint

`POST /api/finance/receivables/{id}/renegotiate`

## Domain entities touched

- `Receivable`, `Renegotiation`

## Acceptance criteria

- [ ] Original receivable(s) move to `RENEGOTIATED` status and are excluded from future aging.
- [ ] New `Receivable`s are created per the agreed installment plan, `status = OPEN`.
- [ ] The `Renegotiation` record preserves the link back to the original title(s), for audit and statement purposes.

## Dependencies

- **Depends on:** UC-M8-01 or UC-M8-02 (an overdue receivable must exist).
- **Blocks:** —
