# UC-M8-06 — Settle Title Manually (`SettleTitleManuallyUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Baixa manual: total ou parcial; campos de juros, multa, desconto e acréscimo."

## Description

User-initiated settlement (full or partial) of an open `Receivable`, e.g. for a cash/offline payment. Captures interest, fine, discount and surcharge adjustments. Creates a `Settlement` (`method = MANUAL`) and updates the receivable's status; triggers `UpdateCustomerCreditStatusPort` so M1's `SetCustomerCreditStatusUseCase` keeps the customer's credit status current.

## Port signature

```java
public interface SettleTitleManuallyUseCase {
    Settlement execute(SettleTitleCommand command);
}
```

`SettleTitleCommand`: `receivableId`, `amount: Money`, `interest`, `fine`, `discount`, `surcharge`, `partial: boolean`. Returns the created `Settlement`.

## Outbound ports required

- `ReceivableRepositoryPort`
- `SettlementRepositoryPort`
- `UpdateCustomerCreditStatusPort`

## REST endpoint

`POST /api/finance/receivables/{id}/settle`

## Domain entities touched

- `Receivable`, `Settlement`

## Acceptance criteria

- [ ] A `Settlement` is created with `method = MANUAL` and the entered interest/fine/discount/surcharge.
- [ ] Partial settlement leaves the receivable `PARTIALLY_SETTLED`; full settlement moves it to `SETTLED`.
- [ ] Settling triggers `UpdateCustomerCreditStatusPort` so `masterdata`'s `Customer.status` stays current.

## Dependencies

- **Depends on:** UC-M8-01 or UC-M8-02.
- **Blocks:** —
