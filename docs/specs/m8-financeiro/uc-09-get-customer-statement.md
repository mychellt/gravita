# UC-M8-09 — Get Customer Statement (`GetCustomerStatementUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "Extrato por cliente: histórico de títulos, baixas, renegociações e saldo em aberto por cliente."

## Description

Read-only per-customer statement combining all `Receivable`s, their `Settlement`s and `Renegotiation`s, plus the current open balance. Used by finance/sales users to answer "what does this customer owe," and feeds M1's `SetCustomerCreditStatusUseCase` delinquency signal.

## Port signature

```java
public interface GetCustomerStatementUseCase {
    CustomerStatement execute(GetCustomerStatementQuery query);
}
```

`GetCustomerStatementQuery`: `customerId`, optional `period`. Returns a `CustomerStatement` (titles, settlements, renegotiations, open balance).

## Outbound ports required

- `ReceivableRepositoryPort`
- `SettlementRepositoryPort`

## REST endpoint

`GET /api/finance/customers/{id}/statement`

## Domain entities touched

- `Receivable`, `Settlement`, `Renegotiation` (all read-only)

## Acceptance criteria

- [ ] The statement lists every title, settlement and renegotiation for the customer, in chronological order.
- [ ] The open balance matches the sum of `OPEN`/`PARTIALLY_SETTLED` titles.
- [ ] The statement feeds the delinquency signal used by `masterdata`'s `Customer.status`.

## Dependencies

- **Depends on:** UC-M8-01, UC-M8-02, UC-M8-06, UC-M8-07.
- **Blocks:** —
