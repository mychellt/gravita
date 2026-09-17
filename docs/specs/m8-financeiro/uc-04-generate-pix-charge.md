# UC-M8-04 — Generate PIX Charge (`GeneratePixChargeUseCase`)

**Module:** M8 — Financeiro ([module spec](../m8-financeiro.md))
**Context package:** `br.gravita.finance`
**Roadmap phase:** Phase 5 — Financeiro (doc §14)

## Functional requirement

§9.1 Contas a Receber — "PIX Cobrança: QR Code dinâmico por título com vencimento e valor; confirmação automática."

## Description

For an existing `Receivable`, generates a dynamic PIX QR code (`PixCharge`) carrying the title's amount and due date. Payment confirmation arrives asynchronously via the bank integration and automatically settles the receivable.

## Port signature

```java
public interface GeneratePixChargeUseCase {
    PixCharge execute(GeneratePixChargeCommand command);
}
```

`GeneratePixChargeCommand`: `receivableId`. Returns the created `PixCharge` (`dynamicQrPayload`, `expiresAt`, `status = PENDING`).

## Outbound ports required

- `ReceivableRepositoryPort`
- `BankIntegrationPort`

## REST endpoint

`POST /api/finance/receivables/{id}/pix-charge`

## Domain entities touched

- `Receivable`, `PixCharge`

## Acceptance criteria

- [ ] A `PixCharge` is created with a valid dynamic QR payload, due date and amount matching the receivable.
- [ ] `status` starts as `PENDING` and moves to `EXPIRED` if unpaid past `expiresAt`.
- [ ] Payment confirmation updates `PixCharge.status` to `PAID` and settles the linked receivable automatically.

## Dependencies

- **Depends on:** UC-M8-01 or UC-M8-02.
- **Blocks:** —
