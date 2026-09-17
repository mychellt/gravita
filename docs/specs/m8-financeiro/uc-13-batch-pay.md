# UC-M8-13 — Batch Pay (`BatchPayUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "Pagamento em lote: seleção múltipla de títulos; geração de remessa CNAB para o banco."

## Description

User selects multiple approved payables and generates a single CNAB remittance file to submit to the bank. Payables move to `PAID` only once [`ConfirmBatchPaymentUseCase`](uc-22-confirm-batch-payment.md) processes the bank's return for this remittance — this use case only generates and sends the remittance.

## Port signature

```java
public interface BatchPayUseCase {
    CnabRemittance execute(BatchPayCommand command);
}
```

`BatchPayCommand`: `payableIds: [id]`, `bankIntegration`. Returns a `CnabRemittance` (the generated remittance file/reference).

## Outbound ports required

- `PayableRepositoryPort`
- `BankIntegrationPort`

## REST endpoint

`POST /api/finance/payables/batch-pay`

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] Only `APPROVED` payables can be included in a batch.
- [ ] A single CNAB remittance is generated covering all selected payables.
- [ ] The remittance references every selected payable so [`ConfirmBatchPaymentUseCase`](uc-22-confirm-batch-payment.md) can match the later bank return back to each one.

## Dependencies

- **Depends on:** UC-M8-12.
- **Blocks:** [UC-M8-22](uc-22-confirm-batch-payment.md).
