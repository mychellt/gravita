# UC-M8-11 — Generate Payable from Receipt (`GeneratePayableFromReceiptUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "Origem: recebimento de compra (automático) ..."

## Description

Triggered internally by M6's `ConfirmPurchaseReceiptUseCase` when a purchase receipt is confirmed. Creates one `Payable` per installment from the purchase NF's payment terms, `origin = PURCHASE_RECEIPT`. No user interaction.

## Port signature

```java
public interface GeneratePayableFromReceiptUseCase {
    List<Payable> execute(GeneratePayableFromReceiptCommand command);
}
```

`GeneratePayableFromReceiptCommand`: `supplier` (ref), `purchaseReceiptRef`, `installments: [{dueDate, amount: Money}]`. Returns the created `Payable` list.

## Outbound ports required

- `PayableRepositoryPort`

## REST endpoint

None — invoked internally via M6's `GeneratePayableFromReceiptPort`, not exposed as a REST endpoint.

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] One `Payable` is created per installment in the purchase NF's terms.
- [ ] `origin` is set to `PURCHASE_RECEIPT`.
- [ ] Creation is idempotent per `purchaseReceiptRef`.

## Dependencies

- **Depends on:** M6 `ConfirmPurchaseReceiptUseCase`.
- **Blocks:** UC-M8-12, UC-M8-13, UC-M8-14, UC-M8-15, UC-M8-16.
