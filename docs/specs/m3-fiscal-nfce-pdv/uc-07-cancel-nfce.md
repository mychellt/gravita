# UC-M3-07 — Cancel NFCe (`CancelNfceUseCase`)

**Module:** M3 — Fiscal: NFCe + PDV ([module spec](../m3-fiscal-nfce-pdv.md))
**Context package:** `br.gravita.tax`
**Roadmap phase:** Phase 2 — Fiscal Core (doc §14)

## Functional requirement

§4.1: "Cancellation: last sale, or any sale of the day, cancellable with a supervisor password." §4.2: "Cancellation: up to 30 minutes after issuance (minimum deadline); the state deadline applies when longer."

## Description

A supervisor authorizes cancellation of the last sale, or any other sale from the current day, on an `AUTHORIZED` `NfceSale`. Cancellation is only allowed within the legal window (30 minutes minimum, longer if the state defines a longer deadline). On success, the cancellation is transmitted to SEFAZ and the sale's status is updated.

## Port signature

```java
public interface CancelNfceUseCase {
    void execute(CancelNfceCommand command);
}
```

`CancelNfceCommand`: `nfceSaleId`, `supervisorCredential`, `reason` (optional).

## Outbound ports required

- `NfceRepositoryPort`
- `SupervisorAuthorizationPort`
- `TransmissionQueuePort`, `SubmitToSefazPort` (cancellation is transmitted to SEFAZ, shared with M2)

## REST endpoint

`POST /api/pdv/sales/{id}/cancel`

## Domain entities touched

- `NfceSale` (status `AUTHORIZED → CANCELLED`)

## Acceptance criteria

- [ ] A valid supervisor password is required (`SupervisorAuthorizationPort`); the request is rejected otherwise.
- [ ] Only the last sale, or any sale from the current day, is eligible for cancellation.
- [ ] The request is rejected once the cancellation deadline has passed (30 minutes minimum, or the state's longer deadline).
- [ ] On success, the sale's status becomes `CANCELLED` and the cancellation is transmitted to SEFAZ.

## Dependencies

- **Depends on:** [04 — Issue NFCe](uc-04-issue-nfce.md) — only an `AUTHORIZED` sale can be cancelled.
- **Blocks:** —
