# UC-M8-14 — Pay via PIX (`PayViaPixUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.2 Contas a Pagar — "PIX pagamento: transferência direta via integração bancária; comprovante salvo automaticamente."

## Description

Direct PIX payment of a single approved payable via the bank integration. On success, the payable moves to `PAID` and the payment receipt is stored automatically as an attachment.

## Port signature

```java
public interface PayViaPixUseCase {
    Payable execute(PayViaPixCommand command);
}
```

`PayViaPixCommand`: `payableId`, `pixKey` (recipient). Returns the updated `Payable` (`status = PAID`, with a receipt attachment).

## Outbound ports required

- `PayableRepositoryPort`
- `BankIntegrationPort`
- `DocumentAttachmentStoragePort`

## REST endpoint

`POST /api/finance/payables/{id}/pix-pay`

## Domain entities touched

- `Payable`

## Acceptance criteria

- [ ] Only `APPROVED` payables can be paid via PIX.
- [ ] On success, `status` moves to `PAID` and a receipt is auto-saved to `attachments`.
- [ ] A transfer failure leaves the payable `APPROVED`, not `PAID`.

## Dependencies

- **Depends on:** UC-M8-12.
- **Blocks:** —
