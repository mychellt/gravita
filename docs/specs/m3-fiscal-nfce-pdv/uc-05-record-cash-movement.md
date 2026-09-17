# UC-M3-05 — Record Cash Movement (`RecordCashMovementUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.1: "Sangria / Suprimento: cash withdrawal or deposit, with justification, recorded immediately."

## Description

The cashier records an out-of-band cash withdrawal (sangria) or deposit (suprimento) against the currently open register session, with a mandatory justification. The movement is recorded immediately, feeding the session's later reconciliation.

## Port signature

```java
public interface RecordCashMovementUseCase {
    CashMovementId execute(RecordCashMovementCommand command);
}
```

`RecordCashMovementCommand`: `sessionId`, `type: {SANGRIA, SUPRIMENTO}`, `amount: Money`, `justification`. Returns the id of the created `CashMovement`.

## Outbound ports required

- `CashMovementRepositoryPort`

## REST endpoint

`POST /api/pdv/cash-movements`

## Domain entities touched

- `CashMovement` (created), `PosSession` (referenced, not mutated)

## Acceptance criteria

- [ ] `type` is either `SANGRIA` or `SUPRIMENTO`.
- [ ] `justification` is mandatory — the request is rejected if blank.
- [ ] The movement is recorded immediately, with a `timestamp`.
- [ ] The movement is linked to the currently open session on the register making the request.

## Dependencies

- **Depends on:** [01 — Open Session](uc-01-open-pos-session.md).
- **Blocks:** [06 — Close Session](uc-06-close-pos-session.md) — the Z report includes cash movements in its reconciliation.
