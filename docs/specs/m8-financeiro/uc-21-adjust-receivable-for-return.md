# UC-M8-21 — Adjust Receivable for Return (`AdjustReceivableForReturnUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

Not a standalone doc bullet — closes the financial half of §8.1's "Devolução de venda: a return NF-e is issued in the same flow; stock and financial positions are reverted," which M7's [`ReturnSalesOrderUseCase`](../m7-vendas-crm/uc-07-return-sales-order.md) triggers. Added after the initial use-case split surfaced that no M8 use case actually performed this reversal.

## Description

Triggered by M7's `ReturnSalesOrderUseCase` when a sales return is registered. Reduces the linked `Receivable`'s open amount by the returned amount (partial return) or cancels it outright (full return covering the remaining balance). If part of the receivable was already settled, only the unsettled portion is adjusted — settled amounts are not clawed back by this use case.

## Port signature

```java
public interface AdjustReceivableForReturnUseCase {
    Receivable execute(AdjustReceivableForReturnCommand command);
}
```

`AdjustReceivableForReturnCommand`: `receivableId`, `returnedAmount: Money`, `salesReturnRef`. Returns the updated `Receivable`.

## Outbound ports required

- `ReceivableRepositoryPort`

## REST endpoint

None — invoked internally by M7's `ReturnSalesOrderUseCase`, not user-facing (same pattern as `GenerateReceivableFromInvoicingUseCase`, uc-01).

## Domain entities touched

- `Receivable` (mutated: `amount` reduced or `status → CANCELLED`)

## Acceptance criteria

- [ ] A full return (returned amount equals the receivable's remaining open balance) sets `status = CANCELLED`.
- [ ] A partial return reduces the receivable's open amount by the returned amount, leaving it `OPEN`/`PARTIALLY_SETTLED` as appropriate.
- [ ] Already-settled amounts on the receivable are never reduced by this use case — only the open (unsettled) portion is adjustable.
- [ ] Rejects a `returnedAmount` greater than the receivable's current open balance.

## Dependencies

- **Depends on:** M7's [`ReturnSalesOrderUseCase`](../m7-vendas-crm/uc-07-return-sales-order.md) (the trigger).
- **Blocks:** —
